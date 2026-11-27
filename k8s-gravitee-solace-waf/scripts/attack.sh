#!/usr/bin/env bash
# Sends malicious requests against the edge. ModSecurity / OWASP CRS answers 403; nothing reaches Gravitee or orders-api.
# Also shows the outbound partner WAF verdict: an order with a SQL-comment note passes inbound CRS PL1
# but is blocked (403) by the stricter partner payments WAF (PL2) -> orders-api answers 422.
# Usage: bash scripts/attack.sh [EDGE_URL]
set -euo pipefail
EDGE=${1:-http://localhost}
show() { printf '%-4s %s\n' "$1" "$2"; }
START_TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)

echo "=== 1. Inbound WAF blocks (never reach Gravitee or orders-api) ==="
show "$(curl -s -o /dev/null -w '%{http_code}' "$EDGE/orders/ORD-1?customer=1%27%20OR%20%271%27%3D%271")" \
  "SQL injection in query:  /orders/ORD-1?customer=1' OR '1'='1"
show "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$EDGE/orders" -H 'Content-Type: application/json' \
  -d '{"customerId":"C-1001","reference":"<script>alert(document.cookie)</script>","items":[{"sku":"SKU-1","quantity":1,"unitPrice":1}]}')" \
  "XSS in JSON body:        reference=<script>alert(document.cookie)</script>"
show "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$EDGE/orders" -H 'Content-Type: application/json' \
  -d '{"customerId":"C-1001 UNION SELECT password FROM users--","reference":"x","items":[{"sku":"SKU-1","quantity":1,"unitPrice":1}]}')" \
  "SQL injection in body:   customerId=C-1001 UNION SELECT password FROM users--"
show "$(curl -s -o /dev/null -w "%{http_code}" "$EDGE/orders/ORD-1?file=..%2f..%2f..%2fetc%2fpasswd")" \
  "Path traversal in query: /orders/ORD-1?file=../../../etc/passwd"

echo "--- ModSecurity / OWASP CRS rule IDs that fired:"
kubectl -n ingress-nginx logs deploy/ingress-nginx-controller --since-time="$START_TS" 2>/dev/null \
  | grep -o '"ruleId":"9[0-9]*"\|"message":"[^"]*"' | grep -v -i 'anomaly\|Inbound' | sort | uniq -c | sort -rn | head -8 || true

echo "--- orders-api logs during inbound attacks (none expected):"
kubectl -n orders logs deploy/orders-api --since-time="$START_TS" 2>/dev/null | tail -3 || true
echo "(end of log)"

echo
echo "=== 2. Outbound WAF verdict (payments.partner.local PL2 blocks -> 403 -> orders-api 422) ==="
RUN=$(date +%H%M%S)
out=$(curl -s -w '\n%{http_code}' -X POST "$EDGE/orders" -H 'Content-Type: application/json' \
  -d '{"customerId":"C-3099","reference":"PO-ATK-'"$RUN"'","currency":"EUR","note":"rush -- check 100/10","items":[{"sku":"SKU-MOUSE-01","quantity":1,"unitPrice":24.90}]}')
show "${out##*$'\n'}" "Outbound partner WAF verdict -> ${out%$'\n'*}"
