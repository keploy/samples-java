#!/usr/bin/env bash
# simulate_tosca_flow.sh — narrated "Tosca drives Fiori; Keploy records the
# 3-way fan-out underneath" demo for the Customer 360 service.
#
# Run this while `keploy record` is recording the service in another terminal.
# Default target is the in-cluster service exposed at http://localhost:30080
# (NodePort from kind). Override with --host for local runs.

set -euo pipefail

cd "$(dirname "$0")"

HOST="${HOST:-http://localhost:30080}"
FAST=0

while [ $# -gt 0 ]; do
  case "$1" in
    --fast) FAST=1; shift ;;
    --host) HOST="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "unknown arg: $1"; exit 2 ;;
  esac
done

BOLD='\033[1m'; DIM='\033[2m'; GREEN='\033[0;32m'; BLUE='\033[0;34m'
YELLOW='\033[1;33m'; MAGENTA='\033[0;35m'; NC='\033[0m'

pause() { [ "$FAST" = "1" ] || sleep "${1:-2}"; }
tosca_step()  { printf "\n${BOLD}${MAGENTA}[TOSCA UI]${NC}  %s\n" "$1"; pause 1; }
tosca_click() { printf "${DIM}           ↳ clicks:${NC} %s\n" "$1"; pause 1; }
backend()     { printf "${BOLD}${BLUE}[KEPLOY ]${NC}  ${DIM}%s${NC}\n" "$1"; }

call() {
  local label="$1"; shift
  local status
  status=$(curl -sw '%{http_code}' -o /tmp/simulate-body.json "$@" || echo "000")
  local color="${GREEN}"
  [[ "$status" =~ ^[45] ]] && color="${YELLOW}"
  printf "${BOLD}${color}[HTTP  ]${NC}  %-48s %s\n" "$label" "$status"
}

check_reachable() {
  if ! curl -sf -o /dev/null "${HOST}/actuator/health" 2>/dev/null; then
    printf "${BOLD}${YELLOW}service not reachable at ${HOST}${NC}\n"
    printf "start it first:\n"
    printf "  ${DIM}./deploy_kind.sh${NC}         (k8s mode)\n"
    printf "  ${DIM}./demo_script.sh record-local${NC}  (local mode)\n"
    exit 1
  fi
}

banner() {
  printf "\n${BOLD}"
  printf "═══════════════════════════════════════════════════════════════════\n"
  printf "  Simulated Tosca-driven Fiori flow: 'Customer 360 in Sales Cockpit'\n"
  printf "  Keploy records every outbound SAP OData call in parallel.\n"
  printf "  Target: ${HOST}\n"
  printf "═══════════════════════════════════════════════════════════════════${NC}\n"
}

check_reachable
banner
pause 2

# ─────────────────────────────────────────────────────────────────────────────
tosca_step "Opening Sales Cockpit → clicking 'Customers' tile"
tosca_click "Customers launchpad tile"
backend "Tile click fans out: list query + KPI count"
call "GET  /api/v1/customers/count           (KPI tile)"  "${HOST}/api/v1/customers/count"
pause 2
call "GET  /api/v1/customers?top=5           (list grid)" "${HOST}/api/v1/customers?top=5"
pause 2

# ─────────────────────────────────────────────────────────────────────────────
tosca_step "On the customer list, opening row BP=11"
tosca_click "Row BusinessPartner=11"
backend "Detail fetch — single SAP OData GET"
call "GET  /api/v1/customers/11              (detail)"    "${HOST}/api/v1/customers/11"
pause 2

# ─────────────────────────────────────────────────────────────────────────────
tosca_step "User clicks '360° view' on BP=202 — this is the fan-out moment"
tosca_click "360° view button"
backend "ONE inbound → THREE parallel SAP OData calls:"
backend "  • /A_BusinessPartner('202')"
backend "  • /A_BusinessPartner('202')/to_BusinessPartnerAddress"
backend "  • /A_BusinessPartner('202')/to_BusinessPartnerRole"
backend "Tosca asserts on the UI tile. Keploy captures all three on the wire."
call "GET  /api/v1/customers/202/360         (360 fan-out)" "${HOST}/api/v1/customers/202/360"
pause 3

# ─────────────────────────────────────────────────────────────────────────────
tosca_step "Drilling into a second customer — BP=11 360"
tosca_click "Back → select BP=11 → 360° view"
call "GET  /api/v1/customers/11/360          (360 fan-out)" "${HOST}/api/v1/customers/11/360"
pause 2

# ─────────────────────────────────────────────────────────────────────────────
printf "\n${BOLD}${GREEN}"
printf "═══════════════════════════════════════════════════════════════════\n"
printf "  Tosca flow complete. What happened in two panes:\n"
printf "═══════════════════════════════════════════════════════════════════${NC}\n\n"

cat <<'EOF'
  ┌─────────────────────────────┬─────────────────────────────────────────┐
  │   TOSCA (this terminal)     │   KEPLOY (other terminal)               │
  ├─────────────────────────────┼─────────────────────────────────────────┤
  │ • 5 Fiori interactions      │ • 5 inbound HTTP test cases captured    │
  │ • Asserted on UI state      │ • ~11 outbound SAP OData mocks          │
  │ • Zero backend visibility   │   (2× 360 fan-out = 6 mocks alone)      │
  │                             │ • Full vertical slice: UI click→DB row  │
  └─────────────────────────────┴─────────────────────────────────────────┘

  One UI flow, two coverage layers. Tosca owns the surface. Keploy owns
  the plumbing Tosca could not see before — especially the hidden parallel
  fan-out behind the 360° tile.

  Stop Keploy in its terminal, then:
    ./demo_script.sh offline-test   (local mode)

  to replay the same flow with SAP blackholed in /etc/hosts.
EOF

printf "\n"
