#!/usr/bin/env bash
#
# flow.sh — keploy-independent orchestration for the
# restheart-mongo sample. Modeled on
# samples-python/doccano-django/flow.sh.
#
# Subcommands:
#   bootstrap      — wait for RESTHeart to start serving, then PUT
#                    the test database + the seed collections
#                    (items, halpeople, gql-apps, acl, files
#                    buckets) that record-traffic exercises.
#   record-traffic — drive RESTHeart's full REST surface (Mongo
#                    CRUD / HAL / aggregations / bulk / GraphQL /
#                    files / ACL / users / sessions / metrics /
#                    OAuth metadata). Fire-and-forget; keploy is
#                    the assertion layer at replay.
#   coverage       — report (method, path) coverage. Denominator is
#                    derived from RESTHeart's known route-mounts
#                    (see SCOPE_PATHS in restheart_list_routes).
#   list-routes    — print the route table the coverage report
#                    uses as its denominator.

set -Eeuo pipefail

RESTHEART_APP_PORT="${RESTHEART_APP_PORT:-8080}"
RESTHEART_APP_CONTAINER="${RESTHEART_APP_CONTAINER:-restheart_app}"
RESTHEART_MONGO_CONTAINER="${RESTHEART_MONGO_CONTAINER:-restheart_mongo}"
RESTHEART_DB="${RESTHEART_DB:-restheart}"
RESTHEART_PHASE="${RESTHEART_PHASE:-local}"
RESTHEART_FIRED_ROUTES_FILE="${RESTHEART_FIRED_ROUTES_FILE:-}"

# RESTHeart 9.x ships with an admin user (admin/secret) for
# protected endpoints. The full traffic loop authenticates as
# admin for every administrative call (db / collection / index /
# acl / users / sessions). Override RESTHEART_ADMIN_AUTH if your
# deployment uses different credentials.
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

    # Seed the collections record-traffic depends on. Each PUT is
    # idempotent (201 first time, 200 on subsequent runs) and
    # tolerated if the collection already exists.
    local coll
    for coll in items people places halpeople relpeople gql-apps acl _schemas \
            avatars.files range_files.files imported_csv; do
        curl -sS -o /dev/null -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "${base}/${RESTHEART_DB}/${coll}" || true
    done

    echo "restheart_bootstrap: db=${RESTHEART_DB} ready"
}

