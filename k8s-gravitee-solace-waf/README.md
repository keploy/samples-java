# Kubernetes: Gravitee + Solace PubSub+ + ModSecurity WAF + PostgreSQL + Redis

A Spring Boot 3.3 / Java 21 `orders-api` deployed on a local Kind cluster behind a **ModSecurity / OWASP CRS** WAF and **Gravitee** API Gateway. The service persists orders in **PostgreSQL**, caches and deduplicates requests with **Redis**, calls an internal `inventory-api` outbound through **Gravitee**, authorizes payments against a partner `payments-api` outbound through **ModSecurity / OWASP CRS**, and publishes and consumes events on **Solace PubSub+** over native **SMF v3 (TCP 55555)**.

Use this sample to record live Kubernetes traffic with Keploy's DaemonSet eBPF agent (no code changes, no SDK, no sidecar) and replay it inside a deny-egress sandbox where all five dependency mock kinds (`Solace`, `PostgresV3`, `Redis`, `Http`, and `DNS`) are served from mocks.

> **Minimum versions:** Requires Keploy Enterprise `vX.Y.Z+`, Keploy Enterprise CLI `vX.Y.Z+`, and `k8s-proxy-chart` `vX.Y.Z+` for native Solace PubSub+ SMF v3 mocks.

## Architecture

```text
client -> ingress-nginx + ModSecurity / OWASP CRS PL1 (inbound WAF)   ns ingress-nginx
       -> Gravitee API Gateway 4.9 (API "Orders API", /orders)        ns gateway
       -> orders-api (Spring Boot 3.3, Java 21)                       ns orders
            |-- Redis 7.2 (SET NX EX idempotency, GET/SETEX cache, DEL) ns data
            |-- ModSecurity / OWASP CRS PL2 (Host: payments.partner.local)
            |     -> payments-api (200 authorized, or WAF 403 -> 422) ns orders
            |-- PostgreSQL 16 (INSERT / SELECT / UPDATE orders)       ns data
            |-- Gravitee API Gateway 4.9 (API "Inventory API", /inventory)
            |     -> inventory-api (POST /inventory/reserve)          ns orders
            |-- Solace PubSub+ Standard (native SMF v3, TCP :55555)   ns messaging
                  -> Publish: JCSMP Direct to topic orders/created
                  <-> Subscribe + Publish + Consume: JCSMP XMLMessageConsumer
                     on topic orders/cancelled/<orderId> (POST /orders/{id}/cancel)
```

## Prerequisites

