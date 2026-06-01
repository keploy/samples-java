# spring-aerospike — Aerospike-Java sample with Keploy record/replay

A Spring Boot 2.7 service that talks to Aerospike CE over the
clear-text service port (3000) using the official
`aerospike-client-jdk8`. Recorded and replayed end-to-end with
Keploy via three bundled scripts that mirror the
`keploy/samples-go/aerospike-tls` shape one-to-one — same endpoints,
same test-set layout, same record-then-replay loop.

What the sample demonstrates:

* **Keploy records binary Aerospike protocol traffic** — Info,
  AS_MSG (single-record PUT/GET/TOUCH/DELETE), BATCH_READ/WRITE,
  SCAN, QUERY, UDF, CDT — and replays them from `mocks.yaml`
  without needing the real cluster.
* **Replay stays deterministic at any concurrency the app exposes** —
  single-client `/parallel`, multi-client round-robin, and per-
  request fresh-client construction all pass cleanly.
* **A pipeline-friendly shape.** Three `scripts/script-{1,2,3}.sh`
  entry points each record and replay one test-set independently,
  so a CI matrix can call them as separate steps.

## Layout

```
spring-aerospike/
├── pom.xml                            # Spring Boot 2.7 + aerospike-client-jdk8
├── src/main/java/com/example/aerospike/
│   ├── SpringAerospikeApplication.java
│   ├── config/                        # client + multi-client pool, warmup, policies
│   └── controller/                    # one @RestController per endpoint group
├── src/main/resources/
│   └── application.properties         # port + Aerospike host/namespace/pool sizing
├── aerospike-conf/
│   └── aerospike.conf                 # CE config: clear-text on 3000
├── docker-compose.yml                 # Aerospike CE + the Spring Boot app
├── Dockerfile                         # eclipse-temurin 17 + mvn package
├── keploy.yml                         # Keploy CLI config (command, ports)
└── scripts/
    ├── common.sh                      # shared boot/build/record/replay/normalise
    ├── script-1.sh                    # records + replays test-set-0 (CRUD)
    ├── script-2.sh                    # records + replays test-set-1 (/parallel)
    └── script-3.sh                    # records + replays test-set-2 (/multiclient + /freshclient)
```

There is no committed `keploy/` directory — the scripts produce it
from scratch every run. Each CI run validates the full
record-then-replay loop instead of replaying stale captures.

## Endpoints

| Method | Path                       | What it does                                                                 |
| ------ | -------------------------- | ---------------------------------------------------------------------------- |
| GET    | `/health`                  | `info build + namespaces`                                                    |
| POST   | `/put`                     | single-record PUT                                                            |
| GET    | `/get/{key}`               | single-record GET                                                            |
| POST   | `/batch/put`               | sequential write loop                                                        |
| GET    | `/batch/get?k=a&k=b`       | BATCH_READ                                                                   |
| POST   | `/scan`                    | full namespace scan                                                          |
| POST   | `/query`                   | secondary-index range query                                                  |
| POST   | `/udf`                     | UDF_EXECUTE                                                                  |
| POST   | `/cdt/list/append`         | CDT list append                                                              |
| POST   | `/cdt/map/put`             | CDT map put                                                                  |
| POST   | `/touch/{key}`             | TOUCH                                                                        |
| DELETE | `/key/{key}`               | DELETE                                                                       |
| POST   | `/parallel?n=N&prefix=P`   | fans out N threads, each PUT+GET on a unique key — **one shared client**     |
| POST   | `/multiclient?n=N&prefix=P`| same, but round-robins across **4 pre-built `AerospikeClient` instances**    |
| POST   | `/freshclient?n=N&prefix=P`| **each thread builds its own `AerospikeClient`** inside the request          |

## Run it manually

```bash
# 1) Boot Aerospike CE on clear-text 3000.
docker compose up -d aerospike

# 2) Build + run the Spring Boot app.
mvn -q -DskipTests package
java -jar target/spring-aerospike.jar

# 3) Hit it.
curl -s localhost:8090/health
curl -s -XPOST localhost:8090/put -H 'Content-Type: application/json' \
     -d '{"key":"alice","bins":{"age":30}}'
curl -s localhost:8090/get/alice
curl -s -XPOST 'localhost:8090/parallel?n=24&prefix=run1'
curl -s -XPOST 'localhost:8090/multiclient?n=24&prefix=mc1'
curl -s -XPOST 'localhost:8090/freshclient?n=8&prefix=fc1'
```

## Record + replay with the scripts

```bash
# Each script is self-contained: brings up Aerospike, builds the
# JAR, records, replays. Exit code is non-zero if any case fails on
# replay.
sudo ./scripts/script-1.sh    # test-set-0: single-endpoint CRUD
sudo ./scripts/script-2.sh    # test-set-1: /parallel n = 4..24
sudo ./scripts/script-3.sh    # test-set-2: /multiclient + /freshclient
```

Pipeline-friendly knobs (env vars):

| Var          | Default       | What it does                                                  |
|--------------|---------------|---------------------------------------------------------------|
| `KEPLOY`     | `sudo keploy` | binary + auth invocation. Override to `keploy` if root        |
| `PORT`       | `8090`        | HTTP port the recorded sample listens on                      |
| `LOG_DIR`    | `/tmp`        | where to drop the keploy record log                           |
| `SKIP_DOCKER`| (unset)       | `=1` skips `docker compose up -d aerospike` (already running) |
| `SKIP_BUILD` | (unset)       | `=1` skips `mvn package` (JAR already in target/)             |

## Concurrency notes — why the warmup + retry matter

Mocked replay through Keploy is roughly 10–20× faster than real
Aerospike for the same op. A burst of N concurrent threads on a
cold client pool then races to open N fresh sockets, and the
thread that loses the race surfaces as `MAX_RETRIES_EXCEEDED` at
the application — even though every peer in the same burst
succeeds.

`AerospikeConfig` paints over this with four layered changes;
together they make `/parallel?n=24`, `/multiclient?n=24`, and
`/freshclient?n=8` replay clean on every run:

1. **Sized pool** — `ClientPolicy.maxConnsPerNode = 256`. The
   `OpeningConnectionThreshold` analogue is kept low (16) so a
   sudden burst doesn't outpace upstream connect rate.
2. **Tolerant per-op policy** — `Policies.parallelWrite()` and
   `Policies.parallelRead()` set `socketTimeout 10s`, `totalTimeout
   30s`, `maxRetries 10`, `sleepBetweenRetries 5ms`.
3. **Two-phase warmup** on the main client at startup: a sequential
   prelude that walks the cluster past cold-start latencies,
   followed by a parallel fill that puts idle connections in the
   pool before the HTTP server accepts the first request.
4. **App-level retry wrapper** (`RetryHelper.doOp`) around each PUT
   and GET in `/parallel`, `/multiclient`, and `/freshclient`.

`/multiclient`'s extra clients are deliberately NOT warmed at
startup — a hundred concurrent dials at boot can stall a record-
time proxy. The retry wrapper covers their first burst instead.

This sample is the Java counterpart of
[`keploy/samples-go/aerospike-tls`](https://github.com/keploy/samples-go/tree/main/aerospike-tls);
the script set is byte-for-byte the same shape so a single CI
matrix can drive both languages with the same harness.
