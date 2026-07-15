# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml file and download dependencies
# This leverages Docker cache if pom.xml doesn't change
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src/ ./src/
RUN mvn package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app

# Install wget and curl for health checks
RUN apt-get update ; apt-get install -y wget curl ; rm -rf /var/lib/apt/lists/*

# Copy the built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Health check for the container
# Checks the /actuator/health endpoint using wget
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
# Set the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
