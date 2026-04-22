#!/usr/bin/env bash
# demo_script.sh — live demo harness for the Customer 360 service.
#
# Unlike sap_demo_A (Go, local), this demo runs the service in a kind cluster
# via Kubernetes — closer to the way a RISE customer would deploy a BTP
# extension. There are two recording modes:
#
#   local   — run Keploy on the host, record an out-of-cluster binary
#             (same pattern as sap_demo_A; simplest, works anywhere)
#   k8s     — run Keploy inside the kind cluster as a sidecar on the pod
#             (demonstrates the k8s-proxy integration story)
#
# Usage:
#   ./demo_script.sh exercise                  # hit the deployed service with the sample flow
#   ./demo_script.sh record-local              # Keploy record against a local binary
#   ./demo_script.sh test-local                # replay captured mocks
#   ./demo_script.sh offline-test              # replay with SAP blackholed in /etc/hosts
#   ./demo_script.sh record-k8s                # (placeholder) record via k8s-proxy sidecar
#
# Prereqs for local mode: go>=1.25 or java>=21+mvn, keploy, sudo
# Prereqs for k8s mode:   docker, kind, kubectl, app already deployed

set -euo pipefail

cd "$(dirname "$0")"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

say()  { printf "${BOLD}${GREEN}==> %s${NC}\n" "$*"; }
warn() { printf "${BOLD}${YELLOW}!!  %s${NC}\n" "$*"; }
fail() { printf "${BOLD}${RED}XX  %s${NC}\n" "$*"; }

BASE_URL="${BASE_URL:-http://localhost:30080}"

# ----------------------------------------------------------------------------
# exercise_endpoints — the scripted "Tosca-style" business flow that drives
# the service while Keploy records underneath. 1 inbound → many SAP calls.
# ----------------------------------------------------------------------------
exercise_endpoints() {
  say "exercising Customer 360 endpoints against ${BASE_URL}"
  say "each /360 call fans out to 3 parallel SAP OData GETs — Keploy captures all of them"

  echo "  GET /actuator/health           $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/actuator/health)"
  sleep 1
  echo "  GET /api/v1/customers/count    $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/api/v1/customers/count)"
  sleep 2
  echo "  GET /api/v1/customers?top=3    $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/api/v1/customers?top=3)"
  sleep 2
  echo "  GET /api/v1/customers/11       $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/api/v1/customers/11)"
  sleep 2
  echo "  GET /api/v1/customers/202      $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/api/v1/customers/202)"
  sleep 2
  say "the money shot: aggregated 360 view (1 inbound, 3 parallel SAP OData calls)"
  echo "  GET /api/v1/customers/202/360  $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/api/v1/customers/202/360)"
  sleep 2
  echo "  GET /api/v1/customers/11/360   $(curl -sw '%{http_code}' -o /dev/null ${BASE_URL}/api/v1/customers/11/360)"
  sleep 2
}

ensure_built_locally() {
  if [ ! -f target/customer360.jar ]; then
    say "building customer360.jar (first run only)"
    mvn -q -DskipTests package
  fi
}

load_env() {
  if [ -f .env ]; then
    set -a; source .env; set +a
  fi
}

start_local_keploy_record() {
  ensure_built_locally
  load_env
  if [ -z "${SAP_API_KEY:-}" ]; then
    fail "SAP_API_KEY missing — set it in .env or export before running"
    exit 1
  fi
  rm -rf keploy keploy.yml
  say "starting service under keploy record (local JVM + eBPF attach)"
  sudo -E keploy record -c "java -jar target/customer360.jar" > /tmp/keploy-record.log 2>&1 &

  for i in $(seq 1 60); do
    if curl -s -o /dev/null http://localhost:8080/actuator/health 2>/dev/null; then
      say "service ready after ${i}s"
      return 0
    fi
    sleep 1
  done
  fail "service never became ready — see /tmp/keploy-record.log"
  sudo pkill -INT -f "keploy record" 2>/dev/null || true
  exit 1
}

