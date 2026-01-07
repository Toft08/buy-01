#!/bin/bash
# Backend test execution script for Jenkins pipeline
# Runs Maven tests and generates test reports

# Don't use set -e, we'll handle errors manually
set +e

echo "=========================================="
echo "Running Backend Tests"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Navigate to backend directory
cd "$WORKSPACE/backend" || {
    echo -e "${RED}❌ Failed to navigate to backend directory${NC}"
    exit 1
}

TEST_FAILED=0

# Function to run tests for a service
run_service_tests() {
    local service_name=$1
    local service_path=$2

    echo -e "${YELLOW}Running tests for $service_name...${NC}"
    echo "Service path: $WORKSPACE/backend/$service_path"

    if [ ! -d "$WORKSPACE/backend/$service_path" ]; then
        echo -e "${RED}❌ Directory not found: $WORKSPACE/backend/$service_path${NC}"
        TEST_FAILED=1
        return 1
    fi

    cd "$WORKSPACE/backend/$service_path" || {
        echo -e "${RED}❌ Failed to change to service directory${NC}"
        TEST_FAILED=1
        return 1
    }

    # Check if mvnw exists
    if [ ! -f "$WORKSPACE/backend/mvnw" ]; then
        echo -e "${RED}❌ mvnw not found at $WORKSPACE/backend/mvnw${NC}"
        TEST_FAILED=1
        cd "$WORKSPACE/backend" || return 1
        return 1
    fi

    # Use mvnw from backend root - capture output
    echo "Running: $WORKSPACE/backend/mvnw test"
    if "$WORKSPACE/backend/mvnw" test; then
        echo -e "${GREEN}✅ $service_name tests passed${NC}"
    else
        local exit_code=$?
        echo -e "${RED}❌ $service_name tests failed (exit code: $exit_code)${NC}"
        TEST_FAILED=1
    fi

    # Copy test reports to workspace
    if [ -d "target/surefire-reports" ]; then
        mkdir -p "$WORKSPACE/test-reports/backend/$service_name"
        cp -r target/surefire-reports/* "$WORKSPACE/test-reports/backend/$service_name/" 2>/dev/null || true
    fi

    cd "$WORKSPACE/backend" || return 1
}

# Run tests for each service
run_service_tests "User Service" "services/user"
run_service_tests "Product Service" "services/product"
run_service_tests "Media Service" "services/media"
run_service_tests "Eureka Server" "services/eureka"
run_service_tests "API Gateway" "api-gateway"

# Check if any tests failed
if [ $TEST_FAILED -eq 1 ]; then
    echo -e "${RED}❌ Some backend tests failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All backend tests passed!${NC}"

# Generate aggregate test report
echo -e "${YELLOW}Test reports available at: $WORKSPACE/test-reports/backend/${NC}"


