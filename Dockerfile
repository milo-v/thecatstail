# Stage 1: Build
FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and configuration files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Fix permissions for the Gradle wrapper
RUN chmod +x gradlew

# Download dependencies separately for better caching
RUN ./gradlew --no-daemon build -x test || true

# Copy source code and build the JAR
COPY src/ src/
RUN ./gradlew --no-daemon bootJar -x test

# Stage 2: Runtime
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR from the build stage
# Assuming only one jar is produced by bootJar
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port (based on src/main/resources/application.yaml)
EXPOSE 8000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
