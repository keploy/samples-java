# Customer 360 — SAP Integration Service (Java / Spring Boot / K8s)

Enterprise-grade reference implementation of the most common SAP integration
pattern in RISE with SAP landscapes: a **Customer 360 aggregator** that
fans out to multiple S/4HANA Business Partner OData endpoints and composes
them into a unified view for downstream CRM / portal / analytics consumers.

Built for the Tricentis evaluation demo. Deliberately looks like code a
Tricentis customer would already recognise: Spring Boot 3, Java 21,
Resilience4j, Actuator, OpenAPI, correlation-id propagation, RFC 7807
problem responses, multi-stage Docker build, production-grade Kubernetes
manifests, kind-ready.

---

## The one-line pitch

> Tosca drives Fiori; Keploy records that **one inbound click fans out to
> three parallel SAP OData calls** that Tosca cannot see.

A typical `GET /api/v1/customers/202/360` produces:

```
   inbound (1)                outbound SAP OData (3, parallel)
   ─────────────              ─────────────────────────────────────────
   /360                  ────► /A_BusinessPartner('202')
                               /A_BusinessPartnerAddress?$filter=…'202'
                               /A_BusinessPartnerRole?$filter=…'202'
```

One Keploy eBPF probe captures every call on that flow. Replay them with
SAP unreachable and the whole 360 aggregation still works, deterministically.

---

## Architecture

```
                               ┌──────────────────────────────────┐
   downstream consumer         │  customer360 (this service)      │
   (CRM, portal, analytics) ───┤                                  │
                               │  ┌───── CustomerController ───┐  │
                               │  │  /customers, /{id},        │  │
                               │  │  /{id}/360, /count         │  │
                               │  └────────┬───────────────────┘  │
                               │           │                       │
                               │  ┌────────▼──────────┐            │
                               │  │ Customer360Aggr.  │ ◄── parallel fan-out
                               │  │ + CustomerService │            │
                               │  └────────┬──────────┘            │
                               │           │                       │
                               │  ┌────────▼──────────────────┐    │
                               │  │ SapBusinessPartnerClient  │    │
                               │  │ + Resilience4j retry/cb   │    │
                               │  │ + RestTemplate            │    │
                               │  └────────┬──────────────────┘    │
                               └───────────┼────────────────────────┘
                                           │  HTTPS + APIKey/Bearer
                                           ▼
                        https://sandbox.api.sap.com/s4hanacloud
                                          (or real BTP tenant)
```

## REST surface

| Method | Path                           | What it does                                |
|--------|--------------------------------|---------------------------------------------|
| GET    | /api/v1/customers              | Paged list of customer summaries            |
| GET    | /api/v1/customers/count        | Total partner count (KPI tile)              |
| GET    | /api/v1/customers/{id}         | Single business partner master data         |
| GET    | **/api/v1/customers/{id}/360** | **Aggregated 360 view — fans out to 3 SAP calls in parallel** |
| GET    | /actuator/health               | K8s liveness / readiness                    |
| GET    | /actuator/prometheus           | Metrics                                     |
| GET    | /swagger-ui.html               | OpenAPI UI                                  |
| GET    | /v3/api-docs                   | OpenAPI 3 spec                              |

Swagger UI inside the kind cluster: `http://localhost:30080/swagger-ui.html`.

---

## Quick start (kind cluster)

```bash
cd sap-demo-java

# 1. One-time: drop your SAP API key into .env
cp .env.example .env
$EDITOR .env              # paste SAP_API_KEY

# 2. Stand everything up: kind cluster + build + load + apply
./deploy_kind.sh

# 3. Exercise the deployed service
./demo_script.sh exercise

# 4. Look inside — preferred URL via Ingress on port 80
curl -s http://customer360.localtest.me/actuator/health | jq .
curl -s http://customer360.localtest.me/api/v1/customers/count | jq .
curl -s http://customer360.localtest.me/api/v1/customers/202/360 | jq '.partner.BusinessPartnerFullName, (.addresses | length), (.roles | length)'

# …or fall back to NodePort 30080 (also works on the default kind-config)
curl -s http://localhost:30080/actuator/health | jq .

# 5. Stream logs (structured JSON with correlationId)
./deploy_kind.sh logs

# 6. Tear down
./deploy_kind.sh destroy
```

The `./deploy_kind.sh` script is idempotent. Re-run after code changes to
rebuild and roll the deployment.

### Targeting a non-default cluster

The script defaults to a cluster named `sap-demo`, but you can point it at
any kind cluster via `--cluster NAME` / `-c NAME` / the `KIND_CLUSTER`
env var:

