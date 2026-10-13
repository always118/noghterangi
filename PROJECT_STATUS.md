# Noghte Rangi — Project Status

## Current Phase

Phase 20 — Environment & Canonical Catalog

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
- Phase 15 — Docker
- Phase 16 — Full V1 Testing
- Phase 17 — V1 Release Preparation
- Phase 18 — Homepage Category UX

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 14 completed: public mobile navigation, search feedback, product/card hierarchy, RTL,
accessible controls and checkout/confirmation presentation refined.

Phases 15–18 completed: Docker deployment, full V1 regression preparation, media editor/category media,
and the final 8-root/49-child category catalog were implemented and verified as recorded below.

## Next Phase

Phase 20 — Environment & Canonical Catalog (IN_PROGRESS)

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
- protected channel product importer with production-profile exclusion, database-owned category resolution,
  per-product/media transactions and idempotence
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

- Channel export contains 90 product candidates and 199 downloaded media files; prices and stock remain subject
  to review before sales. Category slugs resolve against the target database and are never created from JSON.

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
  visually reviewed during the completed UX phase; old screenshot artifacts are not runtime inputs.
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
- Phase 17 completed: shared local Cropper.js image editor, product/category crop-compression flow, category media management, homepage category images, and Flyway V8 are implemented. Java 21 `mvn clean verify` passed with 98 tests; Docker rebuild and in-container `/health` check passed. No automatic commit.
- Phase 18 completed: no Flyway data-seed migration was added. The explicit dev-only catalog importer used
  CategoryService, ProductService and MediaService; database verification found 8 active roots, 57 unique
  categories, 15 products with no orphaned category relations, eight assigned images, and zero absolute media paths.
  Java 21 `mvn clean verify` passed with 98 tests. No automatic commit.

- Phase 19 implemented: Telegram channel export review identified 90 product-caption posts and 199 downloaded
  media files. A generated, reviewable product manifest and protected import endpoint were implemented;
  existing products remain untouched unless the explicit delete-existing option is supplied.

- Phase 19 verification: Java 21 compile, Docker image build and in-container `/health` passed. Full Maven
  verification was attempted but the test database rejected the configured `noghterangi_test` password;
  no import was executed against the database.

- Phase 20 in progress: development manifests were moved from temporary four-category data to the canonical
  eight-root category catalog. Environment boundaries, project structure and execution rules are documented;
  no production database or media write has been performed.

- Phase 20 verification so far: the channel manifest is generated from the canonical catalog with 8 roots and
  49 children, and Java 21 compilation passes. Production
  synchronization remains pending explicit production access, backup and approval.

- Category import correction: both development and channel product importers resolve category slugs from the
  target database and never create or rename categories from uploaded JSON. Test sources compile successfully;
  runtime suite still requires matching `TEST_DB_PASSWORD` and the isolated test database.

- Phase 20 documentation cleanup: obsolete development-product importer, its two tests, old fixtures, reports,
  screenshots and provenance files were removed. The remaining development inputs are the canonical category
  catalog and reviewed channel manifest. Java 21 test compilation passed after the cleanup.

- Phase 20 catalog administration: added a dev/test-only protected API to import the canonical category catalog
  and separate explicit-confirmation APIs to remove products or categories and their media without deleting order history.
  Added the server-rendered `/admin/catalog` page for category import, channel product import and separate resets.
  Java 21 test compilation passed; no destructive catalog operation was executed.

## Last Updated

2026-10-13
