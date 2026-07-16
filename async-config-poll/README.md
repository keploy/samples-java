# async-config-poll

A Spring Boot 1.5 / Java 8 rule-engine sample that demonstrates Keploy's
**async-egress engine**.

The app has two HTTP endpoints backed by MySQL, and it depends on a central
config service in two different ways:

| Interaction | When | Keploy treats it as |
|-------------|------|---------------------|
| `GET /v1/buckets/app-common`, `app-features`, `app-config?watch=false` | once, at boot (blocking) | ordinary synchronous mocks — the app cannot boot without them |
| `GET /v1/buckets/app-config?watch=true&version=N` | forever, from a background daemon thread | **async egress** — fires on the app's own schedule, not tied to any ingress testcase |
| `SELECT ...` on MySQL | per request | ordinary synchronous mocks |

The background watch poll is the interesting part. Because it runs on a timer in
its own thread, it does not line up one-to-one with the recorded testcases. A
naive replay would fail: the app polls at replay time too, and the request
(`?version=17`, `?version=18`, …) never matches a recorded one exactly.

Keploy's async-egress engine handles this. The lane declared in `keploy.yml`
tells Keploy that this endpoint is async:

```yaml
async:
  lanes:
    - name: config-watch
      type: http
      match:
        pathRegex: "^/v1/buckets/app-config$"
      matchQuery:
        watch: "true"          # only the background watch polls, not the boot call
      volatileParams: ["version"]   # the version query param varies every poll — treat as noise
```

At replay the engine serves the recorded watch responses back to the poller
independently of testcase ordering, treats the changing `version` param as
shape-noise, and keep-alives the poller when there is nothing left to serve — so
the app stays happy and the ingress tests still pass. At the end of replay Keploy
prints an `async egress verdict` line (served / shape_flags / not_exercised).

## Endpoints

- `GET /health` — small health payload; runs `SELECT 1` against MySQL.
- `GET /rules/{useCase}` — ordered rules for `(useCase, tenant)` read from MySQL.
  Requires headers `X-Tenant-Id` and `X-Agent-Id`.
  Example: `GET /rules/ORDER_FLOW` with `X-Tenant-Id: ACME`, `X-Agent-Id: 957`.

## Run it locally

Prerequisites: JDK 8, Maven, Docker, Go (for the config stub), and a Keploy
build that includes the async-egress engine.

```bash
# 1. dependencies
docker compose up -d                 # MySQL 5.7 seeded from init.sql
go run ./config-stub &               # config service stub on :9100

# 2. build the app
mvn -B clean package -Dmaven.test.skip=true

# 3. record
sudo -E keploy record -c "java -jar target/async-config-poll.jar"
#   drive traffic, then Ctrl-C keploy:
curl localhost:8080/health
curl -H "X-Tenant-Id: ACME" -H "X-Agent-Id: 957" localhost:8080/rules/ORDER_FLOW

# 4. replay (deps down — Keploy serves everything from mocks)
docker compose down
sudo keploy test -c "java -jar target/async-config-poll.jar" --delay 20
```

To make a watch poll land in the *middle* of a testcase at replay (so the async
lane is actively exercised rather than drained between tests), lower the poll
interval and widen the request window:

```bash
WATCH_INTERVAL_MS=150 RULES_DELAY_MS=800 sudo -E keploy record -c "java -jar target/async-config-poll.jar"
```
