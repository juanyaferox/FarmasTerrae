# ![](./docs/mark.svg) FarmasTerrae

Plataforma de e-commerce para farmácia online renderizada no servidor, desenvolvida com Java 21, Spring Boot 3.4, Thymeleaf, Alpine.js, MySQL 8 e armazenamento compatível com S3.

[![Maven Build](https://github.com/juanyaferox/FarmasTerrae/actions/workflows/maven.yml/badge.svg)](https://github.com/juanyaferox/FarmasTerrae/actions/workflows/maven.yml)

**Idiomas:** [English](README.md) · [Español](README.es.md) · **Português**

**Conteúdo:** [Design](#destaques-de-design-e-implementação) · [Funcionalidades](#funcionalidades-principais) · [Stack](#stack-tecnológica) · [Arquitetura](#arquitetura) · [Primeiros Passos](#primeiros-passos) · [Configuração](#variáveis-de-ambiente-e-perfis) · [Rotas](#controlo-de-acesso-e-rotas-principais) · [Estrutura](#estrutura-do-projeto) · [Build e CI](#compilação-logs-e-ci)

> [!NOTE]
> A interface da aplicação e as mensagens de validação encontram-se atualmente em espanhol. A documentação é mantida em inglês ([README.md](README.md)), espanhol ([README.es.md](README.es.md)) e português ([README.pt.md](README.pt.md)).

---

## Destaques de Design e Implementação

- **Seeding com Liquibase pós-Hibernate** — `LiquibaseRunnerConfig` desativa `LiquibaseAutoConfiguration` em `prod` e `prodminio`, executando `SpringLiquibase` através de um `CommandLineRunner` *depois* de o Hibernate sincronizar o esquema (`ddl-auto=update`) para carregar dados a partir de `db/changelog/data-only-changelog.yaml`.

- **Pipeline de imagens no servidor e armazenamento cloud** — `ImageCompressionService` valida uploads (até 10 MB), redimensiona proporcionalmente até `1200×800` por interpolação bicúbica, achata transparências sobre fundo branco RGB e codifica em JPEG a 85%. `S3StorageService` guarda os ficheiros na AWS S3 (IAM instance role em `prod`) ou MinIO local, enquanto `ProductMapper` gera URLs pré-assinados de 2 horas automaticamente em hooks `@AfterMapping`.

- **JPA Specifications reutilizáveis** — `GenericSpecification` constrói predicados Criteria `LIKE` insensíveis a maiúsculas e comparações `EQUAL` validadas por tipo em caminhos aninhados com `LEFT JOIN` automático.

- **Reserva temporária de stock no checkout** — `StockReservationService` reserva o inventário do carrinho durante 15 minutos (`stock.reservation.timeout.minutes`) para reduzir vendas sem stock durante o pagamento e deduz o stock real na confirmação da encomenda.

- **Eliminação lógica orientada à privacidade** — `UserService` preserva a integridade do histórico de encomendas na remoção de contas ao anonimizar `username` e `email`, limpar telefone e morada, registar `deletedAt`, filtrar com `@Where(clause = "deletedAt IS NULL")` e bloquear a remoção do último administrador.

- **Auditoria global de erros correlacionados** — `GlobalExceptionHandler` combina um `Filter` de servlet com `@ControllerAdvice` para associar cada erro não tratado a um UUID `errorId` único junto com utilizador, IP do cliente (`X-Forwarded-For`), URI, método HTTP e sessão.

---

## Funcionalidades Principais

- **Loja e Catálogo** — Página inicial com produtos em destaque aleatórios, catálogo farmacêutico organizado (medicamentos, vitaminas e suplementos, cuidados pessoais, primeiros socorros, beleza e cuidados da pele, e produtos para bebés), pesquisa por palavra-chave, filtragem por categoria, paginação e ordenação por colunas.

- **Carrinho e Checkout** — Carrinho em sessão (`SessionCart`) com operações de adicionar, aumentar, diminuir, remover e limpar, validação de stock em tempo real, dados de envio pré-preenchidos, múltiplos métodos de pagamento (cartão de crédito, PayPal, transferência bancária) e acompanhamento completo de estados nas áreas de cliente e administração.

- **Autenticação e Recuperação de Conta** — Login com Spring Security 6 por nome de utilizador ou email, encriptação BCrypt, cookies `remember-me` de 7 dias, proteção contra session fixation, política de 1 sessão simultânea, recuperação de palavra-passe via Gmail SMTP e atualização autenticada com validação de complexidade.

- **Painel de Cliente (`/user/dashboard`)** — Edição de perfil, histórico paginado de encomendas com detalhe por artigo e avaliações verificadas limitadas a artigos comprados.

- **Painel de Administração (`/admin/dashboard`)** — Gestão paginada, ordenável e filtrável de produtos, encomendas e utilizadores, incluindo upload multipart de imagens e atualização de estados.

- **Armazenamento Agnóstico ao Ambiente** — Alterna entre AWS S3 (IAM role) em produção e MinIO local sem alterações de código.

---

## Stack Tecnológica

| Camada | Tecnologias |
| --- | --- |
| **Backend** | Java 21, Spring Boot 3.4.1 (Web MVC, Data JPA, Security, Validation, Mail), MapStruct 1.6.3, Lombok |
| **Frontend** | Thymeleaf, `thymeleaf-extras-springsecurity6`, Alpine.js 3, CSS modular (`tokens.css`, `components.css`, `layout.css`, `pages.css`) |
| **Base de Dados e Seed** | MySQL 8.0, Liquibase 4.32.0 (seeding pós-Hibernate), H2 2.3.232 (perfil local `dev`) |
| **Armazenamento** | AWS SDK para Java v2 (`s3`), MinIO (servidor local compatível com S3) |
| **Infraestrutura e DevOps** | Dockerfile multi-stage (`eclipse-temurin:21`), Docker Compose, proxy Nginx TLS, Logback, GitHub Actions |

---

## Arquitetura

```mermaid
flowchart LR
    Browser["Navegador (Thymeleaf + Alpine.js)"]
    Nginx["Proxy Reverso Nginx (80 -> 443 TLS, prod)"]
    App["App Spring Boot (Java 21, :8080)"]
    DB[("MySQL 8.0 / H2 Dev")]
    Storage[("AWS S3 (IAM Role) / MinIO")]
    SMTP["Gmail SMTP (587 STARTTLS)"]

    Browser -->|"HTTPS (prod)"| Nginx
    Nginx -->|"HTTP proxy_pass"| App
    Browser -.->|"HTTP :8080 (local)"| App
    App -->|"Spring Data JPA + Liquibase"| DB
    App -->|"S3 SDK v2 + URLs Pré-assinados"| Storage
    App -->|"Recuperação de Palavra-passe"| SMTP
    Browser -.->|"GET Direto de Imagem Pré-assinada"| Storage
```

---

## Primeiros Passos

**Pré-requisitos:** Docker e Docker Compose (Opções 1 e 2), ou JDK 21 para execução direta com Maven (Opção 3; `./mvnw` está incluído).

### Opção 1 — Ambiente Local Completo com Docker e MinIO (Recomendado)

Inicia **MySQL 8.0**, **Spring Boot** (perfil `prodminio`) e um servidor local **MinIO** compatível com S3 com criação automática do bucket `farmasterrae`.

1. Copie [`.env.example`](.env.example) para `.env` e preencha `DB_NAME`, `DB_USERNAME` e `DB_PASSWORD` (`EMAIL` e `PASSWORD` são opcionais exceto para testar emails de recuperação):

   ```bash
   cp .env.example .env
   ```

2. Compile e inicie os serviços:

   ```bash
   docker compose -f docker-compose.help.yml up --build
   ```

3. Aceda aos serviços:
   - **Loja** — `http://localhost:8080`
   - **Consola MinIO** — `http://localhost:9001` (`minioadmin` / `minioadmin`)
   - **MySQL** — `localhost:3306`

Perfis, utilizadores, produtos, encomendas e avaliações de exemplo são carregados automaticamente via Liquibase (`src/main/resources/db/changelog/data/`).

### Opção 2 — Deploy em Produção / AWS

Inicia **MySQL 8.0**, **Spring Boot** (perfil `prod`) e **Nginx** com redirecionamento HTTP para HTTPS (portas `80` e `443`).

1. Copie [`.env.example`](.env.example) para `.env` e preencha todas as variáveis de produção (`SPRING_PROFILES_ACTIVE=prod`, `SECRET_KEY`, `DB_*`, `EMAIL`, `PASSWORD`, `AWS_REGION`, `AWS_BUCKET`).

2. Coloque o certificado TLS e a chave privada em `./ssl/`:
   - `ssl/farmasterrae.crt`
   - `ssl/farmasterrae.key`

3. Associe ao host uma IAM role com permissões no bucket S3 (`S3Config` usa `DefaultCredentialsProvider` em `prod`, sem chaves estáticas).

4. Inicie a stack:

   ```bash
   docker compose up --build -d
   ```

> [!IMPORTANT]
> Substitua quaisquer certificados de desenvolvimento ou autoassinados em `./ssl/` por certificados válidos emitidos por uma CA (como Let's Encrypt) antes de expor o serviço publicamente.

### Opção 3 — Modo de Desenvolvimento Local com Maven e H2

Executa o Spring Boot diretamente com base de dados H2 em memória (modo compatível com MySQL) inicializada com `src/main/resources/data.sql`.

1. Crie o ficheiro ignorado pelo Git `src/main/resources/application-secrets.properties` para resolver as propriedades de email e S3 no arranque:

   ```properties
   spring.mail.username=dev@example.com
   spring.mail.password=dev-app-password
   aws.accessKey=minioadmin
   aws.secretKey=minioadmin
   aws.region=us-east-1
   aws.s3.bucket-name=farmasterrae
   aws.s3.endpoint=http://localhost:9000
   ```

2. Execute a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Acessos locais:
   - **Aplicação** — `http://localhost:8080`
   - **Consola H2** — `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE`
     - Utilizador: `sa` (palavra-passe vazia)

---

## Variáveis de Ambiente e Perfis

Modelo de referência: [`.env.example`](.env.example).

| Variável | Perfis | Predefinição | Descrição |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | `prod` (`Dockerfile`) | Perfil Spring ativo (`prod` na AWS; fixo em `prodminio` no `docker-compose.help.yml`). |
| `SECRET_KEY` | `prod`, `prodminio` | `uniqueAndSecret` | Chave secreta para assinar cookies `remember-me` de 7 dias (`app.security`). |
| `DB_NAME` | `prod`, `prodminio` | `farmasterrae` | Nome da base de dados MySQL (também define `spring.application.name`). |
| `DB_HOST` | `prod`, `prodminio` | `localhost` / `db` | Host MySQL (`localhost` em `prod`, `db` em `prodminio` / Compose). |
| `DB_PORT` | `prod`, `prodminio` | `3306` | Porta MySQL. |
| `DB_USERNAME` | `prod`, `prodminio` | `root` | Utilizador MySQL. |
| `DB_PASSWORD` | `prod`, `prodminio` | *(vazio)* | Palavra-passe MySQL (também usada como `MYSQL_ROOT_PASSWORD` no Compose). |
| `EMAIL` | `prod`, `prodminio` | *(vazio)* | Endereço Gmail para recuperação de palavra-passe (`spring.mail.username`). |
| `PASSWORD` | `prod`, `prodminio` | *(vazio)* | **App Password** do Gmail para autenticação SMTP (`spring.mail.password`). |
| `AWS_REGION` | `prod` | `us-east-1` | Região AWS S3 (`aws.region`). |
| `AWS_BUCKET` | `prod` | `farmasterrae` | Nome do bucket AWS S3 (`aws.s3.bucket-name`). |

### Perfis Spring

| Perfil | Base de Dados | Armazenamento | Esquema e Dados | Utilização Prevista |
| --- | --- | --- | --- | --- |
| *(predefinido / `dev`)* | H2 em memória (modo MySQL) | Chaves estáticas + endpoint opcional (`application-secrets.properties`) | Hibernate `update` + `data.sql` | Desenvolvimento Java rápido local |
| `prodminio` | MySQL 8.0 (`db:3306`) | MinIO (`http://minio:9000` → `http://localhost:9000`) | Hibernate `update` + Liquibase | Testes full-stack locais em Docker |
| `prod` | MySQL 8.0 | AWS S3 (IAM Instance Role) | Hibernate `update` + Liquibase | Produção em AWS com HTTPS |

---

## Controlo de Acesso e Rotas Principais

| Nível de Acesso | Rotas | Finalidade |
| --- | --- | --- |
| **Público** | `/`, `/products`, `/products/categories`, `/products/{reference}` | Loja, diretório de categorias, pesquisa e detalhe de produto |
| **Público** | `/cart`, `/cart/add/{ref}`, `/cart/increase/{ref}`, `/cart/decrease/{ref}`, `/cart/remove/{ref}`, `/cart/clear` | Operações do carrinho em sessão |
| **Público** | `/auth`, `/auth/register`, `/auth/forgotPassword`, `/logout` | Login, registo, recuperação de palavra-passe e logout |
| **Autenticado** | `/order`, `/order/confirm`, `/auth/changePassword` | Checkout, confirmação de encomenda e alteração de palavra-passe |
| **`USER` ou `ADMIN`** | `/user/dashboard`, `/user/dashboard/info-user`, `/user/dashboard/orders`, `/user/dashboard/orders/{id}`, `/user/dashboard/reviews` | Gestão de perfil, histórico de encomendas e avaliações |
| **`ADMIN`** | `/admin/dashboard`, `/admin/dashboard/products`, `/admin/dashboard/orders`, `/admin/dashboard/users` | Gestão administrativa de catálogo, encomendas e utilizadores |

---

## Estrutura do Projeto

```text
FarmasTerrae/
├── .github/workflows/maven.yml          # CI no GitHub Actions (empacotamento Maven com JDK 21)
├── .env.example                         # Modelo de variáveis de ambiente
├── Dockerfile                           # Imagem multi-stage de compilação e execução Temurin 21
├── docker-compose.yml                   # Stack de produção (MySQL + App + Nginx TLS)
├── docker-compose.help.yml              # Stack local Docker (MySQL + App + MinIO)
├── docs/mark.svg                        # Símbolo SVG da marca usado na documentação
├── nginx.conf                           # Proxy reverso Nginx com redirecionamento HTTP->HTTPS
├── pom.xml                              # Dependências Maven e configuração MapStruct/Lombok
├── ssl/                                 # Certificado e chave TLS montados pelo Nginx
└── src/
    ├── main/
    │   ├── java/com/iyg16260/farmasterrae/
    │   │   ├── config/                  # Security, S3, LiquibaseRunner, MVC, Web, GlobalExceptionHandler
    │   │   ├── controller/              # Controladores da loja, Auth, Cart, Order, User e Admin
    │   │   ├── dto/                     # DTOs de Auth, Order, Payment, Product e User
    │   │   ├── enums/                   # Enumerações de domínio (estados de encomenda, métodos de pagamento, operações)
    │   │   ├── mapper/                  # Mappers MapStruct (Product, Order, Review, User)
    │   │   ├── model/                   # Entidades JPA (User, Profile, Product, Order, OrderDetails, Review)
    │   │   ├── repository/              # Repositórios Spring Data JPA
    │   │   ├── service/                 # Lógica de negócio, StockReservation, ImageCompression, S3Storage
    │   │   ├── spec/                    # JPA Specifications dinâmicas (Generic, Product, Order, User)
    │   │   └── utils/                   # SessionCart, validação de palavras-passe e utilitários
    │   └── resources/
    │       ├── application*.properties  # Perfis dev, prod e prodminio
    │       ├── data.sql                 # Dados iniciais para H2 local
    │       ├── db/changelog/            # Changelog Liquibase e scripts SQL de dados
    │       ├── logback.xml              # Appenders de consola e ficheiros rotativos info/warn/error
    │       ├── static/                  # CSS modular (tokens/components/layout/pages) e ativos
    │       └── templates/               # Vistas Thymeleaf e fragmentos reutilizáveis
    └── test/                            # Testes Spring Boot
```

---

## Compilação, Logs e CI

### Comandos Maven

```bash
# Empacotar o JAR (igual ao Dockerfile e CI)
./mvnw clean package -DskipTests

# Executar testes
./mvnw test
```

### Logs

Configurados em `src/main/resources/logback.xml`:

- **Consola (`STDOUT`)** — Registos `INFO+` sem stack traces completos.
- **Ficheiros rotativos** em `logs/` (ignorado pelo Git):
  - `logs/info.log` — rotação diária, retenção de 30 dias
  - `logs/warn.log` — rotação diária, retenção de 30 dias
  - `logs/error.log` — rotação diária, retenção de 90 dias com stack traces completos

### Integração Contínua

Cada push ou pull request dirigido a `master` dispara [`.github/workflows/maven.yml`](.github/workflows/maven.yml), compilando e empacotando o projeto com Temurin JDK 21 e codificação UTF-8.
