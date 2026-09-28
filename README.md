# Noghte Rangi — نقطه رنگی

A Persian local online shop built incrementally as a simple Spring Boot modular
monolith. Current implementation: **Phase 2 — PostgreSQL and Flyway**.

## Prerequisites

- JDK 21 (`JAVA_HOME` must point to JDK 21)
- Maven 3.6.3 or later
- Docker Engine/Desktop with Compose v2 and a running Linux engine
- For WSL: enable Docker Desktop integration for this distribution

Check `java -version`, `mvn -version`, and `docker compose version`.
Spring Boot 4.1.1 manages dependency versions. The application uses MVC,
Thymeleaf, Validation, JPA, the PostgreSQL JDBC driver, and Flyway (including
its PostgreSQL module). There is no frontend build pipeline.

## Local configuration

For a new checkout, copy `.env.example` to `.env` and replace both placeholder
passwords with different local passwords. Keep the populated file private;
`.gitignore` excludes it. If `.env` already exists, preserve it.

Compose reads `.env` automatically. Spring Boot does **not**, so export the
settings before running Maven or the application. In Bash, for a trusted local
`.env` using shell-compatible values:

```bash
set -a
source .env
set +a
```

Quote values containing shell metacharacters. Never source an untrusted file.
The example contains no production credentials, and database passwords have
no fallback in application configuration.

| Variable | Purpose / local default |
| --- | --- |
| `DB_URL` | JDBC URL; `jdbc:postgresql://localhost:5432/noghterangi` |
| `DB_USERNAME` | Application database user; `noghterangi` |
| `DB_PASSWORD` | Required application database password |
| `DB_PORT` | Compose host port; `5432` |
| `TEST_DB_URL` | Test JDBC URL; `jdbc:postgresql://localhost:5433/noghterangi_test` |
| `TEST_DB_USERNAME` | Test database user; `noghterangi_test` |
| `TEST_DB_PASSWORD` | Required, separate test database password |
| `TEST_DB_PORT` | Test container host port; `5433` |
| `SERVER_PORT` | Application HTTP port; Spring Boot defaults to `8080` |

If you change a Compose host port, update the corresponding JDBC URL too.
Compose uses fixed local database/user names as listed above. When connecting
to an externally provisioned database, supply its JDBC URL and credentials.

## Start PostgreSQL

```bash
docker compose config --quiet
docker compose up -d --wait postgres
docker compose ps
```

PostgreSQL 16.15 runs in Docker, with its port bound to `127.0.0.1`. Development
data lives in the `postgres-data` named volume and survives container recreation.
The container health check uses `pg_isready`; application tests additionally
verify an authenticated JDBC connection and actual queries.

This Compose setup is for local development. The image's `POSTGRES_USER` is a
bootstrap superuser; production must use separately provisioned, appropriately
restricted database roles. Changing `.env` does not change passwords inside an
already initialized database. Change existing database credentials deliberately;
do not delete the data volume to troubleshoot authentication.

## Build and test

Start the optional isolated PostgreSQL test service, then export `.env` as above:

```bash
docker compose --profile test up -d --wait postgres postgres-test
mvn clean verify
```

Tests activate the `test` Spring profile and use only `TEST_DB_*` connection
settings. Never point those settings at a production or shared store database.
Test data is held in a temporary container filesystem and resets when the test
container stops. The development database uses a separate service and volume.
No H2 or additional database-testing libraries are used.

Four integration checks cover:

- `/health` returning HTTP 200 and plain text `OK`.
- The Persian RTL homepage rendered by Thymeleaf as UTF-8 HTML.
- A real PostgreSQL JDBC connection and a JPA query against the migrated schema.
- Flyway validation, the applied foundation migration, and a repeat migration
  run making no changes.

The test server uses a random HTTP port. A database connection or migration
failure must fail the build; tests are not silently skipped without PostgreSQL.

## Run the application

With the development database healthy and `.env` exported:

```bash
mvn spring-boot:run
```

Or, after a successful build:

```bash
java -jar target/noghte-rangi-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080/ and http://localhost:8080/health.
Stop the application with Ctrl+C. Set `SERVER_PORT` to change its HTTP port.
If your shell defines `DEBUG`, Spring Boot may enable verbose logging; unset it
for normal local logging. Public errors omit messages, binding errors, and traces.

## Database migrations

Flyway runs `src/main/resources/db/migration` automatically before JPA starts.
`V1__create_application_schema.sql` creates the empty `noghterangi` schema.
Flyway records applied versions and checksums in `public.flyway_schema_history`.
Hibernate's default schema is `noghterangi`, `ddl-auto=validate`, and Open Session
in View is disabled. SQL script initialization is disabled; Flyway owns DDL.

There are no business entities or tables in this phase, so Hibernate initializes
successfully in validation mode but has no entity mappings to validate yet.
Later phases add numbered migrations and entity/schema checks. Do not modify an
already applied migration; add a new version. Flyway clean is disabled.

## Manual verification

After starting the app:

```bash
curl -i http://localhost:8080/health
curl -i http://localhost:8080/
docker compose exec -T postgres psql -U noghterangi -d noghterangi -c 'SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;'
docker compose exec -T postgres psql -U noghterangi -d noghterangi -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'noghterangi';"
```

Expect HTTP 200 for both routes, migration version `1` with `success = t`, and
schema `noghterangi`. Restart the app: Flyway should report that it is up to date.
The `/health` route remains a simple web liveness check, not a live database probe.

To stop the local database services while preserving development data:

```bash
docker compose --profile test down
```

Do not add `--volumes` unless you intend to erase development database data.
There is no application container yet; that belongs to Phase 15.

## Scope and next phase

Java code lives under `com.noghterangi`. No admin, product, category, cart, or
order functionality is implemented. Phase 3 will add Bootstrap 5, reusable
Thymeleaf fragments, Persian typography, RTL layout, and a small design system.
Development stops after each phase for explicit approval.
