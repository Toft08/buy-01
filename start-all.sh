#!/bin/bash
# filepath: start-all.sh

echo "Starting Buy-01 Microservices..."
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34]'
NC='\033[0m'

# Function to wait for service health
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=60 # 60 attempts = 2 min
    local attempt=0

    echo -e "${BLUE} Waiting for ${service_name} to be ready...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        # Gateway uses HTTPS, internal services use HTTP
        local protocol="http"
        local curl_opts="-s"
        if [ "$port" = "8080" ]; then
            protocol="https"
            curl_opts="-k -s"  # Allow self-signed cert for gateway
        fi
        
        if curl $curl_opts ${protocol}://localhost:$port/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep 2
        echo -ne "${YELLOW} Still waiting... (${attempt}s)${NC}\r"
    done

    echo -e "\n${RED}❌ $service_name failed to start within 2 minutes${NC}"
    echo -e "${RED}   Check logs/$(echo $service_name | tr '[:upper:]' '[:lower:]' | tr ' ' '-').log${NC}"
    return 1
}

wait_for_eureka() {
    local max_attempts=60
    local attempt=0

    echo -e "${BLUE} Waiting for Eureka Server to be ready...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        # Eureka is internal service - uses HTTP
        if curl -s http://localhost:8761/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
            echo -e "${GREEN}✅ Eureka Server is ready!${NC}"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep 2
        echo -ne "${YELLOW} Still waiting... (${attempt}s)${NC}\r"
    done

    echo -e "\n${RED}❌ Eureka failed to start within 2 minutes${NC}"
    echo -e "${RED}   Check logs/eureka.log${NC}"
    return 1
}

# Start Docker Compose for MongoDB and Kafka
echo -e "${YELLOW}🐳 Starting Docker infrastructure (MongoDB, Kafka)...${NC}"
if docker-compose up -d mongodb kafka; then
    echo -e "${GREEN}✅ Docker infrastructure started${NC}"
    echo -e "${BLUE} Waiting for services to be healthy...${NC}"
    sleep 10
else
    echo -e "${RED}❌ Failed to start Docker infrastructure${NC}"
    exit 1
fi

# Check if MongoDB is running (fallback for old setup)
if ! docker ps | grep -q mongodb && ! docker ps | grep -q buy.*mongodb; then
    echo -e "${YELLOW}Starting MongoDB manually (fallback)...${NC}"
    docker run -d -p 27017:27017 --name mongodb mongo
    if [ $? -ne 0 ]; then
        echo -e "${RED} Failed to start MongoDB. Checking if container exists...${NC}"
        docker start mongodb 2>/dev/null || {
            echo -e "${RED} Could not start MongoDB. Please check Docker.${NC}"
            exit 1
        }
    fi
    echo -e "${GREEN}✅ MongoDB started${NC}"
    sleep 3
fi

cd backend || exit 1

# Start each service in background
echo -e "${YELLOW}🔍 Starting Eureka server...${NC}"
cd services/eureka
nohup ../../mvnw spring-boot:run >> ../../../logs/eureka.log 2>&1 &
EUREKA_PID=$!
echo -e "${BLUE} Launched with PID: $EUREKA_PID${NC}"
cd ../..

# Wait for Eureka to be ready
if ! wait_for_eureka; then
    echo -e "${RED} Failed to start Eureka. Stopping all services.${NC}"
    kill $EUREKA_PID 2>/dev/null
    exit 1
fi

echo -e "${YELLOW} Starting User Service...${NC}"
cd services/user
nohup ../../mvnw spring-boot:run >> ../../../logs/user-service.log 2>&1 &
USER_PID=$!
echo -e "${BLUE} Launched with PID: $USER_PID${NC}"
cd ../..

# Wait for User Service
if ! wait_for_service "User Service" 8081; then
    echo -e "${RED} Failed to start User Service. Stopping all services.${NC}"
    kill $EUREKA_PID $USER_PID 2>/dev/null
    exit 1
fi

echo -e "${YELLOW} Starting Product Service...${NC}"
cd services/product
nohup ../../mvnw spring-boot:run >> ../../../logs/product-service.log 2>&1 &
PRODUCT_PID=$!
echo -e "${BLUE}   Launched with PID: $PRODUCT_PID${NC}"
cd ../..

# Wait for Product Service
if ! wait_for_service "Product Service" 8082; then
    echo -e "${RED} Failed to start Product Service. Stopping all services.${NC}"
    kill $EUREKA_PID $USER_PID $PRODUCT_PID 2>/dev/null
    exit 1
fi

echo -e "${YELLOW}  Starting Media Service...${NC}"
cd services/media
nohup ../../mvnw spring-boot:run >> ../../../logs/media-service.log 2>&1 &
MEDIA_PID=$!
echo -e "${BLUE}   Launched with PID: $MEDIA_PID${NC}"
cd ../..

# Wait for Media Service
if ! wait_for_service "Media Service" 8083; then
    echo -e "${RED} Failed to start Media Service. Stopping all services.${NC}"
    kill $EUREKA_PID $USER_PID $PRODUCT_PID $MEDIA_PID 2>/dev/null
    exit 1

fi
echo -e "${YELLOW} Starting API Gateway...${NC}"
cd api-gateway
nohup ../mvnw spring-boot:run >> ../../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo -e "${BLUE}   Launched with PID: $GATEWAY_PID${NC}"
cd ..

# Wait for Gateway
if ! wait_for_service "API Gateway" 8080; then
    echo -e "${RED} Failed to start API Gateway. Stopping all services.${NC}"
    kill $EUREKA_PID $USER_PID $PRODUCT_PID $MEDIA_PID $GATEWAY_PID 2>/dev/null
    exit 1
fi

echo ""
echo -e "${GREEN}════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ All services started successfully!${NC}"
echo -e "${GREEN}════════════════════════════════════════════════${NC}"
echo ""
echo "Services:"
echo "  Eureka:       http://localhost:8761 (PID: $EUREKA_PID) [internal]"
echo "  User Service: http://localhost:8081 (PID: $USER_PID) [internal]"
echo "  Product:      http://localhost:8082 (PID: $PRODUCT_PID) [internal]"
echo "  Media:        http://localhost:8083 (PID: $MEDIA_PID) [internal]"
echo "  Gateway:      https://localhost:8080 (PID: $GATEWAY_PID) [public HTTPS]"
echo ""
echo "Frontend: Run 'cd frontend && npm start' to start on https://localhost:4200"
echo ""
echo "Logs are in: logs/ directory"
echo "To stop all services: ./stop-all.sh"

# Save PIDs to file for stop script
mkdir -p logs
echo "$EUREKA_PID $USER_PID $PRODUCT_PID $MEDIA_PID $GATEWAY_PID" > .service-pids