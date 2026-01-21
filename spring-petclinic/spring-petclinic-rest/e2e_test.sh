#!/bin/bash
#
# E2E Test Script for Spring PetClinic REST API
# Generates 1000 connected transactions across Owner→Pet→Visit and Vet→Specialty chains
# Self-cleaning and idempotent - cleans up existing data before running
#

# set -e  # Disabled for better error visibility

# Configuration
BASE_URL="${BASE_URL:-http://localhost:9966/petclinic/api}"
TOTAL_TRANSACTIONS=1000
REQUESTS_PER_CHAIN=6  # pettype + owner + pet + visit + specialty + vet
CHAINS_NEEDED=$((TOTAL_TRANSACTIONS / REQUESTS_PER_CHAIN))

# Counters - using temp files to persist across subshells
COUNTER_FILE=$(mktemp)
echo "0 0 0" > "$COUNTER_FILE"  # success failure total

increment_success() {
    local counts
    read -r success failure total < "$COUNTER_FILE"
    echo "$((success + 1)) $failure $((total + 1))" > "$COUNTER_FILE"
}

increment_failure() {
    local counts
    read -r success failure total < "$COUNTER_FILE"
    echo "$success $((failure + 1)) $((total + 1))" > "$COUNTER_FILE"
}

get_counts() {
    cat "$COUNTER_FILE"
}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Wait for application to be ready
wait_for_app() {
    log_info "Waiting for application to be ready..."
    local max_attempts=60
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "${BASE_URL}/owners" > /dev/null 2>&1; then
            log_success "Application is ready!"
            return 0
        fi
        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done
    
    log_error "Application not ready after ${max_attempts} attempts"
    return 1
}

# Cleanup function - deletes all data in correct order to respect FK constraints
cleanup_database() {
    log_info "Cleaning up existing data (respecting foreign key constraints)..."
    
    # Get all visits and delete them
    log_info "  Deleting visits..."
    local visits=$(curl -s "${BASE_URL}/visits" 2>/dev/null || echo "[]")
    if [ "$visits" != "[]" ] && [ -n "$visits" ]; then
        echo "$visits" | jq -r '.[].id' 2>/dev/null | while read -r id; do
            if [ -n "$id" ] && [ "$id" != "null" ]; then
                curl -s -X DELETE "${BASE_URL}/visits/${id}" > /dev/null 2>&1 || true
            fi
        done
    fi
    
    # Get all pets and delete them
    log_info "  Deleting pets..."
    local owners=$(curl -s "${BASE_URL}/owners" 2>/dev/null || echo "[]")
    if [ "$owners" != "[]" ] && [ -n "$owners" ]; then
        echo "$owners" | jq -r '.[].id' 2>/dev/null | while read -r owner_id; do
            if [ -n "$owner_id" ] && [ "$owner_id" != "null" ]; then
                local pets=$(curl -s "${BASE_URL}/owners/${owner_id}" 2>/dev/null | jq -r '.pets[].id' 2>/dev/null || echo "")
                for pet_id in $pets; do
                    if [ -n "$pet_id" ] && [ "$pet_id" != "null" ]; then
                        curl -s -X DELETE "${BASE_URL}/pets/${pet_id}" > /dev/null 2>&1 || true
                    fi
                done
            fi
        done
    fi
    
    # Delete all owners
    log_info "  Deleting owners..."
    if [ "$owners" != "[]" ] && [ -n "$owners" ]; then
        echo "$owners" | jq -r '.[].id' 2>/dev/null | while read -r id; do
            if [ -n "$id" ] && [ "$id" != "null" ]; then
                curl -s -X DELETE "${BASE_URL}/owners/${id}" > /dev/null 2>&1 || true
            fi
        done
    fi
    
    # Delete all vets
    log_info "  Deleting vets..."
    local vets=$(curl -s "${BASE_URL}/vets" 2>/dev/null || echo "[]")
    if [ "$vets" != "[]" ] && [ -n "$vets" ]; then
        echo "$vets" | jq -r '.[].id' 2>/dev/null | while read -r id; do
            if [ -n "$id" ] && [ "$id" != "null" ]; then
                curl -s -X DELETE "${BASE_URL}/vets/${id}" > /dev/null 2>&1 || true
            fi
        done
    fi
    
    # Delete all specialties
    log_info "  Deleting specialties..."
    local specialties=$(curl -s "${BASE_URL}/specialties" 2>/dev/null || echo "[]")
    if [ "$specialties" != "[]" ] && [ -n "$specialties" ]; then
        echo "$specialties" | jq -r '.[].id' 2>/dev/null | while read -r id; do
            if [ -n "$id" ] && [ "$id" != "null" ]; then
                curl -s -X DELETE "${BASE_URL}/specialties/${id}" > /dev/null 2>&1 || true
            fi
        done
    fi
    
    # Delete all pet types
    log_info "  Deleting pet types..."
    local pettypes=$(curl -s "${BASE_URL}/pettypes" 2>/dev/null || echo "[]")
    if [ "$pettypes" != "[]" ] && [ -n "$pettypes" ]; then
        echo "$pettypes" | jq -r '.[].id' 2>/dev/null | while read -r id; do
            if [ -n "$id" ] && [ "$id" != "null" ]; then
                curl -s -X DELETE "${BASE_URL}/pettypes/${id}" > /dev/null 2>&1 || true
            fi
        done
    fi
    
    log_success "Database cleanup complete!"
}

