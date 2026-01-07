#!/bin/bash
# Deployment script for Jenkins pipeline

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

cd "$WORKSPACE" || exit 1

# Create deployment log file
DEPLOY_LOG="$WORKSPACE/.deployment-state/deploy-${BUILD_NUMBER:-$(date +%s)}.log"
DEPLOYMENT_STATE_DIR="$WORKSPACE/.deployment-state"
mkdir -p "$DEPLOYMENT_STATE_DIR"

# Function to log to both console and file
log() {
    echo -e "$1" | tee -a "$DEPLOY_LOG"
}

# Function to log plain text (no colors) to file
log_plain() {
    echo "$1" >> "$DEPLOY_LOG"
}

log "=========================================="
log "Deploying E-commerce Application"
log "=========================================="
log "Deployment started at $(date)"
log "Build Number: ${BUILD_NUMBER:-N/A}"
log "Git Commit: ${GIT_COMMIT:-N/A}"
log "=========================================="

# Ensure SSL certificates exist
if [ ! -f "frontend/ssl/localhost-cert.pem" ] || [ ! -f "frontend/ssl/localhost-key.pem" ]; then
    log "${YELLOW}Generating SSL certificates...${NC}"
    ./generate-ssl-certs.sh >> "$DEPLOY_LOG" 2>&1
fi

# Stop existing containers
log "${YELLOW}Stopping existing containers...${NC}"
docker-compose -f docker-compose.yml -f docker-compose.ci.yml down --timeout 30 >> "$DEPLOY_LOG" 2>&1 || true

# Start services (images already built in Docker Build stage)
# Docker Compose will use existing images and wait for health checks automatically
log "${YELLOW}Starting services (using pre-built images)...${NC}"
if ! docker-compose -f docker-compose.yml -f docker-compose.ci.yml up -d --wait >> "$DEPLOY_LOG" 2>&1; then
    log "${RED}❌ Failed to start services${NC}"
    log "${YELLOW}Service logs:${NC}"
    log_plain ""
    log_plain "=== Service Startup Logs ==="
    docker-compose -f docker-compose.yml -f docker-compose.ci.yml logs --tail=50 >> "$DEPLOY_LOG" 2>&1
    log ""
    log "${YELLOW}Full deployment log saved to: $DEPLOY_LOG${NC}"
    exit 1
fi

# Verify all services are actually healthy
log ""
log "=========================================="
log "Verifying services are healthy..."
log "=========================================="

# Wait a bit for services to fully start
sleep 10

# Check container status
log "Checking container status..."
log_plain ""
log_plain "=== Container Status ==="
docker-compose -f docker-compose.yml -f docker-compose.ci.yml ps >> "$DEPLOY_LOG" 2>&1
log_plain ""

# Verify each service is healthy
HEALTH_CHECK_FAILED=0
SERVICES=("ecom-eureka-server:8761" "ecom-user-service:8081" "ecom-product-service:8082" "ecom-media-service:8083" "ecom-api-gateway:8080")

for service_port in "${SERVICES[@]}"; do
    IFS=':' read -r service_name port <<< "$service_port"
    log "Checking $service_name on port $port..."

    # Check if container is running
    if ! docker ps --format "{{.Names}}" | grep -q "^${service_name}$"; then
        log "${RED}❌ $service_name container is not running${NC}"
        HEALTH_CHECK_FAILED=1
        continue
    fi

    # Check container health status
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$service_name" 2>/dev/null || echo "none")
    log "  Health status: $HEALTH_STATUS"

    if [ "$HEALTH_STATUS" != "healthy" ] && [ "$HEALTH_STATUS" != "none" ]; then
        log "${RED}❌ $service_name is not healthy (status: $HEALTH_STATUS)${NC}"
        log_plain "  Recent logs for $service_name:"
        docker logs --tail=20 "$service_name" >> "$DEPLOY_LOG" 2>&1 || true
        log_plain ""
        HEALTH_CHECK_FAILED=1
    else
        log "${GREEN}✅ $service_name is healthy${NC}"
    fi
done

# If health checks failed, show logs and exit
if [ $HEALTH_CHECK_FAILED -eq 1 ]; then
    log ""
    log "${RED}=========================================="
    log "❌ Health checks failed!${NC}"
    log "${RED}=========================================="
    log ""
    log "Collecting service logs..."
    log_plain ""
    log_plain "=== Service Logs ==="
    docker-compose -f docker-compose.yml -f docker-compose.ci.yml logs --tail=50 >> "$DEPLOY_LOG" 2>&1
    log_plain ""
    log ""
    log "${YELLOW}Full deployment log saved to: $DEPLOY_LOG${NC}"
    exit 1
fi

log ""
log "${GREEN}✅ All services are healthy!${NC}"

# Save deployment state for potential rollback
log ""
log "Saving deployment state for rollback..."

# Save current build info
cat > "$DEPLOYMENT_STATE_DIR/last-successful-deployment.txt" << EOF
BUILD_NUMBER=${BUILD_NUMBER}
GIT_COMMIT=${GIT_COMMIT}
DEPLOYMENT_TIME=$(date -u +"%Y-%m-%d %H:%M:%S UTC")
EOF

# Save current images
docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "ecom-|e-commerce" > "$DEPLOYMENT_STATE_DIR/current-images-${BUILD_NUMBER}.txt" || true

# Copy current state as previous for next deployment
if [ -f "$DEPLOYMENT_STATE_DIR/current-images-${BUILD_NUMBER}.txt" ]; then
    cp "$DEPLOYMENT_STATE_DIR/current-images-${BUILD_NUMBER}.txt" "$DEPLOYMENT_STATE_DIR/previous-images-${BUILD_NUMBER}.txt" || true
fi

# Save container status
docker-compose -f docker-compose.yml -f docker-compose.ci.yml ps > "$DEPLOYMENT_STATE_DIR/container-status-${BUILD_NUMBER}.txt" 2>&1 || true

log ""
log "${GREEN}=========================================="
log "✅ Deployment successful!${NC}"
log "${GREEN}=========================================="
log ""
log "Services are now running:"
log "  Frontend:    https://localhost:4200"
log "  API Gateway: https://localhost:8080"
log "  Eureka:      http://localhost:8761"
log ""
log "${YELLOW}Deployment log saved to: $DEPLOY_LOG${NC}"
log "${YELLOW}Deployment state saved for rollback${NC}"
log ""
log "Deployment completed at $(date)"


