FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
