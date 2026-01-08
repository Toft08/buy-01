# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy and build service (eureka doesn't need shared)
COPY services/eureka/pom.xml /build/service/pom.xml
COPY services/eureka/src /build/service/src
WORKDIR /build/service
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
