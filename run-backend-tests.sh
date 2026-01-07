#!/bin/bash
# Run backend tests using Maven wrapper (mvnw)
# This script runs unit and integration tests for all backend services

set -e

echo "=========================================="
echo "Running Backend Tests with Maven"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed${NC}"
    echo -e "${YELLOW}Please install Java 17 or higher to run tests locally${NC}"
    echo ""
    echo "Options:"
    echo "  1. Install Java 17: brew install openjdk@17"
    echo "  2. Use Docker instead: ./run-backend-tests-docker.sh"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17 or higher is required. Found Java $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java $(java -version 2>&1 | head -n 1)${NC}"
echo ""

# Navigate to backend directory
cd "$(dirname "$0")/backend" || {
    echo -e "${RED}❌ Failed to navigate to backend directory${NC}"
    exit 1
}

# Store backend root directory (after navigating)
BACKEND_ROOT="$(pwd)"

# Check if mvnw exists
if [ ! -f "mvnw" ]; then
    echo -e "${RED}❌ mvnw not found in backend directory${NC}"
    exit 1
fi

# Make mvnw executable
chmod +x mvnw

# Store backend root directory (after navigating to backend)
BACKEND_ROOT="$(pwd)"

TEST_FAILED=0

# Function to run tests for a service
run_service_tests() {
    local service_name=$1
    local service_path=$2

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}Testing: $service_name${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    if [ ! -d "$service_path" ]; then
        echo -e "${RED}❌ Directory not found: $service_path${NC}"
        TEST_FAILED=1
        return 1
    fi

    cd "$service_path" || {
        echo -e "${RED}❌ Failed to change to service directory${NC}"
        TEST_FAILED=1
        return 1
    }

    # Determine correct path to mvnw based on service location
    local mvnw_path
    if [[ "$service_path" == services/* ]]; then
        # For services/*, mvnw is two levels up
        mvnw_path="../../mvnw"
    elif [[ "$service_path" == api-gateway ]]; then
        # For api-gateway, mvnw is one level up
        mvnw_path="../mvnw"
    else
        # Fallback to absolute path
        mvnw_path="$BACKEND_ROOT/mvnw"
    fi

    # Run tests using mvnw from backend root
    if "$mvnw_path" test; then
        echo -e "${GREEN}✅ $service_name tests passed${NC}"
    else
        local exit_code=$?
        echo -e "${RED}❌ $service_name tests failed (exit code: $exit_code)${NC}"
        TEST_FAILED=1
    fi

    echo ""
    cd "$BACKEND_ROOT" || return 1
}

# Build shared module first (required dependency)
echo -e "${YELLOW}Building shared module first...${NC}"
cd shared
if ../mvnw clean install -DskipTests; then
    echo -e "${GREEN}✅ Shared module built${NC}"
else
    echo -e "${RED}❌ Failed to build shared module${NC}"
    exit 1
fi
cd "$BACKEND_ROOT"
echo ""

# Run tests for each service
run_service_tests "User Service" "services/user"
run_service_tests "Product Service" "services/product"
run_service_tests "Media Service" "services/media"
run_service_tests "Eureka Server" "services/eureka"
run_service_tests "API Gateway" "api-gateway"

# Check if any tests failed
if [ $TEST_FAILED -eq 1 ]; then
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}❌ Some backend tests failed${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 1
fi

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ All backend tests passed!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

