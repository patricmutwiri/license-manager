# License Manager SaaS - Production
# Author: Patrick Mutwiri
# Contact: patrick@patric.xyz

# Build Stage
FROM eclipse-temurin:21-jdk AS builder

LABEL org.opencontainers.image.title="License Manager SaaS"
LABEL org.opencontainers.image.description="Spring Boot License Management Platform"
LABEL org.opencontainers.image.authors="Patrick Mutwiri <patrick@patric.xyz>"
LABEL org.opencontainers.image.vendor="Patrick Mutwiri"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.source="https://github.com/patricmutwiri/license-manager"

WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Non-root user
RUN useradd -m -u 1001 appuser

COPY --from=builder /build/target/*.jar license-manager.jar
RUN chown -R appuser:appuser /app

USER appuser

# Render uses PORT env variable
ENV PORT=8080

# Optimized for 512MB container
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=60.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Dserver.port=${PORT} \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar license-manager.jar"]