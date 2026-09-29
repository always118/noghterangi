# Noghte Rangi — Project Status

## Current Phase

Phase 10 — Shopping Cart

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
- Phase 10 — Shopping Cart

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 10 complete: anonymous session cart, current product prices and availability,
server-side quantity validation, RTL cart page, quantity badge, and CSRF-protected forms.
All required build, runtime, browser, and regression checks passed. Phase 11 awaits approval.

## Next Phase

Phase 11 — Checkout and Orders (NOT_STARTED; explicit approval required)

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
- Anonymous HTTP-session cart with product-detail add, quantity controls, subtotal, and navigation badge

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
- Cart stores only product IDs and quantities in the HTTP session; no cart tables or migration.
- Current prices are reloaded; stock reductions and unavailable/deleted products remain visible with warnings.
- Invalid quantity changes are rejected with Persian messages; use direct update to correct stock excess.
- Badge counts all stored quantities, including unavailable items; subtotal includes resolvable warning rows.
- Long Toman multiplication/addition overflow suppresses the affected total and marks the cart invalid.
- Session expiry or invalidation loses the cart. No checkout or stock reservation is implemented.
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- `mvn clean verify` PASSED using Java 21: 67 tests, 0 failures/errors/skipped.
- Seven cart integration tests cover empty/session-isolated carts, add/merge/increase/decrease/update,
  invalid/malformed/overflowing quantities, stock bounds, remove/clear, multi-product totals, badge,
  current prices, inactive category/product, zero stock, deleted product, money overflow, CSRF,
  anonymous access, and protected admin routes. Updated product detail tests passed.
- Packaged application started against PostgreSQL; Flyway V1–V6 and Hibernate validation passed.
  No migration, dependency, or security-rule changes.
- Anonymous `/`, `/health`, `/products`, active product detail, and `/cart` returned HTTP 200.
- Chromium passed empty/add/merge/increase/decrease/direct-update/remove/clear, badge and subtotal,
  Persian success/error flashes, live admin price/stock/inactive changes, out-of-stock handling,
  missing-image fallback, and CSRF rejection on every cart POST route.
- Cart forms also passed with JavaScript disabled in an independent anonymous session.
- Cart/home/catalog/detail fit 360/768/1440px without horizontal overflow; RTL, touch targets,
  and cart screenshots at all three widths were checked.
- Browser regressions passed: admin login/logout, category edit/activation, product create/edit/activation,
  image upload/order/removal/fallback, catalog search/filter/sort/pagination, and detail gallery/keyboard/404.
- Temporary 20 products, four categories, images, credentials, and storage removed; server stopped.
- Phase 10 DONE. Phase 11 remains NOT_STARTED. No automatic commit.

## Last Updated

2026-09-29