# ========== ETAPA 1: construcción ==========
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiar todo el proyecto (incluyendo pom.xml, src/, mvnw, .mvn/, etc.)
COPY . .

RUN chmod +x mvnw

# Ejecutar la compilación usando Maven Wrapper (./mvnw).
# Si tu proyecto no trae mvnw/.mvn, habría que cambiar esta línea para usar mvn directamente.
RUN ./mvnw clean package -DskipTests

# ========== ETAPA 2: imagen de ejecución ==========
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR empaquetado en la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto 8080
EXPOSE 8080

# Por defecto arranca en perfil "prod"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
