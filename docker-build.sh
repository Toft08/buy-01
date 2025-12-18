#!/bin/bash

echo "🚀 Building Docker images one at a time..."
echo ""

# Build Eureka Server
echo "📦 Building Eureka Server..."
docker-compose build eureka-server
if [ $? -ne 0 ]; then
    echo "❌ Failed to build eureka-server"
    exit 1
fi

# Build API Gateway
echo "📦 Building API Gateway..."
docker-compose build api-gateway
if [ $? -ne 0 ]; then
    echo "❌ Failed to build api-gateway"
    exit 1
fi

# Build User Service
echo "📦 Building User Service..."
docker-compose build user-service
if [ $? -ne 0 ]; then
    echo "❌ Failed to build user-service"
    exit 1
fi

# Build Product Service
echo "📦 Building Product Service..."
docker-compose build product-service
if [ $? -ne 0 ]; then
    echo "❌ Failed to build product-service"
    exit 1
fi

# Build Media Service
echo "📦 Building Media Service..."
docker-compose build media-service
if [ $? -ne 0 ]; then
    echo "❌ Failed to build media-service"
    exit 1
fi

echo ""
echo "✅ All images built successfully!"
echo "🚀 Starting services..."
echo ""

docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10
echo ""

docker-compose ps

echo ""
echo "🌐 Access Points:"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - API Gateway: https://localhost:8080"
echo "  - MongoDB: mongodb://localhost:27017"
echo "  - Kafka: localhost:9092"
echo ""
echo "📝 View logs with: docker-compose logs -f [service-name]"
echo "🛑 Stop all services with: ./docker-stop.sh"
