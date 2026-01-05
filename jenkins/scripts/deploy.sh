#!/bin/bash
# Deployment script for Jenkins pipeline
# Deploys the application using Docker Compose with health checks

set -e  # Exit on error

echo "=========================================="
echo "Deploying E-commerce Application"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Navigate to workspace root
cd "$WORKSPACE" || exit 1

BUILD_NUMBER=${BUILD_NUMBER:-"manual"}
DEPLOYMENT_STATE_DIR="$WORKSPACE/.deployment-state"
DEPLOYMENT_LOG="$DEPLOYMENT_STATE_DIR/deployment-${BUILD_NUMBER}.log"

# Create deployment state directory
mkdir -p "$DEPLOYMENT_STATE_DIR"

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

# Backup current deployment state
echo -e "${YELLOW}Backing up current deployment state...${NC}"
if docker-compose ps > "$DEPLOYMENT_STATE_DIR/previous-state-${BUILD_NUMBER}.txt" 2>/dev/null; then
    echo -e "${GREEN}✅ State backed up${NC}"
else
    echo -e "${YELLOW}No previous deployment found (first deployment)${NC}"
fi

# Save current image tags for rollback
echo -e "${YELLOW}Saving current image tags...${NC}"
docker images | grep -E "ecom-|e-commerce" > "$DEPLOYMENT_STATE_DIR/previous-images-${BUILD_NUMBER}.txt" || true

# Ensure SSL certificates exist
if [ ! -f "frontend/ssl/localhost-cert.pem" ] || [ ! -f "frontend/ssl/localhost-key.pem" ]; then
    echo -e "${YELLOW}Generating SSL certificates...${NC}"
    ./generate-ssl-certs.sh
fi

# Stop existing containers gracefully
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker-compose down --timeout 30 || true

# Build Docker images
echo -e "${YELLOW}Building Docker images...${NC}"
docker-compose build --no-cache

# Tag images with build number for rollback capability
echo -e "${YELLOW}Tagging images with build number ${BUILD_NUMBER}...${NC}"
docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "ecom-|e-commerce" | while read image; do
    if [[ ! "$image" == *":${BUILD_NUMBER}" ]]; then
        docker tag "$image" "${image%:*}:${BUILD_NUMBER}" 2>/dev/null || true
    fi
done

# Start infrastructure services first
echo -e "${YELLOW}Starting infrastructure services (MongoDB, Kafka)...${NC}"
docker-compose up -d mongodb kafka

# Wait for infrastructure to be ready
echo -e "${YELLOW}Waiting for infrastructure services...${NC}"
sleep 10

# Start backend services
echo -e "${YELLOW}Starting backend services...${NC}"
docker-compose up -d eureka-server

# Wait for Eureka
echo -e "${YELLOW}Waiting for Eureka Server...${NC}"
if ! wait_for_service "Eureka Server" 8761; then
    echo -e "${RED}❌ Eureka Server failed to start${NC}"
    exit 1
fi

# Start microservices
docker-compose up -d user-service product-service media-service

# Wait for microservices
echo -e "${YELLOW}Waiting for microservices...${NC}"
sleep 15

if ! wait_for_service "User Service" 8081; then
    echo -e "${RED}❌ User Service failed to start${NC}"
    exit 1
fi

if ! wait_for_service "Product Service" 8082; then
    echo -e "${RED}❌ Product Service failed to start${NC}"
    exit 1
fi

if ! wait_for_service "Media Service" 8083; then
    echo -e "${RED}❌ Media Service failed to start${NC}"
    exit 1
fi

# Start API Gateway
echo -e "${YELLOW}Starting API Gateway...${NC}"
docker-compose up -d api-gateway

# Wait for API Gateway
echo -e "${YELLOW}Waiting for API Gateway...${NC}"
if ! wait_for_service "API Gateway" 8080; then
    echo -e "${RED}❌ API Gateway failed to start${NC}"
    exit 1
fi

# Start Frontend
echo -e "${YELLOW}Starting Frontend...${NC}"
docker-compose up -d frontend

# Wait a bit for frontend
sleep 5

# Verify all services are running
echo -e "${YELLOW}Verifying all services are running...${NC}"
if docker-compose ps | grep -q "Up"; then
    echo -e "${GREEN}✅ All services are running${NC}"
else
    echo -e "${RED}❌ Some services failed to start${NC}"
    docker-compose ps
    exit 1
fi

# Save successful deployment state
echo "BUILD_NUMBER=${BUILD_NUMBER}" > "$DEPLOYMENT_STATE_DIR/last-successful-deployment.txt"
echo "DEPLOYMENT_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")" >> "$DEPLOYMENT_STATE_DIR/last-successful-deployment.txt"
echo "COMMIT_HASH=${GIT_COMMIT:-unknown}" >> "$DEPLOYMENT_STATE_DIR/last-successful-deployment.txt"

echo -e "${GREEN}=========================================="
echo -e "✅ Deployment successful!${NC}"
echo -e "${GREEN}=========================================="
echo ""
echo "Services:"
echo "  Frontend:    https://localhost:4200"
echo "  API Gateway:  https://localhost:8080"
echo "  Eureka:      http://localhost:8761"
echo ""
echo "Build Number: ${BUILD_NUMBER}"
echo "Deployment state saved to: $DEPLOYMENT_STATE_DIR"


