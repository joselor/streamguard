# StreamGuard Event Generator Dockerfile
# Multi-stage build for optimal image size

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/event-generator-1.0-SNAPSHOT.jar /app/event-generator.jar

# Create non-root user for security
RUN addgroup -S streamguard && adduser -S streamguard -G streamguard
USER streamguard

# Default environment variables
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:29092
ENV KAFKA_TOPIC=security-events
ENV EVENT_RATE=100
ENV DURATION_SECONDS=0

# Expose no ports (producer only)

# Entry point
ENTRYPOINT ["java", "-jar", "/app/event-generator.jar"]

# Default arguments (can be overridden)
CMD ["--broker", "kafka:29092", "--topic", "security-events", "--rate", "100"]
