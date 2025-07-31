# Use the official Eclipse Temurin JRE image for Java 17
FROM eclipse-temurin:17-jre

# Copy the compiled .jar file from the target/ directory to /app.jar in the container
COPY target/*.jar app.jar

# Expose port 8080 for the application
EXPOSE 8080

# Run the application using Java
ENTRYPOINT ["java", "-jar", "/app.jar"]


