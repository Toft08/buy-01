# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy and build shared module
COPY shared /build/shared
WORKDIR /build/shared
RUN mvn clean install -DskipTests -B

# Build service
WORKDIR /build/service
COPY services/media/pom.xml .
COPY services/media/src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /build/service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]