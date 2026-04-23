#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-1000}"

endpoints=(
  "/"
  "/someone"
  "/noone"
  "/everyone"
  "/status"
  "/everything"
  "/somewhere"
  "/api/v1/users"
  "/api/v2/data"
  "/somebody"
  "/ping"
  "/healthz"
  "/info"
  "/anything"
  "/nothing"
  "/nowhere"
  "/products"
  "/search?q=test&limit=5"
  "/items"
  "/api/v1/data"
  "/api/v2/users"
  "/system/logs"
  "/system/metrics"
  "/proxy"
  "/anybody"
  "/everybody"
  "/user/123/profile"
)

echo "--- Running randomized Java dedup endpoints ${TOTAL_REQUESTS} times ---"
echo "Base URL: ${BASE_URL}"
echo "Endpoint count: ${#endpoints[@]}"

success_count=0
error_count=0
start_time="$(date +%s)"

for i in $(seq 1 "${TOTAL_REQUESTS}"); do
  random_index=$((RANDOM % ${#endpoints[@]}))
  selected_endpoint="${endpoints[$random_index]}"
  status_code="$(curl -X GET "${BASE_URL}${selected_endpoint}" -s -o /dev/null -w "%{http_code}")"

  if [[ "${status_code}" == "200" ]]; then
    success_count=$((success_count + 1))
  else
    error_count=$((error_count + 1))
    echo "ERROR: request ${i} failed with status ${status_code} for endpoint ${selected_endpoint}"
  fi

  if (( i % 100 == 0 )); then
    echo "Completed ${i} requests... (Success: ${success_count}, Errors: ${error_count})"
  fi
done

end_time="$(date +%s)"
duration=$((end_time - start_time))
if (( duration == 0 )); then
  requests_per_second="${TOTAL_REQUESTS}+"
else
  requests_per_second=$((TOTAL_REQUESTS / duration))
fi

echo
echo "--- Performance Summary ---"
echo "Total requests: ${TOTAL_REQUESTS}"
echo "Successful requests (200): ${success_count}"
echo "Failed requests: ${error_count}"
echo "Success rate: $(((success_count * 100) / TOTAL_REQUESTS))%"
echo "Total time: ${duration}s"
echo "Requests per second: ${requests_per_second}"
echo "--- Complete ---"

if (( error_count > 0 )); then
  exit 1
fi
