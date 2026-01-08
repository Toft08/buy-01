# E-commerce Platform

Full-stack e-commerce platform built with Spring Boot microservices and Angular 18, featuring JWT authentication, role-based access control, and HTTPS end-to-end.

## Features

**Backend (Spring Boot Microservices)**

- User Management with CLIENT/SELLER roles
- Product CRUD operations with ownership validation
- Media upload with 2MB size limit and image validation
- JWT authentication with token blacklisting
- Eureka service discovery
- Kafka event-driven architecture
- API Gateway with WebFlux

**Frontend (Angular 18)**

- Product browsing and detail pages
- Seller dashboard for product management
- User profile with avatar support
- Route guards and HTTP interceptors
- Responsive design with SCSS

**Security**

- BCrypt password hashing
- HTTPS with self-signed certificates
- HttpOnly cookies + localStorage
- Role-based authorization
- File upload validation

## Quick Start with Docker (Recommended)

### Prerequisites

- Docker and Docker Compose
- 8GB+ RAM recommended

### Setup

```bash
# 1. Generate SSL certificates (first time only)
./generate-ssl-certs.sh

# 2. Build all services
./docker-build.sh

# 3. Start all containers
docker-compose up -d

# 4. Verify health (wait ~30 seconds)
docker-compose ps
```

### Access Points

- **Frontend**: https://localhost:4200 (Angular app)
- **API Gateway**: https://localhost:8080 (REST API)
- **Eureka Dashboard**: http://localhost:8761 (Service registry)
- **MongoDB**: localhost:27017 (admin/password)
- **Kafka**: localhost:9092

**Note**: Accept the self-signed certificate warning in your browser (Advanced → Proceed).

### Stop Services

```bash
docker-compose down
```

## Manual Setup (without Docker)

See detailed instructions in:

- [Backend README](backend/README.md) - Backend services setup
- [Frontend README](frontend/README.md) - Frontend setup

## Architecture

### Microservices

```
┌─────────────┐
│   Angular   │ :4200 (nginx)
│  Frontend   │
└──────┬──────┘
       │ HTTPS
┌──────▼──────────┐
│  API Gateway    │ :8080 (Spring Cloud Gateway)
│   + Eureka      │ :8761 (Service Discovery)
└────────┬────────┘
         │
    ┌────┴────┬────────┬─────────┐
    │         │        │         │
┌───▼───┐ ┌──▼───┐ ┌──▼────┐ ┌──▼──────┐
│ User  │ │Product│ │ Media │ │  Kafka  │
│Service│ │Service│ │Service│ │  :9092  │
│ :8081 │ │ :8082 │ │ :8083 │ └─────────┘
└───┬───┘ └──┬───┘ └──┬────┘
    └────────┴────────┘
         MongoDB :27017
```

### Technology Stack

| Layer       | Technologies                         |
| ----------- | ------------------------------------ |
| Frontend    | Angular 18, TypeScript, SCSS, RxJS   |
| API Gateway | Spring Cloud Gateway, WebFlux        |
| Services    | Spring Boot 3.2+, Java 17+           |
| Database    | MongoDB with authentication          |
| Messaging   | Kafka 3.8.1 (KRaft mode)             |
| Security    | JWT, BCrypt, HTTPS, HttpOnly cookies |
| Container   | Docker Compose, nginx (Alpine)       |

## API Endpoints

### Authentication

- `POST /auth/register` - Register new user (CLIENT or SELLER)
- `POST /auth/login` - Login and receive JWT
- `POST /auth/logout` - Logout and blacklist token

### Products (Public)

- `GET /products` - List all products
- `GET /products/{id}` - Get product details

### Products (Authenticated)

- `POST /products` - Create product (SELLER only)
- `PUT /products/{id}` - Update product (owner only)
- `DELETE /products/{id}` - Delete product (owner only)
- `GET /products/my-products` - Get user's products

### Media

- `POST /media/upload/{productId}` - Upload image (SELLER, 2MB max)
- `GET /media/product/{productId}` - Get product images
- `DELETE /media/{mediaId}` - Delete image (owner only)

## Database Schema

### Users

```json
{
  "_id": "ObjectId",
  "name": "string",
  "email": "string (unique)",
  "password": "string (BCrypt hashed)",
  "role": "CLIENT | SELLER",
  "avatar": "string (optional)"
}
```

### Products

```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "price": "number",
  "quality": "number (0-100)",
  "userId": "string (User._id)",
  "user": "string (User.name)"
}
```

### Media

```json
{
  "_id": "ObjectId",
  "fileName": "string",
  "filePath": "string",
  "contentType": "string",
  "fileSize": "number",
  "productId": "string (Product._id)",
  "sellerId": "string (User._id)"
}
```

## Testing

### Integration Tests (Recommended)

```bash
# Run all backend integration tests
cd backend
./mvnw test

# Test specific service
cd backend/services/user && ../../mvnw test
cd backend/services/product && ../../mvnw test
cd backend/services/media && ../../mvnw test
```

**Test Coverage**: 28 tests using @SpringBootTest, MockMvc, and Testcontainers

- User Service: 8 tests (registration, login, validation)
- Product Service: 10 tests (CRUD, authorization)
- Media Service: 10 tests (upload, download, limits)

See [INTEGRATION-TESTING.md](INTEGRATION-TESTING.md) for details.

### Manual Testing

```bash
# Register seller
curl -X POST https://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@example.com","password":"pass123","role":"SELLER"}'

# Login and save token
TOKEN=$(curl -X POST https://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"pass123"}' | jq -r '.token')

# Create product
curl -X POST https://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Product","description":"Test","price":19.99,"quality":90}'

# View all products (no auth)
curl https://localhost:8080/products
```

## Project Structure

```
e-com/
├── backend/
│   ├── api-gateway/         # Spring Cloud Gateway
│   ├── services/
│   │   ├── eureka/         # Service discovery
│   │   ├── user/           # User management
│   │   ├── product/        # Product CRUD
│   │   └── media/          # Image upload/storage
│   └── shared/             # Shared models/utilities
├── frontend/               # Angular 18 SPA
│   ├── src/app/
│   │   ├── components/    # UI components
│   │   ├── services/      # API services
│   │   ├── guards/        # Route guards
│   │   └── interceptors/  # HTTP interceptors
│   ├── nginx.conf         # Docker reverse proxy
│   └── Dockerfile
├── docker-compose.yml     # Multi-container setup
└── README.md
```

## Documentation

- **[backend/README.md](backend/README.md)** - Backend services, API endpoints, Spring Boot setup
- **[frontend/README.md](frontend/README.md)** - Angular components, services, guards, routing
- **[INTEGRATION-TESTING.md](INTEGRATION-TESTING.md)** - Comprehensive testing guide
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines and branch naming
- **[Task.md](Task.md)** - Original project requirements

## Known Issues

1. **SSL Certificate Warning**: Self-signed certificate triggers browser warning (normal for development). Click "Advanced" → "Proceed to localhost".
2. **File Storage**: Images stored locally at `backend/services/media/uploads/images/`. Use cloud storage (S3, Cloudinary) for production.
3. **Startup Time**: First startup takes ~30-60 seconds for service registration and health checks.

## Development Scripts

```bash
./generate-ssl-certs.sh   # Generate self-signed SSL certificates
./docker-build.sh         # Build all Docker images
./docker-start.sh         # Start all containers
./stop-all.sh             # Stop all containers
./run-tests.sh            # Run integration tests
```

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m 'feat: add feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Submit Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming conventions.

---

**Built with ❤️ using Spring Boot and Angular**