```bash
# Flag form
./deploy_kind.sh --cluster my-existing-cluster apply
./deploy_kind.sh -c keploy-bug2 apply

# Env-var form
KIND_CLUSTER=my-existing-cluster ./deploy_kind.sh apply

# Flag wins over env var if both are set.
```

Useful when deploying into a cluster that already hosts Keploy's
`k8s-proxy` (see `../k8s-proxy/README.md`). The script will skip cluster
creation if the named cluster already exists. If the existing cluster
wasn't created from `kind-config.yaml`, it probably doesn't have port
`30080` mapped to the host — the script will warn and suggest
`kubectl port-forward` instead.

---

## Recording with Keploy — two modes

### Mode A — local binary + eBPF on the host (simplest)

Same mechanic as `sap_demo_A`. Run the JVM directly on Linux; Keploy attaches
eBPF probes to the Go/Java/Node process on the host.

```bash
./demo_script.sh record-local      # builds, starts under keploy record, exercises
./demo_script.sh test-local        # replays
./demo_script.sh offline-test      # replays with SAP blackholed in /etc/hosts
```

Captured artefact: `keploy/test-set-0/tests/*.yaml` + `keploy/test-set-0/mocks.yaml`.

### Mode B — Keploy inside the kind cluster (production-shaped)

The `keploy.io/record: "enabled"` annotation on the Deployment marks this pod
as a candidate for live recording via the `k8s-proxy` Helm chart (see
`../k8s-proxy/README.md`). The `Namespace` is labelled `keploy.io/enabled: "true"`
for the same reason.

Summary — full steps are in `./demo_script.sh record-k8s`:

```bash
# 1. deploy the app (this repo)
./deploy_kind.sh

# 2. install k8s-proxy alongside (from ../k8s-proxy)
helm upgrade --install k8s-proxy ../k8s-proxy/charts/k8s-proxy \
  --namespace keploy --create-namespace

# 3. drive traffic — tests stream back to the enterprise-ui dashboard
./demo_script.sh exercise
```

Mode B demonstrates the story that matters to an enterprise buyer: **Keploy
works in Kubernetes, not just on a developer laptop.**

---

## The two-terminal "Tosca sidecar" demo

Terminal 1 — Keploy recording the app:
```bash
./demo_script.sh record-local    # just the record half — Ctrl+C when done
```

Terminal 2 — the narrated Tosca-driven flow:
```bash
./simulate_tosca_flow.sh --host http://localhost:8080
# or, if the app is in kind:
./simulate_tosca_flow.sh --host http://localhost:30080
```

The narrator script logs what Tosca would "click" in the Fiori UI and
what Keploy captures underneath. The 360 step in particular is the pitch —
**one inbound click, three outbound SAP OData calls**, all recorded.

---

## Configuration

All runtime config is externalised. The ConfigMap (`k8s/configmap.yaml`) and
Secret (`k8s/secret.yaml`, from `.example`) are the K8s sources of truth.
For local / compose runs, `.env` takes their place.

| Env var              | Default                                      | Notes                                 |
|----------------------|----------------------------------------------|---------------------------------------|
| `SAP_API_BASE_URL`   | `https://sandbox.api.sap.com/s4hanacloud`    | Upstream SAP tenant                   |
| `SAP_API_KEY`        | *(empty)*                                    | Sandbox API key                       |
| `SAP_BEARER_TOKEN`   | *(empty)*                                    | Preferred if set (real BTP tenant)    |
| `SERVER_PORT`        | `8080`                                       | Container listen port                 |
| `SPRING_PROFILES_ACTIVE` | `default` locally, `kubernetes` in K8s   | Toggles JSON log formatter            |

Resilience4j retry / circuit-breaker settings live in `application.yml`
under `resilience4j.*`. Defaults: 3 attempts, exponential backoff, CB opens
at 60% failure over 20 calls, 30s open-state cool-down.

---

## Files