# API call function with response validation
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_code=$4
    
    local response
    local http_code
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d "$data" \
            "${BASE_URL}${endpoint}" 2>/dev/null)
    elif [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET \
            -H "Accept: application/json" \
            "${BASE_URL}${endpoint}" 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_code" ]; then
        increment_success
        echo "$body"
        return 0
    else
        increment_failure
        echo -e "${RED}[FAILED]${NC} ${method} ${endpoint} - Expected: ${expected_code}, Got: ${http_code}" >&2
        echo -e "${RED}[BODY]${NC} ${body}" >&2
        return 1
    fi
}

# Create a pet type
create_pettype() {
    local name=$1
    local data="{\"id\":null,\"name\":\"${name}\"}"
    local response
    
    response=$(api_call "POST" "/pettypes" "$data" "201")
    if [ $? -eq 0 ]; then
        echo "$response" | jq -r '.id' 2>/dev/null
    else
        echo ""
    fi
}

# Create an owner
create_owner() {
    local firstName=$1
    local lastName=$2
    local address=$3
    local city=$4
    local telephone=$5
    local data="{\"id\":null,\"firstName\":\"${firstName}\",\"lastName\":\"${lastName}\",\"address\":\"${address}\",\"city\":\"${city}\",\"telephone\":\"${telephone}\"}"
    local response
    
    response=$(api_call "POST" "/owners" "$data" "201")
    if [ $? -eq 0 ]; then
        echo "$response" | jq -r '.id' 2>/dev/null
    else
        echo ""
    fi
}

# Create a pet for an owner
create_pet() {
    local name=$1
    local birthDate=$2
    local typeId=$3
    local typeName=$4
    local ownerId=$5
    local data="{\"id\":null,\"name\":\"${name}\",\"birthDate\":\"${birthDate}\",\"type\":{\"id\":${typeId},\"name\":\"${typeName}\"},\"owner\":{\"id\":${ownerId}}}"
    local response
    
    response=$(api_call "POST" "/owners/${ownerId}/pets" "$data" "201")
    if [ $? -eq 0 ]; then
        echo "$response" | jq -r '.id' 2>/dev/null
    else
        echo ""
    fi
}

# Create a visit for a pet
create_visit() {
    local petId=$1
    local visitDate=$2
    local description=$3
    local data="{\"id\":null,\"date\":\"${visitDate}\",\"description\":\"${description}\",\"pet\":{\"id\":${petId}}}"
    local response
    
    response=$(api_call "POST" "/visits" "$data" "201")
    if [ $? -eq 0 ]; then
        echo "$response" | jq -r '.id' 2>/dev/null
    else
        echo ""
    fi
}

# Create a specialty
create_specialty() {
    local name=$1
    local data="{\"id\":null,\"name\":\"${name}\"}"
    local response
    
    response=$(api_call "POST" "/specialties" "$data" "201")
    if [ $? -eq 0 ]; then
        echo "$response" | jq -r '.id' 2>/dev/null
    else
        echo ""
    fi
}

