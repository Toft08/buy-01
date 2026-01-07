#!/bin/bash

# run-tests.sh - E2E workflow tests for E-com microservices
# REQUIRES: All services must be running (use ./start-all.sh first)

set +e  # Don't exit on error - we need to test failures too

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Base URLs - Use port 8081 in CI (Jenkins uses 8080), 8080 for local
# API Gateway uses HTTPS with self-signed certificates
API_GATEWAY_PORT="${API_GATEWAY_PORT:-8080}"
USER_URL="https://localhost:${API_GATEWAY_PORT}/users"
AUTH_URL="https://localhost:${API_GATEWAY_PORT}/auth"
PRODUCT_URL="https://localhost:${API_GATEWAY_PORT}/products"
MEDIA_URL="https://localhost:${API_GATEWAY_PORT}/media"

# Test data
SELLER_EMAIL="seller-test-$(date +%s)@example.com"
CONSUMER_EMAIL="consumer-test-$(date +%s)@example.com"
SELLER_PASSWORD="SellerPass123"
CONSUMER_PASSWORD="ConsumerPass123"
SELLER_TOKEN=""
CONSUMER_TOKEN=""
PRODUCT_ID=""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}E-com E2E Workflow Tests${NC}"
echo -e "${BLUE}========================================${NC}"

# Function to test expected success
test_success() {
    local test_name="$1"
    local response="$2"
    local http_code="$3"

    if [[ $http_code -ge 200 && $http_code -lt 300 ]]; then
        echo -e "${GREEN}✓${NC} $test_name"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $test_name (Expected success, got $http_code)"
        echo -e "${CYAN}Response: $response${NC}"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Function to test expected failure
test_failure() {
    local test_name="$1"
    local response="$2"
    local http_code="$3"

    if [[ $http_code -ge 400 && $http_code -lt 500 ]]; then
        echo -e "${GREEN}✓${NC} $test_name"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $test_name (Expected 4xx, got $http_code)"
        echo -e "${CYAN}Response: $response${NC}"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Function to extract response body and http code
extract_response() {
    local full_response="$1"
    # Get last line as http code
    http_code=$(echo "$full_response" | tail -1)
    # Get everything except last line as response body
    response_body=$(echo "$full_response" | sed '$d')
}

# Check if services are running (API Gateway uses HTTPS)
# Wait up to 60 seconds for API Gateway to be ready
echo -e "\n${YELLOW}Waiting for API Gateway to be ready on port ${API_GATEWAY_PORT}...${NC}"
GATEWAY_READY=false
for i in {1..30}; do
    if curl -k -s https://localhost:${API_GATEWAY_PORT}/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ API Gateway is ready!${NC}"
        GATEWAY_READY=true
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ API Gateway failed to start after 60 seconds on port ${API_GATEWAY_PORT}!${NC}"
        echo -e "${YELLOW}Please check: docker-compose logs api-gateway${NC}"
        exit 1
    fi
    echo -e "${YELLOW}Waiting... ($i/30)${NC}"
    sleep 2
done

if [ "$GATEWAY_READY" = false ]; then
    echo -e "${RED}❌ Services are not running on port ${API_GATEWAY_PORT}!${NC}"
    exit 1
fi

# Additional wait for services to be fully ready
echo -e "\n${YELLOW}Waiting for services to be fully ready...${NC}"
sleep 5

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}Starting E2E Workflow Tests${NC}"
echo -e "${BLUE}========================================${NC}"

# TEST 1: Unregistered user tries to create product (should fail)
echo -e "\n${CYAN}[1] Unregistered user tries to create product (should fail)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$PRODUCT_URL" \
    -H "Content-Type: application/json" \
    -d '{"name":"Unauthorized Product","description":"Should fail","price":99.99,"quality":5}')
extract_response "$full_response"
test_failure "Unregistered user cannot create product" "$response_body" "$http_code"

# TEST 2: Register seller (should work)
echo -e "\n${CYAN}[2] Register new seller (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$AUTH_URL/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Test Seller\",\"email\":\"$SELLER_EMAIL\",\"password\":\"$SELLER_PASSWORD\",\"role\":\"SELLER\"}")
extract_response "$full_response"
if test_success "Seller registration" "$response_body" "$http_code"; then
    SELLER_TOKEN=$(echo "$response_body" | grep -o '"token":"[^"]*' | sed 's/"token":"//')
fi

# TEST 3: Seller creates product (should work)
echo -e "\n${CYAN}[3] Seller creates product (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$PRODUCT_URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -d '{"name":"Test Product","description":"A test product","price":149.99,"quality":8}')
extract_response "$full_response"
if test_success "Seller creates product" "$response_body" "$http_code"; then
    PRODUCT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*' | sed 's/"id":"//' | head -n1)
fi

