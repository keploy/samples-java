#!/usr/bin/env bash
# Traffic generator for Keploy recording.
#
# Drives the Product Catalog API through a rich, realistic workload so the recorded
# Keploy suite is broad: ~60+ test cases covering the full CRUD lifecycle across several
# categories, filtering, the inventory summary, stock adjustments, and a wide range of
# 400 (validation), 404 (not-found), and 409 (insufficient-stock) paths.
# Ids returned by POST are chained into later GET/PUT/DELETE calls so the suite is coherent.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"

# --- helpers -----------------------------------------------------------------
# grep -m1 stops after the first match and exits 0 (no SIGPIPE from a downstream `head`),
# so it stays well-behaved under `set -e` + `pipefail`.
id_of() { grep -m1 -o '"id":[0-9]*' | cut -d: -f2; }

CREATED=()   # ids of products created, in order

# create '<json>'  -> prints response, appends new id to CREATED
create() {
  local json="$1" body id
  if ! body=$(curl -fsS -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d "$json"); then
    echo "  ERROR: create request failed for: $json" >&2
    exit 1
  fi
  echo "  created: $body"
  id=$(printf '%s' "$body" | id_of || true)
  if [ -z "$id" ]; then
    echo "  ERROR: no id in create response: $body" >&2
    exit 1
  fi
  CREATED+=("$id")
}

# expect '<label>' '<curl args...>'  -> runs curl, prints status + body, never aborts
expect() {
  local label="$1"; shift
  local out code
  out=$(curl -s -w $'\n%{http_code}' "$@")
  code=$(printf '%s' "$out" | tail -1)
  body=$(printf '%s' "$out" | sed '$d')
  echo "  [$label] status=$code body=$body"
}

# --- wait for readiness (real endpoint, not /actuator/health, to keep traffic clean) --
echo "==> Waiting for app at $BASE ..."
for i in $(seq 1 60); do
  if curl -fsS "$BASE/api/products" >/dev/null 2>&1; then echo "app is up"; break; fi
  sleep 2
  [ "$i" = 60 ] && { echo "app never became ready at $BASE after ~120s" >&2; exit 1; }
done

echo "==> PHASE 1: create a catalog across multiple categories"
create '{"name":"Mechanical Keyboard","description":"65% hot-swappable","price":129.99,"stockQuantity":40,"category":"peripherals"}'
create '{"name":"USB-C Hub","description":"7-in-1 aluminium","price":39.50,"stockQuantity":100,"category":"peripherals"}'
create '{"name":"Wireless Mouse","description":"ergonomic, 2.4GHz","price":24.99,"stockQuantity":150,"category":"peripherals"}'
create '{"name":"27-inch 4K Monitor","description":"IPS, 60Hz","price":329.00,"stockQuantity":25,"category":"monitors"}'
create '{"name":"34-inch Ultrawide Monitor","description":"curved, 144Hz","price":599.00,"stockQuantity":12,"category":"monitors"}'
create '{"name":"Laptop Stand","description":"aluminium, adjustable","price":34.95,"stockQuantity":80,"category":"accessories"}'
create '{"name":"Noise-Cancelling Headphones","description":"over-ear, BT 5.3","price":199.99,"stockQuantity":60,"category":"audio"}'
create '{"name":"1080p Webcam","description":"auto-focus","price":59.00,"stockQuantity":45,"category":"peripherals"}'
create '{"name":"External SSD 1TB","description":"USB 3.2 Gen2","price":109.99,"stockQuantity":70,"category":"storage"}'
create '{"name":"Desk Mat XL","description":"900x400mm","price":19.99,"stockQuantity":200,"category":"accessories"}'
create '{"name":"USB Microphone","description":"cardioid, plug-and-play","price":89.00,"stockQuantity":35,"category":"audio"}'
create '{"name":"14-inch Laptop","description":"16GB RAM, 512GB SSD","price":1099.00,"stockQuantity":15,"category":"computers"}'
echo "  -> created ids: ${CREATED[*]}"

echo "==> PHASE 2: read the whole catalog"
curl -fsS "$BASE/api/products"; echo

echo "==> PHASE 3: get every product by id"
for id in "${CREATED[@]}"; do
  echo "  GET /api/products/$id"
  curl -fsS "$BASE/api/products/$id"; echo
done

echo "==> PHASE 4: filter by each category"
for cat in peripherals monitors accessories audio storage computers gaming; do
  echo "  GET /api/products?category=$cat"
  curl -fsS "$BASE/api/products?category=$cat"; echo
