#!/usr/bin/env bash
#
# run-and-measure.sh — bring restheart-mongo up via the sample's
# compose, run flow.sh bootstrap + record-traffic with the
# per-call audit log enabled, run flow.sh coverage, and emit
# `coverage=PCT` onto $GITHUB_OUTPUT for the downstream
# coverage-gate job.
#
# Called from .github/workflows/restheart-mongo.yml's
# build-coverage and release-coverage jobs (one per ref under
# comparison). Both jobs source the same script so the
# measurement is identical across refs — any drift in the
# numerator definition would otherwise produce a misleading
# delta.
#
# Inputs (all from the workflow env):
#   RESTHEART_FIRED_ROUTES_FILE — per-call audit log path; passed
#                                 through to flow.sh so its
#                                 record-traffic loop logs each
#                                 (METHOD, URL) pair, and so its
#                                 coverage subcommand uses that
#                                 file as the standalone
#                                 numerator.
#   RESTHEART_PHASE             — label spliced into the project
#                                 name so build vs. release runs
#                                 don't collide on volume names
#                                 (compose project naming inside
#                                 the GH runner is per-job
#                                 anyway, but RESTHEART_PHASE
#                                 shows up in the test fixtures
#                                 and is useful for diffing logs).
#   GITHUB_OUTPUT               — standard GH Actions sink for
#                                 step outputs.
set -Eeuo pipefail

# Compose-substituted variables. Defaults match the sample's
# docker-compose.yml so a local invocation of this script (no
# overrides) reproduces what CI runs.
export RESTHEART_APP_CONTAINER="${RESTHEART_APP_CONTAINER:-restheart_app}"
export RESTHEART_MONGO_CONTAINER="${RESTHEART_MONGO_CONTAINER:-restheart_mongo}"
export RESTHEART_APP_PORT="${RESTHEART_APP_PORT:-8080}"
export RESTHEART_MONGO_IP="${RESTHEART_MONGO_IP:-172.36.0.10}"
export RESTHEART_NETWORK_SUBNET="${RESTHEART_NETWORK_SUBNET:-172.36.0.0/24}"

# RESTHeart 9.x ships with admin/secret as the default
# bootstrapped principal. flow.sh reads this header for every
# call, so exporting it here keeps the standalone CI run aligned
# with the keploy lanes (which pass the same value through).
export RESTHEART_ADMIN_AUTH="${RESTHEART_ADMIN_AUTH:-Basic YWRtaW46c2VjcmV0}"

: "${RESTHEART_FIRED_ROUTES_FILE:?RESTHEART_FIRED_ROUTES_FILE must be set by the workflow}"

# Reset audit log for this run; otherwise a prior run's entries
# would inflate the numerator on a re-trigger.
: >"$RESTHEART_FIRED_ROUTES_FILE"

# Single-phase bootstrap: RESTHeart embeds its own admin
# principal at first boot, so there's no separate "seed admin
# user" stage the way doccano needs. compose up → wait for app
# port → flow.sh bootstrap (PUTs the db + record-traffic's
# collections) → flow.sh record-traffic → flow.sh coverage.
docker compose up -d

# Wait for the backend to start serving. Per the sample's
# restheart_wait_for_app, both 200 AND 401 are success signals
# — RESTHeart returns 401 on `/` until you authenticate, but
# 401 still proves the HTTP listener and the auth filter are
# both up. Anything before that (000 / connection refused) is
# pre-listen.
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
    echo "----- docker compose ps -----"
    docker compose ps || true
    docker compose down -v --remove-orphans || true
    exit 1
fi

bash flow.sh bootstrap 240

# Drive traffic. flow.sh::restheart_record_traffic gates on
# restheart_wait_for_app internally, so this won't fire curls
# at a half-booted backend.
bash flow.sh record-traffic

# Coverage report — uses RESTHEART_FIRED_ROUTES_FILE as numerator
# since no keploy/test-set-* tree exists in the standalone case.
COVERAGE_REPORT_FILE="$PWD/coverage_report.txt" bash flow.sh coverage

# Pull the percentage out of the report's `Covered N/M (XX.X%)`
# line. Anchored on the parenthesised form so a future change to
# the report's prose doesn't break the parse.
pct=$(grep -oE '\([0-9]+\.[0-9]+%\)' coverage_report.txt | head -1 | tr -d '()%')
if [ -z "$pct" ]; then
    echo "::error::Could not parse coverage percentage from coverage_report.txt"
    cat coverage_report.txt || true
    exit 1
fi
echo "coverage=${pct}" >>"$GITHUB_OUTPUT"
echo "coverage: ${pct}% (audit log: $RESTHEART_FIRED_ROUTES_FILE)"

docker compose down -v --remove-orphans
