# Noghte Rangi — Project Status

## Current Phase

Phase 11 — Checkout and Orders

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
- Phase 11 — Checkout and Orders

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 11 complete: guest checkout, persistent orders and item snapshots, transactional
stock reduction, and configured card-to-card confirmation. All required verification passed.
Phase 12 awaits explicit approval.

## Next Phase

Phase 12 — Admin Orders (NOT_STARTED; explicit approval required)

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
- Guest checkout and persistent orders with historical name/price snapshots (Flyway V7)
- Session-restricted order confirmation and environment-configured card-to-card instructions

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
- Session expiry or invalidation loses the cart and access to confirmation, but persisted orders remain.
- Checkout requires STORE_PAYMENT_CARD_NUMBER, STORE_PAYMENT_ACCOUNT_NAME, and STORE_PAYMENT_INSTRUCTIONS.
- Checkout revalidates current products/prices, locks product rows in ID order, and commits order/items/stock together.
- Stock decreases when the order is created, including unpaid orders; no timers, automatic cancellation, or restocking yet.
- New orders start WAITING_FOR_PAYMENT. Payment verification and admin order management are not implemented.
- Item product IDs have no product FK; name/price snapshots survive later product deletion.
- Public order numbers combine UTC date and random UUID-derived groups; a unique database constraint enforces uniqueness.
- Order totals use whole Toman and exclude shipping charges. No gateway, receipt upload, or customer accounts.
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- Java 21 `mvn clean verify` PASSED: 77 tests, 0 failures/errors/skipped (2026-09-29).
- Ten checkout integration tests cover required fields/lengths/mobile normalization, empty carts,
  server-side current prices, name/price snapshots after product changes/deletion, totals/status/numbers,
  inactive category/product, zero/insufficient stock, missing product, overflow, and payment configuration.
- Real PostgreSQL item-insert failure rolled back order/items/stock and retained the session cart.
  Multi-item failure rolled back earlier stock changes; concurrent checkouts did not oversell;
  simultaneous same-session submissions produced only one order. CSRF/admin/session privacy checks passed.
- Packaged app started against PostgreSQL; new Flyway V7 migration and Hibernate ddl-auto=validate passed.
  Previous migrations were unchanged. No new dependencies or security-rule changes.
- Anonymous home, health, catalog, detail, cart, and populated checkout worked; empty checkout redirected safely.
- Chromium completed Home/Product/Cart/Checkout/Confirmation with Persian delivery data and configured test
  card instructions. Order number, WAITING_FOR_PAYMENT status, 90,000 Toman current-price total, cleared cart,
  persisted order/item values (45,000 × 2), and stock reduction (3 to 1) were verified against PostgreSQL.
- Browser failure cases passed: invalid customer input, stock reduced after opening checkout, zero stock,
  inactive product, and product deleted after opening checkout. Failed attempts retained the cart.
- Price changed after opening checkout was used at submission; later name/price edits did not alter snapshots.
  Another session could not access confirmation. Checkout POST without CSRF returned 403.
- Checkout and confirmation fit 360/768/1440px without horizontal overflow; RTL, touch-friendly fields,
  readable summaries/payment instructions, and all six screenshots were visually reviewed on 2026-09-30.
- Existing browser regressions passed: admin login/logout; category/product create/edit/activation;
  image upload/order/remove/fallback; catalog search/filter/sort/pagination; detail/gallery/keyboard;
  complete shopping-cart lifecycle, live changes, badges/totals, and no-JavaScript cart operation.
- Temporary order/item, 20 products (one deleted in the failure test), four categories, images, credentials,
  and isolated image storage removed; verification server stopped on 2026-09-30.
- Phase 11 DONE. Phase 12 remains NOT_STARTED. No automatic commit.

## Last Updated

2026-09-30