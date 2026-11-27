# Production environment variables (`prod` profile)

Used by [`docker-compose.yml`](../docker-compose.yml) and [`application-prod.properties`](../src/main/resources/application-prod.properties).
Put them in a `.env` file next to `docker-compose.yml` (start from [`.env.example`](../.env.example)) or export them before starting the app.
Never commit real values. `.env` is gitignored and excluded from the Docker build context.

```bash
# Spring profile
export SPRING_PROFILES_ACTIVE="prod"

# Remember-me cookie signing key (required, >= 32 characters, no default): openssl rand -base64 48
export SECRET_KEY="replace-with-at-least-32-random-characters-from-openssl"

# Public origin used in password reset links (include scheme, omit trailing slash)
export APP_BASE_URL="https://farmasterrae.example"

# MySQL (DB_USERNAME and DB_PASSWORD are required; there is no root/empty fallback)
export DB_NAME="farmasterrae"
export DB_HOST="db"                          # default: localhost
export DB_PORT="3306"                        # default: 3306
export DB_USERNAME="farmasterrae_user"
export DB_PASSWORD="a-strong-password"
export MYSQL_ROOT_PASSWORD="another-strong-password"   # MySQL container only; must differ from DB_PASSWORD
export DB_SSL_MODE="PREFERRED"               # optional: PREFERRED (default), REQUIRED, VERIFY_CA, VERIFY_IDENTITY
export HIBERNATE_DDL_AUTO="update"           # optional: default update; use validate once the schema exists

# First ADMIN account (optional). Created on startup only if all three are set and no ADMIN exists.
# Password rules: 12-64 characters, with uppercase, lowercase and a digit; symbols and spaces are allowed.
# Remove these values after the first successful start.
export ADMIN_BOOTSTRAP_USERNAME="admin"
export ADMIN_BOOTSTRAP_EMAIL="admin@example.com"
export ADMIN_BOOTSTRAP_PASSWORD="<your-own-password>"   # placeholder: rejected as-is

# Gmail SMTP for password recovery (use a Google App Password)
export EMAIL="you@example.com"
export PASSWORD="your-app-password"

# AWS S3. Credentials come from the AWS default credentials chain (e.g. the EC2/ECS IAM role);
# static access keys are not used in production.
export AWS_REGION="us-east-1"                # default: us-east-1
export AWS_BUCKET="your-bucket-name"         # default: farmasterrae
```

## What `prod` seeds

Liquibase runs with the `prod` context, which only inserts the `ADMIN` and `USER` profiles.
The demo dataset (users, products, orders and reviews, all with password `123`) needs the `dev` context and only runs in the local `prodminio` stack.
Use the `ADMIN_BOOTSTRAP_*` variables to create the first administrator.

If a production database was seeded by an older version, it may still contain the demo accounts (`admin_juan`, `admin_maria`, `cliente_*`).
Delete or disable them, and change their passwords.

## Schema upgrades (`validate` mode)

If you upgrade an existing production database with `HIBERNATE_DDL_AUTO=validate`, apply the schema additions before restarting:

```sql
ALTER TABLE products ADD COLUMN deletedAt DATETIME NULL;

-- Check for duplicate (userId, productId) reviews before adding the unique constraint:
SELECT userId, productId, COUNT(*) FROM reviews GROUP BY userId, productId HAVING COUNT(*) > 1;
-- Once duplicates (if any) are cleaned up:
ALTER TABLE reviews ADD CONSTRAINT uk_reviews_user_product UNIQUE (userId, productId);
```

## TLS certificates

Nginx reads `ssl/farmasterrae.crt` (certificate plus chain) and `ssl/farmasterrae.key` from the read-only `./ssl` mount.
The directory is gitignored: provision the files on the server, for example from Let's Encrypt.
Then set `server_name` in [`nginx.conf`](../nginx.conf) to the domain(s) on the certificate.
Requests for any other host name are rejected.