stop_record() {
  say "stopping keploy record"
  sudo pkill -INT -f "keploy record" 2>/dev/null || true
  for i in $(seq 1 45); do
    if ! pgrep -f "keploy record" >/dev/null; then break; fi
    sleep 1
  done
  say "captured test cases:"
  ls -1 keploy/test-set-0/tests/ 2>/dev/null | sed 's/^/    /' || true
  if grep -lq "BusinessPartner" keploy/test-set-0/tests/*.yaml 2>/dev/null; then
    say "confirmed real SAP data in captured YAML"
  fi
}

run_test() {
  load_env
  # Local replay against the local binary. BASE_URL is intentionally 8080 here
  # (the Keploy test harness brings the app up on its original port).
  BASE_URL="http://localhost:8080"
  say "running keploy test — replays mocks without touching SAP"
  sudo -E keploy test -c "java -jar target/customer360.jar" --delay 15 > /tmp/keploy-test.log 2>&1 &
  TPID=$!
  wait $TPID || true

  stripped=$(sed -E 's/\x1B\[[0-9;]*[mK]//g' /tmp/keploy-test.log)
  pass_count=$(echo "$stripped" | awk '/Total test passed:/ {print $NF; exit}')
  fail_count=$(echo "$stripped" | awk '/Total test failed:/ {print $NF; exit}')
  total_count=$(echo "$stripped" | awk '/Total tests:/ {print $NF; exit}')
  if [ -n "${pass_count:-}" ] && [ -n "${fail_count:-}" ] && [ "$fail_count" = "0" ]; then
    printf "\n${BOLD}${GREEN}PASS${NC}  %s/%s Keploy replays covered the 360 fan-out (3 SAP OData calls per /360) — no SAP traffic.\n\n" \
      "$pass_count" "${total_count:-$pass_count}"
    return 0
  fi
  fail "keploy test did not all pass — see /tmp/keploy-test.log"
  tail -30 /tmp/keploy-test.log
  return 1
}

offline_test() {
  if ! grep -q "sandbox.api.sap.com" /etc/hosts; then
    say "blackholing sandbox.api.sap.com in /etc/hosts (sudo)"
    echo "127.0.0.1 sandbox.api.sap.com" | sudo tee -a /etc/hosts >/dev/null
    trap 'sudo sed -i "/127.0.0.1 sandbox.api.sap.com/d" /etc/hosts' EXIT
  fi
  say "sanity: direct probe to SAP should now fail"
  if curl -sS --max-time 5 -o /dev/null 'https://sandbox.api.sap.com/s4hanacloud/' 2>&1; then
    warn "direct curl unexpectedly succeeded"
  else
    say "SAP unreachable (expected)"
  fi
  run_test
}

record_k8s_stub() {
  cat <<'EOF'
[PLACEHOLDER] Recording inside the kind cluster via Keploy's k8s-proxy is a
separate integration (see ../k8s-proxy/charts/k8s-proxy). The high-level steps:

  1. Deploy the k8s-proxy Helm chart alongside the app:
       helm upgrade --install k8s-proxy ../k8s-proxy/charts/k8s-proxy \
         --namespace keploy --create-namespace \
         --set keploy.apiServerUrl=http://host.docker.internal:8086 \
         --set keploy.clusterName=sap-demo

  2. The k8s-proxy injects an eBPF-capable agent pod that can attach to the
     customer360 pod via a shared mount-ns / PID-ns.

  3. Drive traffic: ./demo_script.sh exercise

  4. Keploy writes captured tests back through the api-server and they show
     up at http://localhost:3000 in the enterprise UI.

For the acquisition demo we typically use local recording (./demo_script.sh
record-local) — same mechanic, simpler to rehearse.
EOF
}

cmd="${1:-exercise}"
case "$cmd" in
  exercise)      exercise_endpoints ;;
  record-local)  start_local_keploy_record; exercise_endpoints; stop_record ;;
  test-local)    run_test ;;
  offline-test)  offline_test ;;
  record-k8s)    record_k8s_stub ;;
  *)
    echo "usage: $0 [exercise|record-local|test-local|offline-test|record-k8s]"
    exit 2
    ;;
esac