| Path | Purpose |
|---|---|
| `pom.xml` | Maven — Spring Boot 3, Java 21, Resilience4j, Actuator, SpringDoc |
| `src/main/java/.../Customer360Application.java` | Spring Boot entry + OpenAPI metadata |
| `src/main/java/.../config/SapClientConfig.java` | `RestTemplate` + auth / encoding / correlation interceptors + fan-out executor |
| `src/main/java/.../web/CustomerController.java` | REST endpoints |
| `src/main/java/.../web/GlobalExceptionHandler.java` | RFC 7807 problem responses |
| `src/main/java/.../service/CustomerService.java` | Simple lookups |
| `src/main/java/.../service/Customer360AggregatorService.java` | **Fan-out aggregator — the demo money shot** |
| `src/main/java/.../sap/SapBusinessPartnerClient.java` | Low-level SAP gateway with retry / CB |
| `src/main/java/.../sap/CorrelationIdFilter.java` | Inbound correlation-id seeder |
| `src/main/java/.../sap/CorrelationIdInterceptor.java` | Outbound correlation-id propagator |
| `src/main/java/.../sap/SapApiException.java` | SAP-specific exception |
| `src/main/java/.../model/*.java` | DTOs: BusinessPartner, Address, Role, Customer360View, … |
| `src/main/resources/application.yml` | Externalised config |
| `src/main/resources/logback-spring.xml` | Console (dev) + JSON (kubernetes) appenders |
| `Dockerfile` | Multi-stage build, non-root runtime, Spring Boot layers |
| `docker-compose.yml` | Local dev without kind |
| `kind-config.yaml` | Ingress-ready kind cluster (host 80/443 + 30080 mapped, `ingress-ready=true` label) |
| `k8s/namespace.yaml` | Namespace with `keploy.io/enabled: "true"` marker |
| `k8s/configmap.yaml` | Non-secret runtime config |
| `k8s/secret.yaml.example` | Template for `SAP_API_KEY` secret |
| `k8s/deployment.yaml` | Liveness/readiness probes, resource limits, security context |
| `k8s/service.yaml` | NodePort 30080 (works standalone, also the Ingress backend) |
| `k8s/ingress.yaml` | Ingress at `customer360.localtest.me` + catch-all at `localhost` |
| `deploy_kind.sh` | One-shot cluster + build + load + apply |
| `demo_script.sh` | Record / replay / offline-test harness |
| `simulate_tosca_flow.sh` | Narrated Tosca-driven Fiori flow |

---

## What makes this "enterprise-grade" (and why Tricentis folks will recognise it)

- **Spring Boot 3 + Java 21** — the default stack in most RISE customers
- **Layered: controller → service → SAP client** — classic separation
- **Resilience4j retry + circuit breaker** on every SAP call
- **RFC 7807 problem responses** with `X-Upstream-Status` for SAP diagnostics
- **Correlation-id propagation** end-to-end (inbound filter + outbound interceptor)
- **Structured JSON logs** in the `kubernetes` profile
- **Actuator probes** wired to K8s liveness + readiness, with circuit-breaker health included
- **Prometheus metrics** with request histograms / percentiles
- **OpenAPI 3 / Swagger UI** for API discovery
- **Non-root Docker runtime**, `readOnlyRootFilesystem`, capabilities dropped
- **Multi-stage Spring Boot layered image** for fast rebuilds
- **Graceful shutdown** with 30s termination grace period for rolling deploys
- **NodePort 30080** aligned with the `k8s-proxy` convention used elsewhere in this repo

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `401 Unauthorized` from SAP | `SAP_API_KEY` missing or expired. Verify with `curl -H "APIKey: $SAP_API_KEY" https://sandbox.api.sap.com/s4hanacloud/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_BusinessPartner?\$top=1` |
| `ImagePullBackOff` in kind | You forgot to `kind load docker-image customer360:local`. Run `./deploy_kind.sh build`. |
| `ErrImageNeverPull` | `imagePullPolicy: IfNotPresent` and image not loaded. Same fix. |
| Liveness probe failing at startup | 40s `startupProbe` grace should cover the JVM warm-up; if not, raise `failureThreshold` in `k8s/deployment.yaml`. |
| Circuit breaker stuck open | Check SAP is actually reachable; inspect `/actuator/health` → `components.circuitBreakers`. |
| `./deploy_kind.sh` says kind not found | Install with `go install sigs.k8s.io/kind@latest` or from the kind release page. |
| Rate-limited (429) from SAP sandbox | Sandbox is per-minute limited. Wait 60s, or switch to offline replay which never touches SAP. |
| Keploy recording on port 8080 | Runs the JVM; Keploy's default proxy/DNS ports are 16789/26789 — check `ss -ltnp` if you see bind errors. |

---

## Extending this

The obvious next moves for a real RISE customer:

1. **OAuth2 client-credentials** (xsuaa) flow for production BTP tenants —
   see `sap_demo_B` for the Go equivalent; the pattern is the same.
2. **Kafka producer** that publishes `customer.360.changed` events on
   write flows — another recording surface for Keploy.
3. **Caching layer** (Redis) in front of SAP to absorb read bursts —
   Keploy records those hits too.
4. **Ingress + mTLS** replacing NodePort for production topology.
5. **HelmChart** packaging to match the k8s-proxy deployment style.

All of the above extend the integration graph, which extends Keploy's
demonstrable value surface. Nothing about them requires rewriting this
service — the layered design absorbs them cleanly.