restheart_record_traffic() {
    restheart_wait_for_app 60
    sleep 5

    local encoded_doc_keys='%7B%22_id%22:1,%22name%22:1,%22age%22:1%7D'
    local encoded_filter='%7B%22_id%22:%22jane%22%7D'

    # Liveness + root + metrics.
    log_fired GET "$base/ping"
    curl -fsS "$base/ping" >/dev/null || true
    log_fired GET "$base/"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/" >/dev/null || true
    log_fired GET "$base/metrics"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/metrics" >/dev/null || true

    # ------------------------------------------------------------------
    # Round 1: basic CRUD on /people — collection lifecycle, document
    # CRUD, indexes, _size / _meta / _indexes management endpoints.
    # ------------------------------------------------------------------
    log_fired PUT "$base/people"
    curl -fsS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/people" >/dev/null || true
    log_fired GET "$base/people"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people" >/dev/null || true
    log_fired GET "$base/people/_size"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people/_size" >/dev/null || true
    log_fired GET "$base/people/_meta"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people/_meta" >/dev/null || true
    log_fired GET "$base/people/_indexes"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people/_indexes" >/dev/null || true

    log_fired POST "$base/people"
    curl -fsS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X POST "$base/people" \
        -d '{"_id":"jane","name":"Jane","age":30}' >/dev/null || true
    log_fired POST "$base/people"
    curl -fsS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X POST "$base/people" \
        -d '{"_id":"john","name":"John","age":40}' >/dev/null || true

    log_fired GET "$base/people/jane"
    curl -fsS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people/jane?keys=${encoded_doc_keys}" >/dev/null || true
    log_fired GET "$base/people/jane/_meta"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people/jane/_meta" >/dev/null || true
    log_fired PATCH "$base/people/jane"
    curl -fsS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PATCH "$base/people/jane" \
        -d '{"$set":{"age":31}}' >/dev/null || true
    log_fired PUT "$base/people/jane"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PUT "$base/people/jane" \
        -d '{"name":"Jane","age":32,"city":"Paris"}' >/dev/null || true
    log_fired GET "$base/people"
    curl -fsS -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/people?filter=${encoded_filter}&keys=${encoded_doc_keys}&pagesize=1" >/dev/null || true

    log_fired PUT "$base/people/_indexes/by_age"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PUT "$base/people/_indexes/by_age" \
        -d '{"keys":{"age":1},"ops":{"unique":false}}' >/dev/null || true
    log_fired GET "$base/people/_indexes"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/people/_indexes" >/dev/null || true
    log_fired DELETE "$base/people/_indexes/by_age"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/people/_indexes/by_age" >/dev/null || true

    log_fired DELETE "$base/people/john"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/people/john" >/dev/null || true

    log_fired PUT "$base/places"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/places" >/dev/null || true
    log_fired POST "$base/places"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X POST "$base/places" \
        -d '{"_id":"paris","country":"FR"}' >/dev/null || true
    log_fired GET "$base/places/paris"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/places/paris" >/dev/null || true
    log_fired DELETE "$base/places/paris"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/places/paris" >/dev/null || true
    log_fired DELETE "$base/places"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/places" >/dev/null || true

    # ------------------------------------------------------------------
    # HAL representation factories — Accept: application/hal+json drives
    # DocumentRepresentationFactory / CollectionRepresentationFactory /
    # IndexesRepresentationFactory.
    # ------------------------------------------------------------------
    log_fired PUT "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/halpeople" >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X POST "$base/halpeople" \
        -d '{"_id":"alice","name":"Alice","age":29}' >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: application/hal+json' "$base/halpeople" >/dev/null || true
    log_fired GET "$base/halpeople/alice"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: application/hal+json' "$base/halpeople/alice" >/dev/null || true
    log_fired GET "$base/halpeople/_indexes"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: application/hal+json' "$base/halpeople/_indexes" >/dev/null || true
    log_fired GET "$base/"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: application/hal+json' "$base/" >/dev/null || true

    # ------------------------------------------------------------------
    # Aggregations — define a pipeline on the collection then read it.
    # ------------------------------------------------------------------
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PATCH "$base/halpeople/_meta" \
        -d '{"aggrs":[{"uri":"by-age","type":"pipeline","stages":[{"_$group":{"_id":"$age","count":{"_$sum":1}}}]}]}' >/dev/null || true
    log_fired GET "$base/halpeople/_meta"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_meta" >/dev/null || true
    log_fired GET "$base/halpeople/_aggrs/by-age"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_aggrs/by-age" >/dev/null || true

    # ------------------------------------------------------------------
    # Bulk write — POST array body, PATCH-with-filter, DELETE-with-filter.
    # ------------------------------------------------------------------
    log_fired POST "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X POST "$base/halpeople" \
        -d '[{"_id":"bob","name":"Bob","age":35},{"_id":"carol","name":"Carol","age":41},{"_id":"dave","name":"Dave","age":52}]' >/dev/null || true
    log_fired PATCH "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PATCH \
        "$base/halpeople?filter=%7B%22age%22:%7B%22%24gte%22:35%7D%7D" \
        -d '{"$set":{"vip":true}}' >/dev/null || true
    log_fired DELETE "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE \
        "$base/halpeople?filter=%7B%22age%22:%7B%22%24gte%22:50%7D%7D" >/dev/null || true

    # ------------------------------------------------------------------
    # JSON schema validation — define a schema, write conforming and
    # non-conforming docs.
    # ------------------------------------------------------------------
    log_fired PUT "$base/_schemas"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PUT "$base/_schemas" >/dev/null || true
    log_fired PUT "$base/_schemas/person"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PUT "$base/_schemas/person" \
        -d '{"$schema":"http://json-schema.org/draft-04/schema#","type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer","minimum":0}},"required":["name"]}' >/dev/null || true
    log_fired GET "$base/_schemas/person"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_schemas/person" >/dev/null || true
    log_fired GET "$base/_schemas"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_schemas" >/dev/null || true

    # ------------------------------------------------------------------
    # Auth services — /token, /roles, /logout.
    # ------------------------------------------------------------------
    log_fired GET "$base/roles/admin"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/roles/admin" >/dev/null || true
    log_fired GET "$base/token/admin"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/token/admin" >/dev/null || true
    log_fired POST "$base/logout"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/logout" >/dev/null || true

    # ------------------------------------------------------------------
    # Files / GridFS — create a files bucket, upload a small file, fetch
    # binary + metadata, then delete.
    # ------------------------------------------------------------------
    log_fired PUT "$base/avatars.files"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PUT "$base/avatars.files" \
        -d '{"descr":"avatars file bucket"}' >/dev/null || true
    printf 'keploy-coverage' > /tmp/restheart-cov-upload.bin
    log_fired POST "$base/avatars.files"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/avatars.files" \
        -F 'file=@/tmp/restheart-cov-upload.bin' \
        -F 'metadata={"_id":"avatar1","owner":"jane"};type=application/json' >/dev/null || true
    rm -f /tmp/restheart-cov-upload.bin
    log_fired GET "$base/avatars.files"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/avatars.files" >/dev/null || true
    log_fired GET "$base/avatars.files/avatar1"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/avatars.files/avatar1" >/dev/null || true
    log_fired GET "$base/avatars.files/avatar1/binary"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/avatars.files/avatar1/binary" >/dev/null || true
    log_fired DELETE "$base/avatars.files/avatar1"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/avatars.files/avatar1" >/dev/null || true

    # ------------------------------------------------------------------
    # Pagination + sort + counting + 404 paths.
    # ------------------------------------------------------------------
    log_fired GET "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople?pagesize=2&page=1&sort=%7B%22age%22:1%7D&count=true" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople?np=true&pagesize=1" >/dev/null || true
    log_fired GET "$base/health/db"
    curl -sS "$base/health/db" >/dev/null || true
    log_fired GET "$base/no-such-collection"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/no-such-collection" >/dev/null || true
    log_fired GET "$base/halpeople/no-such-doc"
    curl -sS -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/no-such-doc" >/dev/null || true

    # ------------------------------------------------------------------
    # HAL via ?rep=hal — content negotiation route to the
    # mongodb.hal.* representation factories.
    # ------------------------------------------------------------------
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople?rep=hal" >/dev/null || true
    log_fired GET "$base/halpeople/alice"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/alice?rep=hal" >/dev/null || true
    log_fired GET "$base/halpeople/_indexes"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_indexes?rep=hal" >/dev/null || true
    log_fired GET "$base/"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/?rep=hal" >/dev/null || true

    # ------------------------------------------------------------------
    # Relationships — declared in collection _meta.
    # ------------------------------------------------------------------
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" -X PATCH "$base/halpeople/_meta" \
        -d '{"rels":[{"rel":"author","type":"ONE_TO_MANY","role":"OWNING","target-coll":"halpeople","ref-field":"_id"}]}' >/dev/null || true
    log_fired GET "$base/halpeople/alice"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/alice?rep=hal&hal=full" >/dev/null || true

    # Cache invalidator service (/ic) — unsecured.
    log_fired POST "$base/ic"
    curl -sS --max-time 5 -X POST "$base/ic?db=${RESTHEART_DB}&coll=halpeople" >/dev/null || true
    log_fired GET "$base/ic"
    curl -sS --max-time 5 "$base/ic" >/dev/null || true

    # CSV loader service (/csv).
    log_fired POST "$base/csv"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Content-Type: text/csv' \
        -X POST "$base/csv?db=${RESTHEART_DB}&coll=imported_csv&id=col1" \
        --data-binary $'col1,col2,col3\nA1,B1,C1\nA2,B2,C2\nA3,B3,C3' >/dev/null || true
    log_fired GET "$base/imported_csv"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/imported_csv" >/dev/null || true

    # ------------------------------------------------------------------
    # ETag conditional flow — capture the ETag of /halpeople, then
    # issue PUT/DELETE with If-Match plus a conditional GET.
    # ------------------------------------------------------------------
    halpeople_etag=$(curl -sSI --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople" 2>/dev/null \
        | awk 'BEGIN{IGNORECASE=1} /^ETag:/ {gsub(/[\r\n"]/,"",$2); print $2; exit}')
    if [ -n "${halpeople_etag:-}" ]; then
        log_fired PUT "$base/halpeople/_meta"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "If-Match: ${halpeople_etag}" \
            -H "$h_json" -X PUT "$base/halpeople/_meta" \
            -d '{"descr":"keploy CI bumped"}' >/dev/null || true
        log_fired GET "$base/halpeople"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "If-None-Match: ${halpeople_etag}" \
            "$base/halpeople" >/dev/null || true
    fi

    # ------------------------------------------------------------------
    # GraphQL service entry path — empty / introspection / unknown app.
    # ------------------------------------------------------------------
    log_fired POST "$base/graphql"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql" -d '{"query":"{ __typename }"}' >/dev/null || true
    log_fired GET "$base/graphql"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/graphql" >/dev/null || true
    log_fired POST "$base/graphql/no-such-app"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/no-such-app" -d '{"query":"{ __schema { types { name } } }"}' >/dev/null || true

    # Change-streams URI.
    log_fired GET "$base/halpeople/_streams"
    curl -sS --max-time 3 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_streams" >/dev/null || true
    log_fired GET "$base/halpeople/_streams/no-such-stream"
    curl -sS --max-time 3 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: text/event-stream' \
        "$base/halpeople/_streams/no-such-stream" >/dev/null || true

    # ------------------------------------------------------------------
    # Sessions / multi-doc transactions.
    # ------------------------------------------------------------------
    log_fired POST "$base/_sessions"
    session_response=$(curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/_sessions" 2>/dev/null || true)
    session_id=$(printf '%s' "$session_response" | jq -r '._id // empty' 2>/dev/null || true)
    if [ -n "${session_id:-}" ]; then
        log_fired GET "$base/_sessions/${session_id}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_sessions/${session_id}" >/dev/null || true
        log_fired POST "$base/_sessions/${session_id}/_txns"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/_sessions/${session_id}/_txns" >/dev/null || true
        log_fired GET "$base/_sessions/${session_id}/_txns"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_sessions/${session_id}/_txns" >/dev/null || true
    fi

    # ------------------------------------------------------------------
    # Diverse query-string + projection variants.
    # ------------------------------------------------------------------
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople?count=true&pagesize=0" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople?keys=%7B%22name%22:1%7D&sort_by=age" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople?filter=%7B%22vip%22:true%7D&hint=%7B%22age%22:1%7D" >/dev/null || true

    # Method-not-allowed and bad-request paths.
    log_fired TRACE "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X TRACE "$base/halpeople" >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" -d '{not even json}' >/dev/null || true

    # ------------------------------------------------------------------
    # GraphQL application — define a schema bound to halpeople and
    # query it. Drives the entire graphql.* tree.
    # ------------------------------------------------------------------
    log_fired PUT "$base/gql-apps"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/gql-apps" >/dev/null || true
    log_fired PUT "$base/gql-apps/halpeople-gql"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/gql-apps/halpeople-gql" \
        -d '{
            "descriptor": { "name": "halpeople-gql", "uri": "halpeople", "description": "keploy ci graphql probe" },
            "schema": "type Query { people: [Person] person(id: String!): Person count: Int } type Person { _id: String name: String age: Int }",
            "mappings": {
                "Query": {
                    "people": { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": {} },
                    "person": { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "_id": { "$arg": "id" } }, "first": true },
                    "count":  { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": {}, "stages": [ { "$count": "_count" } ] }
                }
            }
        }' >/dev/null || true

    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"{ people { _id name age } }"}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"query Q($id:String!){ person(id:$id) { name age } }","variables":{"id":"alice"}}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"{ __schema { types { name kind } } }"}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"{ __type(name:\"Person\"){ name fields { name type { name } } } }"}' >/dev/null || true

    # Properly-formatted relationship on a fresh collection.
    log_fired PUT "$base/relpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/relpeople" >/dev/null || true
    log_fired PUT "$base/relpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/relpeople/_meta" \
        -d '{"rels":[{"rel":"self","type":"ONE_TO_ONE","role":"OWNING","target-coll":"halpeople","ref-field":"ref_id"}]}' >/dev/null || true
    log_fired POST "$base/relpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/relpeople" \
        -d '{"_id":"link-alice","ref_id":"alice"}' >/dev/null || true
    log_fired GET "$base/relpeople/link-alice"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/relpeople/link-alice?rep=hal&hal=full" >/dev/null || true

    # Token lifecycle.
    log_fired GET "$base/token/admin"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/token/admin" >/dev/null || true
    log_fired POST "$base/token/admin"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/token/admin" >/dev/null || true
    log_fired DELETE "$base/token/admin"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/token/admin" >/dev/null || true

    # Metrics format variants.
    log_fired GET "$base/metrics"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: application/json' "$base/metrics" >/dev/null || true
    log_fired GET "$base/metrics"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept: text/plain' "$base/metrics" >/dev/null || true
    log_fired GET "$base/metrics/${RESTHEART_DB}"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/metrics/${RESTHEART_DB}" >/dev/null || true
    log_fired GET "$base/metrics/${RESTHEART_DB}/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/metrics/${RESTHEART_DB}/halpeople" >/dev/null || true

    # Content-Encoding: gzip on POST.
    log_fired POST "$base/halpeople"
    printf '{"_id":"gzip-doc","name":"Z","age":99}' | gzip -c \
        | curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
            -H "$h_json" -H 'Content-Encoding: gzip' \
            -X POST --data-binary @- "$base/halpeople" >/dev/null || true

    # Bulk write with mixed valid/invalid docs.
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" \
        -d '[{"_id":"eve","name":"Eve","age":-1},{"_id":"frank","name":"Frank","age":24}]' >/dev/null || true

    # Auth probes — wrong password, missing auth header, OPTIONS preflight.
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u admin:wrongpass "$base/halpeople" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 "$base/halpeople" >/dev/null || true
    log_fired OPTIONS "$base/halpeople"
    curl -sS --max-time 5 -X OPTIONS \
        -H 'Origin: https://example.com' \
        -H 'Access-Control-Request-Method: POST' \
        -H 'Access-Control-Request-Headers: content-type,authorization' \
        "$base/halpeople" >/dev/null || true

    # ------------------------------------------------------------------
    # Database lifecycle on a separate db (handlers.database).
    # ------------------------------------------------------------------
    log_fired PUT "$base/keployci_db"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/keployci_db" -d '{"descr":"keploy ci db lifecycle"}' >/dev/null || true
    log_fired GET "$base/keployci_db"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/keployci_db" >/dev/null || true
    log_fired GET "$base/keployci_db/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/keployci_db/_meta" >/dev/null || true
    log_fired GET "$base/keployci_db/_size"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/keployci_db/_size" >/dev/null || true
    log_fired PUT "$base/keployci_db/things"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/keployci_db/things" >/dev/null || true
    log_fired POST "$base/keployci_db/things"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/keployci_db/things" -d '{"_id":"t1","kind":"a"}' >/dev/null || true
    keployci_db_etag=$(curl -sSI --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/keployci_db" 2>/dev/null \
        | awk 'BEGIN{IGNORECASE=1} /^ETag:/ {gsub(/[\r\n"]/,"",$2); print $2; exit}')
    things_etag=$(curl -sSI --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/keployci_db/things" 2>/dev/null \
        | awk 'BEGIN{IGNORECASE=1} /^ETag:/ {gsub(/[\r\n"]/,"",$2); print $2; exit}')
    if [ -n "${things_etag:-}" ]; then
        log_fired DELETE "$base/keployci_db/things"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
            -H "If-Match: ${things_etag}" -X DELETE "$base/keployci_db/things" >/dev/null || true
    fi
    if [ -n "${keployci_db_etag:-}" ]; then
        log_fired DELETE "$base/keployci_db"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
            -H "If-Match: ${keployci_db_etag}" -X DELETE "$base/keployci_db" >/dev/null || true
    fi

    # ------------------------------------------------------------------
    # Schema-violation writes — drives JsonSchemaBeforeWriteChecker.
    # ------------------------------------------------------------------
    log_fired PUT "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/halpeople/_meta" -d '{"schema":"person"}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"badname","name":42,"age":30}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"missingname","age":30}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"negage","name":"Bad","age":-5}' >/dev/null || true

    # Variety of $-operators in PATCH.
    local op_payload
    for op_payload in \
        '{"$inc":{"age":1}}' \
        '{"$push":{"tags":"vip"}}' \
        '{"$addToSet":{"tags":"early"}}' \
        '{"$pull":{"tags":"vip"}}' \
        '{"$unset":{"city":""}}' \
        '{"$rename":{"city":"location"}}' \
        '{"$currentDate":{"updatedAt":true}}'; do
        log_fired PATCH "$base/halpeople/alice"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X PATCH "$base/halpeople/alice" -d "$op_payload" >/dev/null || true
    done

    # writeMode query param on POST / PUT.
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople?writeMode=upsert" -d '{"_id":"upsertdoc","name":"Upserted","age":1}' >/dev/null || true
    log_fired PUT "$base/halpeople/upsertdoc"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/halpeople/upsertdoc?writeMode=insert" -d '{"name":"Insertish","age":2}' >/dev/null || true
    log_fired PUT "$base/halpeople/upsertdoc"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/halpeople/upsertdoc?writeMode=update" -d '{"name":"Updatedish","age":3}' >/dev/null || true

    # Larger bulk write.
    bulk_payload="$(printf '['; for i in $(seq 1 25); do
            printf '{"_id":"bulk-%d","name":"User%d","age":%d}' "$i" "$i" "$((20 + i))"
            [ "$i" -lt 25 ] && printf ',';
        done; printf ']')"
    log_fired POST "$base/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" -d "$bulk_payload" >/dev/null || true
    log_fired PATCH "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople?filter=%7B%22_id%22:%7B%22%24regex%22:%22%5Ebulk-%22%7D%7D" \
        -d '{"$set":{"role":"bulk"}}' >/dev/null || true
    log_fired DELETE "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -X DELETE "$base/halpeople?filter=%7B%22_id%22:%7B%22%24regex%22:%22%5Ebulk-%22%7D%7D" >/dev/null || true

    # ------------------------------------------------------------------
    # GraphQL mutations — extend the app to add a write op.
    # ------------------------------------------------------------------
    log_fired PUT "$base/gql-apps/halpeople-gql"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/gql-apps/halpeople-gql" \
        -d '{
            "descriptor": { "name": "halpeople-gql", "uri": "halpeople" },
            "schema": "type Query { people: [Person] person(id: String!): Person } type Mutation { tag(id: String!, tag: String!): Person } type Person { _id: String name: String age: Int tags: [String] }",
            "mappings": {
                "Query": {
                    "people": { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": {} },
                    "person": { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "_id": { "$arg": "id" } }, "first": true }
                },
                "Mutation": {
                    "tag": { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "update": { "$addToSet": { "tags": { "$arg": "tag" } } }, "filter": { "_id": { "$arg": "id" } } }
                }
            }
        }' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"mutation M($id:String!,$t:String!){ tag(id:$id, tag:$t) { _id tags } }","variables":{"id":"alice","tag":"vip"}}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" -d '{"query":"{ this is not graphql"}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" -d '{"query":"{ people { unknownField } }"}' >/dev/null || true

    # Define a change stream then attempt SSE upgrade.
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople/_meta" \
        -d '{"streams":[{"uri":"all","stages":[{"_$match":{}}]}]}' >/dev/null || true
    log_fired GET "$base/halpeople/_streams/all"
    curl -sS --max-time 3 -H "Authorization: $RESTHEART_ADMIN_AUTH" -N \
        -H 'Accept: text/event-stream' "$base/halpeople/_streams/all" >/dev/null || true

    # JWT bearer + Auth-Token bogus probes.
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.bm90LWEtcmVhbC1qd3Q.signature' \
        "$base/halpeople" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H 'Auth-Token: bogus-token' "$base/halpeople" >/dev/null || true

    # ------------------------------------------------------------------
    # ACL — non-admin role evaluation. Inserts must use POST /acl.
    # User passwords are sent plaintext; userPwdHasher bcrypts on insert.
    # ------------------------------------------------------------------
    log_fired PUT "$base/acl"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/acl" >/dev/null || true

    local acl_rule
    for acl_rule in \
        '{"_id":"reader-get-halpeople","roles":["reader"],"predicate":"method(GET) and path-prefix[/halpeople] and qparams-whitelist[page, pagesize, filter, keys]"}' \
        '{"_id":"reader-blacklist","roles":["reader"],"predicate":"method(GET) and path-prefix[/halpeople] and qparams-blacklist[secret, token]"}' \
        '{"_id":"reader-self-equals","roles":["reader"],"predicate":"path-prefix[/halpeople] and equals[%U, reader]"}' \
        '{"_id":"reader-localhost","roles":["reader"],"predicate":"path-prefix[/halpeople] and in[%h, {127.0.0.1, localhost}]"}' \
        '{"_id":"writer-bson-whitelist","roles":["writer"],"predicate":"path-prefix[/halpeople] and (method(GET) or method(POST) or method(PATCH)) and bson-request-whitelist[name, age, _id, role]"}' \
        '{"_id":"writer-bson-blacklist","roles":["writer"],"predicate":"path-prefix[/halpeople] and method(POST) and bson-request-blacklist[password, secret]"}' \
        '{"_id":"writer-bson-contains","roles":["writer"],"predicate":"path-prefix[/halpeople] and method(POST) and bson-request-contains[name]"}'; do
        log_fired POST "$base/acl"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X POST "$base/acl" -d "$acl_rule" >/dev/null || true
    done

    log_fired GET "$base/acl"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/acl" >/dev/null || true

    # Create non-admin users (plaintext passwords).
    log_fired POST "$base/users"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/users" \
        -d '{"_id":"reader","password":"reader-secret","roles":["reader"]}' >/dev/null || true
    log_fired POST "$base/users"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/users" \
        -d '{"_id":"writer","password":"writer-secret","roles":["writer"]}' >/dev/null || true

    # Wait for the mongoAclAuthorizer cache TTL to refresh.
    sleep 6

    # Reader requests — drive predicate evaluator.
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople?page=1&pagesize=5" >/dev/null || true
    log_fired GET "$base/halpeople/alice"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople/alice" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople?evil=true" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople?secret=leak&page=1" >/dev/null || true
    log_fired DELETE "$base/halpeople/alice"
    curl -sS --max-time 5 -u reader:reader-secret -X DELETE "$base/halpeople/alice" >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"intruder","name":"X"}' >/dev/null || true
    log_fired GET "$base/places"
    curl -sS --max-time 5 -u reader:reader-secret "$base/places" >/dev/null || true

    # Writer requests.
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"writer-doc","name":"W","age":1,"role":"writer"}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"writer-bad","name":"B","extra":"forbidden"}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"writer-pw","name":"B","password":"x"}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X POST "$base/halpeople" -d '{"_id":"writer-noname","age":1}' >/dev/null || true
    log_fired PATCH "$base/halpeople/writer-doc"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X PATCH "$base/halpeople/writer-doc" -d '{"$set":{"role":"writer"}}' >/dev/null || true
    log_fired DELETE "$base/halpeople/writer-doc"
    curl -sS --max-time 5 -u writer:writer-secret -X DELETE "$base/halpeople/writer-doc" >/dev/null || true

    # Wrong-password probe — drives mongoRealmAuthenticator verify-fail.
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:wrongpassword "$base/halpeople" >/dev/null || true

    # ------------------------------------------------------------------
    # Aggregation pipeline with variable interpolation.
    # ------------------------------------------------------------------
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople/_meta" \
        -d '{"aggrs":[{"uri":"older-than","type":"pipeline","stages":[{"_$match":{"age":{"_$gte":{"_$var":"min_age"}}}},{"_$count":"_count"}]}]}' >/dev/null || true
    sleep 2
    avars_25='%7B%22min_age%22:25%7D'
    avars_50='%7B%22min_age%22:50%7D'
    log_fired GET "$base/halpeople/_aggrs/older-than"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople/_aggrs/older-than?avars=${avars_25}" >/dev/null || true
    log_fired GET "$base/halpeople/_aggrs/older-than"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople/_aggrs/older-than?avars=${avars_50}" >/dev/null || true
    log_fired GET "$base/halpeople/_aggrs/older-than"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople/_aggrs/older-than" >/dev/null || true
    log_fired GET "$base/halpeople/_aggrs/older-than"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople/_aggrs/older-than?avars=not-json" >/dev/null || true

    # ------------------------------------------------------------------
    # Additional ACL rules with @user.* / @request.* var set.
    # ------------------------------------------------------------------
    for acl_rule in \
        '{"_id":"reader-roles-array","roles":["reader"],"predicate":"path-prefix[/halpeople] and equals[%U, @user.userid]"}' \
        '{"_id":"reader-qparam-var","roles":["reader"],"predicate":"path-prefix[/halpeople] and qparams-contain[user]"}' \
        '{"_id":"reader-qparam-size","roles":["reader"],"predicate":"path-prefix[/halpeople] and qparams-size[0, 5]"}'; do
        log_fired POST "$base/acl"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X POST "$base/acl" -d "$acl_rule" >/dev/null || true
    done
    sleep 6

    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople?user=reader&page=1" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople?a=1&b=2&c=3&d=4&e=5&f=6" >/dev/null || true

    # ------------------------------------------------------------------
    # GraphQL with BSON scalar types.
    # ------------------------------------------------------------------
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/halpeople" \
        -d '{"_id":"bson-doc","name":"BsonDoc","age":42,"score":{"$numberLong":"9999999999"},"price":{"$numberDecimal":"19.99"},"created":{"$date":"2024-01-15T10:00:00Z"},"oid":{"$oid":"507f1f77bcf86cd799439011"},"data":{"$binary":{"base64":"a2Vwbg==","subType":"00"}}}' >/dev/null || true

    log_fired PUT "$base/gql-apps/bson-types"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/gql-apps/bson-types" \
        -d '{
            "descriptor": { "name": "bson-types", "uri": "bson-types" },
            "schema": "scalar BsonObjectId scalar BsonDecimal128 scalar BsonLong scalar BsonDate scalar BsonBinary type Query { docs: [Doc] doc(id: String!): Doc } type Doc { _id: String name: String age: Int score: BsonLong price: BsonDecimal128 created: BsonDate oid: BsonObjectId data: BsonBinary }",
            "mappings": {
                "Query": {
                    "docs": { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": {} },
                    "doc":  { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "_id": { "$arg": "id" } }, "first": true }
                }
            }
        }' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"{ doc(id:\"bson-doc\") { _id name age score price created oid data } }"}' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"{ docs { _id score price oid } }"}' >/dev/null || true

    # ------------------------------------------------------------------
    # Transactions — session id + txn id come back in Location headers.
    # ------------------------------------------------------------------
    log_fired POST "$base/_sessions"
    sess_loc=$(curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/_sessions" -i 2>/dev/null \
        | awk 'BEGIN{IGNORECASE=1} /^Location:/{gsub(/[\r\n]/,""); print $2; exit}')
    if [ -n "${sess_loc:-}" ]; then
        sid="${sess_loc##*/}"
        log_fired POST "$base/_sessions/${sid}/_txns"
        txn_loc=$(curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/_sessions/${sid}/_txns" -i 2>/dev/null \
            | awk 'BEGIN{IGNORECASE=1} /^Location:/{gsub(/[\r\n]/,""); print $2; exit}')
        txn_id="${txn_loc##*/}"
        log_fired GET "$base/_sessions/${sid}/_txns"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_sessions/${sid}/_txns" >/dev/null || true
        log_fired POST "$base/halpeople"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X POST "$base/halpeople?sid=${sid}&txn=${txn_id}" \
            -d '{"_id":"in-txn-1","name":"InTxn1","age":11}' >/dev/null || true
        log_fired PATCH "$base/halpeople/alice"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X PATCH "$base/halpeople/alice?sid=${sid}&txn=${txn_id}" \
            -d '{"$set":{"in_txn":true}}' >/dev/null || true
        log_fired PATCH "$base/_sessions/${sid}/_txns/${txn_id}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
            -X PATCH "$base/_sessions/${sid}/_txns/${txn_id}" >/dev/null || true
        log_fired GET "$base/halpeople/in-txn-1"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/in-txn-1" >/dev/null || true

        log_fired POST "$base/_sessions/${sid}/_txns"
        txn_loc2=$(curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/_sessions/${sid}/_txns" -i 2>/dev/null \
            | awk 'BEGIN{IGNORECASE=1} /^Location:/{gsub(/[\r\n]/,""); print $2; exit}')
        txn_id2="${txn_loc2##*/}"
        log_fired POST "$base/halpeople"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X POST "$base/halpeople?sid=${sid}&txn=${txn_id2}" \
            -d '{"_id":"in-txn-aborted","name":"WontExist"}' >/dev/null || true
        log_fired DELETE "$base/_sessions/${sid}/_txns/${txn_id2}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
            -X DELETE "$base/_sessions/${sid}/_txns/${txn_id2}" >/dev/null || true
        log_fired GET "$base/halpeople/in-txn-aborted"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/in-txn-aborted" >/dev/null || true
    fi

    # ------------------------------------------------------------------
    # HAL on write responses — drives BulkResultRepresentationFactory.
    # ------------------------------------------------------------------
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -H 'Accept: application/hal+json' \
        -X POST "$base/halpeople?rep=hal" \
        -d '{"_id":"hal-post","name":"HalPost","age":1}' >/dev/null || true
    log_fired PUT "$base/halpeople/hal-post"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -H 'Accept: application/hal+json' \
        -X PUT "$base/halpeople/hal-post?rep=hal" \
        -d '{"name":"HalPut","age":2}' >/dev/null || true
    log_fired PATCH "$base/halpeople/hal-post"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -H 'Accept: application/hal+json' \
        -X PATCH "$base/halpeople/hal-post?rep=hal&hal=full" \
        -d '{"$set":{"age":3}}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -H 'Accept: application/hal+json' \
        -X POST "$base/halpeople?rep=hal" \
        -d '[{"_id":"hal-b1","name":"B1"},{"_id":"hal-b2","name":"B2"}]' >/dev/null || true

    # Aggregation with array + nested var interpolation.
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople/_meta" \
        -d '{"aggrs":[{"uri":"by-name-list","type":"pipeline","stages":[{"_$match":{"name":{"_$in":{"_$var":"names"}}}},{"_$count":"_count"}]}]}' >/dev/null || true
    sleep 2
    avars_arr='%7B%22names%22:%5B%22Alice%22,%22Bob%22%5D%7D'
    log_fired GET "$base/halpeople/_aggrs/by-name-list"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople/_aggrs/by-name-list?avars=${avars_arr}" >/dev/null || true
    avars_nested='%7B%22cfg%22:%7B%22field%22:%22age%22,%22min%22:25%7D%7D'
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople/_meta" \
        -d '{"aggrs":[{"uri":"with-cfg","type":"pipeline","stages":[{"_$match":{"_$expr":{"_$gte":[{"_$var":"cfg.min"},25]}}}]}]}' >/dev/null || true
    sleep 2
    log_fired GET "$base/halpeople/_aggrs/with-cfg"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        "$base/halpeople/_aggrs/with-cfg?avars=${avars_nested}" >/dev/null || true

    # ------------------------------------------------------------------
    # /token grants — password / client_credentials / refresh_token.
    # ------------------------------------------------------------------
    log_fired POST "$base/token"
    grant_pw_resp=$(curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -X POST "$base/token" \
        -d 'grant_type=password&username=admin&password=secret&scope=read' 2>/dev/null || true)
    valid_jwt=$(printf '%s' "$grant_pw_resp" | jq -r '.access_token // empty' 2>/dev/null || true)
    log_fired POST "$base/token"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -X POST "$base/token" \
        -d 'grant_type=client_credentials&client_id=admin&client_secret=secret' >/dev/null || true
    log_fired POST "$base/token"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -X POST "$base/token" \
        -d 'grant_type=refresh_token&refresh_token=ignored' >/dev/null || true
    log_fired POST "$base/token"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -X POST "$base/token" -d 'grant_type=device_code' >/dev/null || true
    log_fired POST "$base/token"
    curl -sS --max-time 5 \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -X POST "$base/token" \
        -d 'grant_type=password&username=admin&password=wrong' >/dev/null || true

    if [ -n "${valid_jwt:-}" ]; then
        log_fired GET "$base/halpeople"
        curl -sS --max-time 5 -H "Authorization: Bearer $valid_jwt" \
            "$base/halpeople" >/dev/null || true
        log_fired GET "$base/halpeople/alice"
        curl -sS --max-time 5 -H "Authorization: Bearer $valid_jwt" \
            "$base/halpeople/alice" >/dev/null || true
        log_fired POST "$base/halpeople"
        curl -sS --max-time 5 -H "Authorization: Bearer $valid_jwt" \
            -H "$h_json" \
            -X POST "$base/halpeople" -d '{"_id":"jwt-doc","name":"JWT","age":1}' >/dev/null || true
    fi
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 \
        -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature' \
        "$base/halpeople" >/dev/null || true

    # OPTIONS preflight on /token + /graphql.
    log_fired OPTIONS "$base/token"
    curl -sS --max-time 5 -X OPTIONS \
        -H 'Origin: https://example.com' \
        -H 'Access-Control-Request-Method: POST' \
        -H 'Access-Control-Request-Headers: content-type,authorization' \
        "$base/token" >/dev/null || true
    log_fired OPTIONS "$base/graphql"
    curl -sS --max-time 5 -X OPTIONS \
        -H 'Origin: https://example.com' \
        -H 'Access-Control-Request-Method: POST' \
        "$base/graphql" >/dev/null || true

    # Accept-Encoding variants.
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept-Encoding: gzip' \
        "$base/halpeople" -o /dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept-Encoding: deflate' \
        "$base/halpeople" -o /dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept-Encoding: gzip, deflate, br' \
        "$base/halpeople?pagesize=2" -o /dev/null || true

    # Multiple Accept-Language.
    log_fired GET "$base/halpeople/alice"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Accept-Language: en-US,en;q=0.9' \
        "$base/halpeople/alice" >/dev/null || true

    # URL pattern variants — drive MongoMountResolverImpl branches.
    log_fired GET "$base/_size"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_size" >/dev/null || true
    log_fired GET "$base/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/_meta" >/dev/null || true
    log_fired GET "$base/halpeople/"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/" >/dev/null || true
    log_fired GET "$base//halpeople"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base//halpeople" >/dev/null || true
    log_fired GET "$base/halpeople/alice/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/alice/_meta/" >/dev/null || true

    # /metrics format variants.
    log_fired GET "$base/metrics"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Accept: application/openmetrics-text; version=1.0.0; charset=utf-8' \
        "$base/metrics" >/dev/null || true
    log_fired GET "$base/metrics"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
        -H 'Accept: text/plain; version=0.0.4' "$base/metrics" >/dev/null || true

    # ------------------------------------------------------------------
    # GraphQL with INPUT-typed BSON scalars.
    # ------------------------------------------------------------------
    log_fired PUT "$base/gql-apps/bson-types"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PUT "$base/gql-apps/bson-types" \
        -d '{
            "descriptor": { "name": "bson-types", "uri": "bson-types" },
            "schema": "scalar BsonObjectId scalar BsonDecimal128 scalar BsonLong scalar BsonDate scalar BsonBinary type Query { docs: [Doc] doc(id: String!): Doc byOid(oid: BsonObjectId!): [Doc] byScore(min: BsonLong!): [Doc] byPrice(min: BsonDecimal128!): [Doc] byCreated(after: BsonDate!): [Doc] } type Doc { _id: String name: String age: Int score: BsonLong price: BsonDecimal128 created: BsonDate oid: BsonObjectId data: BsonBinary }",
            "mappings": {
                "Query": {
                    "docs":   { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": {} },
                    "doc":    { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "_id": { "$arg": "id" } }, "first": true },
                    "byOid":  { "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "oid": { "$arg": "oid" } } },
                    "byScore":{ "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "score": { "$gte": { "$arg": "min" } } } },
                    "byPrice":{ "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "price": { "$gte": { "$arg": "min" } } } },
                    "byCreated":{ "db": "'"${RESTHEART_DB}"'", "collection": "halpeople", "find": { "created": { "$gte": { "$arg": "after" } } } }
                }
            }
        }' >/dev/null || true

    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"query Q($id:BsonObjectId!){ byOid(oid:$id) { _id name } }","variables":{"id":"507f1f77bcf86cd799439011"}}' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"query Q($m:BsonLong!){ byScore(min:$m) { _id score } }","variables":{"m":"100"}}' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"query Q($m:BsonDecimal128!){ byPrice(min:$m) { _id price } }","variables":{"m":"9.99"}}' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"query Q($d:BsonDate!){ byCreated(after:$d) { _id created } }","variables":{"d":"2020-01-01T00:00:00Z"}}' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"{ byOid(oid:\"507f191e810c19729de860ea\") { _id } }"}' >/dev/null || true
    log_fired POST "$base/graphql/bson-types"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/bson-types" \
        -d '{"query":"query Q($id:BsonObjectId!){ byOid(oid:$id) { _id } }","variables":{"id":"not-a-valid-oid"}}' >/dev/null || true

    # ------------------------------------------------------------------
    # More aggregation pipeline forms.
    # ------------------------------------------------------------------
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople/_meta" \
        -d '{"aggrs":[
            {"uri":"sort-by-age","type":"pipeline","stages":[{"_$sort":{"age":-1}},{"_$limit":5}]},
            {"uri":"project-name-only","type":"pipeline","stages":[{"_$project":{"name":1,"_id":0}}]},
            {"uri":"facet-multi","type":"pipeline","stages":[{"_$facet":{"young":[{"_$match":{"age":{"_$lt":30}}},{"_$count":"_count"}],"old":[{"_$match":{"age":{"_$gte":30}}},{"_$count":"_count"}]}}]},
            {"uri":"lookup-self","type":"pipeline","stages":[{"_$lookup":{"from":"halpeople","localField":"_id","foreignField":"_id","as":"self"}}]}
        ]}' >/dev/null || true
    sleep 2
    local agg_name
    for agg_name in sort-by-age project-name-only facet-multi lookup-self; do
        log_fired GET "$base/halpeople/_aggrs/${agg_name}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_aggrs/${agg_name}" >/dev/null || true
    done
    log_fired GET "$base/halpeople/_aggrs"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_aggrs" >/dev/null || true

    # ------------------------------------------------------------------
    # Range requests on file binary.
    # ------------------------------------------------------------------
    log_fired PUT "$base/range_files.files"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/range_files.files" >/dev/null || true
    printf 'keploy-coverage-range-test-payload-1234567890' > /tmp/restheart-cov-range.bin
    log_fired POST "$base/range_files.files"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X POST "$base/range_files.files" \
        -F 'file=@/tmp/restheart-cov-range.bin' \
        -F 'metadata={"_id":"range-doc","kind":"range"};type=application/json' >/dev/null || true
    rm -f /tmp/restheart-cov-range.bin
    log_fired GET "$base/range_files.files/range-doc/binary"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Range: bytes=0-9' \
        "$base/range_files.files/range-doc/binary" -o /dev/null || true
    log_fired GET "$base/range_files.files/range-doc/binary"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Range: bytes=10-19' \
        "$base/range_files.files/range-doc/binary" -o /dev/null || true
    log_fired GET "$base/range_files.files/range-doc/binary"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H 'Range: bytes=99999-' \
        "$base/range_files.files/range-doc/binary" -o /dev/null || true

    log_fired DELETE "$base/token/no-such-user"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/token/no-such-user" >/dev/null || true

    # ------------------------------------------------------------------
    # OAuth metadata endpoints + Digest auth probes.
    # ------------------------------------------------------------------
    log_fired GET "$base/.well-known/oauth-authorization-server"
    curl -sS --max-time 5 "$base/.well-known/oauth-authorization-server" >/dev/null || true
    log_fired GET "$base/.well-known/oauth-protected-resource"
    curl -sS --max-time 5 "$base/.well-known/oauth-protected-resource" >/dev/null || true
    log_fired GET "$base/.well-known/oauth-protected-resource/halpeople"
    curl -sS --max-time 5 "$base/.well-known/oauth-protected-resource/halpeople" >/dev/null || true
    log_fired GET "$base/.well-known/oauth-authorization-server"
    curl -sS --max-time 5 -H 'X-Forwarded-Host: api.example.com' \
        -H 'X-Forwarded-Proto: https' \
        "$base/.well-known/oauth-authorization-server" >/dev/null || true
    log_fired GET "$base/.well-known/oauth-protected-resource"
    curl -sS --max-time 5 -H 'X-Forwarded-Host: api.example.com' \
        -H 'X-Forwarded-Proto: https' \
        "$base/.well-known/oauth-protected-resource" >/dev/null || true

    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 \
        -H 'Authorization: Digest username="admin", realm="RESTHeart Realm", nonce="abc", uri="/halpeople", response="def"' \
        "$base/halpeople" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -i \
        -H 'Authorization: Digest username="admin"' \
        "$base/halpeople" >/dev/null || true

    # ------------------------------------------------------------------
    # ACL with `mongo` permission fields — drives the three permission
    # interceptors (mongoPermissionFilters / mergeRequest /
    # projectResponse).
    # ------------------------------------------------------------------
    log_fired POST "$base/acl"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/acl" \
        -d '{
            "_id":"reader-mongo-perms",
            "roles":["reader"],
            "predicate":"method(GET) and path-prefix[/halpeople]",
            "mongo": {
                "readFilter": { "name": { "$exists": true } },
                "projectResponse": { "_etag": 0 },
                "mergeRequest": { "lastReadAt": "@now" },
                "allowManagementRequests": false,
                "allowBulkPatch": false,
                "allowBulkDelete": false
            }
        }' >/dev/null || true
    log_fired POST "$base/acl"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/acl" \
        -d '{
            "_id":"writer-mongo-perms",
            "roles":["writer"],
            "predicate":"path-prefix[/halpeople] and (method(POST) or method(PATCH) or method(GET))",
            "mongo": {
                "writeFilter": { "role": { "$ne": "admin" } },
                "readFilter": {},
                "projectResponse": { "secret": 0 },
                "mergeRequest": { "writtenBy": "@user.userid", "writtenAt": "@now" },
                "allowManagementRequests": true,
                "allowBulkPatch": true,
                "allowBulkDelete": false
            }
        }' >/dev/null || true
    sleep 6

    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople" >/dev/null || true
    log_fired GET "$base/halpeople/alice"
    curl -sS --max-time 5 -u reader:reader-secret "$base/halpeople/alice" >/dev/null || true

    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X POST "$base/halpeople" \
        -d '{"_id":"writer-perm-ok","name":"OK","age":1}' >/dev/null || true
    log_fired POST "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X POST "$base/halpeople" \
        -d '{"_id":"writer-perm-bad","name":"Bad","role":"admin"}' >/dev/null || true
    log_fired PATCH "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret -H "$h_json" \
        -X PATCH "$base/halpeople?filter=%7B%22name%22:%22OK%22%7D" \
        -d '{"$set":{"role":"writer"}}' >/dev/null || true
    log_fired DELETE "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret \
        -X DELETE "$base/halpeople?filter=%7B%22name%22:%22OK%22%7D" >/dev/null || true

    # ACL extras — filterOperatorsBlacklist + propertiesBlacklist.
    log_fired POST "$base/acl"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/acl" \
        -d '{
            "_id":"writer-extras",
            "roles":["writer"],
            "predicate":"path-prefix[/halpeople] and method(GET)",
            "mongo": {
                "filterOperatorsBlacklist": ["$where", "$expr", "$function"],
                "propertiesBlacklist": ["password", "token", "secret"],
                "writeFilter": {},
                "readFilter": {}
            }
        }' >/dev/null || true
    sleep 6
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret \
        "$base/halpeople?filter=%7B%22%24where%22:%221%3D%3D1%22%7D" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret \
        "$base/halpeople?filter=%7B%22%24expr%22:%7B%22%24eq%22:%5B%22%24age%22,%2230%22%5D%7D%7D" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret \
        "$base/halpeople?keys=%7B%22password%22:1%7D" >/dev/null || true
    log_fired GET "$base/halpeople"
    curl -sS --max-time 5 -u writer:writer-secret \
        "$base/halpeople?filter=%7B%22age%22:%7B%22%24gte%22:1%7D%7D" >/dev/null || true

    # ------------------------------------------------------------------
    # Multiple collections + databases — drives MongoMountResolverImpl.
    # ------------------------------------------------------------------
    local coll encoded
    for coll in coll_a coll_b coll_with_dashes coll.with.dots; do
        encoded=$(printf '%s' "$coll" | sed 's/\./%2E/g')
        log_fired PUT "$base/${encoded}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/$encoded" >/dev/null || true
        log_fired POST "$base/${encoded}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X POST "$base/$encoded" -d '{"_id":"d1","v":1}' >/dev/null || true
        log_fired GET "$base/${encoded}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/$encoded" >/dev/null || true
        log_fired GET "$base/${encoded}/_size"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/$encoded/_size" >/dev/null || true
        log_fired DELETE "$base/${encoded}/d1"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/$encoded/d1" >/dev/null || true
    done

    local db_name d_etag t_etag
    for db_name in db_alpha db_beta; do
        log_fired PUT "$base/${db_name}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/$db_name" >/dev/null || true
        log_fired PUT "$base/${db_name}/things"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X PUT "$base/$db_name/things" >/dev/null || true
        log_fired POST "$base/${db_name}/things"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
            -X POST "$base/$db_name/things" -d '{"_id":"x","v":1}' >/dev/null || true
        log_fired GET "$base/${db_name}/things"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/$db_name/things" >/dev/null || true
        d_etag=$(curl -sSI --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/$db_name" 2>/dev/null \
            | awk 'BEGIN{IGNORECASE=1} /^ETag:/{gsub(/[\r\n"]/,"",$2); print $2; exit}')
        t_etag=$(curl -sSI --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/$db_name/things" 2>/dev/null \
            | awk 'BEGIN{IGNORECASE=1} /^ETag:/{gsub(/[\r\n"]/,"",$2); print $2; exit}')
        if [ -n "${t_etag:-}" ]; then
            log_fired DELETE "$base/${db_name}/things"
            curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
                -H "If-Match: ${t_etag}" -X DELETE "$base/$db_name/things" >/dev/null || true
        fi
        if [ -n "${d_etag:-}" ]; then
            log_fired DELETE "$base/${db_name}"
            curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" \
                -H "If-Match: ${d_etag}" -X DELETE "$base/$db_name" >/dev/null || true
        fi
    done

    # ------------------------------------------------------------------
    # More aggregations + GraphQL alias / fragments / multi-op.
    # ------------------------------------------------------------------
    log_fired PATCH "$base/halpeople/_meta"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X PATCH "$base/halpeople/_meta" \
        -d '{"aggrs":[
            {"uri":"group-by-tag","type":"pipeline","stages":[{"_$unwind":"$tags"},{"_$group":{"_id":"$tags","count":{"_$sum":1}}}]},
            {"uri":"sort-asc","type":"pipeline","stages":[{"_$sort":{"_id":1}}]},
            {"uri":"limit-3","type":"pipeline","stages":[{"_$limit":3}]}
        ]}' >/dev/null || true
    sleep 2
    for agg_name in group-by-tag sort-asc limit-3; do
        log_fired GET "$base/halpeople/_aggrs/${agg_name}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" "$base/halpeople/_aggrs/${agg_name}" >/dev/null || true
    done

    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"{ first: people { _id name } second: people { _id age } }"}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"fragment P on Person { _id name age } query { people { ...P } }"}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"query A { people { _id } } query B { people { name } }","operationName":"B"}' >/dev/null || true
    log_fired POST "$base/graphql/halpeople"
    curl -sS --max-time 8 -H "Authorization: $RESTHEART_ADMIN_AUTH" -H "$h_json" \
        -X POST "$base/graphql/halpeople" \
        -d '{"query":"query Q($id:String){ person(id:$id) { _id } }","variables":{"id":null}}' >/dev/null || true

    # ------------------------------------------------------------------
    # Cleanup — drop the non-admin users + ACL rules created above.
    # ------------------------------------------------------------------
    log_fired DELETE "$base/users/reader"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/users/reader" >/dev/null || true
    log_fired DELETE "$base/users/writer"
    curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/users/writer" >/dev/null || true
    local rule_id
    for rule_id in reader-get-halpeople reader-blacklist reader-self-equals \
            reader-localhost writer-bson-whitelist writer-bson-blacklist \
            writer-bson-contains reader-roles-array reader-qparam-var \
            reader-qparam-size reader-mongo-perms writer-mongo-perms writer-extras; do
        log_fired DELETE "$base/acl/${rule_id}"
        curl -sS --max-time 5 -H "Authorization: $RESTHEART_ADMIN_AUTH" -X DELETE "$base/acl/$rule_id" >/dev/null || true
    done
}

