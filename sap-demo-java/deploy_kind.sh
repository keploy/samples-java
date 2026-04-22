#!/usr/bin/env bash
# deploy_kind.sh — one-shot helper to stand up the Customer 360 service
# inside a local kind cluster ready for Keploy recording.
#
# Usage:
#   ./deploy_kind.sh [--cluster NAME | -c NAME] [SUBCOMMAND]
#   KIND_CLUSTER=NAME ./deploy_kind.sh [SUBCOMMAND]
#
# The cluster name defaults to "sap-demo". Override via flag or env var to
# target an existing cluster (e.g. one that hosts the Keploy k8s-proxy).
#
# Subcommands:
#   (none) / all  — full pipeline: cluster + build + load + apply + wait
#   cluster       — only create the kind cluster (skipped if it exists)
#   build         — only (re)build the jar + docker image and kind-load it
#   apply         — only apply k8s manifests (assumes cluster + image ready)
#   status        — show pod, service and recent events
#   logs          — tail the app logs
#   destroy       — delete the kind cluster (and only that cluster)
#
# Examples:
#   ./deploy_kind.sh                                # default: fresh sap-demo cluster
#   KIND_CLUSTER=my-cluster ./deploy_kind.sh apply  # apply into an existing cluster
#   ./deploy_kind.sh -c keploy-bug2 apply           # same via flag
#
# Prereqs: docker, kind, kubectl

set -euo pipefail

cd "$(dirname "$0")"

# --- defaults ---------------------------------------------------------------
CLUSTER_NAME="${KIND_CLUSTER:-sap-demo}"
IMAGE_TAG="customer360:local"
NS="sap-demo"

# --- flag parsing -----------------------------------------------------------
# Accepts --cluster NAME / -c NAME anywhere in the arg list.
POSITIONAL=()
while [ $# -gt 0 ]; do
  case "$1" in
    -c|--cluster)
      if [ -z "${2:-}" ]; then
        echo "error: $1 requires a cluster name"; exit 2
      fi
      CLUSTER_NAME="$2"
      shift 2
      ;;
    --cluster=*)
      CLUSTER_NAME="${1#--cluster=}"
      shift
      ;;
    -h|--help)
      sed -n '1,28p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    --)
      shift; POSITIONAL+=("$@"); break ;;
    *)
      POSITIONAL+=("$1"); shift ;;
  esac
done
set -- "${POSITIONAL[@]:-}"

# --- colours ----------------------------------------------------------------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

say()  { printf "${BOLD}${GREEN}==> %s${NC}\n" "$*"; }
warn() { printf "${BOLD}${YELLOW}!!  %s${NC}\n" "$*"; }
fail() { printf "${BOLD}${RED}XX  %s${NC}\n" "$*"; }

check_prereqs() {
  for bin in docker kind kubectl; do
    command -v "$bin" >/dev/null || { fail "$bin not found in PATH"; exit 1; }
  done
  say "target kind cluster: '${CLUSTER_NAME}' (kubectl context: kind-${CLUSTER_NAME})"
}

cluster_exists() {
  kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"
}

# Probe the control-plane container for which host→node port mappings exist.
# Purely advisory; we don't fail if something is missing.
check_port_mappings() {
  local container="${CLUSTER_NAME}-control-plane"
  if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "${container}"; then
    return 0
  fi
  local ports
  ports="$(docker port "${container}" 2>/dev/null || true)"

  HAS_HTTP=0
  HAS_NODEPORT=0
  echo "${ports}" | grep -q '80/tcp' && HAS_HTTP=1
  echo "${ports}" | grep -q '30080/tcp' && HAS_NODEPORT=1

  if [ "${HAS_HTTP}" = 1 ]; then
    say "host:80 → ${container}:80 mapping present (Ingress path available)"
  else
    warn "host:80 is NOT mapped on ${container} — Ingress won't be reachable on localhost"
  fi
  if [ "${HAS_NODEPORT}" = 1 ]; then
    say "host:30080 → ${container}:30080 mapping present (NodePort path available)"
  else
    warn "host:30080 is NOT mapped on ${container} — NodePort won't be reachable on localhost"
  fi
  if [ "${HAS_HTTP}" = 0 ] && [ "${HAS_NODEPORT}" = 0 ]; then
    warn "Neither access path is mapped. Fallback:"
    warn "  kubectl -n ${NS} port-forward svc/customer360 8080:8080"
  fi
}

