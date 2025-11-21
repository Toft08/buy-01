# Buy-01 E-commerce Platform

A full-stack e-commerce platform built with **Spring Boot microservices** and **Angular**. This platform supports user registration as clients or sellers, authentication, product CRUD functionality exclusively for sellers, and media management for product images.

## Features

### Backend (Spring Boot)
- **User Management**: Registration as client or seller with JWT authentication
- **Product Management**: CRUD operations for products (seller-only)
- **Media Management**: Image upload with 2MB size limit and file type validation
- **Role-based Authorization**: CLIENT and SELLER roles with appropriate permissions
- **Security**: BCrypt password hashing, HTTPS support, JWT tokens with HttpOnly cookies
- **Database**: MongoDB with proper relationships between User, Product, and Media entities

### Frontend (Angular)
- **Authentication**: Login/Register forms with role selection
- **Home Page**: Featured products display with responsive design
- **Product Browsing**: Public product listing with images
- **Seller Dashboard**: Product management interface (planned)
- **Media Upload**: Image management for products (planned)
- **Responsive Design**: Mobile-friendly interface

## Prerequisites

- **Java 17+**
- **Node.js 18+** 
- **Maven**
- **MongoDB** (local or Docker)
- **Angular CLI** (optional but recommended)

## Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd buy-01
```

### 2. Backend Setup

#### Start MongoDB
```bash
# Using Docker (recommended)
docker run -d -p 27017:27017 --name mongodb mongo

# Or use local MongoDB installation
```

#### Configure and Run Spring Boot
```bash
cd backend

# Make Maven wrapper executable (if needed)
chmod +x mvnw

# Build and run the application
./mvnw spring-boot:run
```

The backend will be available at: `https://localhost:8443`

#### SSL Certificate
The application uses HTTPS with a self-signed certificate. In your browser:
1. Navigate to `https://localhost:8443`
2. Click "Advanced" → "Proceed to localhost" to accept the certificate

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start the development server
npm start
```

The frontend will be available at: `http://localhost:4200`

## Usage

### Getting Started

1. **Visit the Application**: Open `http://localhost:4200` in your browser
2. **Create an Account**: 
   - Click "Sign Up" 
   - Choose account type: Client (buy products) or Seller (sell products)
   - For sellers, optionally add an avatar URL
3. **Explore**: Browse featured products on the home page

### API Endpoints

#### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - User login
- `POST /auth/logout` - User logout

#### Products (Public)
- `GET /products` - List all products
- `GET /products/{id}` - Get product by ID

#### Products (Authenticated)
- `POST /products` - Create product (sellers only)
- `PUT /products/{id}` - Update product (owner only)
- `DELETE /products/{id}` - Delete product (owner only)
- `GET /products/my-products` - Get current user's products

#### Media Management
- `POST /media/upload/{productId}` - Upload product image (seller only)
- `GET /media/product/{productId}` - Get product images
- `GET /media/file/{mediaId}` - Serve image file
- `DELETE /media/{mediaId}` - Delete media (owner only)

## Database Schema

### User Collection
```json
{
  "_id": "ObjectId",
  "name": "string",
  "email": "string",
  "password": "string (hashed)",
  "role": "client|seller",
  "avatar": "string (optional)"
}
```

### Product Collection
```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "price": "number",
  "quality": "number (0-100)",
  "userId": "string (User._id)"
}
```

### Media Collection
```json
{
  "_id": "ObjectId",
  "imagePath": "string",
  "productId": "string (Product._id)",
  "fileName": "string",
  "contentType": "string",
  "fileSize": "number"
}
```

## Security Features

- **Password Security**: BCrypt hashing with salt
- **JWT Authentication**: Secure token-based authentication
- **HttpOnly Cookies**: Protection against XSS attacks
- **HTTPS**: Encrypted data transmission
- **Role-based Access**: Clients can't manage products
- **File Upload Security**: 2MB limit, image-only validation
- **CORS Configuration**: Proper cross-origin setup

## Testing

### Quick Start
```bash
# Run all integration tests
./run-tests.sh

# Run specific service tests
cd backend/services/user && ../../mvnw test
cd backend/services/product && ../../mvnw test
cd backend/services/media && ../../mvnw test
```

