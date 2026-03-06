# Build stage
FROM amazoncorretto:17-alpine-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and configuration files
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies
# RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application, excluding tests to speed up the docker build
RUN ./gradlew clean build -x test

# Run stage
FROM amazoncorretto:17-alpine
WORKDIR /app

# Copy the built jar file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
