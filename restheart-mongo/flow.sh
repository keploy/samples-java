#!/usr/bin/env bash
#
# flow.sh — keploy-independent orchestration for the
# restheart-mongo sample. Modeled on
# samples-python/doccano-django/flow.sh.
#
# Subcommands:
#   bootstrap      — RESTHeart's default config has no admin auth
#                    setup needed; the bootstrap step here just
#                    creates the test database and seed
#                    collections so subsequent reads have
#                    something to find.
#   record-traffic — drive RESTHeart's REST surface (Mongo / GraphQL
#                    / files / users / acl). Fire-and-forget;
#                    keploy is the assertion layer at replay.
#   coverage       — report (method, path) coverage. Denominator is
#                    derived from RESTHeart's known route-mounts
#                    (see SCOPE_PATHS in restheart_list_routes).
#   list-routes    — print the route table the coverage report
#                    uses as its denominator.
#
# HANDOFF NOTE: SCAFFOLD. The full traffic loop the existing keploy
# lane drives (`compat_trigger_record_traffic` in
# enterprise/.ci/scripts/restheart-linux.sh, ~600 lines covering
# CRUD on /<db>/<coll> + GraphQL + files + ACL + users + bulk +
# aggregations) needs to be ported into
# `restheart_record_traffic` here. The stub below covers enough
# to prove the sample boots end-to-end without keploy. See the
# migration plan in the PR description / linked issue.
set -Eeuo pipefail

RESTHEART_APP_PORT="${RESTHEART_APP_PORT:-8080}"
RESTHEART_APP_CONTAINER="${RESTHEART_APP_CONTAINER:-restheart_app}"
RESTHEART_MONGO_CONTAINER="${RESTHEART_MONGO_CONTAINER:-restheart_mongo}"
RESTHEART_DB="${RESTHEART_DB:-keploy}"
RESTHEART_PHASE="${RESTHEART_PHASE:-local}"
RESTHEART_FIRED_ROUTES_FILE="${RESTHEART_FIRED_ROUTES_FILE:-}"

# RESTHeart 9.x ships with an admin user (admin/secret) for protected
# endpoints; the unauthenticated paths are fine for the smoke set we
# drive in record-traffic. Override RESTHEART_ADMIN_AUTH to add
# `Authorization: Basic <b64>` to authenticated calls when porting
# the full lane traffic.
RESTHEART_ADMIN_AUTH="${RESTHEART_ADMIN_AUTH:-Basic YWRtaW46c2VjcmV0}"

base="http://127.0.0.1:${RESTHEART_APP_PORT}"
h_json='Content-Type: application/json'

log_fired() {
    [ -z "$RESTHEART_FIRED_ROUTES_FILE" ] && return 0
    printf '%s %s\n' "$1" "$2" >>"$RESTHEART_FIRED_ROUTES_FILE"
}

restheart_wait_for_app() {
    local timeout=${1:-180}
    local start_ts code
    start_ts=$(date +%s)
    while true; do
        code=$(curl -sS -o /dev/null -w '%{http_code}' "${base}/" 2>/dev/null || echo "")
        # 401 (auth required on root) is a SUCCESS signal — it
        # means RESTHeart is up and responding to HTTP.
        if [ "$code" = "200" ] || [ "$code" = "401" ]; then return 0; fi
        if [ $(( $(date +%s) - start_ts )) -ge "$timeout" ]; then
            echo "restheart_wait_for_app: timed out (last code: ${code:-<empty>})" >&2
            return 1
        fi
        sleep 2
    done
}

restheart_bootstrap() {
    local timeout=${1:-180}
    restheart_wait_for_app "$timeout"

    # Create the test database. PUT on /<db> is idempotent —
    # 201 first time, 200 on subsequent runs.
    curl -sS -o /dev/null -H "$RESTHEART_ADMIN_AUTH" -X PUT "${base}/${RESTHEART_DB}" || true
    # Seed a collection so reads have something to find.
    curl -sS -o /dev/null -H "$RESTHEART_ADMIN_AUTH" -X PUT "${base}/${RESTHEART_DB}/items" || true
    echo "restheart_bootstrap: db=${RESTHEART_DB} ready"
}