# restheart_report_coverage (real Java line coverage via JaCoCo).
#
# Requires the docker-compose.coverage.yml overlay — the base
# compose is uninstrumented so keploy CI lanes (enterprise,
# integrations) pay zero JVM-instrumentation cost. When called
# from a base-compose run this function detects the missing
# coverage image and exits 0 cleanly so `flow.sh coverage || true`
# informational hooks don't break.
#
# Mechanics:
#   - The overlay's Dockerfile.coverage layers JaCoCo's agent jar
#     into the upstream restheart image; the overlay compose sets
#     JAVA_TOOL_OPTIONS=-javaagent:.../jacocoagent.jar=output=tcpserver,...
#     so the agent listens on port 6300 inside the container.
#   - This function uses the coverage image (which has java +
#     jacococli.jar) to dump execution data over TCP into
#     /coverage/jacoco.exec, then renders a JaCoCo XML report
#     against /opt/restheart/restheart.jar's classfiles.
#   - The XML's <counter type="LINE" missed covered/> rows under
#     <report> aggregate every analysed class; we sum and emit a
#     `Covered N/M (XX.X%)` line in the helper-script's expected
#     format.
restheart_report_coverage() {
    local app="${RESTHEART_APP_CONTAINER:-restheart_app}"
    local data_dir="${RESTHEART_COVERAGE_DATA_DIR:-${PWD}/coverage}"
    local report_file="${COVERAGE_REPORT_FILE:-coverage_report.txt}"
    local image="${RESTHEART_COVERAGE_IMAGE:-restheart-mongo:local-coverage}"
    local jacoco_port="${RESTHEART_JACOCO_PORT:-6300}"

    if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${app}$"; then
        echo "INFO: ${app} not running — coverage report skipped"
        : >"$report_file"
        return 0
    fi
    if ! docker image inspect "$image" >/dev/null 2>&1; then
        echo "INFO: coverage image ${image} not built — base image is uninstrumented (apply docker-compose.coverage.yml overlay to enable)"
        : >"$report_file"
        return 0
    fi

    # Locate the docker network the running container is on so the
    # one-off jacococli container can reach :6300 via container DNS.
    local network
    network=$(docker inspect "$app" --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{println}}{{end}}' 2>/dev/null | head -1 | tr -d ' \r\n')
    if [ -z "$network" ]; then
        echo "ERROR: could not resolve docker network for ${app}" >&2
        return 1
    fi

    docker run --rm --network "$network" -v "${data_dir}:/coverage" --entrypoint java "$image" \
        -jar /opt/jacoco/jacococli.jar dump \
        --address "$app" --port "$jacoco_port" \
        --destfile /coverage/jacoco.exec >/dev/null

    docker run --rm -v "${data_dir}:/coverage" --entrypoint java "$image" \
        -jar /opt/jacoco/jacococli.jar report /coverage/jacoco.exec \
        --xml /coverage/report.xml \
        --classfiles /opt/restheart/restheart.jar >/dev/null

    # Parse the top-level <counter type="LINE" .../> rows from the
    # JaCoCo XML. Use python3 inside the alpine helper so we don't
    # rely on the host having lxml/xmlstarlet/etc.
    local pct missed covered total
    read -r missed covered <<<"$(docker run --rm -v "${data_dir}:/coverage" python:3.12-alpine python3 -c '
import xml.etree.ElementTree as ET
root = ET.parse("/coverage/report.xml").getroot()
miss = sum(int(c.get("missed",0)) for c in root.findall("counter") if c.get("type") == "LINE")
cov  = sum(int(c.get("covered",0)) for c in root.findall("counter") if c.get("type") == "LINE")
print(miss, cov)
')"
    total=$((missed + covered))
    pct=$(awk -v c="$covered" -v t="$total" 'BEGIN{if(t>0)printf "%.1f", c*100/t; else print "0.0"}')

    {
        echo "============== RESTHeart line coverage (JaCoCo) =============="
        echo "Lines missed:  ${missed}"
        echo "Lines covered: ${covered}"
        echo "Lines total:   ${total}"
        echo ""
        echo "Covered ${covered}/${total} (${pct}%)"
        echo "=============================================================="
    } | tee "$report_file"
}

case "${1:-}" in
    bootstrap)        restheart_bootstrap "${2:-180}" ;;
    record-traffic)   restheart_record_traffic ;;
    coverage)         restheart_report_coverage ;;
    *)
        echo "usage: $0 {bootstrap|record-traffic|coverage}" >&2
        exit 2 ;;
esac
