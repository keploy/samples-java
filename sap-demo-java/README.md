# sap-demo-java — Customer 360 aggregation service

Spring Boot 3 / Java 21 reference service that builds a **Customer 360 view**
on the fly by fanning out to SAP S/4HANA Business Partner OData endpoints
and merging the result with locally stored CRM annotations (tags + notes)
from Postgres. Used inside the Keploy project as the canonical regression
fixture for the SAP fan-out path and the v3 HTTPS + Postgres parsers.

---

## What this app does

This is a small "Customer 360" aggregator, the kind of service an internal
CRM dashboard team would ship on SAP BTP. When a user hits
`GET /api/v1/customers/{id}/360`, the service fans out: **one synchronous
SAP OData call** for the BusinessPartner master record, then **two more
parallel SAP OData calls** (addresses + roles), **in parallel with two
Postgres queries** (tags + notes). The five results are merged into a single
JSON response.

The real-world analog is an in-house CRM dashboard that needs a unified
customer view by calling the system-of-record (SAP) plus a local CRM
annotations DB (Postgres), without any surface area that hides how those
downstream calls behave on the wire.

---

## Why this shape is interesting for Keploy

The service is deliberately structured to exercise the trickiest parts of
Keploy's interception layer in a single flow.

- **Parallel outbound TLS** — every `/360` request opens 3 concurrent HTTPS
  connections to the SAP sandbox plus 2 concurrent TLS-enabled Postgres
  queries. This shape reliably surfaces parser-level concurrency bugs.
