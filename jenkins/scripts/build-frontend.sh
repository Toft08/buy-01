#!/bin/bash
# Frontend build script for Jenkins pipeline
# Builds Angular application for production

set -e  # Exit on error

echo "=========================================="
echo "Building Frontend Application"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Navigate to frontend directory
cd "$WORKSPACE/frontend" || exit 1

echo -e "${YELLOW}Installing dependencies...${NC}"
npm ci

echo -e "${YELLOW}Building Angular application for production...${NC}"
npm run build

# Verify build output exists
if [ ! -d "dist" ]; then
    echo -e "${RED}❌ Build failed: dist directory not found${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Frontend build complete!${NC}"

# Archive build artifacts
echo -e "${YELLOW}Archiving build artifacts...${NC}"
mkdir -p "$WORKSPACE/artifacts/frontend"
cp -r dist/* "$WORKSPACE/artifacts/frontend/" 2>/dev/null || true

echo -e "${GREEN}✅ Frontend artifacts archived!${NC}"


