#!/bin/bash
# Clean start script - removes all containers and volumes

echo "🧹 Cleaning up old containers and volumes..."

# Stop and remove everything
docker-compose down -v

# Remove orphaned Zookeeper
docker rm -f buyapp-zookeeper 2>/dev/null

# Remove volumes
docker volume rm buy-01_mongodb_data 2>/dev/null

echo "✅ Cleanup complete!"
echo ""
echo "💡 Make sure to run ./build-all.sh first if you haven't already!"
echo ""
echo "🚀 Building and starting services one by one..."
echo ""

# Pull images for services that don't need building
echo "📥 Pulling MongoDB and Kafka images..."
docker-compose pull mongodb kafka

# Build services sequentially to avoid timeout issues
build_services=("eureka-server" "api-gateway" "user-service" "product-service" "media-service" "frontend")

for service in "${build_services[@]}"; do
    echo "📦 Building $service..."
    docker-compose build "$service"
    if [ $? -ne 0 ]; then
        echo "❌ Failed to build $service"
        exit 1
    fi
done

echo ""
echo "🚀 Starting all services..."

# Start all services
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 5

docker-compose ps

echo ""
echo "🌐 Access Points:"
echo "  - Frontend: http://localhost:4200"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - API Gateway: http://localhost:8080"
echo "  - MongoDB: mongodb://admin:password@localhost:27017"
echo "  - Kafka: localhost:9092"
echo ""
echo "📝 View logs with: docker-compose logs -f [service-name]"
echo "🛑 Stop all services with: docker-compose down"
