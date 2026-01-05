#!/bin/bash
# Deployment script for Jenkins pipeline

set -e

echo "=========================================="
echo "Deploying E-commerce Application"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

cd "$WORKSPACE" || exit 1

# Find available port (try 8080-8089)
find_available_port() {
    for port in {8080..8089}; do
        # Check if port is actually available on host
        if ! (lsof -i :$port >/dev/null 2>&1 || \
              ss -lnt 2>/dev/null | grep -q ":$port " || \
              netstat -an 2>/dev/null | grep -q ":$port " || \
              docker ps --format "{{.Ports}}" | grep -q ":$port->"); then
            echo $port
            return 0
        fi
    done
    echo 8080  # Fallback
}

export API_GATEWAY_PORT=$(find_available_port)
echo -e "${YELLOW}Using port ${API_GATEWAY_PORT} for API Gateway${NC}"

# Wait for service health
wait_for_service() {
    local service_name=$1
    local port=$2
    local protocol=$([ "$port" -ge 8080 ] && [ "$port" -le 8099 ] && echo "https" || echo "http")
    local opts=$([ "$protocol" = "https" ] && echo "-k" || echo "")

    echo -e "${BLUE}Waiting for ${service_name}...${NC}"
    for i in {1..60}; do
        if curl $opts -s ${protocol}://localhost:$port/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
            echo -e "${GREEN}✅ ${service_name} ready!${NC}"
            return 0
        fi
        sleep 2
    done
    echo -e "${RED}❌ ${service_name} failed to start${NC}"
    return 1
}

# Ensure SSL certificates exist
if [ ! -f "frontend/ssl/localhost-cert.pem" ] || [ ! -f "frontend/ssl/localhost-key.pem" ]; then
    echo -e "${YELLOW}Generating SSL certificates...${NC}"
    ./generate-ssl-certs.sh
fi

# Stop existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.ci.yml down --timeout 30 || true

# Build and start services
echo -e "${YELLOW}Building Docker images...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.ci.yml build --no-cache

echo -e "${YELLOW}Starting services...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d

# Wait for services
echo -e "${YELLOW}Waiting for services to be ready...${NC}"
sleep 10
wait_for_service "Eureka Server" 8761 || exit 1
sleep 5
wait_for_service "User Service" 8081 || exit 1
wait_for_service "Product Service" 8082 || exit 1
wait_for_service "Media Service" 8083 || exit 1
wait_for_service "API Gateway" "$API_GATEWAY_PORT" || exit 1

echo -e "${GREEN}=========================================="
echo -e "✅ Deployment successful!${NC}"
echo -e "${GREEN}=========================================="
echo ""
echo "Services:"
echo "  Frontend:    https://localhost:4200"
echo "  API Gateway: https://localhost:${API_GATEWAY_PORT}"
echo "  Eureka:      http://localhost:8761"


