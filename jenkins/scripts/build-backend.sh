#!/bin/bash
# Backend build script for Jenkins pipeline
# Builds all Spring Boot microservices using Maven

set -e  # Exit on error

echo "=========================================="
echo "Building Backend Services"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Navigate to backend directory
cd "$WORKSPACE/backend" || exit 1

echo -e "${YELLOW}Building shared common module...${NC}"
cd shared
../mvnw clean install -DskipTests
cd ..

echo -e "${YELLOW}Building Eureka Server...${NC}"
cd services/eureka
../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building User Service...${NC}"
cd services/user
../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building Product Service...${NC}"
cd services/product
../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building Media Service...${NC}"
cd services/media
../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building API Gateway...${NC}"
cd api-gateway
../mvnw clean package -DskipTests
cd ..

echo -e "${GREEN}✅ All backend services built successfully!${NC}"

# Archive JAR artifacts for later use
echo -e "${YELLOW}Archiving JAR artifacts...${NC}"
mkdir -p "$WORKSPACE/artifacts/backend"

find . -name "*.jar" -not -path "*/target/original-*" -exec cp {} "$WORKSPACE/artifacts/backend/" \;

echo -e "${GREEN}✅ Backend build complete!${NC}"


