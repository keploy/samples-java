#!/usr/bin/env bash
# Configures the orders-audit queue on the Solace PubSub+ default VPN (idempotent):
#   orders-audit subscribed to orders/> (spools OrderCreated and OrderCancelled events)
set -euo pipefail
S=(kubectl -n messaging exec deploy/solace -- curl -s -u admin:admin -H content-type:application/json)
BASE="http://localhost:8080/SEMP/v2/config/msgVpns/default"

req() {
  local label=$1; shift
  local resp status
  resp=$("${S[@]}" "$@")
  status=$(printf '%s' "$resp" | grep -o '"responseCode":[0-9]*\|ALREADY_EXISTS' | tr '\n' ' ' || true)
  printf '%s: %s\n' "$label" "${status:-no-response}"
  printf '%s' "$status" | grep -q -E '"responseCode":200|ALREADY_EXISTS'
}

req "queue orders-audit" -X POST "$BASE/queues" \
  -d '{"queueName":"orders-audit","accessType":"exclusive","ingressEnabled":true,"egressEnabled":true,"permission":"consume"}'
req "sub orders-audit" -X POST "$BASE/queues/orders-audit/subscriptions" \
  -d '{"subscriptionTopic":"orders/>"}'
