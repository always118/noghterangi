# Noghte Rangi — Project Status

## Current Phase

Phase 6 — Products

Status: DONE

## Completed Phases

- Phase 0 — Repository Inspection
- Phase 1 — Spring Boot Foundation
- Phase 2 — PostgreSQL and Flyway
- Phase 3 — Base UI, RTL and Design System
- Phase 4 — Admin Security
- Phase 5 — Categories
- Phase 6 — Products

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 6 complete: Product domain and protected admin list/create/edit/activation,
integer Toman prices, stock, featured/order settings, and board-game validation.
Category compatibility correction completed in V3 before Phase 6 began.
Awaiting explicit approval for Phase 7.

## Next Phase

Phase 7 — Product Images (NOT_STARTED; explicit approval required)

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
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- Category audit: missing parent corrected with V3; 22 tests passed before Phase 6.
- Build: `mvn clean verify` PASSED on Java 21; 32 tests, 0 failures/errors/skipped.
- Runtime: packaged application started; PostgreSQL connectivity and Hibernate validation passed.
- Flyway: V1–V4 applied successfully; original V1/V2 unchanged. Only category/product business tables.
- Product: authenticated create/edit/activation, price/stock updates, featured/order,
  unique slug/SKU, category selection, and board-game validation passed.
- Regression: Category parent/admin operations, public active categories, security/CSRF,
  anonymous / and /health HTTP 200; anonymous product admin redirected to login.
- UI: Chromium 360/768/1440px passed; mobile form and desktop list screenshots reviewed.
- Temporary runtime data/credentials removed; verification server stopped.

## Last Updated

2026-09-29