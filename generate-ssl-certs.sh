#!/bin/bash

# Script to generate self-signed SSL certificates for public-facing services
# Only Gateway and Frontend need HTTPS - internal services use HTTP behind Gateway
# For production, replace with Let's Encrypt certificates

set -e

echo "Generating SSL certificates for public-facing services..."

PASSWORD="changeit"
VALIDITY_DAYS=365

# Create directories if they don't exist
mkdir -p backend/api-gateway/src/main/resources
mkdir -p frontend/ssl

# Generate keystore for API Gateway (HTTPS endpoint)
echo "Generating certificate for API Gateway..."
keytool -genkeypair -alias api-gateway \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore backend/api-gateway/src/main/resources/keystore.p12 \
  -validity $VALIDITY_DAYS \
  -storepass $PASSWORD \
  -keypass $PASSWORD \
  -dname "CN=localhost, OU=API Gateway, O=Buy-01, L=City, ST=State, C=US"

# Generate PEM certificates for Angular dev server
echo "Generating certificate for Frontend..."
openssl req -x509 -newkey rsa:2048 \
  -keyout frontend/ssl/localhost-key.pem \
  -out frontend/ssl/localhost-cert.pem \
  -days $VALIDITY_DAYS \
  -nodes \
  -subj "/CN=localhost/O=Buy-01/OU=Frontend"

echo ""
echo "✅ SSL certificates generated successfully!"
echo ""
echo "Certificates created for:"
echo "  - API Gateway: backend/api-gateway/src/main/resources/keystore.p12"
echo "  - Frontend: frontend/ssl/localhost-*.pem"
echo ""
echo "Gateway keystore password: $PASSWORD"
echo ""
echo "⚠️  Your browser will show security warnings for self-signed certificates."
echo "    Click 'Advanced' → 'Proceed to localhost' to continue."
echo ""
echo "Architecture:"
echo "  - Frontend (HTTPS): https://localhost:4200"
echo "  - Gateway (HTTPS): https://localhost:8080"
echo "  - Internal services (HTTP): Eureka, User, Product, Media"
echo ""
echo "For production deployment:"
echo "  - Use Let's Encrypt for free trusted certificates"
echo "  - Update keystore files and passwords in application.yml"
echo "  - Deploy frontend with proper web server (nginx, CDN)"
