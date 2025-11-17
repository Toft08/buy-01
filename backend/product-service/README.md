# Product Service

A microservice for handling product management in the Buy-01 e-commerce platform.

## Features

- Product CRUD operations
- User ownership validation via User Service
- Public product browsing endpoints
- Seller-only product creation and management
- Inter-service communication with User Service
- Eureka service discovery integration

## API Endpoints

### Public Endpoints (No Authentication)
- `GET /products` - List all products
- `GET /products/{id}` - Get product by ID

### Protected Endpoints (Authentication Required)
- `POST /products` - Create product (seller/admin only)
- `PUT /products/{id}` - Update product (owner/admin only)
- `DELETE /products/{id}` - Delete product (owner/admin only)
- `GET /products/my-products` - Get current user's products

### Internal Endpoints (Service-to-Service)
- `GET /products/user/{userId}` - Get products by user ID
- `DELETE /products/user/{userId}` - Delete all products by user ID

## Configuration

### Database
- MongoDB database: `product_service_db`
- Default connection: `localhost:27017`

### Service Discovery
- Eureka server: `http://localhost:8761/eureka/`
- Service port: `8082`

### Security
- JWT authentication using shared-common module
- Role-based access control (SELLER, ADMIN)
- Public endpoints for product browsing

## Dependencies

- Spring Boot 3.5.5
- Spring Security
- Spring Data MongoDB
- Spring Cloud Eureka Client
- Spring WebFlux (for inter-service communication)
- shared-common module

## Running the Service

```bash
cd backend/product-service
../mvnw spring-boot:run
```

The service will start on port 8082 and register with Eureka discovery server.

## Inter-Service Communication

The service communicates with User Service for:
- User validation during product creation
- Ownership verification for product updates/deletions
- User information display in product listings

### User Service Integration Points:
- `GET http://user-service/users/{id}` - Get user by ID
- `GET http://user-service/users/email/{email}` - Get user by email

## Business Rules

- Only sellers and admins can create products
- Only product owners and admins can modify/delete products
- Public users can view all products without authentication
- Product ownership is validated through User Service calls