restheart_record_traffic() {
    restheart_wait_for_app 60

    log_fired GET "$base/"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" "$base/" >/dev/null || true

    log_fired GET "$base/${RESTHEART_DB}"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" "$base/${RESTHEART_DB}" >/dev/null || true

    log_fired GET "$base/${RESTHEART_DB}/items"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" "$base/${RESTHEART_DB}/items" >/dev/null || true

    # Insert a document.
    log_fired POST "$base/${RESTHEART_DB}/items"
    curl -fsS -H "$RESTHEART_ADMIN_AUTH" -H "$h_json" -X POST \
        "$base/${RESTHEART_DB}/items" \
        -d "{\"_id\":\"keploy-${RESTHEART_PHASE}\",\"name\":\"sample item\",\"score\":42}" >/dev/null || true

    # Read it back.
    log_fired GET "$base/${RESTHEART_DB}/items/keploy-${RESTHEART_PHASE}"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" \
        "$base/${RESTHEART_DB}/items/keploy-${RESTHEART_PHASE}" >/dev/null || true

    # Update it.
    log_fired PATCH "$base/${RESTHEART_DB}/items/keploy-${RESTHEART_PHASE}"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" -H "$h_json" -X PATCH \
        "$base/${RESTHEART_DB}/items/keploy-${RESTHEART_PHASE}" \
        -d '{"$set":{"score":100}}' >/dev/null || true

    # Aggregation surface.
    log_fired GET "$base/${RESTHEART_DB}/items/_size"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" "$base/${RESTHEART_DB}/items/_size" >/dev/null || true
    log_fired GET "$base/${RESTHEART_DB}/_meta"
    curl -sS -H "$RESTHEART_ADMIN_AUTH" "$base/${RESTHEART_DB}/_meta" >/dev/null || true
}

# RESTHeart's routes are pattern-mount based, not file-system
# based. The denominator is curated here from the upstream docs +
# the routes the lane intends to exercise. Update this list when
# adding new traffic to record-traffic so the coverage stays in
# lockstep.
restheart_list_routes() {
    cat <<'ROUTES'
GET /
GET /{db}
PUT /{db}
DELETE /{db}
GET /{db}/_meta
GET /{db}/{coll}
PUT /{db}/{coll}
DELETE /{db}/{coll}
POST /{db}/{coll}
GET /{db}/{coll}/{docid}
PUT /{db}/{coll}/{docid}
PATCH /{db}/{coll}/{docid}
DELETE /{db}/{coll}/{docid}
GET /{db}/{coll}/_size
GET /{db}/{coll}/_aggrs/{name}
GET /{db}/{coll}/_indexes
ROUTES
}

restheart_list_recorded_routes() {
    local f method route
    local found_keploy=0
    while IFS= read -r f; do
        found_keploy=1
        method=$(awk '/^    method:/{print $2; exit}' "$f")
        route=$(awk '/^    url:/{print $2; exit}' "$f")
        route="${route%%\?*}"
        case "$route" in http://*|https://*) route="/${route#*://*/}" ;; esac
        if [ -n "$method" ] && [ -n "$route" ]; then echo "$method $route"; fi
    done < <(find keploy -type f -path '*/tests/*.yaml' 2>/dev/null) | sort -u
    if [ "$found_keploy" = "1" ]; then return 0; fi

    if [ -n "$RESTHEART_FIRED_ROUTES_FILE" ] && [ -f "$RESTHEART_FIRED_ROUTES_FILE" ]; then
        while IFS= read -r line; do
            method="${line%% *}"; route="${line#* }"
            route="${route%%\?*}"
            case "$route" in http://*|https://*) route="/${route#*://*/}" ;; esac
            [ -n "$method" ] && [ -n "$route" ] && echo "$method $route"
        done <"$RESTHEART_FIRED_ROUTES_FILE" | sort -u
    fi
}

restheart_report_coverage() {
    local routes_file recorded_file
    routes_file="$(mktemp)"; recorded_file="$(mktemp)"
    restheart_list_routes >"$routes_file"
    restheart_list_recorded_routes >"$recorded_file"

    local total covered missing pct
    total=$(wc -l <"$routes_file" | tr -d ' '); covered=0; missing=""
    while IFS= read -r line; do
        local method="${line%% *}"
        local route="${line#* }"
        # Replace {param} placeholders with [^/]+ for matching.
        local pattern
        pattern="^${method} $(printf '%s' "$route" | sed -E 's/\{[^}]+\}/[^\/]+/g')$"
        if grep -qE "$pattern" "$recorded_file"; then
            covered=$((covered + 1))
        else
            missing+="  ${method} ${route}"$'\n'
        fi
    done <"$routes_file"
    if [ "$total" -gt 0 ]; then
        pct=$(awk -v c="$covered" -v t="$total" 'BEGIN{printf "%.1f", c*100/t}')
    else pct="0.0"; fi
    {
        echo "================ RESTHeart API coverage ================"
        echo "Covered ${covered}/${total} (${pct}%)"
        if [ -n "$missing" ]; then echo "Uncovered:"; printf '%s' "$missing"; fi
        echo "========================================================"
    } | tee "${COVERAGE_REPORT_FILE:-coverage_report.txt}"
    rm -f "$routes_file" "$recorded_file"
}

case "${1:-}" in
    bootstrap)        restheart_bootstrap "${2:-180}" ;;
    record-traffic)   restheart_record_traffic ;;
    coverage)         restheart_report_coverage ;;
    list-routes)      restheart_list_routes ;;
    *)
        echo "usage: $0 {bootstrap|record-traffic|coverage|list-routes}" >&2
        exit 2 ;;
esac
