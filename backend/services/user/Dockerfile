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
COPY services/user ./services/user

# Build shared module first
WORKDIR /build
RUN mvn clean install -DskipTests -f pom.xml -pl shared -am

# Build user service
WORKDIR /build/services/user
RUN mvn clean package -DskipTests -f pom.xml

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=builder /build/services/user/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
