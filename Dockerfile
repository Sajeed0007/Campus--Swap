# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage
#
# Pins Java 21 to match <java.version> in pom.xml. This also sidesteps the
# local toolchain problem: the host JDK is 26, which the Lombok version managed
# by Spring Boot 3.2.1 predates. Building in this image keeps the container
# build reproducible regardless of what is installed on the developer machine.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so the layer caches across source-only changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------------------------------------------------------------------------
# Runtime stage
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system --gid 1001 appuser \
    && useradd --system --uid 1001 --gid appuser appuser

COPY --from=build /build/target/*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

# MaxRAMPercentage lets the JVM size its heap from the container limit rather
# than the host's total memory.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

# Matches the actuator endpoint exposed in application.yml. ALB/ECS health
# checks should point at /actuator/health.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8080/actuator/health || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
