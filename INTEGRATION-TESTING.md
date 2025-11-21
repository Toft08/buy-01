# Integration Testing Guide

## Overview

This guide covers the Spring Boot integration tests for the Buy-01 microservices platform. All tests use:
- **JUnit 5** (@SpringBootTest)
- **MockMvc** for HTTP endpoint testing
- **Testcontainers** for isolated MongoDB instances
- **JwtTestUtil** for generating real JWT tokens

## Test Structure

### User Service Tests
**File**: `backend/services/user/src/test/java/com/buyapp/userservice/controller/AuthControllerIntegrationTest.java`

**Test Cases** (8 tests):
1. Register CLIENT user
2. Register SELLER user
3. Reject duplicate email registration
4. Login with valid credentials
5. Logout successfully
6. Reject login with wrong password
7. Default to CLIENT role when not specified
8. Get user by ID

**Coverage**:
- ✅ User registration (CLIENT, SELLER)
- ✅ Email uniqueness validation
- ✅ Authentication (login/logout)
- ✅ Password validation
- ✅ Default role assignment
- ✅ User retrieval

### Product Service Tests
**File**: `backend/services/product/src/test/java/com/buyapp/productservice/controller/ProductControllerIntegrationTest.java`

**Test Cases** (10 tests):
1. Create product as SELLER
2. Reject product creation as CLIENT
3. Get all products without auth
4. Get product by ID
5. Return 404 for non-existent product
6. Update product as owner
7. Reject update when not owner
8. Delete product as owner
9. Reject product with missing fields
10. Reject product with negative price

**Coverage**:
- ✅ Product CRUD operations
- ✅ Role-based access control (SELLER only)
- ✅ Ownership validation
- ✅ Input validation
- ✅ Public read access
- ✅ Authorization checks

### Media Service Tests
**File**: `backend/services/media/src/test/java/com/buyapp/mediaservice/controller/MediaControllerIntegrationTest.java`

**Test Cases** (10 tests):
1. Upload media file successfully
2. Reject file exceeding 2MB limit
3. Reject upload without authentication
4. Download media file by ID
5. Return 404 for non-existent media
6. Delete media as owner
7. Reject deletion when not owner
8. Handle multiple file uploads
9. Upload various image types (JPEG, PNG, GIF, WebP)
10. Reject empty file upload

**Coverage**:
- ✅ File upload with authentication
- ✅ 2MB size limit validation
- ✅ File download
- ✅ Media deletion with ownership
- ✅ Multiple file handling
- ✅ MIME type support
- ✅ Empty file validation

## Running Tests

### Run All Tests
```bash
# From project root
cd backend

# Run tests for all services
mvn clean test

# Or individually
cd services/user && mvn test
cd services/product && mvn test
cd services/media && mvn test
```

### Run Specific Test Class
```bash
cd backend/services/user
mvn test -Dtest=AuthControllerIntegrationTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=AuthControllerIntegrationTest#testRegisterClientUser
```

### Run with Coverage (Optional)
```bash
# Using JaCoCo plugin
mvn clean verify

# Coverage reports will be in target/site/jacoco/
```

## Test Configuration

### Testcontainers Setup
Each test class uses Testcontainers to spin up an isolated MongoDB instance:

```java
@Container
static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withExposedPorts(27017);

@DynamicPropertySource
static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
}
```

**Benefits**:
- ✅ Isolated test environment
- ✅ No interference between tests
- ✅ Automatic cleanup
- ✅ Consistent test results

### JWT Token Generation
Tests use `JwtTestUtil` from the shared module to generate real JWT tokens:

```java
String sellerToken = JwtTestUtil.generateSellerToken("seller@test.com");
String clientToken = JwtTestUtil.generateClientToken("client@test.com");
```

**File**: `backend/shared/src/test/java/com/buyapp/common/test/JwtTestUtil.java`

## Test Data Management

### Database Cleanup
Each test class uses `@BeforeEach` to clean the database:

```java
@BeforeEach
void setUp() {
    repository.deleteAll();
}
```

### Test Ordering
Tests use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order(n)` for predictable execution.

## Troubleshooting

### Docker Not Running
**Error**: `Could not start container`

**Solution**:
```bash
# Start Docker Desktop or Docker daemon
# On macOS:
open -a Docker

# Verify Docker is running:
docker ps
```

### Port Conflicts
**Error**: `Port 27017 already in use`

**Solution**: Testcontainers automatically handles port assignment, but if issues occur:
```bash
# Kill processes using MongoDB port
lsof -ti:27017 | xargs kill -9
```

### Memory Issues
**Error**: `OutOfMemoryError during tests`

**Solution**:
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m"
mvn test
```

### Testcontainers Slow Startup
Testcontainers downloads Docker images on first run. Subsequent runs use cached images.

**Speed up**:
```bash
# Pre-pull MongoDB image
docker pull mongo:7.0
```

## Best Practices

### 1. Isolation
- Each test is independent
- Database cleanup between tests
- Fresh MongoDB container per test class

### 2. Real Authentication
- Use JwtTestUtil for real tokens
- Test actual JWT validation logic
- Cover both SELLER and CLIENT roles

### 3. Comprehensive Coverage
- Test success scenarios
- Test failure scenarios (401, 403, 404, 400)
- Test validation errors
- Test authorization checks

### 4. MockMvc Assertions
```java
mockMvc.perform(get("/endpoint"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.field").value("expected"))
    .andExpect(jsonPath("$.array", hasSize(2)));
```

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: cd backend && mvn test
```

## Test Coverage Summary

| Service | Tests | Coverage | Status |
|---------|-------|----------|--------|
| User Service | 8 | Authentication, Registration | ✅ Complete |
| Product Service | 10 | CRUD, Authorization | ✅ Complete |
| Media Service | 10 | Upload, Download, Validation | ✅ Complete |

**Total**: 28 integration tests

## Next Steps

1. **Gateway Tests**: Create integration tests for API Gateway routing and authentication
2. **End-to-End Tests**: Test complete user workflows across services
3. **Performance Tests**: Add load testing with JMeter or Gatling
4. **Security Tests**: Test JWT expiration, refresh tokens, role escalation
5. **Contract Tests**: Add Spring Cloud Contract for service contracts

## Additional Resources

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
