#!/bin/bash
# Run frontend tests using npm (Angular/Karma)
# This script runs unit tests for the Angular frontend application

set -e

echo "=========================================="
echo "Running Frontend Tests with npm"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed${NC}"
    echo -e "${YELLOW}Please install Node.js 20 or higher to run tests locally${NC}"
    echo ""
    echo "Options:"
    echo "  1. Install Node.js 20: brew install node@20"
    echo "  2. Use Docker instead: ./run-frontend-tests-docker.sh"
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo -e "${RED}❌ Node.js 18 or higher is required. Found Node.js v${NODE_VERSION}${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Node.js $(node -v)${NC}"
echo -e "${GREEN}✅ npm $(npm -v)${NC}"
echo ""

# Navigate to frontend directory
cd "$(dirname "$0")/frontend" || {
    echo -e "${RED}❌ Failed to navigate to frontend directory${NC}"
    exit 1
}

# Store frontend root directory
FRONTEND_ROOT="$(pwd)"

# Check if package.json exists
if [ ! -f "package.json" ]; then
    echo -e "${RED}❌ package.json not found in frontend directory${NC}"
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing dependencies...${NC}"
    npm install
    echo ""
fi

# Check if Chrome/Chromium is available for headless testing
CHROME_AVAILABLE=false
CHROME_BIN=""

# Check for Chrome/Chromium in common locations
if [ -n "$CHROME_BIN" ] && [ -f "$CHROME_BIN" ]; then
    # Use existing CHROME_BIN if set and valid
    CHROME_AVAILABLE=true
elif command -v google-chrome &> /dev/null && [ -f "$(command -v google-chrome)" ]; then
    CHROME_AVAILABLE=true
    CHROME_BIN="$(command -v google-chrome)"
elif command -v chromium &> /dev/null && [ -f "$(command -v chromium)" ]; then
    CHROME_AVAILABLE=true
    CHROME_BIN="$(command -v chromium)"
elif command -v chromium-browser &> /dev/null && [ -f "$(command -v chromium-browser)" ]; then
    CHROME_AVAILABLE=true
    CHROME_BIN="$(command -v chromium-browser)"
# macOS-specific Chrome locations
elif [ -f "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" ]; then
    CHROME_AVAILABLE=true
    CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
elif [ -f "/Applications/Chromium.app/Contents/MacOS/Chromium" ]; then
    CHROME_AVAILABLE=true
    CHROME_BIN="/Applications/Chromium.app/Contents/MacOS/Chromium"
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Running Angular unit tests...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ "$CHROME_AVAILABLE" = true ] && [ -n "$CHROME_BIN" ]; then
    echo -e "${GREEN}✅ Chrome/Chromium found: $CHROME_BIN${NC}"
    echo ""
    
    # Export CHROME_BIN for Karma
    export CHROME_BIN="$CHROME_BIN"
    
    # Run tests in headless mode
    if npm test -- --watch=false --browsers=ChromeHeadless --code-coverage; then
        echo ""
        echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${GREEN}✅ All frontend tests passed!${NC}"
        echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        
        # Show coverage if available
        if [ -d "coverage" ]; then
            echo ""
            echo -e "${BLUE}Coverage report available at: ${FRONTEND_ROOT}/coverage/index.html${NC}"
        fi
    else
        echo ""
        echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "${RED}❌ Frontend tests failed${NC}"
        echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Chrome/Chromium not found${NC}"
    echo -e "${YELLOW}   Angular tests require a browser to run${NC}"
    echo ""
    echo "Options:"
    echo "  1. Install Chrome: brew install --cask google-chrome"
    echo "  2. Install Chromium: brew install chromium"
    echo "  3. Use Docker instead (recommended): ./run-frontend-tests-docker.sh"
    echo ""
    echo -e "${YELLOW}Note: Docker version includes Chrome and doesn't require local installation${NC}"
    exit 1
fi

