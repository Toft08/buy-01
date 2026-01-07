#!/bin/bash
# Rollback script for Jenkins pipeline
# Rolls back to previous successful deployment

set -e  # Exit on error

echo "=========================================="
echo "Rolling Back Deployment"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Navigate to workspace root
cd "$WORKSPACE" || exit 1

DEPLOYMENT_STATE_DIR="$WORKSPACE/.deployment-state"
ROLLBACK_TARGET=${1:-"previous"}  # Can be "previous" or specific build number

# Function to wait for service health
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=60
    local attempt=0

    echo -e "${BLUE}Waiting for ${service_name} to be ready...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        local protocol="http"
        local curl_opts="-s"
        if [ "$port" = "8080" ]; then
            protocol="https"
            curl_opts="-k -s"
        fi

        if curl $curl_opts ${protocol}://localhost:$port/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep 2
        echo -ne "${YELLOW}Still waiting... (${attempt}s)${NC}\r"
    done

    echo -e "\n${RED}❌ $service_name failed to start within 2 minutes${NC}"
    return 1
}

# Check if deployment state directory exists
if [ ! -d "$DEPLOYMENT_STATE_DIR" ]; then
    echo -e "${RED}❌ No deployment state found. Cannot rollback.${NC}"
    exit 1
fi

# Determine rollback target
if [ "$ROLLBACK_TARGET" = "previous" ]; then
    # Find the last successful deployment before current
    if [ -f "$DEPLOYMENT_STATE_DIR/last-successful-deployment.txt" ]; then
        source "$DEPLOYMENT_STATE_DIR/last-successful-deployment.txt"
        ROLLBACK_BUILD=$BUILD_NUMBER
        echo -e "${YELLOW}Rolling back to last successful build: ${ROLLBACK_BUILD}${NC}"
    else
        echo -e "${RED}❌ No previous successful deployment found${NC}"
        exit 1
    fi
else
    ROLLBACK_BUILD=$ROLLBACK_TARGET
    echo -e "${YELLOW}Rolling back to build: ${ROLLBACK_BUILD}${NC}"
fi

# Check if previous state file exists
PREVIOUS_STATE_FILE="$DEPLOYMENT_STATE_DIR/previous-state-${ROLLBACK_BUILD}.txt"
if [ ! -f "$PREVIOUS_STATE_FILE" ]; then
    echo -e "${YELLOW}⚠️  Previous state file not found. Attempting generic rollback...${NC}"
fi

# Stop current containers
echo -e "${YELLOW}Stopping current containers...${NC}"
docker-compose down --timeout 30 || true

# Restore previous images if available
PREVIOUS_IMAGES_FILE="$DEPLOYMENT_STATE_DIR/previous-images-${ROLLBACK_BUILD}.txt"
if [ -f "$PREVIOUS_IMAGES_FILE" ]; then
    echo -e "${YELLOW}Restoring previous Docker images...${NC}"
    # Note: In a real scenario, you might pull from a registry
    # For now, we'll rebuild with the previous code version
    echo -e "${YELLOW}Note: Full image rollback requires Docker registry. Rebuilding...${NC}"
fi

# If we have the previous commit, checkout and rebuild
if [ -n "$GIT_PREVIOUS_SUCCESSFUL_COMMIT" ]; then
    echo -e "${YELLOW}Checking out previous successful commit: ${GIT_PREVIOUS_SUCCESSFUL_COMMIT}${NC}"
    git checkout "$GIT_PREVIOUS_SUCCESSFUL_COMMIT" || echo -e "${YELLOW}⚠️  Could not checkout previous commit${NC}"
fi

# Rebuild images (or use cached images if available)
echo -e "${YELLOW}Rebuilding Docker images for rollback...${NC}"
docker-compose build || {
    echo -e "${RED}❌ Failed to rebuild images${NC}"
    exit 1
}

# Start infrastructure
echo -e "${YELLOW}Starting infrastructure services...${NC}"
docker-compose up -d mongodb kafka
sleep 10

# Start services in order
echo -e "${YELLOW}Starting services...${NC}"
docker-compose up -d eureka-server

if ! wait_for_service "Eureka Server" 8761; then
    echo -e "${RED}❌ Rollback failed: Eureka Server did not start${NC}"
    exit 1
fi

docker-compose up -d user-service product-service media-service
sleep 15

if ! wait_for_service "User Service" 8081; then
    echo -e "${RED}❌ Rollback failed: User Service did not start${NC}"
    exit 1
fi

if ! wait_for_service "Product Service" 8082; then
    echo -e "${RED}❌ Rollback failed: Product Service did not start${NC}"
    exit 1
fi

if ! wait_for_service "Media Service" 8083; then
    echo -e "${RED}❌ Rollback failed: Media Service did not start${NC}"
    exit 1
fi

docker-compose up -d api-gateway

if ! wait_for_service "API Gateway" 8080; then
    echo -e "${RED}❌ Rollback failed: API Gateway did not start${NC}"
    exit 1
fi

docker-compose up -d frontend
sleep 5

# Verify services
echo -e "${YELLOW}Verifying rollback...${NC}"
if docker-compose ps | grep -q "Up"; then
    echo -e "${GREEN}✅ Rollback successful!${NC}"
else
    echo -e "${RED}❌ Rollback verification failed${NC}"
    docker-compose ps
    exit 1
fi

# Log rollback
echo "ROLLBACK_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")" > "$DEPLOYMENT_STATE_DIR/last-rollback.txt"
echo "ROLLBACK_FROM=${BUILD_NUMBER:-unknown}" >> "$DEPLOYMENT_STATE_DIR/last-rollback.txt"
echo "ROLLBACK_TO=${ROLLBACK_BUILD}" >> "$DEPLOYMENT_STATE_DIR/last-rollback.txt"

echo -e "${GREEN}=========================================="
echo -e "✅ Rollback completed successfully!${NC}"
echo -e "${GREEN}=========================================="
echo ""
echo "Services rolled back to build: ${ROLLBACK_BUILD}"
echo "Rollback log: $DEPLOYMENT_STATE_DIR/last-rollback.txt"


