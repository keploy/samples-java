#!/usr/bin/env bash
#
# run-and-measure.sh — bring restheart-mongo up under the
# coverage overlay (JaCoCo agent attached via JAVA_TOOL_OPTIONS),
# run flow.sh bootstrap + record-traffic, dump JaCoCo execution
# data over the agent's TCP server, render a Java line-coverage
# report, and emit `coverage=PCT` onto $GITHUB_OUTPUT for the
# downstream coverage-gate job.
#
# Coverage isolation contract:
#   * Base `Dockerfile` and `docker-compose.yml` are untouched.
#   * The overlay `Dockerfile.coverage` + `docker-compose.coverage.yml`
#     attach JaCoCo and expose its TCP server. ONLY this script
#     applies the overlay; keploy/integrations and keploy/enterprise
#     CI lanes consume the base compose and pay zero JVM-instrument
#     cost (jacocoagent adds ~5-10% per-call overhead).
#
# Inputs (from the workflow env):
#   RESTHEART_PHASE   — label for log diffing.
#   GITHUB_OUTPUT     — standard GH Actions sink for step outputs.
set -Eeuo pipefail

export RESTHEART_APP_CONTAINER="${RESTHEART_APP_CONTAINER:-restheart_app}"
export RESTHEART_MONGO_CONTAINER="${RESTHEART_MONGO_CONTAINER:-restheart_mongo}"
export RESTHEART_APP_PORT="${RESTHEART_APP_PORT:-8080}"
export RESTHEART_MONGO_IP="${RESTHEART_MONGO_IP:-172.36.0.10}"
export RESTHEART_NETWORK_SUBNET="${RESTHEART_NETWORK_SUBNET:-172.36.0.0/24}"
export RESTHEART_ADMIN_AUTH="${RESTHEART_ADMIN_AUTH:-Basic YWRtaW46c2VjcmV0}"

mkdir -p coverage
chmod 777 coverage
sudo rm -rf coverage/jacoco.exec coverage/report.xml coverage/coverage_report.txt 2>/dev/null \
    || rm -rf coverage/jacoco.exec coverage/report.xml coverage/coverage_report.txt 2>/dev/null \
    || true

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.coverage.yml)

"${COMPOSE[@]}" up -d --build

# Both 200 and 401 are success signals.
for i in $(seq 1 120); do
    code=$(curl -sS -o /dev/null -w '%{http_code}' \
        "http://127.0.0.1:${RESTHEART_APP_PORT}/" 2>/dev/null || echo "")
    if [ "$code" = "200" ] || [ "$code" = "401" ]; then break; fi
    sleep 2
done

if [ "$code" != "200" ] && [ "$code" != "401" ]; then
    echo "::error::restheart did not bind on port ${RESTHEART_APP_PORT} within 240s (last code: ${code:-empty})"
    echo "----- restheart container logs -----"
    docker logs "${RESTHEART_APP_CONTAINER}" --tail 200 2>&1 || true
    echo "----- mongo container logs -----"
    docker logs "${RESTHEART_MONGO_CONTAINER}" --tail 100 2>&1 || true
    "${COMPOSE[@]}" down -v --remove-orphans || true
    exit 1
fi

bash flow.sh bootstrap 240
bash flow.sh record-traffic

# JaCoCo TCP-dump + report (no JVM stop needed).
COVERAGE_REPORT_FILE="$PWD/coverage_report.txt" bash flow.sh coverage

if [ ! -f coverage_report.txt ]; then
    echo "::error::flow.sh coverage produced no coverage_report.txt"
    exit 1
fi

pct=$(grep -oE '\([0-9]+\.[0-9]+%\)' coverage_report.txt | head -1 | tr -d '()%')
if [ -z "$pct" ]; then
    echo "::error::Could not parse coverage percentage from coverage_report.txt"
    cat coverage_report.txt || true
    exit 1
fi
echo "coverage=${pct}" >>"$GITHUB_OUTPUT"
echo "coverage: ${pct}% (Java line coverage via JaCoCo)"

"${COMPOSE[@]}" down -v --remove-orphans
