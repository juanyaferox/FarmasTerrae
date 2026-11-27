# ![](./docs/mark.svg) FarmasTerrae

Plataforma e-commerce de farmacia online renderizada en servidor, construida con Java 21, Spring Boot 3.4, Thymeleaf, Alpine.js, MySQL 8 y almacenamiento compatible con S3.

[![Maven Build](https://github.com/juanyaferox/FarmasTerrae/actions/workflows/maven.yml/badge.svg)](https://github.com/juanyaferox/FarmasTerrae/actions/workflows/maven.yml)

**Idiomas:** [English](README.md) · **Español** · [Português](README.pt.md)

**Contenido:** [Demo](#demo) · [Diseño](#aspectos-destacados-de-diseño-e-implementación) · [Funcionalidades](#funcionalidades-principales) · [Stack](#stack-tecnológico) · [Arquitectura](#arquitectura) · [Primeros Pasos](#primeros-pasos) · [Configuración](#variables-de-entorno-y-perfiles) · [Rutas](#control-de-acceso-y-rutas-principales) · [Estructura](#estructura-del-proyecto) · [Build y CI](#compilación-logs-y-ci)

> [!NOTE]
> La interfaz de la aplicación y los mensajes de validación están en español. La documentación se mantiene en inglés ([README.md](README.md)), español ([README.es.md](README.es.md)) y portugués ([README.pt.md](README.pt.md)).

---

## Demo

[![Demo de FarmasTerrae: portada, búsqueda de productos, ficha de producto, carrito y panel de administración](./docs/farmasterrae-demo.gif)](./docs/farmasterrae-demo.mp4)

<sub>Haz clic en la vista previa para abrir el vídeo en calidad completa (MP4, 27 s).</sub>

---

## Aspectos Destacados de Diseño e Implementación

- **Carga de datos con Liquibase post-Hibernate** — `LiquibaseRunnerConfig` desactiva `LiquibaseAutoConfiguration` en `prod` y `prodminio`, ejecutando `SpringLiquibase` desde un `CommandLineRunner` *después* de que Hibernate sincronice el esquema (`ddl-auto=update`) para poblar los datos desde `db/changelog/data-only-changelog.yaml`.

- **Pipeline de imágenes en servidor y almacenamiento cloud** — `ImageCompressionService` valida subidas (hasta 10 MB), redimensiona proporcionalmente a un máximo de `1200×800` con interpolación bicúbica, aplana transparencias sobre fondo blanco RGB y codifica en JPEG al 85%. `S3StorageService` almacena los recursos en AWS S3 (rol IAM de instancia en `prod`) o MinIO local, mientras `ProductMapper` resuelve URLs prefirmadas de 2 horas automáticamente en hooks `@AfterMapping`.

- **JPA Specifications reutilizables** — `GenericSpecification` construye predicados Criteria `LIKE` insensibles a mayúsculas y comparaciones `EQUAL` validadas por tipo sobre relaciones anidadas mediante `LEFT JOIN` automático.

- **Reserva temporal de stock en checkout** — `StockReservationService` retiene las unidades del carrito durante 15 minutos (`stock.reservation.timeout.minutes`) para reducir sobreventas durante el pago y descuenta el stock real al confirmar el pedido.

- **Eliminación lógica orientada a privacidad** — `UserService` preserva la integridad histórica de los pedidos al eliminar cuentas anonimizando `username` y `email`, limpiando teléfono y dirección, registrando `deletedAt`, filtrando mediante `@Where(clause = "deletedAt IS NULL")` e impidiendo eliminar al último administrador.

- **Auditoría global de errores correlacionados** — `GlobalExceptionHandler` combina un `Filter` de servlet con `@ControllerAdvice` para asociar cada error no controlado a un UUID `errorId` único junto con usuario, IP del cliente (`X-Forwarded-For`), URI, método HTTP y sesión.

---

## Funcionalidades Principales

- **Escaparate y Catálogo** — Portada con productos destacados aleatorios, catálogo farmacéutico organizado (medicamentos, vitaminas y suplementos, cuidado personal, primeros auxilios, belleza y cuidado de la piel, y productos para bebés), búsqueda por palabra clave, filtrado por categoría, paginación y ordenación por columnas.

- **Carrito y Checkout** — Carrito en sesión (`SessionCart`) con operaciones de añadir, incrementar, reducir, eliminar y vaciar, validación de stock en tiempo real, datos de envío precargados, múltiples métodos de pago (tarjeta de crédito, PayPal, transferencia bancaria) y seguimiento completo del estado de pedidos en las vistas de cliente y administrador.

- **Autenticación y Recuperación de Cuenta** — Acceso con Spring Security 6 mediante usuario o email, cifrado BCrypt, cookies `remember-me` de 7 días, migración ante session fixation, política de una sesión concurrente, recuperación de contraseña por Gmail SMTP y actualización autenticada con validación de complejidad.

- **Panel de Cliente (`/user/dashboard`)** — Edición de perfil, historial paginado de pedidos con detalle por artículo y reseñas verificadas limitadas a productos comprados.

- **Panel de Administración (`/admin/dashboard`)** — Gestión paginada, ordenable y filtrable de productos, pedidos y usuarios, incluyendo subida multipart de imágenes y actualización de estados.

- **Almacenamiento Agnóstico al Entorno** — Alterna entre AWS S3 (rol IAM) en producción y MinIO local sin modificar código.

---

## Stack Tecnológico

| Capa | Tecnologías |
| --- | --- |
| **Backend** | Java 21, Spring Boot 3.4.1 (Web MVC, Data JPA, Security, Validation, Mail), MapStruct 1.6.3, Lombok |
| **Frontend** | Thymeleaf, `thymeleaf-extras-springsecurity6`, Alpine.js 3, CSS modular (`tokens.css`, `components.css`, `layout.css`, `pages.css`) |
| **Base de Datos y Seed** | MySQL 8.0, Liquibase 4.32.0 (seed post-Hibernate), H2 2.3.232 (perfil local `dev`) |
| **Almacenamiento** | AWS SDK para Java v2 (`s3`), MinIO (servidor local compatible con S3) |
| **Infraestructura y DevOps** | Dockerfile multietapa (`eclipse-temurin:21`), Docker Compose, proxy Nginx TLS, Logback, GitHub Actions |

---

## Arquitectura

```mermaid
flowchart LR
    Browser["Navegador (Thymeleaf + Alpine.js)"]
    Nginx["Proxy Inverso Nginx (80 -> 443 TLS, prod)"]
    App["App Spring Boot (Java 21, :8080)"]
    DB[("MySQL 8.0 / H2 Dev")]
    Storage[("AWS S3 (Rol IAM) / MinIO")]
    SMTP["Gmail SMTP (587 STARTTLS)"]

    Browser -->|"HTTPS (prod)"| Nginx
    Nginx -->|"HTTP proxy_pass"| App
    Browser -.->|"HTTP :8080 (local)"| App
    App -->|"Spring Data JPA + Liquibase"| DB
    App -->|"S3 SDK v2 + URLs Prefirmadas"| Storage
    App -->|"Recuperación de Contraseña"| SMTP
    Browser -.->|"GET Directo a Imagen Prefirmada"| Storage
```

---

## Primeros Pasos

**Requisitos previos:** Docker y Docker Compose (Opciones 1 y 2), o JDK 21 para ejecución directa con Maven (Opción 3; `./mvnw` está incluido).

### Opción 1 — Entorno Local Completo con Docker y MinIO (Recomendado)

Inicia **MySQL 8.0**, **Spring Boot** (perfil `prodminio`) y un servidor local **MinIO** compatible con S3 con creación automática del bucket `farmasterrae`.

1. Copia [`.env.example`](.env.example) a `.env` y rellena `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` (`EMAIL` y `PASSWORD` son opcionales salvo para probar correos de recuperación):

   ```bash
   cp .env.example .env
   ```

2. Construye y levanta los servicios:

   ```bash
   docker compose -f docker-compose.help.yml up --build
   ```

3. Accede a los servicios:
   - **Tienda** — `http://localhost:8080`
   - **Consola MinIO** — `http://localhost:9001` (`minioadmin` / `minioadmin`)
   - **MySQL** — `localhost:3306`

Los perfiles, usuarios, productos, pedidos y reseñas de ejemplo se cargan automáticamente mediante Liquibase (`src/main/resources/db/changelog/data/`).

### Opción 2 — Despliegue en Producción / AWS

Inicia **MySQL 8.0**, **Spring Boot** (perfil `prod`) y **Nginx** con redirección HTTP a HTTPS (puertos `80` y `443`).

1. Copia [`.env.example`](.env.example) a `.env` y completa todas las variables de producción (`SPRING_PROFILES_ACTIVE=prod`, `SECRET_KEY`, `DB_*`, `EMAIL`, `PASSWORD`, `AWS_REGION`, `AWS_BUCKET`).

2. Coloca el certificado TLS y la clave privada en `./ssl/`:
   - `ssl/farmasterrae.crt`
   - `ssl/farmasterrae.key`

3. Asocia al host un rol IAM con permisos sobre el bucket S3 (`S3Config` utiliza `DefaultCredentialsProvider` en `prod`, sin claves estáticas).

4. Inicia el despliegue:

   ```bash
   docker compose up --build -d
   ```

> [!IMPORTANT]
> Sustituye cualquier certificado de desarrollo o autofirmado en `./ssl/` por certificados válidos emitidos por una CA (por ejemplo, Let's Encrypt) antes de publicar el servicio.

### Opción 3 — Modo Desarrollo Local con Maven y H2

Ejecuta Spring Boot directamente contra una base de datos H2 en memoria (modo compatibilidad MySQL) inicializada con `src/main/resources/data.sql`.

1. Crea el archivo ignorado por Git `src/main/resources/application-secrets.properties` para resolver las propiedades de correo y S3 al arrancar:

   ```properties
   spring.mail.username=dev@example.com
   spring.mail.password=dev-app-password
   aws.accessKey=minioadmin
   aws.secretKey=minioadmin
   aws.region=us-east-1
   aws.s3.bucket-name=farmasterrae
   aws.s3.endpoint=http://localhost:9000
   ```

2. Arranca la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Accesos locales:
   - **Aplicación** — `http://localhost:8080`
   - **Consola H2** — `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE`
     - Usuario: `sa` (sin contraseña)

---

## Variables de Entorno y Perfiles

Plantilla de referencia: [`.env.example`](.env.example).

| Variable | Perfiles | Valor por defecto | Descripción |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | `prod` (`Dockerfile`) | Perfil activo de Spring (`prod` en AWS; fijado a `prodminio` en `docker-compose.help.yml`). |
| `SECRET_KEY` | `prod`, `prodminio` | `uniqueAndSecret` | Clave de firma para cookies `remember-me` de 7 días (`app.security`). |
| `DB_NAME` | `prod`, `prodminio` | `farmasterrae` | Nombre de la base de datos MySQL (también define `spring.application.name`). |
| `DB_HOST` | `prod`, `prodminio` | `localhost` / `db` | Host MySQL (`localhost` en `prod`, `db` en `prodminio` / Compose). |
| `DB_PORT` | `prod`, `prodminio` | `3306` | Puerto de MySQL. |
| `DB_USERNAME` | `prod`, `prodminio` | `root` | Usuario de MySQL. |
| `DB_PASSWORD` | `prod`, `prodminio` | *(vacío)* | Contraseña de MySQL (también usada como `MYSQL_ROOT_PASSWORD` en Compose). |
| `EMAIL` | `prod`, `prodminio` | *(vacío)* | Cuenta Gmail para recuperación de contraseñas (`spring.mail.username`). |
| `PASSWORD` | `prod`, `prodminio` | *(vacío)* | **Contraseña de aplicación** de Gmail para SMTP (`spring.mail.password`). |
| `AWS_REGION` | `prod` | `us-east-1` | Región de AWS S3 (`aws.region`). |
| `AWS_BUCKET` | `prod` | `farmasterrae` | Nombre del bucket de AWS S3 (`aws.s3.bucket-name`). |

### Perfiles Spring

| Perfil | Base de Datos | Almacenamiento | Esquema y Datos | Uso Previsto |
| --- | --- | --- | --- | --- |
| *(defecto / `dev`)* | H2 en memoria (modo MySQL) | Claves estáticas + endpoint opcional (`application-secrets.properties`) | Hibernate `update` + `data.sql` | Desarrollo Java rápido en local |
| `prodminio` | MySQL 8.0 (`db:3306`) | MinIO (`http://minio:9000` → `http://localhost:9000`) | Hibernate `update` + Liquibase | Pruebas full-stack locales en Docker |
| `prod` | MySQL 8.0 | AWS S3 (Rol IAM de instancia) | Hibernate `update` + Liquibase | Producción en AWS con HTTPS |

---

## Control de Acceso y Rutas Principales

| Nivel de Acceso | Rutas | Propósito |
| --- | --- | --- |
| **Público** | `/`, `/products`, `/products/categories`, `/products/{reference}` | Escaparate, directorio de categorías, búsqueda y detalle de producto |
| **Público** | `/cart`, `/cart/add/{ref}`, `/cart/increase/{ref}`, `/cart/decrease/{ref}`, `/cart/remove/{ref}`, `/cart/clear` | Operaciones del carrito en sesión |
| **Público** | `/auth`, `/auth/register`, `/auth/forgotPassword`, `/logout` | Login, registro, recuperación de contraseña y cierre de sesión |
| **Autenticado** | `/order`, `/order/confirm`, `/auth/changePassword` | Checkout, confirmación de pedido y cambio de contraseña |
| **`USER` o `ADMIN`** | `/user/dashboard`, `/user/dashboard/info-user`, `/user/dashboard/orders`, `/user/dashboard/orders/{id}`, `/user/dashboard/reviews` | Gestión de perfil, historial de pedidos y reseñas |
| **`ADMIN`** | `/admin/dashboard`, `/admin/dashboard/products`, `/admin/dashboard/orders`, `/admin/dashboard/users` | Gestión administrativa de catálogo, pedidos y usuarios |

---

## Estructura del Proyecto

```text
FarmasTerrae/
├── .github/workflows/maven.yml          # CI con GitHub Actions (empaquetado Maven con JDK 21)
├── .env.example                         # Plantilla de variables de entorno
├── Dockerfile                           # Imagen multietapa de compilación y ejecución Temurin 21
├── docker-compose.yml                   # Stack de producción (MySQL + App + Nginx TLS)
├── docker-compose.help.yml              # Stack local Docker (MySQL + App + MinIO)
├── docs/mark.svg                        # Símbolo SVG de marca usado en documentación
├── nginx.conf                           # Proxy inverso Nginx con redirección HTTP->HTTPS
├── pom.xml                              # Dependencias Maven y procesadores MapStruct/Lombok
├── ssl/                                 # Certificado y clave TLS montados por Nginx
└── src/
    ├── main/
    │   ├── java/com/iyg16260/farmasterrae/
    │   │   ├── config/                  # Security, S3, LiquibaseRunner, MVC, Web, GlobalExceptionHandler
    │   │   ├── controller/              # Controladores de tienda, Auth, Cart, Order, User y Admin
    │   │   ├── dto/                     # DTOs de Auth, Order, Payment, Product y User
    │   │   ├── enums/                   # Enumeraciones de dominio (estados de pedido, métodos de pago, operaciones)
    │   │   ├── mapper/                  # Mappers MapStruct (Product, Order, Review, User)
    │   │   ├── model/                   # Entidades JPA (User, Profile, Product, Order, OrderDetails, Review)
    │   │   ├── repository/              # Repositorios Spring Data JPA
    │   │   ├── service/                 # Lógica de negocio, StockReservation, ImageCompression, S3Storage
    │   │   ├── spec/                    # JPA Specifications dinámicas (Generic, Product, Order, User)
    │   │   └── utils/                   # SessionCart, validación de contraseñas y utilidades
    │   └── resources/
    │       ├── application*.properties  # Perfiles dev, prod y prodminio
    │       ├── data.sql                 # Datos iniciales para H2 local
    │       ├── db/changelog/            # Changelog Liquibase y scripts SQL de datos
    │       ├── logback.xml              # Appenders de consola y ficheros rotativos info/warn/error
    │       ├── static/                  # CSS modular (tokens/components/layout/pages) y recursos
    │       └── templates/               # Vistas Thymeleaf y fragmentos reutilizables
    └── test/                            # Pruebas de Spring Boot
```

---

## Compilación, Logs y CI

### Comandos Maven

```bash
# Empaquetar el JAR (igual que en Dockerfile y CI)
./mvnw clean package -DskipTests

# Ejecutar tests
./mvnw test
```

### Logs

Configurados en `src/main/resources/logback.xml`:

- **Consola (`STDOUT`)** — Mensajes `INFO+` sin trazas completas.
- **Archivos rotativos** en `logs/` (ignorado por Git):
  - `logs/info.log` — rotación diaria, retención de 30 días
  - `logs/warn.log` — rotación diaria, retención de 30 días
  - `logs/error.log` — rotación diaria, retención de 90 días con stack traces completos

### Integración Continua

Cada push o pull request sobre `master` ejecuta [`.github/workflows/maven.yml`](.github/workflows/maven.yml), compilando y empaquetando el proyecto con Temurin JDK 21 y codificación UTF-8.
