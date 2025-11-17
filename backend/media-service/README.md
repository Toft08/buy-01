# Media Service

Media Service microservice for Buy-01 e-commerce platform, responsible for file upload and management.

## Overview

The Media Service handles all media file operations including:
- File upload with validation (2MB limit, image types only)
- File serving
- Ownership validation via Product Service integration
- File deletion with cleanup

## Features

- **File Upload**: Upload media files for products with validation
- **File Serving**: Serve media files directly
- **Size Limits**: 2MB maximum file size enforcement
- **Type Validation**: Only image files allowed (JPEG, PNG, GIF, WebP)
- **Ownership Validation**: Integration with Product Service for ownership checks
- **Eureka Integration**: Service discovery for inter-service communication
- **MongoDB Storage**: Separate database for media metadata

## API Endpoints

### Public Endpoints
- `GET /media/product/{productId}` - Get media by product
- `GET /media/file/{id}` - Serve media file

### Protected Endpoints (Seller/Admin only)
- `POST /media/upload/{productId}` - Upload media
- `DELETE /media/{id}` - Delete media
- `DELETE /media/product/{productId}` - Delete all product media

### Internal Endpoints (Service-to-Service)
- `DELETE /media/internal/product/{productId}` - Delete all product media (no auth)

## Configuration

- **Port**: 8083
- **Database**: media_service_db
- **Eureka**: http://localhost:8761/eureka/
- **File Storage**: uploads/images/ directory

## Inter-Service Communication

### Product Service Integration
- Validates product existence and ownership before upload/delete operations
- Uses WebClient with load balancing via Eureka

### User Service Integration
- Validates user information for ownership checks

## Dependencies

- Spring Boot 3.5.5
- Spring Cloud 2024.0.0
- Spring Data MongoDB
- Spring Security
- Eureka Client
- WebClient for inter-service communication
- Shared-common module

## Security

- JWT authentication via shared-common module
- Role-based access control (SELLER, ADMIN)
- File type and size validation
- Ownership validation via Product Service

## File Storage

- Files stored in local `uploads/images/` directory
- Unique filenames generated with UUID
- Original filename preserved in metadata
- Automatic directory creation on startup