#!/usr/bin/env bash
# run_flow.sh — drive the Customer 360 business flow against a deployed API.
#
# Each "iteration" plays a 20-call slice of realistic enterprise behaviour:
# KPI refresh → detail drill-downs → 360° composites → write operations
# (tags + notes) → cleanup → one negative validation test. 25 iterations
# = 500 inbound calls = roughly 1000+ outbound SAP/Postgres operations
# for Keploy to record at the wire level.
#
# Varies the customer ID across a small pool (11, 202, 203) to get
# diverse SAP responses, and generates unique tag names per call so
# writes always take the insert path (not the idempotent no-op path).
#
# Auto-detects the base URL; override with --host.
#
# Usage:
#   ./run_flow.sh                         # default: 25 iterations → 500 calls
#   ./run_flow.sh --iterations 50         # 50 iterations → 1000 calls
#   ./run_flow.sh --host http://...       # explicit URL
#   ./run_flow.sh --trace                 # + tail correlated pod logs at end
#   ./run_flow.sh --quiet                 # summary only
#   ./run_flow.sh --iterations 1 --verbose  # narrate every call in detail
#
# Exit code: 0 if all calls passed. Non-zero otherwise.

set -euo pipefail

cd "$(dirname "$0")"

# --- colours ----------------------------------------------------------------
BOLD='\033[1m'; DIM='\033[2m'; NC='\033[0m'
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'

say()  { [ "${QUIET:-0}" = 1 ] || printf "${BOLD}${GREEN}==>${NC} %s\n" "$*"; }
note() { [ "${QUIET:-0}" = 1 ] || printf "${DIM}%s${NC}\n" "$*"; }
warn() { printf "${BOLD}${YELLOW}!!${NC}  %s\n" "$*"; }
fail() { printf "${BOLD}${RED}XX${NC}  %s\n" "$*"; }

# --- args -------------------------------------------------------------------
HOST=""
ITERATIONS=25
TRACE=0
QUIET=0
VERBOSE=0
while [ $# -gt 0 ]; do
  case "$1" in
    --host)        HOST="$2"; shift 2 ;;
    --iterations)  ITERATIONS="$2"; shift 2 ;;
    --trace)       TRACE=1; shift ;;
    --quiet)       QUIET=1; shift ;;
    --verbose)     VERBOSE=1; shift ;;
    -h|--help)
      sed -n '2,26p' "$0" | sed 's/^# \{0,1\}//'
      exit 0 ;;
    *) fail "unknown arg: $1"; exit 2 ;;
  esac
done

# --- URL auto-detect --------------------------------------------------------
CANDIDATES=(
  "${HOST:-}"
  "http://customer360.localtest.me"
  "http://localhost"
  "http://localhost:30080"
  "http://localhost:8080"
)
BASE=""
for u in "${CANDIDATES[@]}"; do
  [ -z "$u" ] && continue
  if curl -s -o /dev/null -m 3 -f "$u/actuator/health/liveness" 2>/dev/null; then
    BASE="$u"
    break
  fi
done
if [ -z "$BASE" ]; then
  fail "Customer 360 not reachable on any of:"
  for u in "${CANDIDATES[@]}"; do [ -n "$u" ] && printf "   %s\n" "$u"; done
  fail "Is the deployment up? Try:  sudo kubectl -n sap-demo get pods"
  exit 1
fi
say "target: ${BOLD}${BASE}${NC}   iterations: ${ITERATIONS}   expected calls: $((ITERATIONS * 20))"

# --- per-run correlation prefix --------------------------------------------
RUN_ID="flow-$(date +%Y%m%d-%H%M%S)-$$"

# --- tracking state --------------------------------------------------------
PASS_COUNT=0
FAIL_COUNT=0
FAIL_LAST=""
CALL_SEQ=0
CNT_SAP=0
CNT_DB=0
CNT_MIXED=0
CNT_WRITE=0

