# tidb-stmt-cache

A Spring Boot (Java 17) sample that drives two distinct keploy regressions
against TiDB and Apache Pulsar in a single app:

| Endpoint | Exercises |
| --- | --- |
| `GET /api/kv/{v}` and `GET /api/kv/insert-select/{v}` | MySQL Connector/J prepared-statement cache + HikariCP LIFO pool → orphan `COM_STMT_EXECUTE` matcher path |
| `POST /events/patch` | Hibernate INSERT + Pulsar `SEND` on a **partitioned** topic with default `RoundRobinPartitionRouter` → the partition-routing replay regression |

Both flows share the same `HikariDataSource` bean (Flipkart's actual shape:
`autoCommit=false`, `prepStmtCacheSize=500`, `prepStmtCacheSqlLimit=2048`,
JPA `provider_disables_autocommit=true`).

## Why the Pulsar partitioned topic matters

The Pulsar Java client's default `RoundRobinPartition` router picks a
**random starting partition** when a producer is constructed, then walks
through partitions in order. So:

* During recording, the producer might start on partition 5.
* During replay, a freshly-constructed producer starts on partition 7.

The recorded `SEND` mock targets `…events-partition-5`; the live `SEND`
during replay targets `…events-partition-7`. Without keploy's
`baseTopic()` matcher loosening
(`enterprise/pkg/core/proxy/integrations/pulsar/replayer/replayer.go`),
no recorded mock matches the live topic and replay fails with
`pulsar replay: payload-aware mock mismatch`.

## Layout

```
.
├── docker-compose.yml          local TiDB + Pulsar (+ partitioned-topic init)
├── Dockerfile                  two-stage build → tidb-pulsar-app:dev
├── k8s/                        manifests for the k8s-proxy auto-replay path
│   ├── 00-namespace.yaml
│   ├── 10-tidb.yaml
│   ├── 20-pulsar.yaml          includes a Job that pre-creates the partitioned topic
│   └── 30-app.yaml             carries keploy.io/record-session=true for the webhook
├── pom.xml
└── src/main/java/com/example/tidbstmtcache/
    ├── DataSourceConfig.java   Flipkart-shape HikariCP bean
    ├── EventsController.java   POST /events/patch — JPA save + Pulsar send
    ├── EventEntity.java        JPA entity for the `events` table
    ├── EventRepository.java
    ├── PulsarConfig.java       PulsarClient + Producer<byte[]> with RoundRobinPartition
    ├── QueryController.java    existing orphan-EXECUTE endpoints (unchanged)
    ├── SchemaInitializer.java  creates the `kv` table; Hibernate creates `events`
    └── TidbStmtCacheApplication.java
```

## Quick path — local docker-compose smoke test

Use this to confirm the app boots and the partition routing is
non-deterministic across producer creations. Does **not** drive keploy.

```bash
cd samples-java/tidb-stmt-cache
docker compose up -d
# Wait for tidb (port 4000) and pulsar (port 6650) to be ready, and the
# pulsar-init container to exit 0.

mvn -DskipTests spring-boot:run &     # or run from your IDE
APP_PID=$!

curl -s -X POST http://localhost:8080/events/patch \
  -H 'Content-Type: application/json' \
  -d '{
    "entity_id": "FMPP4037630682",
    "event_name": "delivered",
    "event_timestamp": "2026-05-23T17:07:22+05:30",
    "task_orchestrator": "FSD"
  }'
# Expect: {"message":"Event patched"}

kill $APP_PID
docker compose down -v
```

To see the round-robin in action, restart the app between curls and
diff `bin/pulsar-admin topics partitioned-stats persistent://public/default/events`
output — partition message counts will land on different partitions
each cold start.

## Full path — k8s-proxy auto-replay (matches Flipkart prod flow)

The k8s-proxy controller watches the namespace for pods carrying
`keploy.io/record-session=true` and injects the keploy-agent sidecar.
After a recording is captured, an auto-replay session reconstructs the
app pod in isolation and feeds the recorded HTTP requests back through
it; the agent replays MySQL and Pulsar from mocks.

### 1 · Build the patched enterprise agent image

The matcher fix lives in
`enterprise/pkg/core/proxy/integrations/pulsar/replayer/replayer.go`
(`baseTopic` function + its callsites). Build a keploy-agent image that
includes it — the exact `make` target depends on your enterprise repo
layout; from the workspace root:

