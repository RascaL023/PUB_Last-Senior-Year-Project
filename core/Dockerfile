# syntax=docker/dockerfile:1

# ============================================================
# Stage 1 — Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build

# GitHub Packages credentials (passed from docker-compose build args)
ARG GITHUB_USER=RascaL023
ARG GITHUB_TOKEN

WORKDIR /app

# Maven settings for GitHub Packages
# Stored outside /root/.m2 because /root/.m2 is used as BuildKit cache
RUN printf '<settings>\n\
  <servers>\n\
    <server>\n\
      <id>github</id>\n\
      <username>%s</username>\n\
      <password>%s</password>\n\
    </server>\n\
  </servers>\n\
</settings>\n' "$GITHUB_USER" "$GITHUB_TOKEN" > /root/maven-settings.xml

# --- Layer 1: Copy POM files ---
# Parent POM
COPY pom.xml .

# Module POMs — order matters! (layered build)
COPY common/pom.xml           common/pom.xml
COPY auth-api/pom.xml         auth-api/pom.xml
COPY auth-core/pom.xml        auth-core/pom.xml
COPY menu-api/pom.xml         menu-api/pom.xml
COPY menu-core/pom.xml        menu-core/pom.xml
COPY order-api/pom.xml        order-api/pom.xml
COPY order-core/pom.xml       order-core/pom.xml
COPY payment-core/pom.xml     payment-core/pom.xml
COPY payment-api/pom.xml      payment-api/pom.xml
COPY payment-xendit/pom.xml   payment-xendit/pom.xml
COPY dining-api/pom.xml       dining-api/pom.xml
COPY dining-core/pom.xml      dining-core/pom.xml
COPY core-app/pom.xml         core-app/pom.xml

# --- Layer 2: Resolve dependencies using persistent Maven cache ---
RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /root/maven-settings.xml dependency:go-offline -B

# --- Layer 3: Copy source code & build ---
COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /root/maven-settings.xml clean package -DskipTests -B

# Remove Maven settings containing GitHub credentials
RUN rm -f /root/maven-settings.xml

# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre

# Security: run as non-root
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=build /app/core-app/target/core-app-0.0.1-SNAPSHOT-exec.jar app.jar

# Create config directory for external config mounting
RUN mkdir -p /app/config && chown -R appuser:appuser /app

USER appuser

EXPOSE 8081

# Tuning tipis tipis
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-jar", "app.jar"]