# Install the kind-flavoured ingress-nginx controller if the target cluster
# has no IngressClass yet. Idempotent: skips if already present.
ensure_ingress_controller() {
  kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
  if kubectl get ingressclass nginx >/dev/null 2>&1; then
    say "ingress-nginx already present (IngressClass 'nginx' found)"
    return 0
  fi
  if kubectl get ingressclass -o name 2>/dev/null | head -n1 | grep -q .; then
    warn "an IngressClass other than 'nginx' exists; skipping ingress-nginx install."
    warn "edit k8s/ingress.yaml → ingressClassName to match if needed."
    return 0
  fi
  say "installing ingress-nginx (kind variant) — one-time per cluster"
  local manifest="https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/kind/deploy.yaml"
  if ! kubectl apply -f "${manifest}"; then
    warn "could not fetch ingress-nginx manifest from ${manifest}"
    warn "install it manually, or stick to the NodePort URL at http://localhost:30080"
    return 1
  fi
  say "waiting for ingress-nginx controller pod to become ready"
  kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=180s || warn "ingress-nginx didn't reach ready — Ingress URL may 503 until it does"

  # The controller pod being "ready" is not enough: the admission webhook
  # service is a separate endpoint seeded by two short-lived Jobs
  # (ingress-nginx-admission-create / -patch). If we apply the Ingress
  # before those complete, the apiserver can't reach the validating
  # webhook and the apply fails with a dial-tcp connection-refused error.
  say "waiting for ingress-nginx admission jobs to complete"
  kubectl wait --namespace ingress-nginx \
    --for=condition=complete job \
    --selector=app.kubernetes.io/component=admission-webhook \
    --timeout=120s || warn "admission jobs didn't complete — apply may need a retry"
}

ensure_secret() {
  if [ ! -f k8s/secret.yaml ]; then
    warn "k8s/secret.yaml missing — creating from example"
    if [ ! -f k8s/secret.yaml.example ]; then
      fail "k8s/secret.yaml.example missing"
      exit 1
    fi
    if [ -f .env ] && grep -q '^SAP_API_KEY=' .env; then
      KEY=$(grep '^SAP_API_KEY=' .env | cut -d= -f2- | tr -d '"'"'")
      sed "s|<PASTE_YOUR_SAP_API_KEY_HERE>|${KEY}|" k8s/secret.yaml.example > k8s/secret.yaml
      say "secret.yaml generated from .env"
    else
      fail "No SAP_API_KEY in .env and no k8s/secret.yaml — copy and edit k8s/secret.yaml.example"
      exit 1
    fi
  fi
}

create_cluster() {
  if cluster_exists; then
    say "kind cluster '${CLUSTER_NAME}' already exists — reusing"
  else
    if [ "${CLUSTER_NAME}" != "sap-demo" ]; then
      warn "cluster '${CLUSTER_NAME}' does not exist; 'cluster'/'all' will create it"
      warn "using kind-config.yaml (NodePort 30080 → host 30080 mapping)."
      warn "if that's not what you want, create the cluster externally first,"
      warn "then run './deploy_kind.sh -c ${CLUSTER_NAME} apply' to skip creation."
    fi
    say "creating kind cluster '${CLUSTER_NAME}'"
    # kind-config.yaml hardcodes name: sap-demo; override via --name
    kind create cluster --config kind-config.yaml --name "${CLUSTER_NAME}"
  fi
  kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
  say "kubectl context: $(kubectl config current-context)"
}

build_and_load() {
  if cluster_exists; then :; else
    fail "cluster '${CLUSTER_NAME}' does not exist — run '$0 -c ${CLUSTER_NAME} cluster' first"
    exit 1
  fi
  if [ ! -f target/customer360.jar ] || [ src -nt target/customer360.jar ]; then
    say "building customer360.jar (mvn package)"
    mvn -q -B -DskipTests package
  else
    say "using existing target/customer360.jar"
  fi
  say "building docker image ${IMAGE_TAG}"
  DOCKER_BUILDKIT=0 docker build --pull=false -t "${IMAGE_TAG}" .
  say "loading image into kind cluster '${CLUSTER_NAME}'"
  kind load docker-image "${IMAGE_TAG}" --name "${CLUSTER_NAME}"
}

# Check if ${IMAGE_TAG} is already loaded into the target cluster's node.
# `kind load` is per-cluster — an image loaded into cluster A is invisible
# to cluster B. This guards against ImagePullBackOff on cross-cluster applies.
image_in_cluster() {
  local node_container="${CLUSTER_NAME}-control-plane"
  if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "${node_container}"; then
    return 1
  fi
  docker exec "${node_container}" crictl images 2>/dev/null \
    | awk '{print $1":"$2}' | grep -qx "docker.io/library/${IMAGE_TAG}"
}

# Ensure the image is present in the cluster; load it if not.
# Called from apply_manifests so that `./deploy_kind.sh apply` against a
# fresh cluster or a different cluster still works without a prior `build`.
ensure_image_in_cluster() {
  if image_in_cluster; then
    say "image ${IMAGE_TAG} already present in cluster '${CLUSTER_NAME}'"
    return 0
  fi
  warn "image ${IMAGE_TAG} not present in cluster '${CLUSTER_NAME}' — loading it now"
  # Make sure the image exists on the host first.
  if ! docker image inspect "${IMAGE_TAG}" >/dev/null 2>&1; then
    say "host image ${IMAGE_TAG} missing too — running full build"
    build_and_load
    return $?
  fi
  say "loading host-cached ${IMAGE_TAG} into cluster '${CLUSTER_NAME}'"
  kind load docker-image "${IMAGE_TAG}" --name "${CLUSTER_NAME}"
}

