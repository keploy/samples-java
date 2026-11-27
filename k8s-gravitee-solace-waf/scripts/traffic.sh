#!/usr/bin/env bash
# Drives realistic order traffic through the full edge path:
#   ModSecurity / OWASP CRS WAF (ingress-nginx) -> Gravitee -> orders-api
#
# Exercises every dependency:
#   - Redis: SET NX EX idempotency (201 + 409 duplicate), GET cache (MISS -> PostgreSQL -> SETEX, then HIT), DEL on cancel
#   - ModSecurity / OWASP CRS WAF outbound -> payments-api: 200 authorized + 403 blocked egress -> 422 payment_rejected_by_waf
#   - PostgreSQL: INSERT on create, SELECT on GET, UPDATE on cancel
#   - Gravitee outbound -> inventory-api: POST /inventory/reserve (with X-Api-Key + Gravitee headers)
#   - Solace PubSub+ native SMF v3 (:55555): publish OrderCreated (orders/created) + subscribe/publish/consume
#     OrderCancelled (orders/cancelled via POST /orders/{id}/cancel -> PostgreSQL UPDATE + Redis DEL)
#
# Usage: bash scripts/traffic.sh [EDGE_URL]
set -euo pipefail
EDGE=${1:-http://localhost}
RUN=${RUN:-$(date +%H%M%S)}
skus=(SKU-LAPTOP-14 SKU-MOUSE-01 SKU-DOCK-USB4 SKU-MONITOR-27 SKU-KEYBOARD-MX SKU-HEADSET-BT)
prices=(1299.00 24.90 189.50 349.99 109.00 79.95)
curs=(EUR EUR USD EUR GBP INR)
chans=(web mobile partner web mobile web)
ids=()
first_body=""

post() {
  local out code body id
  out=$(curl -s -w '\n%{http_code}' -X POST "$EDGE/orders" -H 'Content-Type: application/json' -H "X-Sales-Channel: $2" -d "$1")
  code=${out##*$'\n'}; body=${out%$'\n'*}
  printf '%-4s POST /orders  %s\n' "$code" "$(printf '%s' "$body" | cut -c1-125)"
  id=$(printf '%s' "$body" | sed -n 's/.*"orderId":"\([^"]*\)".*/\1/p')
  if [ "$code" = 201 ] && [ -n "$id" ]; then
    ids+=("$id")
  fi
}

# 1. Eight normal orders (201): Redis SETNX -> WAF->payments 200 -> PostgreSQL INSERT -> Gravitee->inventory 200 -> Solace SMF publish
for n in $(seq 0 7); do
  i=$((n % 6)); j=$(((n + 2) % 6)); q=$((n % 3 + 1))
  body=$(printf '{"customerId":"C-%d","reference":"PO-%s-%02d","currency":"%s","note":"standard delivery","items":[{"sku":"%s","quantity":%d,"unitPrice":%s},{"sku":"%s","quantity":1,"unitPrice":%s}]}' \
    $((1001 + n)) "$RUN" "$n" "${curs[$i]}" "${skus[$i]}" "$q" "${prices[$i]}" "${skus[$j]}" "${prices[$j]}")
  [ "$n" = 0 ] && first_body="$body"
  post "$body" "${chans[$i]}"
  sleep 0.3
done

# 2. Duplicate order (same customerId + reference as order 0) -> 409 from Redis SET NX idempotency
post "$first_body" "${chans[0]}"

# 3. Outbound WAF-blocked payment: benign inbound note that trips the stricter partner payments WAF (CRS PL2 -> 403 -> app 422)
post '{"customerId":"C-2099","reference":"PO-'"$RUN"'-WAF","currency":"EUR","note":"rush -- check 100/10","items":[{"sku":"SKU-MOUSE-01","quantity":1,"unitPrice":24.90}]}' web

# 4. Business-rule rejection (quantity 0) -> 400 from orders-api, no dependencies touched
post '{"customerId":"C-2001","reference":"PO-'"$RUN"'-BAD","currency":"EUR","items":[{"sku":"SKU-MOUSE-01","quantity":0,"unitPrice":24.90}]}' web

# 5. Reads:
#   - first order created this run (Redis MISS -> PostgreSQL SELECT -> Redis SETEX)
#   - ORD-SEED-1001 first read (Redis MISS -> PostgreSQL SELECT -> Redis SETEX)
#   - ORD-SEED-1001 second read (Redis HIT — served purely from Redis)
#   - ORD-SEED-1002 before cancel (status CREATED)
first_id="${ids[0]:-ORD-SEED-1001}"
for url in "/orders/$first_id" "/orders/ORD-SEED-1001" "/orders/ORD-SEED-1001?view=cached" "/orders/ORD-SEED-1002"; do
  out=$(curl -s -w '\n%{http_code}' "$EDGE$url")
  code=${out##*$'\n'}; body=${out%$'\n'*}
  printf '%-4s GET  %-36s %s\n' "$code" "$url" "$(printf '%s' "$body" | grep -o '"status":"[^"]*"\|"cache":"[^"]*"' | tr '\n' ' ')"
done

# 6. Solace PubSub+ native SMF publish + consume path (two distinct cancel shapes):
#    POST /orders/{id}/cancel -> orders-api subscribes to orders/cancelled via SMF, publishes OrderCancelled,
#    consumes the broker-pushed SMF delivery, updates PostgreSQL (CANCELLED) + evicts Redis cache.
cancel_id="${ids[1]:-ORD-SEED-1002}"
out=$(curl -s -w '\n%{http_code}' -X POST "$EDGE/orders/$cancel_id/cancel" -H 'Content-Type: application/json' -d '{"reason":"customer_requested"}')
code=${out##*$'\n'}; body=${out%$'\n'*}
printf '%-4s POST %-36s %s\n' "$code" "/orders/$cancel_id/cancel" "$(printf '%s' "$body" | cut -c1-120)"

out=$(curl -s -w '\n%{http_code}' -X POST "$EDGE/orders/ORD-SEED-1002/cancel" -H 'Content-Type: application/json' -d '{"reason":"duplicate_purchase","initiatedBy":"support"}')
code=${out##*$'\n'}; body=${out%$'\n'*}
printf '%-4s POST %-36s %s\n' "$code" "/orders/ORD-SEED-1002/cancel" "$(printf '%s' "$body" | cut -c1-120)"

out=$(curl -s -w '\n%{http_code}' "$EDGE/orders/$cancel_id?afterCancel=1")
code=${out##*$'\n'}; body=${out%$'\n'*}
printf '%-4s GET  %-36s %s\n' "$code" "/orders/$cancel_id?afterCancel=1" "$(printf '%s' "$body" | grep -o '"status":"[^"]*"\|"cache":"[^"]*"' | tr '\n' ' ')"

# 7. Unknown order -> 404 (Redis MISS -> PostgreSQL SELECT 0 rows)
printf '%-4s GET  /orders/ORD-DOESNOTEXIST\n' "$(curl -s -o /dev/null -w '%{http_code}' "$EDGE/orders/ORD-DOESNOTEXIST")"
