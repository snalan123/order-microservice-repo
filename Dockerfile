# ------------------------------------------------------------------------------
# Dockerfile — Spring Boot (Java 17) | Multi-stage | Layered | Non-root | Small
# ------------------------------------------------------------------------------
# Requirements:
# - Maven wrapper (mvnw) checked in and executable in git
# - Spring Boot 2.3+ (or any Boot 3.x) for layered jar support
# - Recommended: .gitattributes sets LF endings to avoid needing dos2unix
#
# Build (with BuildKit):  DOCKER_BUILDKIT=1 docker build -t myapp:latest .
# Run:                    docker run --rm -p 8081:8081 myapp:latest
# ------------------------------------------------------------------------------

########################
# 1) BUILD STAGE
########################
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# ---- Prime the Maven cache for faster incremental builds
# Copy only files needed to resolve dependencies to maximize cache hits.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Ensure wrapper is executable; use BuildKit cache for ~/.m2
RUN apt-get update && apt-get install -y dos2unix
RUN dos2unix ./mvnw
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

# ---- Now copy sources and build (tests skipped here; run tests in CI instead)
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package

# ---- Extract Spring Boot layers to keep deps cached across code changes
# This enables very fast rebuilds when only application classes change.
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

########################
# 2) RUNTIME STAGE
########################
# Use a slim, well-supported JRE base.
# For even smaller images, consider distroless (see note below).
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Security: create a non-root user with no shell
RUN useradd -r -u 10001 -s /usr/sbin/nologin spring

# Copy layers in stable → volatile order for better Docker layer cache reuse
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./

# Ownership to non-root user (optional if not writing to /app at runtime)
RUN chown -R spring:spring /app
USER spring

# Expose your app port (change if different)
EXPOSE 8081

# Sensible JVM defaults for containers. Users can override with `-e JAVA_OPTS=...`.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"
# (JAVA_TOOL_OPTIONS is auto-read by the JVM; you can prefer one or the other)
# ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

# Optional: Strict runtime (read-only FS) + writable temp dirs (uncomment if your app supports it)
# RUN mkdir -p /app/tmp && chown spring:spring /app/tmp
# VOLUME ["/app/tmp"]
# ENV TMPDIR=/app/tmp
# NOTE: When enabling read-only, run container with: --read-only --tmpfs /tmp --tmpfs /app/tmp

# Start via Spring Boot launcher so classpath stays correct with layers
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

# ------------------------------------------------------------------------------
# Notes:
# - Healthchecks: require a HTTP client (curl/wget). Installing them increases size.
#   Prefer Kubernetes/LB health probes, or include curl at the cost of a bigger image:
#   RUN microdnf install -y curl && microdnf clean all
#   HEALTHCHECK --interval=30s --timeout=3s CMD curl -fsS http://localhost:8081/actuator/health || exit 1
#
# - Distroless (smaller, more secure, no shell):
#   Replace runtime stage with: FROM gcr.io/distroless/java17-debian12
#   Then set: WORKDIR /app, USER 10001:10001, same COPY lines,
#   and ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]
#   (No shell expansion for $JAVA_OPTS; pass flags via JAVA_TOOL_OPTIONS instead.)
# ------------------------------------------------------------------------------
