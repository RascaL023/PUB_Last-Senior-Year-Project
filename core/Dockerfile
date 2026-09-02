# ============================================================
# Stage 1 — Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build

# GitHub Packages credentials (passed from docker-compose build args)
ARG GITHUB_USER=RascaL023
ARG GITHUB_TOKEN

WORKDIR /app

RUN mkdir -p /root/.m2 && \
    printf '<settings>\n\
  <servers>\n\
    <server>\n\
      <id>github</id>\n\
      <username>%s</username>\n\
      <password>%s</password>\n\
    </server>\n\
  </servers>\n\
</settings>\n' "$GITHUB_USER" "$GITHUB_TOKEN" > /root/.m2/settings.xml

# --- Layer 1: Copy only POM files  ---
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

RUN mvn dependency:go-offline -B 2>/dev/null || true

# --- Layer 2: Copy source code & build ---
COPY . .
RUN mvn clean package -DskipTests -B

RUN rm -f /root/.m2/settings.xml

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
