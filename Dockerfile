# ========== Stage 1: build ==========
FROM eclipse-temurin:21-jdk-noble AS builder

WORKDIR /app

# Maven wrapper and POM first, so the dependency layer is cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Sources (local secrets, certificates and build output are excluded by .dockerignore)
COPY src src
RUN ./mvnw -B -q clean package -DskipTests

# ========== Stage 2: runtime ==========
FROM eclipse-temurin:21-jre-noble

# Unprivileged system user that owns the application directory
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=builder --chown=app:app /app/target/*.jar app.jar

# Logback writes its rolling files to ./logs
RUN mkdir -p /app/logs && chown -R app:app /app

USER app

EXPOSE 8080

# Por defecto arranca en perfil "prod"
ENV SPRING_PROFILES_ACTIVE=prod

# Lightweight liveness probe: a static resource served by Spring MVC (no session, no database)
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS -o /dev/null http://127.0.0.1:8080/favicon.ico || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
