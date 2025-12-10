#!/bin/bash

echo "🛑 Stopping Buy-01 E-commerce Platform..."
echo ""

# Stop all services
docker-compose down

echo ""
echo "✅ All services stopped successfully!"
echo ""
echo "💡 To remove volumes (data will be lost), use: docker-compose down -v"
