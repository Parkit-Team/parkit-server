FROM amazoncorretto:17-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
USER spring:spring

# Jenkins에서 생성한 jar를 복사
COPY build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
