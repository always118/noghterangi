# Noghte Rangi Project Structure

This document is the fast orientation guide for developers and AI agents. Read it before scanning the repository. Inspect only the feature and configuration files relevant to the task.

## Technology and architecture

- Java 21
- Spring Boot 4.1.x
- Spring MVC and server-rendered Thymeleaf
- Spring Security with one environment-configured admin
- Bootstrap 5 RTL and local static assets
- PostgreSQL 16, Spring Data JPA and Flyway
- Simple package-by-feature modular monolith
- Maven build; no frontend build pipeline

Do not introduce React, a SPA, microservices, Redis, Kafka, Elasticsearch, GraphQL, or unnecessary abstractions.

## Repository map

```text
src/main/java/com/noghterangi/
├── admin/       Admin login, security configuration and admin home
├── cart/        Anonymous HTTP-session cart
├── catalog/     Public catalog queries plus protected dev/test catalog management
├── category/    Category entity, repository, service and admin UI
├── channel/     Protected channel-product JSON import endpoint and service
├── devdata/     Development-only canonical category catalog importer
├── home/        Database-driven homepage
├── media/       Product/category media metadata, storage and admin upload UI
├── order/       Guest checkout, orders and admin fulfillment
├── product/     Product entity, service, public detail and admin CRUD
└── shared/      Health and shared application controllers

src/main/resources/
├── db/migration/    Flyway schema migrations; never edit an applied migration
├── templates/       Thymeleaf views grouped by feature
├── static/          CSS, JavaScript, fonts and vendored browser assets
├── application.properties
└── messages.properties

src/test/java/com/noghterangi/   Feature integration and unit tests
dev-data/                         Canonical category catalog and reviewed channel manifest
noghterangi-export/               Raw channel export and source media; development input only
scripts/                          Small data-preparation utilities, not application runtime code
docker-compose.yml                Local development PostgreSQL/app and isolated test PostgreSQL
Dockerfile                        Java 21 image with separate Spring Boot dependency and application layers
compose.production.yml            Production app and existing external media/network
.github/workflows/ci-cd.yml        PostgreSQL tests, GHCR release and opt-in deployment
PROJECT_STATUS.md                 Phase history, current work and verification facts
PHASE_STATUS.md                   One-row status/verification per phase
ENVIRONMENT_GUIDE.md              Dev/test/production boundaries and synchronization rules
DEVELOPMENT_RUNBOOK.md             Commands for AI agents and human developers
CATALOG_MANAGEMENT_GUIDE.md        Safe catalog reset and canonical category import commands
```

## Source of truth

- Database schema: `src/main/resources/db/migration/`.
- Runtime category taxonomy: the target database `noghterangi.category` table.
- Canonical category reference: `dev-data/category-catalog.json` (8 roots, 49 children).
- Channel import input: `dev-data/channel-products.json`.
- Raw channel source: `noghterangi-export/posts.json` and `noghterangi-export/media/`.
- Runtime media: `MEDIA_STORAGE_PATH`; it is not the raw export directory.
- Environment boundaries: `ENVIRONMENT_GUIDE.md`.

JSON category names never create or rename runtime categories. Importers resolve each product `categorySlug` against the target database and reject missing categories.

## Request and data flow

```text
Browser -> Controller -> Feature Service -> JPA Repository -> PostgreSQL
                                      \-> MediaService -> MediaStorageService -> media filesystem
```

The channel import flow is:

```text
admin multipart request
  -> AdminChannelProductImportController
  -> ChannelProductImportService
  -> database category lookup
  -> ProductService
  -> MediaService
  -> PostgreSQL + configured media storage
```

The endpoint is `POST /admin/import/products`, requires admin authentication and CSRF, and is disabled by default through `CHANNEL_IMPORT_ENABLED=false`. It is not registered under `prod` or `production`.

## Change navigation

- UI route: start with the relevant controller and Thymeleaf template.
- Business rule: start with the feature service and its integration tests.
- Persistence/schema: inspect entity, repository and the latest Flyway migration together.
- Images: inspect `MediaService`, `MediaStorageService` and `LocalMediaStorageService`.
- Environment or import: read `ENVIRONMENT_GUIDE.md`, then `DEVELOPMENT_RUNBOOK.md`.
- Catalog reset/category import: read `CATALOG_MANAGEMENT_GUIDE.md` before using the protected API.
- Phase/status: read and update `PROJECT_STATUS.md` and `PHASE_STATUS.md`.

Do not use `target/`, generated files, logs, uploads, binaries or raw media as primary code context.

## Phase documentation rule

At the start of a phase, set its row to `IN_PROGRESS`. At completion, record the exact build, test and verification evidence before marking it `DONE`. Every phase that changes commands, environments, import inputs or architecture must update this file and `DEVELOPMENT_RUNBOOK.md` in the same change.
