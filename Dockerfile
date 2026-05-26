# ----------------------------------------------------------------------------------
# Production Stage: Final Execution Environment (Uses Pre-Built JAR)
# The CI/CD pipeline builds the JAR, and this stage simply copies it over.
# ----------------------------------------------------------------------------------

# Use a lightweight JRE-only image based on Alpine Linux for smallest size
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file built by the GitHub Actions workflow into the final image.
# We assume the JAR is in the standard build/libs/ directory.
COPY build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# ENTRYPOINT: Command to run when the container starts
# -Dspring.profiles.active=prod: Activates the production profile
# -Djava.security.egd=file:/dev/./urandom: Speeds up random number generation for faster startup
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
