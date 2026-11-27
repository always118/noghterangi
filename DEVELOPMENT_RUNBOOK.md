# Development Runbook

This runbook is for both human developers and AI agents. It defines how to run the application and tests without mixing environments.

## Before any task

1. Read `AGENTS.md`, `PROJECT_STATUS.md`, `PHASE_STATUS.md` and, for environment work, `ENVIRONMENT_GUIDE.md`.
2. State the target environment explicitly: `dev`, `test`, or `production`.
3. Inspect only relevant feature files. Do not scan raw media, `target/`, logs or generated output.
4. Never connect to or write Production without explicit credentials, backup and approval.

## Prerequisites

- JDK 21
- Maven 3.6.3+
- Docker Engine/Desktop with Compose v2
- A private `.env` copied from `.env.example`

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
docker compose version
```

## Local development environment

The default Compose stack is the local development environment:

- PostgreSQL database: `noghterangi`, host port `5432`
- persistent volumes: `postgres-data` and `media-data`
- application: `http://localhost:8080`
- profile: use `dev` when a profile is needed explicitly

```bash
cp .env.example .env
# Set DB_PASSWORD, TEST_DB_PASSWORD, ADMIN_USERNAME, ADMIN_PASSWORD_HASH
# and the three STORE_PAYMENT_* values in .env.
docker compose config --quiet
docker compose up -d --build
docker compose ps
curl -fsS http://localhost:8080/health
```

For a Maven-launched application, start PostgreSQL first and export local variables:

```bash
set -a
source .env
set +a
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The normal app image does not import data automatically. Stop it with `Ctrl+C` or `docker compose down`. Do not use `docker compose down -v` unless deliberately deleting local database and media data.

## Isolated test environment

The test database is separate from development:

- database: `noghterangi_test`
- host port: `5433`
- Compose profile: `test`
- storage: temporary `tmpfs`
- credentials: `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`

The test password in `.env` must match the password used when the `postgres-test` container was initialized. If it changes, recreate only the test container; never point test settings at the development or Production database.

```bash
docker compose --profile test up -d --wait postgres-test
set -a
source .env
set +a
mvn clean verify
```

Useful targeted commands:

```bash
mvn -DskipTests compile
mvn -DskipTests test-compile
mvn -Dtest=ProductIntegrationTest test
```

A database connection, Flyway migration or authentication failure is a real test failure; do not silently skip it. Test fixtures must create their own records and must not depend on development data.

## CI/CD and production

Connect the GitHub repository to a published Codex Cloud environment from web or desktop, then select that environment for tasks on mobile. Ask Codex to open a PR and report test results. On GitHub, require PRs and the `verify` status check on the default branch. Restrict direct pushes and review changes to workflows and deployment files. The workflow verifies PRs and pushes against an isolated PostgreSQL 16 service, then publishes a commit-tagged image to GHCR after a successful default-branch push.

Before enabling deployment:

1. Back up production PostgreSQL and media together. Identify the existing Compose project name, media volume, DB network, and DB host from production. Check server architecture against the published Linux amd64 image. Keep the current application image and release digest for recovery.
2. Install `compose.production.yml` and `scripts/deploy-production.sh` into a trusted server-side deployment directory. Keep them and the directory protected from modification by unrelated users. Configure the directory's private `.env` with `COMPOSE_PROJECT_NAME` (the existing project's name for an in-place switch), `PRODUCTION_MEDIA_VOLUME`, `PRODUCTION_DB_NETWORK`, and optional `PRODUCTION_APP_PORT`. Put production JDBC and application secrets in `.env.production`, with restrictive permissions. Database access must work independently of this app-only Compose file. Verify the existing app is managed as service `app`, and the reverse proxy targets the configured loopback port. For private GHCR images, log in on the server as the deployment user with a package read credential.
3. Give a dedicated deploy key a forced command in the deployment user's `authorized_keys`. Example entry (replace all paths, repository and key):

   ```text
   restrict,command="/bin/bash /srv/noghterangi/scripts/deploy-production.sh /srv/noghterangi ghcr.io/owner/repo" ssh-ed25519 PUBLIC_KEY
   ```

   The account must be able to use Docker. Docker access is effectively root on the host, so treat this as a privileged deployment identity. Keep the private key in GitHub Actions secrets, never in Codex or Git.
4. Configure GitHub environment `production`, restricting it to the default branch where supported. Set variables `DEPLOY_HOST` (SSH hostname or IP on port 22), `DEPLOY_USER`, and secrets `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS` (host key verified via the server console). Set repository variable `PRODUCTION_DEPLOY_ENABLED=true` only when ready for the first approved cutover. Redeployments run after a default-branch push that passes verification and publishing.
5. At cutover, verify the GitHub deployment, public HTTPS `/health`, homepage and a known existing image. The host waits for app container health and reports failure to GitHub. For recovery, use the previous digest with `PRODUCTION_IMAGE` and the same app-only Compose file after checking Flyway/schema compatibility. If the schema changed incompatibly, follow the production backup recovery plan. Retain the failed release logs and the matching DB/media backups.

## Channel product import

Input and source files:

- `dev-data/channel-products.json`: reviewed manifest
- `noghterangi-export/posts.json`: raw channel posts
- `noghterangi-export/media/`: source images
- `dev-data/category-catalog.json`: canonical category reference

The endpoint is disabled by default. For an intentional local `dev` or `test` import, set `CHANNEL_IMPORT_ENABLED=true`, make `PRODUCT_IMPORT_MEDIA_ROOT` point to the export root, log in as admin, obtain a CSRF token from the same session cookie, and upload the manifest:

```bash
curl -b admin.cookies -H 'X-CSRF-TOKEN: TOKEN' \
  -F 'manifest=@dev-data/channel-products.json;type=application/json' \
  http://localhost:8080/admin/import/products
