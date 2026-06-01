#!/usr/bin/env bash
# Shared helpers for scripts/script-*.sh.
#
# Each script-N.sh sources this file and then calls:
#   run_test_set <target-test-set>  <curl-fn>
# where <curl-fn> is a shell function that fires the HTTP requests
# the test-set should capture.
#
# Layered behaviour:
#   - Boots Aerospike CE via docker compose if not already up.
#   - Builds the Spring Boot JAR.
#   - Starts `keploy record` in the background; waits for the app to
#     answer /health before firing the curls.
#   - SIGINTs keploy when curls are done.
#   - Normalises the recorded test-set path to ./keploy/<target>.
#   - Adds `body.duration: []` to noise on any /parallel, /multiclient
#     or /freshclient test (their responses carry wall-clock duration
#     that drifts every run).
#   - Replays the test-set and exits non-zero if any case fails.

set -euo pipefail

: "${KEPLOY:=sudo keploy}"
: "${PORT:=8090}"
: "${LOG_DIR:=/tmp}"
: "${SKIP_DOCKER:=}"
: "${SKIP_BUILD:=}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

bring_up_aerospike() {
    [ -n "${SKIP_DOCKER:-}" ] && return 0
    echo "==> docker compose up -d aerospike"
    docker compose up -d aerospike
    for _ in $(seq 1 30); do
        if docker compose ps aerospike --format '{{.Health}}' 2>/dev/null | grep -q healthy; then
            return 0
        fi
        sleep 2
    done
    echo "ERROR: aerospike never reported healthy" >&2
    docker compose logs aerospike | tail -30 >&2
    return 1
}

build_app() {
    [ -n "${SKIP_BUILD:-}" ] && return 0
    echo "==> mvn -q -DskipTests package"
    mvn -q -DskipTests package
}

wait_for_app_ready() {
    echo "==> waiting for the sample to answer on :$PORT"
    for _ in $(seq 1 120); do
        if curl -sf -o /dev/null --max-time 1 "http://127.0.0.1:$PORT/health"; then
            sleep 3
            return 0
        fi
        sleep 1
    done
    echo "ERROR: app never answered /health" >&2
    return 1
}

stop_keploy() {
    sudo pkill -SIGINT keploy 2>/dev/null || true
    for _ in $(seq 1 15); do
        if ! pgrep -af "keploy record" >/dev/null 2>&1; then return 0; fi
        sleep 1
    done
    echo "WARN: keploy didn't exit on SIGINT, killing"
    sudo pkill -KILL keploy 2>/dev/null || true
}

# After record, keploy may have written ./keploy/keploy/test-set-N
# (nested) instead of ./keploy/test-set-N (flat), and it auto-numbers
# the fresh recording off whatever already exists under ./keploy/.
# Normalise: flatten the nested case, then rename whichever fresh
# test-set-* we got to the requested target.
normalise_recording() {
    local target="$1"
    if [ -d "./keploy/keploy" ]; then
        sudo chown -R "$(id -u):$(id -g)" ./keploy/keploy
        for d in ./keploy/keploy/test-set-*; do
            [ -d "$d" ] || continue
            mv "$d" "./keploy/"
        done
        rmdir ./keploy/keploy 2>/dev/null || true
    fi
    [ -d "./keploy/$target" ] && return 0

    local newest="" newest_mtime=0
    for d in ./keploy/test-set-*; do
        [ -d "$d" ] || continue
        local m
        m=$(stat -c %Y "$d")
        if [ "$m" -gt "$newest_mtime" ]; then
            newest="$d"; newest_mtime="$m"
        fi
    done
    if [ -n "$newest" ]; then
        mv "$newest" "./keploy/$target"
        return 0
    fi
    echo "ERROR: $target was not recorded — check $LOG_DIR/keploy-record-$target.log" >&2
    return 1
}

apply_duration_noise() {
    local target="$1"
    local applied=0
    for f in ./keploy/"$target"/tests/post-parallel-*.yaml \
             ./keploy/"$target"/tests/post-multiclient-*.yaml \
             ./keploy/"$target"/tests/post-freshclient-*.yaml; do
        [ -e "$f" ] || continue
        if ! grep -q "body.duration:" "$f"; then
            sed -i 's|header.Date: \[\]|header.Date: []\n      body.duration: []|' "$f"
            applied=$((applied+1))
        fi
    done
    [ "$applied" -gt 0 ] && echo "==> applied body.duration noise to $applied test(s)"
    return 0
}

run_test_set() {
    local target="$1"
    local curl_fn="$2"

    bring_up_aerospike
    build_app

    echo "==> clearing any stale ./keploy/$target"
    sudo rm -rf "./keploy/$target"

    local log="$LOG_DIR/keploy-record-$target.log"
    echo "==> starting keploy record (logging to $log)"
    $KEPLOY record > "$log" 2>&1 &
    local keploy_pid=$!
    trap 'stop_keploy' EXIT

    wait_for_app_ready
    echo "==> firing curls for $target"
    $curl_fn
    sleep 3

    echo "==> stopping keploy record"
    stop_keploy
    trap - EXIT
    wait "$keploy_pid" 2>/dev/null || true

    normalise_recording "$target"
    apply_duration_noise "$target"

    echo "==> $KEPLOY test --test-sets $target"
    $KEPLOY test --test-sets "$target"
}
