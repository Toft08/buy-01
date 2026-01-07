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

# Ensure SSL certificates exist
if [ ! -f "frontend/ssl/localhost-cert.pem" ] || [ ! -f "frontend/ssl/localhost-key.pem" ]; then
    echo -e "${YELLOW}Generating SSL certificates...${NC}"
    ./generate-ssl-certs.sh
fi

# Stop existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.ci.yml down --timeout 30 || true

# Start services (images already built in Docker Build stage)
# Docker Compose will use existing images and wait for health checks automatically
echo -e "${YELLOW}Starting services (using pre-built images)...${NC}"
if ! docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d --wait; then
    echo -e "${RED}❌ Failed to start services${NC}"
    echo -e "${YELLOW}Service logs:${NC}"
    docker-compose -f docker-compose.yml -f docker-compose.ci.yml logs --tail=50
    exit 1
fi

# Verify all services are healthy (Docker Compose --wait should handle this, but double-check)
echo -e "${YELLOW}Verifying services are healthy...${NC}"
sleep 5

# Quick health check (Docker Compose already waited, this is just verification)
if curl -k -s https://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ API Gateway is healthy${NC}"
else
    echo -e "${YELLOW}⚠️  API Gateway health check failed, but services are running${NC}"
fi

echo -e "${GREEN}=========================================="
echo -e "✅ Deployment successful!${NC}"
echo -e "${GREEN}=========================================="
echo ""
echo "Services:"
echo "  Frontend:    https://localhost:4200"
echo "  API Gateway: https://localhost:8080"
echo "  Eureka:      http://localhost:8761"


