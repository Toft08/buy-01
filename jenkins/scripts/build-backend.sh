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

# Ensure JAVA_HOME is set and exported
if [ -z "${JAVA_HOME}" ]; then
    echo -e "${RED}ERROR: JAVA_HOME is not set${NC}"
    exit 1
fi

if [ ! -f "${JAVA_HOME}/bin/java" ]; then
    echo -e "${RED}ERROR: Java not found at ${JAVA_HOME}/bin/java${NC}"
    echo "JAVA_HOME: ${JAVA_HOME}"
    exit 1
fi

export JAVA_HOME="${JAVA_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

echo -e "${YELLOW}Using Java from: ${JAVA_HOME}${NC}"
java -version

# Navigate to backend directory
cd "$WORKSPACE/backend" || exit 1

echo -e "${YELLOW}Building shared common module...${NC}"
cd shared
JAVA_HOME="${JAVA_HOME}" ../mvnw clean install -DskipTests
cd ..

echo -e "${YELLOW}Building Eureka Server...${NC}"
cd services/eureka
JAVA_HOME="${JAVA_HOME}" ../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building User Service...${NC}"
cd services/user
JAVA_HOME="${JAVA_HOME}" ../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building Product Service...${NC}"
cd services/product
JAVA_HOME="${JAVA_HOME}" ../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building Media Service...${NC}"
cd services/media
JAVA_HOME="${JAVA_HOME}" ../../mvnw clean package -DskipTests
cd ../..

echo -e "${YELLOW}Building API Gateway...${NC}"
cd api-gateway
JAVA_HOME="${JAVA_HOME}" ../mvnw clean package -DskipTests
cd ..

echo -e "${GREEN}✅ All backend services built successfully!${NC}"

# Archive JAR artifacts for later use
echo -e "${YELLOW}Archiving JAR artifacts...${NC}"
mkdir -p "$WORKSPACE/artifacts/backend"

find . -name "*.jar" -not -path "*/target/original-*" -exec cp {} "$WORKSPACE/artifacts/backend/" \;

echo -e "${GREEN}✅ Backend build complete!${NC}"