```bash
cd ../enterprise
make docker-image AGENT_IMAGE=keploy-agent:partition-fix
kind load docker-image keploy-agent:partition-fix --name <your-kind-cluster>
```

### 2 · Install the k8s-proxy chart pointing at the patched agent

```bash
cd ../k8s-proxy
helm upgrade --install k8s-proxy ./charts/k8s-proxy \
  --namespace k8s-proxy --create-namespace \
  --set agent.image=keploy-agent:partition-fix \
  --set webhook.watchNamespaces='{tidb-pulsar-replay}'
```

### 3 · Build and load the sample app image

```bash
cd ../samples-java/tidb-stmt-cache
mvn -DskipTests package
docker build -t tidb-pulsar-app:dev .
kind load docker-image tidb-pulsar-app:dev --name <your-kind-cluster>
```

### 4 · Apply the manifests

```bash
kubectl apply -f k8s/
kubectl -n tidb-pulsar-replay wait deploy/tidb deploy/pulsar deploy/tidb-pulsar-app \
  --for=condition=Available --timeout=5m
kubectl -n tidb-pulsar-replay wait --for=condition=complete job/pulsar-init-topic --timeout=2m
```

### 5 · Record a session

Drive a few `POST /events/patch` requests through the in-cluster Service.
The keploy-agent sidecar attached to `tidb-pulsar-app` will capture the
MySQL and Pulsar traffic.

```bash
kubectl -n tidb-pulsar-replay port-forward svc/tidb-pulsar-app 8080:80 &
PF_PID=$!

for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8080/events/patch \
    -H 'Content-Type: application/json' \
    -d "{\"entity_id\":\"FMPP$i\",\"event_name\":\"delivered\",\"event_timestamp\":\"2026-05-23T17:07:22+05:30\",\"task_orchestrator\":\"FSD\"}"
done

kill $PF_PID
```

Confirm a `SEND` mock landed on a specific partition:

```bash
kubectl -n tidb-pulsar-replay logs deploy/tidb-pulsar-app -c keploy-agent \
  | grep -E 'commandType.*SEND|topic.*partition-'
```

### 6 · Trigger auto-replay

Use the k8s-proxy `Replay` CR (or REST API, depending on your install).
Example via the openapi-described endpoint:

```bash
kubectl -n k8s-proxy port-forward svc/k8s-proxy 8000:80 &
curl -s -X POST http://localhost:8000/api/v1/replays \
  -H 'Content-Type: application/json' \
  -d '{
    "namespace": "tidb-pulsar-replay",
    "deployment": "tidb-pulsar-app",
    "testSetIDs": ["<the test-set ID printed in the agent logs>"]
  }'
```

### 7 · Assert the regression is fixed

```bash
kubectl -n tidb-pulsar-replay logs deploy/tidb-pulsar-app -c keploy-agent \
  | grep -E 'payload-aware mock mismatch|Test passed|result.*passed'
```

* **Without the patch** — at least one of the recorded sessions fails
  with `pulsar replay: payload-aware mock mismatch for SEND (topic=…events-partition-<N>)`,
  the app returns HTTP 500, the testcase is marked failed.
* **With the patch** — the live `SEND` to `…events-partition-<N>` matches
  the recorded mock for `…events-partition-<M>` (same base topic
  `…events`, same payload), the synthetic `SEND_RECEIPT` is returned,
  the app returns HTTP 200, the testcase passes.

## Reproducing the Flipkart symptom exactly

The Strowger recording in `Strowger Playgro Global Shipment (1)/testset/`
matches this sample structurally — the `POST /events/patch` body, the
HikariCP shape, and the partitioned Pulsar topic. To run the customer's
mocks against this app:

1. Replace `k8s/30-app.yaml`'s `PULSAR_TOPIC` env with the customer's
   topic name (`persistent://toss-relayer/gsm-relayers-prod/toss_EKL-E2E-ORCHESTRATOR_gsm_ns`).
2. Use the customer's `mocks.yaml` instead of a fresh recording.
3. Run step 6 with the customer's test-set ID.

Without the fix you should see the same `payload-aware mock mismatch
for SEND (topic=…partition-7)` log line the customer reported. With the
fix you should see the `SEND` resolve against the customer's
`partition-5` mock and the testcase pass.
