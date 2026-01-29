# Multi-stage Dockerfile for Fineract Config CLI
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL maintainer="Fineract Config CLI Team"
LABEL description="Build stage for Fineract Config CLI"

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -Dspotless.check.skip=true -B

# Verify the JAR was created
RUN ls -lh target/ && \
    test -f target/fineract-config-cli.jar || (echo "JAR file not found!" && exit 1)

# Stage 2: Create the runtime image
# Using Debian-based image for multi-platform support (amd64 and arm64)
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="Fineract Config CLI Team"
LABEL description="Fineract Config CLI - Configuration-as-Code tool for Apache Fineract"
LABEL version="1.0.0-SNAPSHOT"
LABEL org.opencontainers.image.source="https://github.com/apache/fineract"
LABEL org.opencontainers.image.documentation="https://github.com/apache/fineract/tree/develop/docs/data-collection/fineract-config-cli"
LABEL org.opencontainers.image.licenses="Apache-2.0"

# Install additional tools for debugging (optional)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    bash \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1000 fineract && \
    useradd -r -u 1000 -g fineract fineract

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/fineract-config-cli.jar /app/fineract-config-cli.jar

# Create directories for configuration files and data
RUN mkdir -p /config /data && \
    chown -R fineract:fineract /app /config /data

# Switch to non-root user
USER fineract

# Set default environment variables
ENV FINERACT_BASE_URL=http://localhost:8443/fineract-provider \
    FINERACT_TENANT=default \
    FINERACT_USERNAME=mifos \
    FINERACT_PASSWORD=password \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=default

# Expose volume for configuration files
VOLUME ["/config", "/data"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD java -version || exit 1

# Set the entrypoint to run the Spring Shell CLI
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/fineract-config-cli.jar"]

# Default command (can be overridden)
CMD []
