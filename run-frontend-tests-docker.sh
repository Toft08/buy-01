#!/bin/bash
# Run frontend tests using Docker (no Node.js installation needed)
# This script runs Angular tests inside a Docker container with Node.js 20

set -e

echo "=========================================="
echo "Running Frontend Tests with Docker"
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
    -w /workspace/frontend \
    --shm-size=2gb \
    node:20-alpine \
    sh -c "
        echo 'Installing dependencies...' && \
        npm ci && \
        echo '' && \
        echo 'Running Angular tests...' && \
        npm test -- --watch=false --browsers=ChromeHeadless --code-coverage && \
        echo '' && \
        echo '✅ All tests passed!'
    "

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✅ All frontend tests passed!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Check if coverage was generated
    if [ -d "$PROJECT_ROOT/frontend/coverage" ]; then
        echo ""
        echo -e "${BLUE}Coverage report available at: ${PROJECT_ROOT}/frontend/coverage/index.html${NC}"
    fi
else
    echo ""
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}❌ Some tests failed${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit 1
fi