apply_manifests() {
  if cluster_exists; then :; else
    fail "cluster '${CLUSTER_NAME}' does not exist — nothing to apply to"
    exit 1
  fi
  kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
  ensure_secret
  ensure_image_in_cluster
  ensure_ingress_controller
  say "applying k8s manifests into context kind-${CLUSTER_NAME}"
  kubectl apply -f k8s/namespace.yaml
  kubectl apply -f k8s/postgres.yaml
  say "waiting for Postgres rollout"
  kubectl -n "${NS}" rollout status deployment/postgres --timeout=120s
  kubectl apply -f k8s/configmap.yaml
  kubectl apply -f k8s/secret.yaml
  kubectl apply -f k8s/deployment.yaml
  kubectl apply -f k8s/service.yaml
  # Retry the Ingress apply a couple of times — the admission webhook can
  # briefly 503 right after the controller comes up.
  for attempt in 1 2 3; do
    if kubectl apply -f k8s/ingress.yaml; then
      break
    fi
    warn "ingress apply failed (attempt ${attempt}/3), retrying in 5s…"
    sleep 5
  done

  say "waiting for rollout"
  if ! kubectl -n "${NS}" rollout status deployment/customer360 --timeout=180s; then
    fail "rollout did not complete — diagnosing"
    kubectl -n "${NS}" get pods -o wide
    # Surface ImagePullBackOff specifically, since it's the most common
    # cross-cluster apply failure mode.
    local ipbp
    ipbp=$(kubectl -n "${NS}" get pod -l app.kubernetes.io/name=customer360 \
      -o jsonpath='{.items[*].status.containerStatuses[*].state.waiting.reason}' 2>/dev/null)
    if echo "${ipbp}" | grep -qE "ImagePullBackOff|ErrImagePull|ErrImageNeverPull"; then
      warn "pod can't find the image — '${IMAGE_TAG}' is not on the cluster's node."
      warn "fix:  ./deploy_kind.sh -c ${CLUSTER_NAME} build"
      warn "      (that rebuilds + kind-loads into THIS cluster specifically)"
    fi
    exit 1
  fi

  check_port_mappings

  say "ready. preferred URL (Ingress, port 80):"
  if [ "${HAS_HTTP:-0}" = 1 ]; then
    echo "  curl -s http://customer360.localtest.me/actuator/health | jq ."
    echo "  curl -s http://customer360.localtest.me/api/v1/customers/count | jq ."
    echo "  curl -s http://customer360.localtest.me/api/v1/customers/202/360 | jq ."
    echo "  open  http://customer360.localtest.me/swagger-ui.html"
  fi
  if [ "${HAS_NODEPORT:-0}" = 1 ]; then
    say "also available (NodePort, port 30080):"
    echo "  curl -s http://localhost:30080/actuator/health | jq ."
  fi
  if [ "${HAS_HTTP:-0}" = 0 ] && [ "${HAS_NODEPORT:-0}" = 0 ]; then
    say "fallback via port-forward:"
    echo "  kubectl -n ${NS} port-forward svc/customer360 8080:8080"
    echo "  curl -s http://localhost:8080/actuator/health | jq ."
  fi
}

show_status() {
  kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
  say "nodes"
  kubectl get nodes -o wide
  say "pods"
  kubectl -n "${NS}" get pods -o wide
  say "service"
  kubectl -n "${NS}" get svc
  say "events (last 10)"
  kubectl -n "${NS}" get events --sort-by='.lastTimestamp' | tail -10
}

tail_logs() {
  kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
  POD=$(kubectl -n "${NS}" get pod -l app.kubernetes.io/name=customer360 -o name | head -n1)
  if [ -z "${POD}" ]; then
    fail "no customer360 pod found in namespace ${NS} on kind-${CLUSTER_NAME}"
    exit 1
  fi
  kubectl -n "${NS}" logs -f "${POD}"
}

destroy() {
  say "deleting kind cluster '${CLUSTER_NAME}'"
  kind delete cluster --name "${CLUSTER_NAME}" || true
}

cmd="${1:-all}"
case "$cmd" in
  all|"")
    check_prereqs
    create_cluster
    build_and_load
    apply_manifests
    ;;
  cluster)   check_prereqs; create_cluster ;;
  build)     check_prereqs; build_and_load ;;
  apply)     check_prereqs; apply_manifests ;;
  status)    check_prereqs; show_status ;;
  logs)      check_prereqs; tail_logs ;;
  destroy)   check_prereqs; destroy ;;
  *)
    echo "usage: $0 [--cluster NAME | -c NAME] [cluster|build|apply|status|logs|destroy|all]"
    exit 2
    ;;
esac
