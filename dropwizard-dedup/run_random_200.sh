#!/usr/bin/env bash
set -Eeuo pipefail

# Drives the dropwizard-dedup sample with a varied request mix so that
# `keploy record` ends up with ~200 testcases that exercise every
# resource path. Mirrors java-dedup/run_random_1000.sh.
#
# Usage (during a keploy record session):
#   bash run_random_200.sh

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-200}"

categories=(books electronics home outdoor)
skus_ok=(BK-1 BK-2 EL-1 EL-2 HM-1 HM-2 OD-1 OD-2)
skus_missing=(MISSING NOPE ZZ-9 FOO X-1)
search_terms=(phone book tent speaker kettle lamp knife)
sorts=(relevance price popularity newest)
file_paths=(
  "reports/2026/q1.csv"
  "reports/2025/q4.csv"
  "exports/users.json"
  "logs/app.log"
  "media/banner.png"
  "exports/orders/2026-04.csv"
)
order_ids=(ORD-1 ORD-42 ORD-100 ORD-PRIORITY ORD-X9 ORD-7)
regions=(us-east us-west eu-central ap-south)
zones=(az1 az2 az3)
tenants=(acme globex umbrella soylent)
request_ids=(req-001 req-002 req-abc req-xyz missing)
event_types=(signup login purchase logout)
customers=(alice bob carol dave)

pick() { local -n a=$1; echo "${a[$((RANDOM % ${#a[@]}))]}"; }

requests=()
for _ in $(seq 1 8); do requests+=("GET /healthz"); done
for c in "${categories[@]}"; do for l in 1 2 3 5; do requests+=("GET /catalog?category=$c&limit=$l"); done; done
requests+=("GET /catalog")
for sku in "${skus_ok[@]}"; do for _ in 1 2 3; do requests+=("GET /catalog/$sku"); done; done
for sku in "${skus_missing[@]}"; do requests+=("GET /catalog/$sku"); done
for t in "${search_terms[@]}"; do for s in "${sorts[@]}"; do requests+=("GET /search?term=$t&sort=$s"); done; done
for fp in "${file_paths[@]}"; do for d in true false; do requests+=("GET /files/$fp?download=$d"); done; done
for t in "${tenants[@]}"; do for r in "${request_ids[@]}"; do requests+=("HEADERS $t $r"); done; done
for r in "${regions[@]}"; do for z in "${zones[@]}"; do requests+=("GET /platform/routes/$r/$z"); done; done
for _ in 1 2 3 4; do requests+=("GET /platform/content/html"); done
for t in "${event_types[@]}"; do requests+=("EVENT $t"); done
for cust in "${customers[@]}"; do for sku in BK-1 BK-2 EL-1 EL-2; do for prio in true false; do requests+=("ORDER $cust $sku $prio"); done; done; done
for oid in "${order_ids[@]}"; do for ex in true false; do requests+=("GET /orders/$oid?expand=$ex"); done; done
for oid in "${order_ids[@]:0:4}"; do for st in shipped delivered cancelled; do requests+=("PUT /orders/$oid $st"); done; done
for oid in "${order_ids[@]:0:5}"; do requests+=("DELETE /orders/$oid"); done

# Trim or pad to TOTAL_REQUESTS
while (( ${#requests[@]} < TOTAL_REQUESTS )); do requests+=("GET /healthz"); done
requests=("${requests[@]:0:$TOTAL_REQUESTS}")

issued=0
for spec in "${requests[@]}"; do
  set -- $spec
  case "$1" in
    GET)
      curl -s -o /dev/null -w "%{http_code} GET %{url_effective}\n" "$BASE_URL$2"
      ;;
    PUT)
      curl -s -o /dev/null -w "%{http_code} PUT %{url_effective}\n" -X PUT \
        -H 'Content-Type: application/json' -d "{\"status\":\"$3\"}" "$BASE_URL$2"
      ;;
    DELETE)
      curl -s -o /dev/null -w "%{http_code} DELETE %{url_effective}\n" -X DELETE "$BASE_URL$2"
      ;;
    HEADERS)
      curl -s -o /dev/null -w "%{http_code} GET %{url_effective}\n" \
        -H "X-Tenant: $2" -H "X-Request-Id: $3" "$BASE_URL/headers"
      ;;
    EVENT)
      curl -s -o /dev/null -w "%{http_code} POST %{url_effective}\n" -X POST \
        -H 'Content-Type: application/json' \
        -d "{\"type\":\"$2\",\"actor\":\"user\",\"ts\":\"2026-04-30T00:00:00Z\"}" \
        "$BASE_URL/platform/events"
      ;;
    ORDER)
      curl -s -o /dev/null -w "%{http_code} POST %{url_effective}\n" -X POST \
        -H 'Content-Type: application/json' \
        -d "{\"customer\":\"$2\",\"sku\":\"$3\",\"quantity\":2,\"priority\":$4}" \
        "$BASE_URL/orders"
      ;;
  esac
  issued=$((issued + 1))
done

echo "issued $issued requests"
