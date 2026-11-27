# ![](./docs/mark.svg) FarmasTerrae

Server-rendered online pharmacy e-commerce platform built with Java 21, Spring Boot 3.4, Thymeleaf, Alpine.js, MySQL 8, and S3-compatible object storage.

[![Maven Build](https://github.com/juanyaferox/FarmasTerrae/actions/workflows/maven.yml/badge.svg)](https://github.com/juanyaferox/FarmasTerrae/actions/workflows/maven.yml)

**Languages:** **English** · [Español](README.es.md) · [Português](README.pt.md)

**Contents:** [Demo](#demo) · [Highlights](#design--implementation-highlights) · [Features](#key-features) · [Stack](#tech-stack) · [Architecture](#architecture) · [Getting Started](#getting-started) · [Configuration](#environment-variables--profiles) · [Routes](#access-control--main-routes) · [Structure](#project-structure) · [Build & CI](#build-logging--ci)

> [!NOTE]
> The application UI and validation messages are currently in Spanish. Documentation is maintained in English ([README.md](README.md)), Spanish ([README.es.md](README.es.md)), and Portuguese ([README.pt.md](README.pt.md)).

---

## Demo

[![FarmasTerrae demo: storefront, product search, product detail, cart and admin dashboard](./docs/farmasterrae-demo.gif)](./docs/farmasterrae-demo.mp4)

<sub>Click the preview to open the full-quality video (MP4, 27 s).</sub>

---

## Design & Implementation Highlights

- **Post-Hibernate Liquibase Data Seeding** — `LiquibaseRunnerConfig` disables default `LiquibaseAutoConfiguration` in `prod` and `prodminio`, triggering `SpringLiquibase` from a `CommandLineRunner` *after* Hibernate completes schema synchronization to seed datasets from `db/changelog/data-only-changelog.yaml`. Demo fixtures require the `dev` context (used by `prodminio`); `prod` seeds only the `ADMIN`/`USER` profiles, and `AdminBootstrapRunner` creates the first administrator from `ADMIN_BOOTSTRAP_*` variables.

- **Server-Side Image Pipeline & Cloud Storage** — `ImageCompressionService` validates JPEG/PNG uploads (magic bytes, pixel dimensions, up to 10 MB), downscales proportionally to at most `1200×800` via bicubic interpolation, flattens alpha channels onto white RGB, and encodes 85% JPEG. `S3StorageService` stores assets under `product-images/` in AWS S3 (IAM instance role in `prod`) or local MinIO and caches short-lived presigned URLs with placeholder fallback for mappers.

- **Composable JPA Specifications** — `GenericSpecification` builds multi-field, case-insensitive `LIKE` and type-validated `EQUAL` Criteria predicates across nested entity paths using automatic `LEFT JOIN` traversal.

- **Temporary Checkout Stock Reservations** — `StockReservationService` holds session cart quantities for 15 minutes (`stock.reservation.timeout.minutes`) to reduce overselling during checkout and commits real inventory deductions on order confirmation.

- **Privacy-Aware Soft Deletion** — `UserService` preserves historical order integrity when accounts are removed by anonymizing `username` and `email`, clearing phone and address, recording `deletedAt`, filtering transparently via `@SQLRestriction("deletedAt IS NULL")`, and blocking deletion of the final administrator.

- **Correlated Global Error Auditing** — `GlobalExceptionHandler` pairs a servlet `Filter` with `@ControllerAdvice` to tag unhandled errors with a unique UUID `errorId` alongside user identity, client IP (`X-Forwarded-For`), URI, HTTP method, and session metadata.

---

## Key Features

- **Storefront & Catalog** — Home page with randomized featured products, curated pharmacy categories (medicines, vitamins & supplements, personal care, first aid, skincare & beauty, and baby care), keyword search, category filtering, pagination, and multi-column sorting.

- **Shopping Cart & Checkout** — Session-backed cart (`SessionCart`) with add, increment, decrement, remove, and clear operations, live stock validation, prefilled shipping details, multiple payment options (credit card, PayPal, bank transfer), and end-to-end order status tracking across customer and admin views.

- **Authentication & Account Recovery** — Spring Security 6 login by username or email, BCrypt password hashing, 7-day `remember-me` cookies, session fixation migration, single concurrent session policy, Gmail SMTP password recovery, and authenticated password updates with complexity checks.

- **Customer Dashboard (`/user/dashboard`)** — Profile editing, paginated order history with line-item details, and verified product reviews limited to purchased items.

- **Admin Dashboard (`/admin/dashboard`)** — Searchable, sortable, paginated management for products, orders, and users, including multipart image uploads and order status updates.

- **Environment-Agnostic Storage** — Switches cleanly between AWS S3 (IAM instance role) in production and local MinIO with no code changes.

---

## Tech Stack

| Layer | Technologies |
| --- | --- |
| **Backend** | Java 21, Spring Boot 3.4.1 (Web MVC, Data JPA, Security, Validation, Mail), MapStruct 1.6.3, Lombok |
| **Frontend** | Thymeleaf, `thymeleaf-extras-springsecurity6`, Alpine.js 3, modular CSS (`tokens.css`, `components.css`, `layout.css`, `pages.css`) |
| **Database & Seeding** | MySQL 8.0, Liquibase 4.32.0 (post-Hibernate seeding), H2 2.3.232 (tests and local `dev` profile only) |
| **Object Storage** | AWS SDK for Java v2 (`s3`), MinIO (local S3-compatible server) |
| **Infrastructure & DevOps** | Multi-stage Dockerfile (`eclipse-temurin:21`, non-root runtime), Docker Compose, Nginx TLS proxy, Logback, GitHub Actions |

---

## Architecture

```mermaid
flowchart LR
    Browser["Browser (Thymeleaf + Alpine.js)"]
    Nginx["Nginx Reverse Proxy (80 -> 443 TLS, prod)"]
    App["Spring Boot App (Java 21, :8080)"]
    DB[("MySQL 8.0 / H2 Dev")]
    Storage[("AWS S3 (IAM Role) / MinIO")]
    SMTP["Gmail SMTP (587 STARTTLS)"]

    Browser -->|"HTTPS (prod)"| Nginx
    Nginx -->|"HTTP proxy_pass"| App
    Browser -.->|"HTTP :8080 (local)"| App
    App -->|"Spring Data JPA + Liquibase"| DB
    App -->|"S3 SDK v2 + Presigned URLs"| Storage
    App -->|"Password Recovery"| SMTP
    Browser -.->|"Direct Presigned Image GET"| Storage
```

---

## Getting Started

**Prerequisites:** Docker & Docker Compose (Options 1 and 2), or JDK 21 for direct Maven execution (Option 3; `./mvnw` is bundled).

### Option 1 — Local Full Stack with Docker & MinIO (Recommended)

Starts **MySQL 8.0**, **Spring Boot** (`prodminio` profile), and a local **MinIO** S3-compatible server with automatic bucket initialization (`farmasterrae`).

1. Copy [`.env.example`](.env.example) to `.env` and fill in `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MINIO_ROOT_USER`, and `MINIO_ROOT_PASSWORD` (`EMAIL` and `PASSWORD` are optional unless testing password recovery):

   ```bash
   cp .env.example .env
   ```

2. Build and start the stack:

   ```bash
   docker compose -f docker-compose.help.yml up --build
   ```

3. Open the running services (every port is bound to `127.0.0.1` only):
   - **Storefront** — `http://localhost:8080`
   - **MinIO Console** — `http://localhost:9001` (`MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`)
   - **MySQL** — `localhost:3306`

Sample profiles, users, products, orders, and reviews are seeded automatically via Liquibase with the `dev` context (`src/main/resources/db/changelog/data/`). The demo accounts use the password `123`, so keep this stack local. The bucket stays private; product images are served through presigned URLs. The official MinIO images are no longer published, so the stack uses the community-maintained [`pgsty/minio`](https://github.com/pgsty/minio) builds.

### Option 2 — Production / AWS Deployment

Starts **MySQL 8.0**, **Spring Boot** (`prod` profile), and **Nginx** with HTTP-to-HTTPS redirection. Only Nginx publishes ports (`80` and `443`); MySQL and the application are reachable on the internal Docker network only.

1. Copy [`.env.example`](.env.example) to `.env` and set all production variables (`SPRING_PROFILES_ACTIVE=prod`, `SECRET_KEY`, `DB_*`, `MYSQL_ROOT_PASSWORD`, `EMAIL`, `PASSWORD`, `AWS_REGION`, `AWS_BUCKET`), plus `ADMIN_BOOTSTRAP_USERNAME`, `ADMIN_BOOTSTRAP_EMAIL`, and `ADMIN_BOOTSTRAP_PASSWORD` for the first start (reference: [`docs/env-vars-for-prod.md`](docs/env-vars-for-prod.md)).

2. Provide your TLS certificate and private key under `./ssl/` (gitignored and mounted read-only; no certificate ships with the repository):
   - `ssl/farmasterrae.crt`
   - `ssl/farmasterrae.key`

3. Set `server_name` in [`nginx.conf`](nginx.conf) to the domain(s) on your certificate (default: `localhost`). Requests for any other host name are rejected.

4. Attach an IAM role with S3 bucket access to the host (`S3Config` uses the AWS default credentials chain in `prod`, requiring no static AWS keys).

5. Launch the stack:

   ```bash
   docker compose up --build -d
   ```

> [!IMPORTANT]
> `prod` seeds only the `ADMIN`/`USER` profiles, never the demo accounts. The first administrator is created from the `ADMIN_BOOTSTRAP_*` variables when no `ADMIN` exists yet; clear them after the first successful start. Use CA-issued certificates (such as Let's Encrypt) before exposing the application publicly.

### Option 3 — Local Maven Dev Mode (H2 In-Memory)

Runs Spring Boot directly against an in-memory H2 database (MySQL compatibility mode) seeded from `src/main/resources/data.sql`. H2 is only on the classpath with the `dev` Maven profile, which also activates the `dev` Spring profile.

1. Start the application:

   ```bash
   ./mvnw -Pdev spring-boot:run
   ```

   Development placeholders for mail and S3 are built in (`application-dev.properties`). To use real values, such as a local MinIO, create the gitignored `src/main/resources/application-secrets.properties`; it overrides the dev defaults:

   ```properties
   spring.mail.username=dev@example.com
   spring.mail.password=dev-app-password
   aws.accessKey=minioadmin
   aws.secretKey=minioadmin
   aws.s3.endpoint=http://localhost:9000
   ```

2. Open the local endpoints:
   - **Application** — `http://localhost:8080`
   - **H2 Console** — `http://localhost:8080/h2-console` (sign in to the app as an `ADMIN` first, e.g. the `data.sql` user `juan` / `123`)
     - JDBC URL: `jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE`
     - Username: `sa` (empty password)

---

## Environment Variables & Profiles

Reference template: [`.env.example`](.env.example).

| Variable | Profiles | Default | Description |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | `prod` (`Dockerfile`) | Active Spring profile (`prod` on AWS; fixed to `prodminio` in `docker-compose.help.yml`). |
| `SECRET_KEY` | `prod`, `prodminio` | *(required in `prod`)* | Signing key for 7-day `remember-me` cookies (`app.security`). |
| `DB_NAME` | `prod`, `prodminio` | `farmasterrae` | MySQL database name. |
| `DB_HOST` | `prod`, `prodminio` | `localhost` / `db` | MySQL host (`localhost` in `prod`, `db` in `prodminio` / Compose). |
| `DB_PORT` | `prod`, `prodminio` | `3306` | MySQL port. |
| `DB_USERNAME` | `prod`, `prodminio` | *(required)* | MySQL username. |
| `DB_PASSWORD` | `prod`, `prodminio` | *(required)* | MySQL password. |
| `MYSQL_ROOT_PASSWORD` | Compose | *(required)* | Password of the MySQL container's `root` account (separate from `DB_PASSWORD`; never used by the app). |
| `DB_SSL_MODE` | `prod`, `prodminio` | `PREFERRED` | Connector/J `sslMode` (`REQUIRED`, `VERIFY_CA`, or `VERIFY_IDENTITY` for managed databases). |
| `HIBERNATE_DDL_AUTO` | `prod` | `update` | Hibernate schema mode; switch to `validate` once the schema exists. |
| `ADMIN_BOOTSTRAP_USERNAME`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD` | `prod` | *(empty)* | Create the first `ADMIN` account on startup when all three are set and no `ADMIN` exists yet. |
| `EMAIL` | `prod`, `prodminio` | *(empty)* | Gmail address for SMTP password recovery (`spring.mail.username`). |
| `PASSWORD` | `prod`, `prodminio` | *(empty)* | Gmail **App Password** for SMTP authentication (`spring.mail.password`). |
| `AWS_REGION` | `prod` | `us-east-1` | AWS S3 region (`aws.region`). |
| `AWS_BUCKET` | `prod` | `farmasterrae` | AWS S3 bucket name (`aws.s3.bucket-name`). |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | `prodminio` | `minioadmin` / `minioadmin` | Local MinIO root credentials, also used by the app as its S3 keys. |
| `AWS_PUBLIC_ENDPOINT` | `prodminio` | `http://localhost:9000` | Browser-facing MinIO address used for presigned image URLs. |

### Spring Profiles

| Profile | Database | Object Storage | Schema & Seed | Intended Use |
| --- | --- | --- | --- | --- |
| `dev` (`./mvnw -Pdev`) | H2 in-memory (MySQL mode) | Static dev keys + optional endpoint (`application-secrets.properties`) | Hibernate `update` + `data.sql` | Fast local Java development |
| `prodminio` | MySQL 8.0 (`db:3306`) | MinIO (`http://minio:9000`, presigned for `http://localhost:9000`) | Hibernate `update` + Liquibase (`dev` context: demo data) | Local Docker full-stack testing |
| `prod` | MySQL 8.0 | AWS S3 (IAM Instance Role) | Hibernate (`HIBERNATE_DDL_AUTO`, default `update`) + Liquibase (`prod` context: profiles only) | AWS / HTTPS production deployment |

---

## Access Control & Main Routes

| Access Level | Routes | Purpose |
| --- | --- | --- |
| **Public** | `/`, `/products`, `/products/categories`, `/products/{reference}` | Storefront, category directory, catalog search, and product details |
| **Public** | `/cart`, `/cart/add/{ref}`, `/cart/increase/{ref}`, `/cart/decrease/{ref}`, `/cart/remove/{ref}`, `/cart/clear` | Session shopping cart operations |
| **Public** | `/auth`, `/auth/register`, `/auth/forgotPassword`, `/logout` | Login, registration, password recovery, and logout |
| **Authenticated** | `/order`, `/order/confirm`, `/auth/changePassword` | Checkout, order confirmation, and password update |
| **`USER` or `ADMIN`** | `/user/dashboard`, `/user/dashboard/info-user`, `/user/dashboard/orders`, `/user/dashboard/orders/{id}`, `/user/dashboard/reviews` | Profile management, personal order history, and product reviews |
| **`ADMIN` only** | `/admin/dashboard`, `/admin/dashboard/products`, `/admin/dashboard/orders`, `/admin/dashboard/users` | Back-office management of catalog, orders, and user accounts |

---

## Project Structure

```text
FarmasTerrae/
├── .github/workflows/maven.yml          # GitHub Actions CI (JDK 21 Maven package)
├── .dockerignore                        # Keeps secrets, certificates & build output out of the image
├── .env.example                         # Environment variable template
├── Dockerfile                           # Multi-stage Temurin 21 build & non-root runtime image
├── docker-compose.yml                   # Production stack (MySQL + App + Nginx TLS)
├── docker-compose.help.yml              # Local Docker stack (MySQL + App + MinIO)
├── docs/env-vars-for-prod.md            # Production environment variable reference
├── docs/mark.svg                        # Brand mark SVG used in documentation
├── frontend/                            # Optional Tailwind/PostCSS tooling (never packaged or served)
├── nginx.conf                           # Nginx HTTP->HTTPS reverse proxy configuration
├── pom.xml                              # Maven dependencies & MapStruct/Lombok compiler config
├── ssl/                                 # TLS certificate & key mounted by Nginx (gitignored, provided at deploy time)
└── src/
    ├── main/
    │   ├── java/com/iyg16260/farmasterrae/
    │   │   ├── config/                  # Security, S3, LiquibaseRunner, MVC, Web, GlobalExceptionHandler
    │   │   ├── controller/              # Storefront, Auth, Cart, Order, User, and Admin controllers
    │   │   ├── dto/                     # Auth, Order, Payment, Product, and User DTOs
    │   │   ├── enums/                   # Domain enumerations (order statuses, payment methods, operations)
    │   │   ├── mapper/                  # MapStruct mappers (Product, Order, Review, User)
    │   │   ├── model/                   # JPA entities (User, Profile, Product, Order, OrderDetails, Review)
    │   │   ├── repository/              # Spring Data JPA repositories
    │   │   ├── service/                 # Business logic, StockReservation, ImageCompression, S3Storage
    │   │   ├── spec/                    # Dynamic JPA Specifications (Generic, Product, Order, User)
    │   │   └── utils/                   # SessionCart, password validation, message/price helpers
    │   └── resources/
    │       ├── application*.properties  # dev, prod, and prodminio Spring profiles
    │       ├── data.sql                 # H2 local dev seed data
    │       ├── db/changelog/            # Liquibase data-only changelog & SQL seed files
    │       ├── logback.xml              # Console + rolling info/warn/error file appenders
    │       ├── static/                  # Modular CSS, assets, self-hosted fonts & vendored Alpine.js
    │       └── templates/               # Thymeleaf views & reusable fragments
    └── test/                            # Spring Boot test suite
```

---

## Build, Logging & CI

### Maven Commands

```bash
# Package the production JAR (matches Dockerfile and CI; H2 is not bundled)
./mvnw clean package -DskipTests

# Run test suite
./mvnw test

# Run locally against the in-memory H2 database
./mvnw -Pdev spring-boot:run
```

### Logging

Configured in `src/main/resources/logback.xml`:

- **Console (`STDOUT`)** — Formatted `INFO+` entries without full stack traces.
- **Rolling log files** under `logs/` (gitignored):
  - `logs/info.log` — daily rotation, 30-day retention
  - `logs/warn.log` — daily rotation, 30-day retention
  - `logs/error.log` — daily rotation, 90-day retention, full stack traces

### Continuous Integration

Every push and pull request targeting `master` runs [`.github/workflows/maven.yml`](.github/workflows/maven.yml), compiling and packaging the artifact with Temurin JDK 21 and UTF-8 encoding.