# TEST 4: Seller updates own product (should work)
echo -e "\n${CYAN}[4] Seller updates own product (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X PUT "$PRODUCT_URL/$PRODUCT_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -d '{"name":"Updated Product","description":"Updated description","price":199.99,"quality":9}')
extract_response "$full_response"
test_success "Seller updates own product" "$response_body" "$http_code"

# TEST 5: Register consumer (should work)
echo -e "\n${CYAN}[5] Register new consumer (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$AUTH_URL/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Test Consumer\",\"email\":\"$CONSUMER_EMAIL\",\"password\":\"$CONSUMER_PASSWORD\",\"role\":\"CLIENT\"}")
extract_response "$full_response"
if test_success "Consumer registration" "$response_body" "$http_code"; then
    CONSUMER_TOKEN=$(echo "$response_body" | grep -o '"token":"[^"]*' | sed 's/"token":"//')
fi

# TEST 6: Consumer finds created product (should work)
echo -e "\n${CYAN}[6] Consumer finds created product (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X GET "$PRODUCT_URL/$PRODUCT_ID")
extract_response "$full_response"
test_success "Consumer can view product" "$response_body" "$http_code"

# TEST 7: Consumer tries to update product (should fail)
echo -e "\n${CYAN}[7] Consumer tries to update product (should fail)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X PUT "$PRODUCT_URL/$PRODUCT_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CONSUMER_TOKEN" \
    -d '{"name":"Hacked Product","description":"Should not work","price":1.00,"quality":1}')
extract_response "$full_response"
test_failure "Consumer cannot update product" "$response_body" "$http_code"

# TEST 8: Consumer tries to delete product (should fail)
echo -e "\n${CYAN}[8] Consumer tries to delete product (should fail)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X DELETE "$PRODUCT_URL/$PRODUCT_ID" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")
extract_response "$full_response"
test_failure "Consumer cannot delete product" "$response_body" "$http_code"

# TEST 9: Consumer tries to create product (should fail)
echo -e "\n${CYAN}[9] Consumer tries to create product (should fail)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$PRODUCT_URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CONSUMER_TOKEN" \
    -d '{"name":"Consumer Product","description":"Should not work","price":99.99,"quality":5}')
extract_response "$full_response"
test_failure "Consumer cannot create product" "$response_body" "$http_code"

# TEST 10: Delete consumer (should work)
echo -e "\n${CYAN}[10] Delete consumer (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X DELETE "$USER_URL/me" \
    -H "Authorization: Bearer $CONSUMER_TOKEN")
extract_response "$full_response"
test_success "Delete consumer" "$response_body" "$http_code"

# TEST 11: Re-login with seller (should work)
echo -e "\n${CYAN}[11] Re-login with seller (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$AUTH_URL/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$SELLER_EMAIL\",\"password\":\"$SELLER_PASSWORD\"}")
extract_response "$full_response"
if test_success "Seller re-login" "$response_body" "$http_code"; then
    SELLER_TOKEN=$(echo "$response_body" | grep -o '"token":"[^"]*' | sed 's/"token":"//')
fi

# TEST 12: Seller deletes the product (should work)
echo -e "\n${CYAN}[12] Seller deletes the product (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X DELETE "$PRODUCT_URL/$PRODUCT_ID" \
    -H "Authorization: Bearer $SELLER_TOKEN")
extract_response "$full_response"
test_success "Seller deletes product" "$response_body" "$http_code"

# TEST 13: Seller creates new product (should work)
echo -e "\n${CYAN}[13] Seller creates new product (should work)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X POST "$PRODUCT_URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -d '{"name":"Final Test Product","description":"For deletion test","price":299.99,"quality":10}')
extract_response "$full_response"
if test_success "Seller creates new product" "$response_body" "$http_code"; then
    NEW_PRODUCT_ID=$(echo "$response_body" | grep -o '"id":"[^"]*' | sed 's/"id":"//' | head -n1)
fi

# TEST 14: Delete seller (should work and cascade delete products)
echo -e "\n${CYAN}[14] Delete seller (should cascade delete all products)${NC}"
full_response=$(curl -k -s -w "\n%{http_code}" -X DELETE "$USER_URL/me" \
    -H "Authorization: Bearer $SELLER_TOKEN")
extract_response "$full_response"
test_success "Delete seller" "$response_body" "$http_code"

# TEST 15: Verify product is deleted (should fail to find)
echo -e "\n${CYAN}[15] Verify product was cascade deleted (should 404)${NC}"
sleep 1  # Give cascade delete a moment
full_response=$(curl -k -s -w "\n%{http_code}" -X GET "$PRODUCT_URL/$NEW_PRODUCT_ID")
extract_response "$full_response"
test_failure "Product cascade deleted with seller" "$response_body" "$http_code"

# Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
echo -e "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ All E2E workflow tests passed!${NC}"
    exit 0
else
    echo -e "\n${RED}✗ Some tests failed${NC}"
    exit 1
fi