- **Chunked HTTP/1.1 + keep-alive reuse** — SAP's sandbox returns chunked
  responses over a reused keep-alive connection. This is the path that
  exposed a 60-second idle-timeout stall inside Keploy (a single `/360`
  request went from ~50 s down to ~586 ms after the fix). See
  [keploy/keploy#4110](https://github.com/keploy/keploy/pull/4110).
- **Schema diversity in a single repo** — GET / POST / DELETE verbs, JSON
  request bodies, a custom `X-Correlation-Id` header, actuator health
  probes, both chunked and Content-Length responses, and the OpenAPI
  `/v3/api-docs` catalog endpoint.
- **Stateful local DB** — Flyway-migrated schema behind a HikariCP
  connection pool, which exercises the v3 Postgres parser's
  prepared-statement cache handling and pool-reuse semantics.

---

## Requirements

- Java 21 + Maven 3.9+
- Docker (Postgres 16 is brought up as a sidecar via `docker compose`)
- A Keploy binary if you want to record / replay (any v3.3.x or newer is fine)
- An SAP API sandbox key — grab one for free from the SAP Business
  Accelerator Hub:
  [api.sap.com/api/API_BUSINESS_PARTNER](https://api.sap.com/api/API_BUSINESS_PARTNER).
  Click *Show API Key* once signed in.

---

## Local quickstart

```bash
cd sap-demo-java

# 1. Bring up Postgres in the background (or use ./deploy_kind.sh for k8s)
docker compose up -d postgres

# 2. Point the app at the SAP sandbox
export SAP_API_KEY=<your-sandbox-key>
export SAP_SANDBOX_BASE_URL=https://sandbox.api.sap.com/s4hanacloud

# 3. Build and run
mvn spring-boot:run
```

The service listens on `:8080`. Smoke-test it:

```bash
curl -s http://localhost:8080/actuator/health | jq .
curl -s http://localhost:8080/api/v1/customers/202/360 | jq .
```

---

## Recording with Keploy (native CLI)

Run the service under `keploy record`, exercise it with `run_flow.sh`
(which fires 20 distinct request shapes covering every endpoint and
verb), then replay:

```bash
# terminal 1 — record
keploy record -c "java -jar target/customer360.jar"

# terminal 2 — drive traffic
bash run_flow.sh

# Ctrl+C the record command. Testcases land under ./keploy/
# then replay:
keploy test -c "java -jar target/customer360.jar"
```

---

## Recording inside Kubernetes (k8s-proxy)

The same flow runs in-cluster through the Keploy k8s-proxy. Deploy the
app to kind:

```bash
./deploy_kind.sh
kubectl -n sap-demo annotate deploy/customer360 keploy.io/record=enabled

# start recording
curl -k -X POST https://<k8s-proxy-svc>:8080/record/start \
  -H "Authorization: Bearer $KEPLOY_SHARED_TOKEN_OVERRIDE" \
  -d '{"namespace":"sap-demo","deployment":"customer360"}'

# drive traffic (e.g. run_flow.sh against the NodePort / Ingress host)
./run_flow.sh

# stop recording — auto-replay then fires on a standalone pod
curl -k -X POST https://<k8s-proxy-svc>:8080/record/stop \
  -d '{"record_id":"sap-demo-customer360"}'
```

Replay results land in the enterprise dashboard at
[app.keploy.io](https://app.keploy.io).

---

## Key endpoints

| Method  | Path                                     | Purpose                                 | Downstream                       |
|---------|------------------------------------------|-----------------------------------------|----------------------------------|
| GET     | `/actuator/health`                       | Liveness / readiness probe              | none                             |
| GET     | `/api/v1/customers/count`                | KPI tile — total partner count          | Postgres only                    |
| GET     | `/api/v1/customers/{id}`                 | Business partner detail                 | SAP only                         |
| GET     | `/api/v1/customers/{id}/tags`            | Customer tags                           | Postgres only                    |
| GET     | **`/api/v1/customers/{id}/360`**         | **Full aggregation**                    | **SAP × 3 + Postgres × 2 parallel** |
| POST    | `/api/v1/customers/{id}/tags`            | Add a tag                               | Postgres only                    |
| POST    | `/api/v1/customers/{id}/notes`           | Add a note                              | Postgres only                    |
| DELETE  | `/api/v1/customers/{id}/tags/{tag}`      | Remove a tag                            | Postgres only                    |
| GET     | `/v3/api-docs`                           | OpenAPI catalog                         | none                             |

---

## Noise configuration

`keploy.yml` marks three fields as global noise so replays stay
deterministic across runs:

- `header.X-Correlation-Id` — generated per-request by `CorrelationIdFilter`;
  it's intentionally unique per call, so it can never match on replay.
- `body.timestamp` / `body.installedOn` / `body.id` — server-generated
  values on write paths (tag / note rows). The semantic content is stable;
  the numeric/temporal surface is not.
- `ETag` on SAP responses (and `Date` headers) — SAP regenerates these on
  every fetch, independent of the underlying record state.

If your team adds more generated fields, extend `test.globalNoise.global`
in `keploy.yml`.

---

## Architecture

Classic Spring Boot layering, with one custom wrinkle for the fan-out:

- **Controller** — `web/Customer360Controller.java` (+ `CustomerController`,
  `TagController`, `NoteController`, `AuditController`). RFC 7807 problem
  responses come from `web/GlobalExceptionHandler`.
- **Aggregator** — `service/Customer360AggregatorService.java`. Builds
  three `CompletableFuture`s for the SAP calls and two more for the
  Postgres queries, all dispatched on a dedicated `sapCallExecutor` thread
  pool, then joins them via `CompletableFuture.allOf`. Partial-failure
  policy: the SAP partner fetch is mandatory; everything else degrades
  gracefully.
- **SAP client** — `sap/SapBusinessPartnerClient.java`. Spring
  `RestTemplate` backed by the Apache `HttpComponents5` client factory
  (keep-alive + transparent gzip handling, which the JDK default doesn't
  offer). Retries + circuit breaker via Resilience4j (`sapApi` instance in
  `application.yml`).
- **Persistence** — `repository/CustomerTagRepository.java` and
  `CustomerNoteRepository.java` (Spring Data JPA), plus
  `AuditEventRepository`. Schema is Flyway-migrated
  (`src/main/resources/db/migration/V1__init_schema.sql`); pool is
  HikariCP with `maximum-pool-size=10`.
- **Correlation** — inbound `CorrelationIdFilter` seeds the MDC;
  outbound `CorrelationIdInterceptor` propagates the ID on every SAP call.

---

## Troubleshooting

- **`502 SAP upstream error` on `/360`.** Check `SAP_API_KEY`; the SAP
  sandbox also rate-limits at roughly 120 requests/minute. The built-in
  Resilience4j circuit breaker will open if you punch through that.
- **Recording stalls / `/360` takes ~60 s.** You're probably on Keploy
  < v3.3, which had an HTTP chunked-terminator bug on keep-alive reuse.
  Upgrade to v3.3.x or newer (fixed in
  [keploy/keploy#4110](https://github.com/keploy/keploy/pull/4110)).
- **Tests fail only on `X-Correlation-Id`.** Make sure the header is in
  `test.globalNoise.global` in `keploy.yml`; it's generated per request
  and can never match otherwise.
- **`ImagePullBackOff` / `ErrImageNeverPull` in kind.** You forgot to
  `kind load docker-image customer360:local` — run `./deploy_kind.sh build`.
- **Liveness probe flaps at startup.** The 40 s `startupProbe` grace is
  usually enough for the JVM; raise `failureThreshold` in
  `k8s/deployment.yaml` if your host is slow.

---

## Files

| Path | Purpose |
|---|---|
| `pom.xml` | Spring Boot 3, Java 21, Resilience4j, Flyway, HikariCP, SpringDoc |
| `src/main/java/com/keploy/sapdemo/customer360/...` | Application source (see *Architecture* above) |
| `src/main/resources/application.yml` | Externalised config |
| `src/main/resources/db/migration/V1__init_schema.sql` | Flyway schema: `customer_tag`, `customer_note`, `audit_event` |
| `docker-compose.yml` | Local Postgres 16 sidecar |
| `Dockerfile` | Multi-stage, non-root Spring Boot layered image |
| `k8s/*.yaml` | Namespace / ConfigMap / Secret / Deployment / Service / Ingress |
| `deploy_kind.sh` | One-shot kind cluster + build + load + apply |
| `run_flow.sh` | 20-request exerciser used during `keploy record` |
| `demo_script.sh` | Record / replay / offline-test harness |
| `simulate_fiori_flow.sh` | Narrated Fiori-style flow for two-terminal demos |
| `keploy.yml` | Recorded-mock metadata + global noise rules |
