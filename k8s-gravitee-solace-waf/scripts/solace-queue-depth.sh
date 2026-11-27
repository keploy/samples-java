#!/usr/bin/env bash
# Shows how many order events are spooled on the Solace PubSub+ orders-audit queue.
set -euo pipefail
kubectl -n messaging exec deploy/solace -- curl -s -u admin:admin \
  'http://localhost:8080/SEMP/v2/monitor/msgVpns/default/queues/orders-audit?select=queueName,spooledMsgCount' \
  | python3 -c 'import json,sys; d=json.load(sys.stdin)["data"]; print("queue", d["queueName"], "spooled messages:", d.get("spooledMsgCount"))'
