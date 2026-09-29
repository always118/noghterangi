# Noghte Rangi — Project Status

## Current Phase

Phase 8 — Product Catalog

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
- Phase 8 — Product Catalog

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 8 complete: database-backed public catalog with Persian name search, category/price/
availability filters, three safe sorts, pagination, reusable RTL cards, and homepage links.
All required checks passed. Awaiting explicit approval for Phase 9.

## Next Phase

Phase 9 — Product Detail (NOT_STARTED; explicit approval required)

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
- Public product catalog with PostgreSQL search/filter/sort and Spring Data pagination

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
- Catalog search is normalized substring matching on names; no full-text index or search engine.
- Public visibility uses the product and its own category active flags; parent activation is independent.
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- Build: `mvn clean verify` PASSED using Java 21; 53 tests, 0 failures/errors/skipped.
- Catalog tests: active visibility, direct-child category filtering, inclusive Long price ranges,
  stock filtering, all three deterministic sorts, bounded Spring Data pagination, Persian character/
  whitespace normalization and product renames, literal wildcard search, and combined filters.
- HTTP tests: anonymous catalog, invalid input fallbacks, escaped names, URL filter preservation
  including literal plus/ampersand through pagination, primary images/placeholders and missing media.
- Existing authentication/category/product/media regressions all passed.
- Runtime: packaged application started; PostgreSQL connection, V6 migration and Hibernate validation passed.
  V1–V5 and dependencies unchanged. V6 adds only the small normalization function; no demo data migration.
- Chromium: 20 temporary products across four categories; no filters, search/Persian variants,
  category/direct-child, min/max price, available-only, all sorts, pagination, combined filters,
  invalid URLs, empty states, and homepage category links passed.
- Product primary PNG preview, JPEG/PNG admin upload/order/removal, absent-image placeholder,
  and physical-file-missing 404 with visible placeholder passed.
- Admin login/logout, category create/edit/activation, product create/edit/price/activation,
  image management and CSRF regression passed. Anonymous admin remained protected.
- `/`, `/products`, and `/health` returned HTTP 200 anonymously.
- Catalog and homepage fit 360/768/1440px without horizontal overflow; mobile/desktop screenshots reviewed.
- Temporary 20 products/four categories/credentials removed; image storage empty; verification app stopped.
- Phase 9 remains NOT_STARTED. No automatic commit.

## Last Updated

2026-09-29