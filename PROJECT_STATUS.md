# Noghte Rangi — Project Status

## Current Phase

Phase 12 — Admin Orders

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
- Phase 12 — Admin Orders

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 12 complete: protected paginated order list/detail, explicit status transitions,
and transactional cancellation with exactly-once stock restoration. All required checks passed.
Phase 13 awaits explicit approval.

## Next Phase

Phase 13 — Real Homepage (NOT_STARTED; explicit approval required)

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
- Protected admin order list/detail, status filter, 20-row pagination, and explicit fulfillment/cancellation actions

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
- Stock decreases when the order is created, including unpaid orders; no timers or automatic cancellation.
- Admin flow: WAITING_FOR_PAYMENT → PAID → PREPARING → READY → DELIVERED; cancellation is allowed before delivery.
- DELIVERED and CANCELLED are terminal. Other fulfillment changes never alter stock.
- Cancellation locks the order and then products in ID order, atomically restoring ordered quantities once.
- Repeated/concurrent cancellation cannot restock twice; missing products or integer overflow reject the whole cancellation.
- Missing-product/overflow cases require manual investigation; no partial cancellation or inventory override exists.
- Admin order pages use no-store caching and escaped snapshot/customer text. New status-failure logging excludes customer data.
- New orders start WAITING_FOR_PAYMENT. Admin status changes are manual; no payment verification automation or refunds.
- Customer confirmation shows card instructions only while WAITING_FOR_PAYMENT.
- Item product IDs have no product FK; name/price snapshots survive later product deletion.
- Public order numbers combine UTC date and random UUID-derived groups; a unique database constraint enforces uniqueness.
- Order totals use whole Toman and exclude shipping charges. No gateway, receipt upload, or customer accounts.
- Temporary runtime credentials/data removed and verification server stopped.

## Last Verification

- Java 21 `mvn clean verify` PASSED: 87 tests, 0 failures/errors/skipped (2026-09-30).
- Ten admin-order integration tests cover anonymous blocking, admin access, CSRF, newest-first ordering,
  20-row database paging, status filtering/paging links, malformed input/missing orders, historical snapshots,
  escaped customer data, allowed/invalid transitions, all pre-delivery cancellation states, and terminal states.
- Exact quantities restored across multiple products, including inactive products. Repeated and concurrent
  cancellation restored stock once. Missing product, integer overflow, and forced PostgreSQL status-update
  failure rolled back all status/stock changes. Cancelled confirmation no longer requests payment.
- Packaged app started against PostgreSQL; Flyway V1–V7 validation and Hibernate ddl-auto=validate passed.
  No migration, dependency, or security-rule changes were required.
- Anonymous admin list/detail were blocked; authenticated list/detail/filter/paging worked. CSRF-less status
  POST returned 403; invalid/repeated transitions and missing orders produced Persian messages.
- Chromium exercised WAITING_FOR_PAYMENT → PAID → PREPARING → READY → DELIVERED; PostgreSQL confirmed
  stock stayed unchanged. Separate cancellation restored stock 47 → 50 for three units; retry remained 50.
  Cancelled orders could not return to fulfillment; delivered orders could not move backward.
- Twenty-three real checkout orders verified newest-first listing, 20/3 pagination, waiting-status 20/1 paging,
  cancelled filtering, current status labels, persisted totals, and historical snapshot display.
- Admin order list/detail fit 360/768/1440px without horizontal overflow; RTL and all six screenshots reviewed.
- Browser regressions passed: homepage/health, catalog search/filter/sort/pagination, product detail/gallery,
  complete cart lifecycle and no-JavaScript cart operation, checkout success/failure/current-price snapshots,
  order creation, session privacy, admin login/logout, category/product management, and image management.
- Temporary 23 orders/items, 20 products (one deleted during failure verification), four categories, images,
  credentials, and isolated image storage removed; verification server stopped.
- Phase 12 DONE. Phase 13 remains NOT_STARTED. No automatic commit.

## Last Updated

2026-09-30