done

echo "==> PHASE 5: updates (PUT) then re-read"
KB=${CREATED[0]}; MON=${CREATED[3]}; HP=${CREATED[6]}
curl -fsS -X PUT "$BASE/api/products/$KB" -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical Keyboard v2","description":"75% layout, RGB","price":149.99,"stockQuantity":35,"category":"peripherals"}'; echo
curl -fsS "$BASE/api/products/$KB"; echo
curl -fsS -X PUT "$BASE/api/products/$MON" -H 'Content-Type: application/json' \
  -d '{"name":"27-inch 4K Monitor","description":"IPS, 60Hz, HDR400","price":299.00,"stockQuantity":30,"category":"monitors"}'; echo
curl -fsS "$BASE/api/products/$MON"; echo
curl -fsS -X PUT "$BASE/api/products/$HP" -H 'Content-Type: application/json' \
  -d '{"name":"Noise-Cancelling Headphones Pro","description":"over-ear, BT 5.3, ANC+","price":249.99,"stockQuantity":50,"category":"audio"}'; echo
curl -fsS "$BASE/api/products/$HP"; echo

echo "==> PHASE 6: deletes then verify"
DEL1=${CREATED[2]}; DEL2=${CREATED[9]}   # Wireless Mouse, Desk Mat XL
curl -fsS -o /dev/null -w "  delete $DEL1 status=%{http_code}\n" -X DELETE "$BASE/api/products/$DEL1"
curl -fsS -o /dev/null -w "  delete $DEL2 status=%{http_code}\n" -X DELETE "$BASE/api/products/$DEL2"
echo "  LIST after deletes:"; curl -fsS "$BASE/api/products"; echo
echo "  FILTER peripherals after deleting the mouse:"; curl -fsS "$BASE/api/products?category=peripherals"; echo

echo "==> PHASE 7: 404 not-found paths"
expect "GET missing"    "$BASE/api/products/99999"
expect "GET deleted"    "$BASE/api/products/$DEL1"
expect "PUT missing"    -X PUT "$BASE/api/products/99999" -H 'Content-Type: application/json' \
  -d '{"name":"ghost","price":1.00,"stockQuantity":1,"category":"none"}'
expect "DELETE missing" -X DELETE "$BASE/api/products/88888"

echo "==> PHASE 8: 400 validation paths"
expect "blank name"        -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"","price":10.00,"stockQuantity":5,"category":"x"}'
expect "missing name"      -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"price":10.00,"stockQuantity":5,"category":"x"}'
expect "negative price"    -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"Bad","price":-5.00,"stockQuantity":5,"category":"x"}'
expect "zero price"        -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"Bad","price":0,"stockQuantity":5,"category":"x"}'
expect "missing price"     -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"Bad","stockQuantity":5,"category":"x"}'
expect "missing stock"     -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"Bad","price":10.00,"category":"x"}'
expect "negative stock"    -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"Bad","price":10.00,"stockQuantity":-3,"category":"x"}'
expect "multi-field fail"  -X POST "$BASE/api/products" -H 'Content-Type: application/json' -d '{"name":"","price":-1}'
expect "invalid PUT"       -X PUT "$BASE/api/products/$KB" -H 'Content-Type: application/json' -d '{"name":"","price":-9,"stockQuantity":-1}'

echo "==> PHASE 9: inventory summary + stock adjustments"
echo "  GET /api/products/summary (default threshold):"; curl -fsS "$BASE/api/products/summary"; echo
echo "  GET /api/products/summary?lowStockThreshold=15:"; curl -fsS "$BASE/api/products/summary?lowStockThreshold=15"; echo
echo "  PATCH stock $KB -5 (ship units):"
curl -fsS -X PATCH "$BASE/api/products/$KB/stock" -H 'Content-Type: application/json' -d '{"delta":-5}'; echo
echo "  PATCH stock $KB +100 (restock):"
curl -fsS -X PATCH "$BASE/api/products/$KB/stock" -H 'Content-Type: application/json' -d '{"delta":100}'; echo
expect "stock over-decrement 409" -X PATCH "$BASE/api/products/$KB/stock" -H 'Content-Type: application/json' -d '{"delta":-99999}'
expect "stock on missing 404"     -X PATCH "$BASE/api/products/99999/stock" -H 'Content-Type: application/json' -d '{"delta":1}'

echo "==> seed traffic complete — created ${#CREATED[@]} products, exercised CRUD + filters + summary + stock + 404 + 400"
