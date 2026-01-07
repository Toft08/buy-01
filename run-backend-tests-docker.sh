#!/bin/bash
# Run backend tests using Docker (no Java installation needed)
# This script runs tests inside a Docker container with Java 17

set -e

echo "=========================================="
echo "Running Backend Tests with Docker"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi

# Get the project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo -e "${BLUE}Project root: $PROJECT_ROOT${NC}"
echo ""

# Run tests in Docker container
echo -e "${YELLOW}Running tests in Docker container...${NC}"
echo ""

docker run --rm \
    -v "$PROJECT_ROOT:/workspace" \
    -w /workspace/backend \
    eclipse-temurin:17-jdk-jammy \
    bash -c "
        chmod +x mvnw && \
        echo 'Building shared module...' && \
        cd shared && \
        ../mvnw clean install -DskipTests && \
        cd .. && \
        echo '' && \
        echo 'Running User Service tests...' && \
        cd services/user && \
        ../../mvnw test && \
        cd ../.. && \
        echo '' && \
        echo 'Running Product Service tests...' && \
        cd services/product && \
        ../../mvnw test && \
        cd ../.. && \
        echo '' && \
        echo 'Running Media Service tests...' && \
        cd services/media && \
        ../../mvnw test && \
        cd ../.. && \
        echo '' && \
        echo 'Running Eureka Server tests...' && \
        cd services/eureka && \
        ../../mvnw test && \
        cd ../.. && \
        echo '' && \
        echo 'Running API Gateway tests...' && \
        cd api-gateway && \
        ../mvnw test && \
        echo '' && \
        echo '✅ All tests passed!'
    "

if [ $? -eq 0 ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✅ All backend tests passed!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}❌ Some tests failed${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 1
fi