# Create a vet
create_vet() {
    local firstName=$1
    local lastName=$2
    local specialtyId=$3
    local specialtyName=$4
    local data
    
    if [ -n "$specialtyId" ] && [ "$specialtyId" != "" ]; then
        data="{\"id\":null,\"firstName\":\"${firstName}\",\"lastName\":\"${lastName}\",\"specialties\":[{\"id\":${specialtyId},\"name\":\"${specialtyName}\"}]}"
    else
        data="{\"id\":null,\"firstName\":\"${firstName}\",\"lastName\":\"${lastName}\",\"specialties\":[]}"
    fi
    local response
    
    response=$(api_call "POST" "/vets" "$data" "201")
    if [ $? -eq 0 ]; then
        echo "$response" | jq -r '.id' 2>/dev/null
    else
        echo ""
    fi
}

# Generate random data helpers
FIRST_NAMES=("James" "Mary" "John" "Patricia" "Robert" "Jennifer" "Michael" "Linda" "William" "Elizabeth" "David" "Barbara" "Richard" "Susan" "Joseph" "Jessica" "Thomas" "Sarah" "Charles" "Karen")
LAST_NAMES=("Smith" "Johnson" "Williams" "Brown" "Jones" "Garcia" "Miller" "Davis" "Rodriguez" "Martinez" "Hernandez" "Lopez" "Gonzalez" "Wilson" "Anderson" "Thomas" "Taylor" "Moore" "Jackson" "Martin")
PET_NAMES=("Max" "Bella" "Charlie" "Luna" "Cooper" "Daisy" "Buddy" "Sadie" "Rocky" "Molly" "Bear" "Bailey" "Duke" "Maggie" "Tucker" "Sophie" "Jack" "Chloe" "Leo" "Zoe")
PET_TYPES=("Cat" "Dog" "Bird" "Hamster" "Snake" "Lizard" "Rabbit" "Guinea Pig" "Fish" "Turtle")
SPECIALTIES=("Radiology" "Surgery" "Dentistry" "Cardiology" "Dermatology" "Oncology" "Neurology" "Ophthalmology" "Orthopedics" "Internal Medicine")
CITIES=("New York" "Los Angeles" "Chicago" "Houston" "Phoenix" "Philadelphia" "San Antonio" "San Diego" "Dallas" "San Jose")