# Quiet call helper: no per-line output unless VERBOSE=1. Still counts.
# Args: category (SAP|DB|MIXED|WRITE), method, path, [extra curl args...]
# Optional trailing `-- EXPECT N` at the end to assert a specific HTTP code.
call() {
  local category="$1" method="$2" path="$3"; shift 3
  local expect=""
  # Extract optional "-- EXPECT N" from args
  local filtered=()
  while [ $# -gt 0 ]; do
    if [ "$1" = "--" ] && [ "${2:-}" = "EXPECT" ] && [ -n "${3:-}" ]; then
      expect="$3"; shift 3
    else
      filtered+=("$1"); shift
    fi
  done

  local cid="${RUN_ID}-${CALL_SEQ}"
  CALL_SEQ=$((CALL_SEQ + 1))

  local status
  if [ ${#filtered[@]} -gt 0 ]; then
    status=$(curl -sS -m 90 -o /dev/null -w "%{http_code}" \
      -X "${method}" \
      -H "X-Correlation-ID: ${cid}" \
      "${filtered[@]}" \
      "${BASE}${path}" || echo "000")
  else
    status=$(curl -sS -m 90 -o /dev/null -w "%{http_code}" \
      -X "${method}" \
      -H "X-Correlation-ID: ${cid}" \
      "${BASE}${path}" || echo "000")
  fi

  local pass=0
  if [ -n "${expect}" ]; then
    [ "${status}" = "${expect}" ] && pass=1
  else
    [[ "${status}" =~ ^2 ]] && pass=1
  fi

  if [ "${pass}" = 1 ]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    case "${category}" in
      SAP)   CNT_SAP=$((CNT_SAP + 1)) ;;
      DB)    CNT_DB=$((CNT_DB + 1)) ;;
      MIXED) CNT_MIXED=$((CNT_MIXED + 1)) ;;
      WRITE) CNT_WRITE=$((CNT_WRITE + 1)) ;;
    esac
    if [ "${VERBOSE}" = 1 ]; then
      printf "  ${GREEN}${status}${NC}  %-5s %-42s  ${DIM}[%s]${NC}\n" "${method}" "${path}" "${category}"
    fi
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    FAIL_LAST="${method} ${path} → got ${status}${expect:+ (expected ${expect})}"
    printf "  ${RED}${status}${NC}  %-5s %-42s  ${DIM}[%s]${NC}  ${RED}FAIL${NC}\n" \
      "${method}" "${path}" "${category}"
  fi
}

# --- the per-iteration flow (20 calls) -------------------------------------
# Breakdown:
#   A tile-refresh block       → 5 calls  (2 DB-light health, 2 SAP, 1 DB-read)
#   B detail drill-downs       → 6 calls  (2 SAP + 4 DB)
#   C 360° composites          → 3 calls  (MIXED: each fires 3 SAP + 2 DB + 1 audit)
#   D write operations         → 4 calls  (2 tag inserts + 2 note inserts, all WRITE)
#   E cleanup + negative test  → 2 calls  (1 DELETE, 1 validation 400)
BP_POOL=(11 202 203)
run_once() {
  local iter="$1"
  local bp_a="${BP_POOL[$((iter % 3))]}"
  local bp_b="${BP_POOL[$(((iter + 1) % 3))]}"
  local bp_c="${BP_POOL[$(((iter + 2) % 3))]}"

  # Block A — platform/KPI refresh
  call DB    GET /actuator/health/liveness
  call DB    GET /actuator/health/readiness
  call SAP   GET /api/v1/customers/count
  call SAP   GET "/api/v1/customers?top=5"
  call DB    GET /api/v1/customers/recent-views

  # Block B — detail drill-downs
  call SAP   GET "/api/v1/customers/${bp_a}"
  call SAP   GET "/api/v1/customers/${bp_b}"
  call DB    GET "/api/v1/customers/${bp_a}/tags"
  call DB    GET "/api/v1/customers/${bp_b}/tags"
  call DB    GET "/api/v1/customers/${bp_a}/notes"
  call DB    GET "/api/v1/customers/${bp_b}/notes"

  # Block C — 360° (each: 3 SAP + 2 DB + 1 audit insert — the fan-out story)
  call MIXED GET "/api/v1/customers/${bp_a}/360"
  call MIXED GET "/api/v1/customers/${bp_b}/360"
  call MIXED GET "/api/v1/customers/${bp_c}/360"

  # Block D — writes (unique tag/note per call so every INSERT takes)
  local nonce="i${iter}-$(printf '%04x' $((RANDOM + CALL_SEQ)))"
  call WRITE POST "/api/v1/customers/${bp_a}/tags" \
    -H "Content-Type: application/json" \
    -d "{\"tag\":\"demo-${nonce}\",\"createdBy\":\"flow\"}"
  call WRITE POST "/api/v1/customers/${bp_b}/tags" \
    -H "Content-Type: application/json" \
    -d "{\"tag\":\"priority-${nonce}\",\"createdBy\":\"flow\"}"
  call WRITE POST "/api/v1/customers/${bp_a}/notes" \
    -H "Content-Type: application/json" \
    -d "{\"body\":\"Iteration ${iter}: customer profile reviewed by flow harness\",\"author\":\"flow\"}"
  call WRITE POST "/api/v1/customers/${bp_b}/notes" \
    -H "Content-Type: application/json" \
    -d "{\"body\":\"Iteration ${iter}: follow-up scheduled\",\"author\":\"flow\"}"

  # Block E — cleanup + negative validation
  call WRITE DELETE "/api/v1/customers/${bp_a}/tags/demo-${nonce}"
  call DB    GET "/api/v1/customers/bad!!id/360" -- EXPECT 400
}