```

The importer reads category slugs from the target database. It does not create or rename categories from JSON. Existing products are retained by default. `deleteExisting=true` is destructive and requires an explicit decision and backup.

## Development/test catalog reset and category import

The catalog-management API is disabled by default and is unavailable under the `prod` and `production` profiles. Enable it only for the intended local `dev` or isolated `test` environment:

```env
CATALOG_MANAGEMENT_ENABLED=true
```

After logging in, the preferred human workflow is the server-rendered admin page at `/admin/catalog`. It provides category import, channel product import and the explicitly confirmed catalog reset form with CSRF handled by the application.

After logging in as an admin and obtaining a CSRF token, import the canonical two-level category catalog:

```bash
curl -b admin.cookies -H 'X-CSRF-TOKEN: TOKEN' \
  -F 'catalog=@dev-data/category-catalog.json;type=application/json' \
  http://localhost:8080/admin/catalog/categories/import
```

The operation creates missing categories, keeps matching existing categories, validates duplicate slugs and rejects an existing category whose parent does not match the catalog. It does not delete or rename existing categories.

To remove products from the selected `dev` or `test` database, use the product-specific endpoint and explicit confirmation token:

```bash
curl -b admin.cookies -H 'X-CSRF-TOKEN: TOKEN' -X POST \
  'http://localhost:8080/admin/catalog/products/clear?confirmation=DELETE_ALL_CATALOG'
```

To remove categories, first remove all products, then use:

```bash
curl -b admin.cookies -H 'X-CSRF-TOKEN: TOKEN' -X POST \
  'http://localhost:8080/admin/catalog/categories/clear?confirmation=DELETE_ALL_CATALOG'
```

The category endpoint refuses to run while products remain. Neither operation deletes orders; order items keep their historical product snapshots. Both endpoints are destructive, so take a database/media backup and verify the target environment before using them. Image files are cleaned up after a successful database commit.

To regenerate the manifest after reviewing the raw export:

```bash
node scripts/extract_channel_manifest.js
```

## Completion checklist

- Relevant Java 21 compile/test command passed.
- Target environment and database were recorded.
- No Production write occurred without approval.
- Status files were updated with exact verification evidence.
- `PROJECT_STRUCTURE.md` and this runbook were updated if architecture, commands, environments or import contracts changed.
- No automatic Git commit was created.
