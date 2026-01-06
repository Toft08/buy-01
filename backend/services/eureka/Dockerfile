# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy parent POM and service
COPY pom.xml ./
COPY services/eureka ./services/eureka

# Build Eureka server
WORKDIR /build/services/eureka
RUN mvn clean package -DskipTests -f pom.xml

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/services/eureka/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