# --- main loop --------------------------------------------------------------
START_TS=$(date +%s)
LAST_PROGRESS=0
for i in $(seq 1 "${ITERATIONS}"); do
  run_once "$i"
  # Progress line every 5% or every 5 iterations, whichever is coarser.
  local_mod=$(( ITERATIONS / 20 ))
  [ "${local_mod}" -lt 1 ] && local_mod=1
  if [ $((i % local_mod)) = 0 ] || [ "$i" = "${ITERATIONS}" ]; then
    if [ "${QUIET}" != 1 ]; then
      printf "${DIM}  [iter %3d/%d]  calls=%-4d  pass=%d  fail=%d${NC}\n" \
        "$i" "${ITERATIONS}" "${CALL_SEQ}" "${PASS_COUNT}" "${FAIL_COUNT}"
    fi
  fi
done
END_TS=$(date +%s)

# --- optional log tail ------------------------------------------------------
if [ "${TRACE}" = 1 ]; then
  say "trace: a sample of pod log lines tagged with run id ${RUN_ID} (first 60)"
  if command -v kubectl >/dev/null; then
    sudo kubectl -n sap-demo logs deploy/customer360 --tail=$((ITERATIONS * 100)) 2>/dev/null \
      | grep -F "${RUN_ID}" \
      | head -60 \
      | python3 -c "
import json,sys
for line in sys.stdin:
    try:
        d=json.loads(line)
        logger=d.get('logger','').split('.')[-1]
        print(f\"  {d['ts']}  {logger:<32}  {d['msg'][:140]}\")
    except Exception: pass" || true
  else
    warn "kubectl not on PATH — skipping log tail"
  fi
fi

# --- summary ----------------------------------------------------------------
TOTAL=$((PASS_COUNT + FAIL_COUNT))
WALL=$((END_TS - START_TS))
[ "${WALL}" -le 0 ] && WALL=1
RPS=$(awk -v t="${TOTAL}" -v w="${WALL}" 'BEGIN { printf "%.1f", t/w }')

printf "\n${BOLD}───────────────────────────────────────────────────────────────${NC}\n"
printf "${BOLD}  %s${NC}\n" "$(basename "${BASE}") — ${ITERATIONS} iterations in ${WALL}s (${RPS} req/s)"
printf "${BOLD}───────────────────────────────────────────────────────────────${NC}\n"
printf "  SAP-backed reads                 %d\n" "${CNT_SAP}"
printf "  Postgres-only reads              %d\n" "${CNT_DB}"
printf "  MIXED (SAP + DB fan-outs)        %d  ${DIM}(each ≈ 6 backend ops)${NC}\n" "${CNT_MIXED}"
printf "  Writes (inserts/deletes)         %d\n" "${CNT_WRITE}"
printf "  ──────────────────────────────────────\n"
if [ "${FAIL_COUNT}" = 0 ]; then
  printf "  ${BOLD}${GREEN}PASS${NC}   %d/%d calls ok\n" "${PASS_COUNT}" "${TOTAL}"
  printf "  ${DIM}run id: ${RUN_ID}${NC}\n"
  printf "\n  ${DIM}estimated backend operations captured by Keploy:${NC}\n"
  # SAP: 1 HTTP each. DB: 1 Postgres query each. MIXED: ~6 (3 SAP + 2 DB + 1 INSERT).
  # WRITE: 2 (1 INSERT + 1 audit INSERT).
  BACKEND=$(( CNT_SAP + CNT_DB + (CNT_MIXED * 6) + (CNT_WRITE * 2) ))
  printf "  ${DIM}  SAP HTTPS:     ~%d   Postgres:      ~%d   Total: ~%d${NC}\n" \
    "$(( CNT_SAP + (CNT_MIXED * 3) ))" \
    "$(( CNT_DB + (CNT_MIXED * 3) + (CNT_WRITE * 2) ))" \
    "${BACKEND}"
  exit 0
else
  printf "  ${BOLD}${RED}FAIL${NC}   %d/%d calls failed\n" "${FAIL_COUNT}" "${TOTAL}"
  printf "  last error: %s\n" "${FAIL_LAST}"
  exit 1
fi
