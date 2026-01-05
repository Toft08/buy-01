#!/bin/bash
# Frontend test execution script for Jenkins pipeline
# Runs Angular unit tests

set -e  # Exit on error

echo "=========================================="
echo "Running Frontend Tests"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Navigate to frontend directory
cd "$WORKSPACE/frontend" || exit 1

# Ensure dependencies are installed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing dependencies...${NC}"
    npm ci
fi

echo -e "${YELLOW}Running Angular unit tests...${NC}"

# Run tests in headless mode (no browser UI)
# Generate JUnit XML report for Jenkins
npm test -- --watch=false --browsers=ChromeHeadless --code-coverage

# Check if tests passed
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Frontend tests passed!${NC}"
else
    echo -e "${RED}❌ Frontend tests failed${NC}"
    exit 1
fi

# Copy test reports to workspace
if [ -d "coverage" ]; then
    mkdir -p "$WORKSPACE/test-reports/frontend"
    cp -r coverage/* "$WORKSPACE/test-reports/frontend/" 2>/dev/null || true
fi

# Look for JUnit XML reports (if karma-junit-reporter is configured)
if [ -d "test-results" ]; then
    mkdir -p "$WORKSPACE/test-reports/frontend"
    cp -r test-results/* "$WORKSPACE/test-reports/frontend/" 2>/dev/null || true
fi

echo -e "${GREEN}✅ Frontend test execution complete!${NC}"
echo -e "${YELLOW}Test reports available at: $WORKSPACE/test-reports/frontend/${NC}"


