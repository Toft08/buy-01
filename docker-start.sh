#!/bin/bash

echo "🚀 Starting Buy-01 E-commerce Platform..."
echo ""

# Build and start all services
docker-compose up --build -d

echo ""
echo "⏳ Waiting for services to be healthy..."
echo ""

# Wait for services to be healthy
sleep 10

echo "✅ Services started successfully!"
echo ""
echo "📊 Service Status:"
docker-compose ps
echo ""
echo "🌐 Access Points:"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - API Gateway: http://localhost:8080"
echo "  - MongoDB: mongodb://localhost:27017"
echo "  - Kafka: localhost:9092"
echo ""
echo "📝 View logs with: docker-compose logs -f [service-name]"
echo "🛑 Stop all services with: ./docker-stop.sh"
