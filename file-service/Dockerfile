# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./

COPY src src

RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app


# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
