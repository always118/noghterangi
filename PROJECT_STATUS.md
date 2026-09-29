# Noghte Rangi — Project Status

## Current Phase

Phase 7 — Product Images

Status: DONE

## Completed Phases

- Phase 0 — Repository Inspection
- Phase 1 — Spring Boot Foundation
- Phase 2 — PostgreSQL and Flyway
- Phase 3 — Base UI, RTL and Design System
- Phase 4 — Admin Security
- Phase 5 — Categories
- Phase 6 — Products
- Phase 7 — Product Images

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 7 complete: local product image storage, protected multiple upload/order/removal,
primary-image previews, controlled public retrieval, and Persian validation messages.
All required checks passed. Awaiting explicit approval for Phase 8.

## Next Phase

Phase 8 — Product Catalog (NOT_STARTED; explicit approval required)

## Existing Foundation

- Java 21
- Spring Boot
- Spring MVC + Spring Security
- Thymeleaf
- Bootstrap 5 RTL
- PostgreSQL 16
- Spring Data JPA
- Flyway
- Hibernate ddl-auto=validate
- Docker Compose for PostgreSQL
- reusable Thymeleaf fragments
- Noghte Rangi RTL design system
- Category domain and admin management with optional two-level parent
- Product domain and protected admin management
- Media metadata and replaceable local storage; JPEG/PNG product images
- active category display on public homepage

## Important Technical Decisions

- Single Spring Boot application
- Simple modular monolith
- Package-by-feature
- Server-rendered Thymeleaf UI
- Bootstrap 5 RTL
- No SPA frontend
- PostgreSQL
- Flyway-managed schema
- Prices use Long / BIGINT in whole Toman
- Session-based shopping cart
- Guest checkout
- Manual card-to-card payment
- Session-based admin authentication
- No customer accounts in Store V1
- Content module belongs to V1.1

## Known Issues / Notes

- Configure ADMIN_USERNAME and ADMIN_PASSWORD_HASH before startup; no defaults.
- One environment-configured admin; no registration, reset, or admin user table.
- Set SESSION_COOKIE_SECURE=true for HTTPS; local HTTP defaults to false.
- Java 21 is required. PostgreSQL runs in Compose; application not containerized.
- Local images: MEDIA_STORAGE_PATH (default uploads/media), JPEG/PNG only; 5 MiB/file, 10 files/batch.
- Filesystem/DB are not atomic; crashes or cleanup failures may leave orphan files (documented in README).
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- Build: `mvn clean verify` PASSED on Java 21; 43 tests, 0 failures/errors/skipped.
- Media tests: real JPEG/PNG content, MIME/extension/empty/size/dimension validation,
  safe filenames/traversal/symlinks, ownership/order/primary selection, exact HTTP retrieval,
  missing-file handling, removal, transaction rollback cleanup, authentication and CSRF.
- Existing category/product/admin/security and homepage/health tests all passed.
- Runtime: packaged application started against PostgreSQL; V5 applied successfully,
  Hibernate ddl-auto=validate passed, V1–V4 and dependency list unchanged.
- Chromium: JPEG/PNG upload and display, multiple images including a 10-file batch,
  reorder/primary preview, removal and empty placeholder passed. Physical storage empty after removal.
- Invalid type and oversized upload rejected with Persian errors; oversized browser response HTTP 413.
- Anonymous admin upload/delete blocked; authenticated upload and logout passed; missing CSRF rejected.
- Product create/edit/price/stock/activation and category create/edit/activation/public visibility passed.
- Anonymous `/` and `/health` HTTP 200. Public image bytes/MIME and missing image HTTP 404 passed.
- RTL image management/product list/edit fit 360/768/1440px; mobile/desktop and 413 screenshots reviewed.
- Temporary runtime product/category/credentials removed; verification server stopped.
- Phase 8 remains NOT_STARTED. No automatic commit.

## Last Updated

2026-09-29