get_random_element() {
    local -n arr=$1
    local len=${#arr[@]}
    echo "${arr[$((RANDOM % len))]}"
}

# Main execution
main() {
    local start_time=$(date +%s)
    
    echo "=============================================="
    echo "  Spring PetClinic E2E Test Script"
    echo "  Target: ${TOTAL_TRANSACTIONS} transactions"
    echo "  Chains: ${CHAINS_NEEDED} (${REQUESTS_PER_CHAIN} requests each)"
    echo "=============================================="
    echo ""
    
    # Wait for app (disabled - assuming app is already running)
    # wait_for_app || exit 1
    log_info "Skipping wait - assuming application is ready"
    
    # Cleanup existing data
    cleanup_database
    
    log_info "Starting transaction generation..."
    echo ""
    
    # Pre-create some pet types and specialties to reuse
    log_info "Creating base pet types..."
    declare -a PETTYPE_IDS
    declare -a PETTYPE_NAMES
    for i in {0..9}; do
        local type_name="${PET_TYPES[$i]}"
        local type_id=$(create_pettype "$type_name")
        if [ -n "$type_id" ] && [ "$type_id" != "null" ]; then
            PETTYPE_IDS+=("$type_id")
            PETTYPE_NAMES+=("$type_name")
        fi
    done
    log_success "Created ${#PETTYPE_IDS[@]} pet types"
    
    log_info "Creating base specialties..."
    declare -a SPECIALTY_IDS
    declare -a SPECIALTY_NAMES
    for i in {0..9}; do
        local spec_name="${SPECIALTIES[$i]}"
        local spec_id=$(create_specialty "$spec_name")
        if [ -n "$spec_id" ] && [ "$spec_id" != "null" ]; then
            SPECIALTY_IDS+=("$spec_id")
            SPECIALTY_NAMES+=("$spec_name")
        fi
    done
    log_success "Created ${#SPECIALTY_IDS[@]} specialties"
    
    echo ""
    log_info "Creating ${CHAINS_NEEDED} transaction chains..."
    
    # Create transaction chains
    for i in $(seq 1 $CHAINS_NEEDED); do
        # Progress indicator
        if [ $((i % 20)) -eq 0 ]; then
            local percent=$((i * 100 / CHAINS_NEEDED))
            read -r SUCCESS_COUNT FAILURE_COUNT TOTAL_REQUESTS < "$COUNTER_FILE"
            echo -e "${BLUE}[PROGRESS]${NC} Chain ${i}/${CHAINS_NEEDED} (${percent}%) - Requests: ${TOTAL_REQUESTS}, Success: ${SUCCESS_COUNT}, Failed: ${FAILURE_COUNT}"
        fi
        
        # Random data for this chain
        local firstName=$(get_random_element FIRST_NAMES)
        local lastName=$(get_random_element LAST_NAMES)
        local city=$(get_random_element CITIES)
        local petName=$(get_random_element PET_NAMES)
        local address="${i} Main Street"
        local telephone="555$(printf '%07d' $i)"
        local birthDate="2020-$(printf '%02d' $((RANDOM % 12 + 1)))-$(printf '%02d' $((RANDOM % 28 + 1)))"
        local visitDate="2025-$(printf '%02d' $((RANDOM % 12 + 1)))-$(printf '%02d' $((RANDOM % 28 + 1)))"
        
        # Get random existing IDs and names
        local typeIdx=$((RANDOM % ${#PETTYPE_IDS[@]}))
        local typeId=${PETTYPE_IDS[$typeIdx]}
        local typeName=${PETTYPE_NAMES[$typeIdx]}
        local specIdx=$((RANDOM % ${#SPECIALTY_IDS[@]}))
        local specId=${SPECIALTY_IDS[$specIdx]}
        local specName=${SPECIALTY_NAMES[$specIdx]}
        
        # Create Owner - use letters only for names (API validates ^[a-zA-Z]*$)
        local uniqueSuffix=$(printf '%03d' $i | tr '0-9' 'a-j')
        local ownerId=$(create_owner "${firstName}" "${lastName}${uniqueSuffix}" "$address" "$city" "$telephone")
        
        if [ -n "$ownerId" ] && [ "$ownerId" != "null" ]; then
            # Create Pet for Owner - use letters only for pet names too
            local petId=$(create_pet "${petName}${uniqueSuffix}" "$birthDate" "$typeId" "$typeName" "$ownerId")
            
            if [ -n "$petId" ] && [ "$petId" != "null" ]; then
                # Create Visit for Pet
                create_visit "$petId" "$visitDate" "Regular checkup" > /dev/null
            fi
        fi
        
        # Create Vet with specialty
        create_vet "${firstName}" "${lastName}${uniqueSuffix}" "$specId" "$specName" > /dev/null
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    echo "=============================================="
    echo "  E2E Test Complete!"
    echo "=============================================="
    echo ""
    log_info "Summary:"
    read -r SUCCESS_COUNT FAILURE_COUNT TOTAL_REQUESTS < "$COUNTER_FILE"
    echo "  Total Requests:    ${TOTAL_REQUESTS}"
    echo "  Successful:        ${SUCCESS_COUNT}"
    echo "  Failed:            ${FAILURE_COUNT}"
    echo "  Duration:          ${duration} seconds"
    echo "  Requests/sec:      $(echo "scale=2; ${TOTAL_REQUESTS} / ${duration}" | bc 2>/dev/null || echo "N/A")"
    echo ""
    
    # Verify counts
    log_info "Verifying final entity counts..."
    local owner_count=$(curl -s "${BASE_URL}/owners" | jq 'length' 2>/dev/null || echo "N/A")
    local vet_count=$(curl -s "${BASE_URL}/vets" | jq 'length' 2>/dev/null || echo "N/A")
    local pettype_count=$(curl -s "${BASE_URL}/pettypes" | jq 'length' 2>/dev/null || echo "N/A")
    local specialty_count=$(curl -s "${BASE_URL}/specialties" | jq 'length' 2>/dev/null || echo "N/A")
    
    echo "  Owners:       ${owner_count}"
    echo "  Vets:         ${vet_count}"
    echo "  Pet Types:    ${pettype_count}"
    echo "  Specialties:  ${specialty_count}"
    echo ""
    
    # Cleanup temp file
    rm -f "$COUNTER_FILE"
    
    if [ $FAILURE_COUNT -eq 0 ]; then
        log_success "All requests completed successfully!"
        exit 0
    else
        log_warning "${FAILURE_COUNT} requests failed"
        exit 1
    fi
}

# Run main
main "$@"
