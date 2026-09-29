# Noghte Rangi — Project Status

## Current Phase

Phase 9 — Product Detail

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
- Phase 9 — Product Detail

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 9 complete: public slug-based product detail, accessible gallery, safe descriptions,
optional game specifications, stock-bounded quantity UI, and clearly disabled Add to Cart.
All required checks passed. Awaiting explicit approval for Phase 10.

## Next Phase

Phase 10 — Shopping Cart (NOT_STARTED; explicit approval required)

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
- Public product detail by slug with gallery, breadcrumbs, and friendly 404

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
- Add to Cart remains explicitly disabled until Phase 10; quantity UI reserves no stock.
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- Build: `mvn clean verify` PASSED on Java 21; 60 tests, 0 failures/errors/skipped.
- Detail tests: anonymous slug access, active product/category visibility, friendly unknown/inactive
  404, formatted prices, unavailable state, stock-bounded quantity, disabled cart control,
  category/catalog links, escaped optional text, populated/null game fields, ordered images and placeholders.
- Existing security/category/product/media/catalog regression tests passed, including new card links.
- Runtime: packaged application started against PostgreSQL; Flyway V1–V6 validation and Hibernate
  ddl-auto=validate succeeded. No migration or dependency changes required.
- Anonymous `/`, `/health`, `/products`, and active detail returned HTTP 200;
  unknown/inactive product and inactive-category detail returned friendly HTTP 404.
- Chromium: catalog-to-detail navigation, breadcrumbs/category links, price/availability,
  multiline escaped descriptions, board-game fields, and no-image placeholder passed.
- Gallery: JPEG/PNG upload/order, primary image selection by mouse and keyboard,
  physical-file-missing fallback, and image removal passed.
- Quantity: zero/negative/above-stock values failed native validity; in-stock quantity remained usable;
  unavailable quantity and all Add to Cart controls disabled. No cart backend or submitting cart form added.
- Product detail/catalog/home fit 360/768/1440px without horizontal overflow;
  price remained visible early, thumbnail targets were usable, and mobile/desktop screenshots reviewed.
- Admin login/logout, category CRUD/activation, product CRUD/activation, images/CSRF,
  and catalog search/filter/sort/pagination regressions passed with 20 temporary products in four categories.
- Temporary fixtures/credentials removed; image storage empty; verification server stopped.
- Phase 10 remains NOT_STARTED. No automatic commit.

## Last Updated

2026-09-29