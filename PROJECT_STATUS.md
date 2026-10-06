# Noghte Rangi — Project Status

## Current Phase

Phase 17 — V1 Release Preparation

Status: IN_PROGRESS

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
- Phase 13 — Real Homepage
- Phase 14 — UX Polish

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 14 completed: public mobile navigation, search feedback, product/card hierarchy,
RTL, accessible controls and checkout/confirmation presentation refined. Business data is unchanged.

Phase 15 completed: PostgreSQL and Spring Boot run through Compose with a Java 21 multi-stage
image, external environment configuration, health-gated startup, Flyway validation, and
persistent database/media volumes.

Phase 16 completed: full automated V1 regression coverage and Docker runtime verification passed;
no source or business-behavior fixes were required.

Phase 17 in progress: replacing application typography with locally hosted Vazirmatn web fonts and
adding a lightweight client-side editor for product-image crop, resize, and JPEG compression.

## Next Phase

Phase 18 — Content Module / V1.1 (NOT_STARTED; explicit approval required)

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
- database-driven homepage: active root categories, newest/featured/board-game sections (eight products each), batched primary images
- supplied logo and four reviewed category photos; shared catalog cards and GET search
- opt-in dev/test importer with production-profile exclusion, per-product/media transactions, idempotence, and reports
- Public product catalog with PostgreSQL search/filter/sort and Spring Data pagination
- Public product detail by slug with gallery, breadcrumbs, and friendly 404
- Anonymous HTTP-session cart with product-detail add, quantity controls, subtotal, and navigation badge
- Persistent mobile cart link, keyboard/Escape menu with no-JavaScript fallback, consistent public form controls
- Accessible checkout errors/focus, compact expandable item summary, readable isolated order/payment numbers
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
- Java 21 is required. PostgreSQL and the application run in Compose for Docker verification and local deployment.
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

- Supplied development catalog: 15 products, 17 stored product images, four categories and four group photos.
- All prices are user-authorized development samples, not confirmed retail prices; unknown stock remains zero.
- Three featured flags are documented demo selections. Required generated DEV-P13 SKUs must later be replaced.
- Three Sport Life variants have no individual photo and use the normal placeholder; their shared group photo is used only for the category.
- Twenty broader/conflicting chat records are isolated in dev-data/data-review.md. No board-game products were supplied.
- Import requires dev/test plus app.dev-data.enabled=true; prod/production exclude it. Use only a development DB URL.
- No Flyway migration, dependency, security-rule or production seed changes in Phase 13.

## Last Verification

- Final Java 21 `mvn clean verify` PASSED: 96 tests, 0 failures/errors/skipped (2026-10-01).
- Existing tests extended for public search/navigation routes, parent/child category labels,
  retained catalog query state and checkout error associations. Security and business regressions passed.
- Packaged app started with the development importer disabled; PostgreSQL, Flyway V1–V7 and Hibernate validation passed.
- Real imported catalog: homepage, catalog, pictured/unpictured detail, empty cart, no-results and 404 pages
  checked in Chromium at 360/768/1440px. No horizontal overflow, broken photos or browser script errors.
- Mobile cart stays visible; menu Enter/Escape (including rapid Escape while opening), focus return,
  category-anchor focus, skip link, search/reset/empty query and category links passed. No-JavaScript navigation passed.
- Complete Home → Search → List → Detail → Cart → Checkout → Confirmation flow passed using temporary
  test inventory because all 15 real imported products remain unavailable. No real inventory was changed.
- Cart live price/stock/unavailable/missing states, quantity controls, CSRF and no-JavaScript operations passed.
  Checkout labels/keyboards, field errors/aria-invalid, focused error summary, expandable item details and
  mobile total-before-form order passed. Confirmation numbers/payment, session privacy and 404 passed.
- Admin login/logout, category/product activation/editing, media upload/order/removal, order paging/filter/status,
  full fulfillment flow, cancellation/retry and database stock restoration assertions passed.
- Cart, checkout, confirmation, detail and admin order pages also fit 360/768/1440px. Representative screenshots
  visually reviewed; final captures and report are in dev-data/verification/phase14/.
- Availability-label contrast measured at 8.29:1 (available) and 4.92:1 (unavailable) against their background.
  This is a targeted accessibility review, not a claim of a formal whole-site conformance audit.
- Performance: removed unused demo styles/redundant navigation, kept existing lazy images/batched queries;
  category parents fetched in the active-category query to avoid lazy queries for the new labels.
  New progressive-enhancement JavaScript is small; no new dependency, asset copy, framework or migration.
- The browser pagination check initially read a partially parsed page; awaiting the expected card count fixed
  the verification race. Rapid Escape during Bootstrap opening was an actual UX issue and was corrected.
- Temporary test products/categories/media/orders and credentials removed; verification server stopped.
  Remaining development data: 15 products, 17 media, four categories, zero orders. Before/after fingerprints
  of imported product/category/media rows match; all 17 stored photos still match reviewed source bytes.
- Docker image built and verified with Java 21 inside the runtime image; final process runs as
  non-root `appuser` and writes to the named media volume.
- Compose stack validated with PostgreSQL health-gated app startup; Flyway validated all 7
  migrations, Hibernate validation passed, and `/health`, `/`, and `/products` returned HTTP 200.
- Normal app restart and app-container recreation preserved health, database product count, and
  a media-volume persistence marker; named `postgres-data` and `media-data` volumes were retained.
- `mvn clean verify` passed with 96 tests. Temporary Docker verification credentials/files were
  removed; no production values were added to source, Dockerfile, Compose, or README.
- Phase 16 final Java 21 `mvn clean verify` PASSED: 96 tests, 0 failures/errors/skipped (2026-10-02).
- Docker `compose config` and `compose up --build` passed with healthy PostgreSQL and app containers;
  Flyway validated all 7 migrations and reported the schema up to date.
- Docker `/health`, `/`, `/products`, public detail/404, Persian-normalized search, combined filters,
  and anonymous admin protection passed. Product count remained 15 across restart and app recreation;
  the named media volume survived recreation. The temporary verification stack was stopped without `-v`.
- Existing integration coverage passed for the full customer journey, cart/checkout/order snapshots,
  admin categories/products/media/orders, fulfillment/cancellation/restock concurrency, CSRF, input
  validation, inactive/missing products, upload safety, and development-data isolation.
- No application bugs were found; no source files were changed. Phase 16 DONE. Phase 17 NOT_STARTED.
- `.env` remained ignored and untracked. No automatic commit.

## Last Updated

2026-10-02