### Integration Tests
**28 comprehensive integration tests** using:
- ✅ **@SpringBootTest** - Full application context
- ✅ **MockMvc** - HTTP endpoint testing
- ✅ **Testcontainers** - Isolated MongoDB per test class
- ✅ **JUnit 5** - Modern testing framework
- ✅ **JwtTestUtil** - Real JWT token generation

#### Test Coverage
| Service | Tests | Coverage |
|---------|-------|----------|
| User Service | 8 | Registration, Login/Logout, Validation |
| Product Service | 10 | CRUD, Authorization, Ownership |
| Media Service | 10 | Upload, Download, Size Limits |

#### Documentation
- **[INTEGRATION-TESTING.md](INTEGRATION-TESTING.md)** - Complete testing guide
- **[TESTING-QUICK-REFERENCE.md](TESTING-QUICK-REFERENCE.md)** - Quick reference card
- **[TEST-IMPLEMENTATION-SUMMARY.md](TEST-IMPLEMENTATION-SUMMARY.md)** - Implementation details

### Backend Testing (Legacy Bash Script)
```bash
cd backend

# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

### Frontend Testing
```bash
cd frontend

# Run unit tests
npm test

# Run e2e tests
npm run e2e
```

### Manual Testing with Postman

1. **Register a Seller**:
   ```json
   POST https://localhost:8443/auth/register
   {
     "name": "John Seller",
     "email": "john@example.com",
     "password": "password123",
     "role": "seller",
     "avatar": "https://example.com/avatar.jpg"
   }
   ```

2. **Create a Product**:
   ```json
   POST https://localhost:8443/products
   Authorization: Bearer <token>
   {
     "name": "Awesome Product",
     "description": "Best product ever",
     "price": 29.99,
     "quality": 95
   }
   ```

3. **Upload Product Image**:
   ```
   POST https://localhost:8443/media/upload/{productId}
   Authorization: Bearer <token>
   Content-Type: multipart/form-data
   file: <image-file>
   ```

## Project Structure

```
buy-01/
├── backend/                 # Spring Boot application
│   ├── src/main/java/
│   │   └── com/buyapp/ecommerce/
│   │       ├── model/       # User, Product, Media, Role
│   │       ├── repository/  # MongoDB repositories
│   │       ├── service/     # Business logic
│   │       ├── controller/  # REST endpoints
│   │       ├── security/    # JWT, Security config
│   │       ├── dto/         # Data transfer objects
│   │       └── exception/   # Error handling
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── keystore.p12     # SSL certificate
│   └── pom.xml
├── frontend/                # Angular application
│   ├── src/app/
│   │   ├── components/      # UI components
│   │   ├── services/        # API services
│   │   ├── models/          # TypeScript interfaces
│   │   └── app.routes.ts    # Routing configuration
│   ├── package.json
│   └── angular.json
└── README.md
```

## Planned Features

- [ ] **Seller Dashboard**: Complete product management interface
- [ ] **Product Search**: Filter and search functionality
- [ ] **Shopping Cart**: Add to cart and checkout for clients
- [ ] **Order Management**: Order tracking and history
- [ ] **Advanced Media**: Multiple images per product, image optimization
- [ ] **Microservices**: Split into separate User, Product, and Media services
- [ ] **Kafka Integration**: Inter-service communication
- [ ] **Docker Deployment**: Containerized deployment

## Known Issues

1. **SSL Certificate Warning**: Self-signed certificate triggers browser warning (normal for development)
2. **CORS**: May require additional CORS configuration for production deployment
3. **File Storage**: Currently stores files locally (should use cloud storage for production)

## Development Notes

### Backend Architecture
- Clean and professional codebase with proper package structure
- Extended User model with Role enum and avatar support
- Added Media entity for image management
- Implemented comprehensive security and validation

### Frontend Architecture  
- Modern Angular application with clean architecture
- Replaced CAPTCHA components with e-commerce components
- Modern Angular with standalone components
- Reactive services with RxJS

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit a Pull Request

---

**Ready for development and testing!**
