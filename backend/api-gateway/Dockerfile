# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy parent POM and modules
COPY pom.xml ./
COPY shared ./shared
COPY api-gateway ./api-gateway

# Build shared module first
WORKDIR /build
RUN mvn clean install -DskipTests -f pom.xml -pl shared -am

# Build API Gateway
WORKDIR /build/api-gateway
RUN mvn clean package -DskipTests -f pom.xml

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=builder /build/api-gateway/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
