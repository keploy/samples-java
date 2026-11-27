#!/usr/bin/env bash
# Resets orders-api, Redis, and PostgreSQL back to the clean two-seed-order state between runs.
set -euo pipefail
kubectl -n orders set image deploy/orders-api orders-api=orders-api:1.0
kubectl -n data exec deploy/redis -- redis-cli FLUSHALL
kubectl -n data exec deploy/postgres -- psql -U orders -d orders -c \
  "DELETE FROM orders WHERE order_id NOT IN ('ORD-SEED-1001','ORD-SEED-1002'); UPDATE orders SET status='CREATED' WHERE order_id IN ('ORD-SEED-1001','ORD-SEED-1002');"
kubectl -n orders rollout restart deploy/orders-api
kubectl -n orders rollout status deploy/orders-api --timeout=180s
sleep 2