- **Docker** (with at least 12 GB RAM available for Kind, Solace PubSub+, and replay sandboxes)
- **git**
- **Kind** (`v0.27+`)
- **kubectl** (`v1.31+`)
- **Helm** (`v3.16+`)
- **curl** and **python3**
- A **Keploy Enterprise** account at [app.keploy.io](https://app.keploy.io) and the [Keploy Enterprise CLI](https://keploy.io/docs/keploy-cloud/cloud-installation/) (`keploy`) for local cloud replay and triage

Verify your local tools:

```bash
docker --version
git --version
kind --version
kubectl version --client
helm version
```

## 1. Clone the repository

```bash
git clone https://github.com/keploy/samples-java.git
cd samples-java/k8s-gravitee-solace-waf
```

## 2. Create the Kind cluster and install ingress-nginx

```bash
kind create cluster --name keploy-k8s-sample --config kind-config.yaml --wait 180s
```

Install the Kind `ingress-nginx` controller (pinned to its final upstream release `controller-v1.15.1`) and wait until it is ready:

> **Note:** Upstream `kubernetes/ingress-nginx` was retired and archived in March 2026 (`controller-v1.15.1` is its final release). It is used here only to provide a self-contained local Kind ModSecurity / OWASP CRS WAF sandbox.

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx rollout status deploy/ingress-nginx-controller --timeout=300s
```

> **Linux / Docker-in-Docker note:** If `/sys/fs/bpf` is not mounted as `bpffs` inside the Kind node, mount it so the Keploy DaemonSet agent can pin its eBPF maps:
>
> ```bash
> docker exec keploy-k8s-sample-control-plane sh -c 'mount | grep -q " /sys/fs/bpf type bpf" || mount -t bpf bpf /sys/fs/bpf'
> ```

## 3. Build and load the application images

Build the baseline `1.0` image plus three change variants (`2.0`, `2.1`, `2.2`) and the two downstream services (`inventory-api`, `payments-api`):

| Image | Variant patch | What changes |
|---|---|---|
| `orders-api:1.0` | Baseline | All dependencies healthy |
| `orders-api:2.0` | `app/v2.patch` | Multi-dependency regression: renames `warehouse` → `fulfillmentCenter` on Gravitee `/inventory/reserve`, renames `totalAmount` → `amount` and bumps `eventVersion` `1` → `2` on Solace PubSub+ SMF, and adds `, note = 'cancelled-v2'` to the PostgreSQL `UPDATE` |
| `orders-api:2.1` | `app/v3.patch` | Subtle value drift: upper-cases `channel` (`"web"` → `"WEB"`) in the Solace PubSub+ `OrderCreated` event while keeping the HTTP response identical |
| `orders-api:2.2` | `app/v4.patch` | Intended API change: `POST /orders/{id}/cancel` returns `"status": "REFUND_PENDING"` instead of `"CANCELLED"` with every dependency call unchanged |

```bash
docker build -t orders-api:1.0 ./app
docker build --build-arg VARIANT=v2 -t orders-api:2.0 ./app
docker build --build-arg VARIANT=v3 -t orders-api:2.1 ./app
docker build --build-arg VARIANT=v4 -t orders-api:2.2 ./app
docker build -t inventory-api:1.0 ./inventory-app
docker build -t payments-api:1.0 ./payments-app
```

Load all images into the Kind cluster:

```bash
kind load docker-image --name keploy-k8s-sample \
  orders-api:1.0 orders-api:2.0 orders-api:2.1 orders-api:2.2 \
  inventory-api:1.0 payments-api:1.0
```

## 4. Deploy the stack

Apply the namespaces, data stores, Solace PubSub+, ModSecurity / OWASP CRS WAF configuration, Gravitee API Gateway (both `/orders` and `/inventory` APIs), downstream services, and ingresses:

```bash
kubectl apply -f k8s/00-namespaces.yaml \
  -f k8s/10-solace.yaml \
  -f k8s/12-postgres.yaml \
  -f k8s/13-redis.yaml \
  -f k8s/06-waf-modsecurity.yaml \
  -f k8s/15-inventory-api.yaml \
  -f k8s/16-payments-api.yaml \
  -f k8s/30-gravitee-gateway.yaml \
  -f k8s/31-gravitee-orders-api-definition.yaml \
  -f k8s/32-gravitee-inventory-api-definition.yaml \
  -f k8s/20-orders-api.yaml \
  -f k8s/40-ingress-waf.yaml \
  -f k8s/41-ingress-payments-waf.yaml
```

Wait for all deployments to become ready (Solace PubSub+ takes about 60 seconds on first boot):

```bash
kubectl -n data rollout status deploy/postgres --timeout=300s
kubectl -n data rollout status deploy/redis --timeout=300s
kubectl -n orders rollout status deploy/inventory-api --timeout=300s
kubectl -n orders rollout status deploy/payments-api --timeout=300s
kubectl -n gateway rollout status deploy/gravitee-gateway --timeout=300s
kubectl -n orders rollout status deploy/orders-api --timeout=300s
kubectl -n messaging rollout status deploy/solace --timeout=300s
```

Configure the Solace PubSub+ `orders-audit` queue (`orders/>`) and wait for the edge ingress route to sync:

```bash
until bash scripts/solace-setup-queue.sh; do sleep 5; done
until curl -fsS http://localhost/orders/ORD-SEED-1001 >/dev/null 2>&1; do sleep 3; done
```

Verify that a single order flows end to end through the WAF, Gravitee, Redis, partner payments WAF, PostgreSQL, outbound Gravitee `/inventory`, and Solace PubSub+:

```bash
curl -s -i -X POST http://localhost/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C-4242","reference":"PO-SMOKE-1","currency":"EUR","note":"standard delivery","items":[{"sku":"SKU-DOCK-USB4","quantity":1,"unitPrice":189.50}]}'

bash scripts/solace-queue-depth.sh
```

Verify inbound ModSecurity / OWASP CRS WAF blocks and the outbound partner WAF `403` verdict:

```bash
bash scripts/attack.sh
```

Reset the state back to the two seed orders before recording:

```bash
bash scripts/reset.sh
```

## 5. Connect the cluster to Keploy

1. Open [app.keploy.io/clusters](https://app.keploy.io/clusters) and click **Connect New Cluster**.
2. Expand **Advanced settings** and configure:
   - **Cluster Name**: `keploy-k8s-sample`
   - **How should Keploy reach this cluster?**: **Keploy tunnel — the agent dials out (no inbound access needed)**
   - **Deployment Type**: **SaaS**
   - Check **Enable DaemonSet mode**
3. Click **Connect to Cluster**. This registers the cluster and copies the generated Helm install command (with your cluster access key).
4. Create the `keploy` namespace and label it so Kubernetes Pod Security Admission permits the privileged DaemonSet agent:

```bash
kubectl create namespace keploy --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace keploy pod-security.kubernetes.io/enforce=privileged --overwrite
```

5. Run the copied install command, or store the cluster access key in a Kubernetes Secret and install `k8s-proxy` directly. Replace `<CHART_VERSION>` with the chart version shown in the dialog (or `X.Y.Z` from Minimum versions above, without a leading `v`):

```bash
kubectl -n keploy create secret generic keploy-credentials \
  --from-literal=access-key="<YOUR_ACCESS_KEY>" \
  --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install k8s-proxy oci://docker.io/keploy/k8s-proxy-chart \
  --version <CHART_VERSION> \
  --namespace keploy \
  --set selfHosted=false \
  --set environment=prod \
  --set keploy.existingSecret=keploy-credentials \
  --set keploy.existingSecretKey=access-key \
  --set keploy.clusterName=keploy-k8s-sample \
  --set keploy.apiServerUrl=https://api.keploy.io \
  --set keploy.tunnel.enabled=true \
  --set service.type=ClusterIP \
  --set ingress.enabled=false \
  --set daemonset.enabled=true
```

Wait for `k8s-proxy` and `k8s-proxy-daemonset` to become ready:

```bash
kubectl -n keploy rollout status deploy/k8s-proxy --timeout=180s
kubectl -n keploy rollout status ds/k8s-proxy-daemonset --timeout=180s
```

Confirm in **Clusters** that `keploy-k8s-sample` shows **Connected** and lists `orders/orders-api`.

## 6. Record live traffic

1. In the Keploy Console, open **Clusters → keploy-k8s-sample**, click **Record** on `orders/orders-api`, click **Next**, leave **Static deduplication** on under **Record config**, click **Next**, then click **Start Recording**.
2. Wait until the `orders-api` Pod is `1/1 Running`:

```bash
kubectl -n orders rollout status deploy/orders-api --timeout=180s
```

3. Drive realistic traffic through the WAF and Gravitee:

```bash
bash scripts/traffic.sh
```

`scripts/traffic.sh` sends 19 requests:
- 8 valid `POST /orders` (`201`) exercising Redis `SET NX EX`, outbound WAF → `payments-api` (`200`), PostgreSQL `INSERT`, outbound Gravitee → `inventory-api` (`200`), and Solace PubSub+ SMF Direct publish (`orders/created`).
- 1 duplicate `POST /orders` (`409`) rejected by Redis `SET NX` idempotency.
- 1 `POST /orders` (`422`) whose customer note trips the outbound ModSecurity / OWASP CRS PL2 rules (`403`) on `payments.partner.local`, causing `orders-api` to release its Redis key (`DEL`) and return `422 payment_rejected_by_waf`.
- 1 invalid `POST /orders` (`400`) rejected by input validation.
- 4 `GET /orders/{id}` (`200`) exercising Redis cache `MISS` → PostgreSQL `SELECT` → Redis `SETEX`, plus a Redis cache `HIT` (`?view=cached`).
- 2 distinct `POST /orders/{id}/cancel` (`200`, `"consumedVia":"solace-smf"`) exercising Solace PubSub+ native SMF `subscribe`, `publish`, and broker-pushed `consume` on `orders/cancelled/<orderId>`, followed by PostgreSQL `UPDATE` and Redis `DEL`.
- 1 post-cancel `GET /orders/{id}?afterCancel=1` (`200`, `"status":"CANCELLED"`) and 1 `GET /orders/ORD-DOESNOTEXIST` (`404`).

4. Click **Stop Recording** in the Keploy Console. Keploy drains the persistent SMF, PostgreSQL, and Redis sockets cleanly and automatically runs sandbox auto-replay.

## 7. Inspect recorded tests and mocks

Open the newest recording for `orders/orders-api` in the Keploy Console. With **Static deduplication** enabled, Keploy condenses the 19 requests into **13 tests** (collapsing repeated `POST /orders` creates and the duplicate `GET /orders/ORD-SEED-1001?view=cached` response shape) backed by **all five mock kinds**:

- **Solace (`Solace`):** Native SMF v3 (`:55555`) session `login`, `subscribe` (`orders/cancelled/<orderId>`), `control`, deduplicated `keepalive`, `publish` (`orders/created` and `orders/cancelled/<orderId>` with JSON payloads and correlation IDs), and broker-pushed **`consume`** (`orders/cancelled/<orderId>`).
- **PostgreSQL (`PostgresV3`):** `INSERT INTO orders ...`, `SELECT ... FROM orders WHERE order_id = $1` (with returned rows or 0 rows on `404`), and `UPDATE orders SET status = 'CANCELLED' WHERE order_id = $1` (recorded wire SQL; during replay mismatch reporting Keploy normalizes SQL literals to positional placeholders such as `$2`, `$3`).
- **Redis (`Redis`):** `SET idempotency:... NX EX 3600` (`OK` and `nil` for the `409` duplicate), `GET order:...`, `SETEX order:... 300`, and `DEL` on WAF-403 cleanup and cancellation.
- **HTTP (`Http`) — Gravitee outbound:** `POST /inventory/reserve` through `gravitee-gateway.gateway.svc.cluster.local:82` (`inventory-api` validates `X-Api-Key`, while Gravitee adds `X-Gravitee-Transaction-Id`, `X-Served-By: gravitee-inventory`, and `X-Gateway-Api: Inventory API`). Inbound `POST /orders` tests also preserve `X-Gateway: gravitee-apim-4.9`, `X-Api-Plan: keyless`, and `X-Gravitee-Transaction-Id`.
- **HTTP (`Http`) — ModSecurity / OWASP CRS WAF outbound:** `POST /payments/authorize` (`Host: payments.partner.local`) showing both `200 AUTHORIZED` and the **`403 Forbidden`** WAF block verdict that drives the `422` response. Inbound attacks from `scripts/attack.sh` never reach `orders-api` and never pollute the recording.
- **DNS (`DNS`):** Cluster service name lookups.

## 8. Auto-replay and regression scenarios

### Baseline auto-replay (`orders-api:1.0`)

Auto-replay spins up a sandbox Pod with a deny-egress `NetworkPolicy` so Solace PubSub+, PostgreSQL, Redis, Gravitee/`inventory-api`, and WAF/`payments-api` are all unreachable and answered purely from mocks. Expected result: **13 passed, 0 failed**.

### Multi-dependency regression (`orders-api:2.0`)

In the Console, click **Replay** on `orders/orders-api`, click **Next**, set **Replay image (optional)** to `orders-api:2.0` (leave **Strict mock noise** off), and click **Start Replay**.

Expected result: **8 passed, 5 failed** — the same three `POST /orders` tests (`201 → 502`) each report both the Gravitee and Solace PubSub+ diffs, and the two `POST /orders/{id}/cancel` tests (`200 → 500`) report the PostgreSQL diff:
- **Gravitee → `inventory-api`** (`POST /orders`, 3 tests): `body.warehouse: recorded "EU-WEST-1", absent in live call; body.fulfillmentCenter: live "EU-WEST-1", absent in recording`.
- **Solace PubSub+ SMF** (`POST /orders`, same 3 tests): `body.eventVersion: recorded "1", live "2"; body.totalAmount: recorded "...", absent in live call; body.amount: live "...", absent in recording`.
- **PostgreSQL** (`POST /orders/{id}/cancel`, 2 tests): `UPDATE orders SET status = $2, note = $3 WHERE order_id = $1 (no_schema_candidates)` (Keploy normalizes SQL literals to positional placeholders).

### Default versus strict mock noise (`orders-api:2.1`)

1. Click **Replay** → **Next**, set **Replay image (optional)** to `orders-api:2.1`, leave **Strict mock noise** OFF, and click **Start Replay**: **13 passed, 0 failed** (`201 → 201`), while showing the outgoing Solace PubSub+ diff (`body.channel: recorded "web", live "WEB"`).
2. Click **Replay** → **Next**, set **Replay image (optional)** to `orders-api:2.1`, turn **Strict mock noise** ON, and click **Start Replay**: **10 passed, 3 failed** — the three `POST /orders` tests fail purely on the Solace PubSub+ value drift (`body.channel`: `"web"` → `"WEB"`, `"mobile"` → `"MOBILE"`), even though every HTTP status is still `201`.

### Intended response change (`orders-api:2.2`)

Click **Replay** → **Next**, set **Replay image (optional)** to `orders-api:2.2` (**Strict mock noise** OFF), and click **Start Replay**: **11 passed, 2 failed** — only the two `POST /orders/{id}/cancel` tests (`200 → 200`) fail, each showing `"status": "CANCELLED"` → `"REFUND_PENDING"` while every dependency call matches.

## 9. Local cloud replay and triage

Authenticate the [Keploy Enterprise CLI](https://keploy.io/docs/keploy-cloud/cloud-installation/) once (`keploy login`, or export `KEPLOY_API_KEY="<YOUR_API_KEY>"`), then replay the cloud-stored recording locally against any local Docker image. Omit `--test-sets` to replay every test set recorded for the app, or pass `--test-sets "<TEST_SET_ID>"` using the Test Set ID shown on the Console recording page:

```bash
# Baseline replay locally in Docker
keploy cloud replay --app orders.orders-api --cluster keploy-k8s-sample

# Reproduce the 2.0 multi-dependency regression locally
keploy cloud replay --app orders.orders-api --cluster keploy-k8s-sample --image orders-api:2.0

# Reproduce the 2.2 intended cancel-status change locally
keploy cloud replay --app orders.orders-api --cluster keploy-k8s-sample --image orders-api:2.2
```

When a replay fails, copy `<RUN_ID>` (the ID between `/tr/` and `?appId=` in the report link printed at the end of the replay output) and run `keploy cloud triage`:

```bash
keploy cloud triage --app orders.orders-api --cluster keploy-k8s-sample --run <RUN_ID>
```

For `orders-api:2.0`, triage groups the five failures into two dependency groups (`CALL_CHANGED` on `POST /orders` for Gravitee + Solace PubSub+, and `CALL_ADDED` on `POST /orders/{id}/cancel` for PostgreSQL). For `orders-api:2.2`, triage groups both cancel failures under `RESPONSE_CHANGED` (`2 tests on POST /orders/{id}/cancel`, `$.status: "CANCELLED" → "REFUND_PENDING"`).

## 10. Cleanup

Reset application state between demo runs:

```bash
bash scripts/reset.sh
```

When you are finished with the sample, uninstall `k8s-proxy`, delete the Kind cluster, remove local CLI replay artifacts, and delete the `keploy-k8s-sample` entry under **Clusters** in [app.keploy.io](https://app.keploy.io/clusters):

```bash
helm uninstall k8s-proxy -n keploy
kind delete cluster --name keploy-k8s-sample
rm -rf keploy keploy.yml
```

## Third-party components and licences

This sample pulls third-party container images and libraries subject to their own licences:

- **Solace PubSub+ Standard** (`solace/solace-pubsub-standard:10.26.0`) and **`com.solacesystems:sol-jcsmp:10.24.0`** are proprietary free-to-use software distributed under the [Solace Corporation License Agreement](https://solace.com/license-software), not open source. Pulling the Solace container image or building `orders-api` with `sol-jcsmp` requires accepting Solace's licence terms.
- **Gravitee APIM Gateway CE** (`graviteeio/apim-gateway`), **ModSecurity v3**, **OWASP CRS**, and **`ingress-nginx`** are licensed under **Apache-2.0**.
- **PostgreSQL 16** (`postgres:16.16-alpine`) is licensed under the **PostgreSQL License**.
- **Redis 7.2** (`redis:7.2.17-alpine`) is licensed under **BSD-3-Clause**.
