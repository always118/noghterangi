# Noghte Rangi — Project Status

## Current Phase

Phase 13 — Real Homepage

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
- Phase 13 — Real Homepage

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Phase 13 completed: database-driven homepage with approved branding and reviewed supplied development products.
Original logo/photos are preserved. User explicitly authorized sample prices for the visual preview.

## Next Phase

Phase 14 — UX Polish (NOT_STARTED; explicit approval required)

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

- Supplied development catalog: 15 products, 17 stored product images, four categories and four group photos.
- All prices are user-authorized development samples, not confirmed retail prices; unknown stock remains zero.
- Three featured flags are documented demo selections. Required generated DEV-P13 SKUs must later be replaced.
- Three Sport Life variants have no individual photo and use the normal placeholder; their shared group photo is used only for the category.
- Twenty broader/conflicting chat records are isolated in dev-data/data-review.md. No board-game products were supplied.
- Import requires dev/test plus app.dev-data.enabled=true; prod/production exclude it. Use only a development DB URL.
- No Flyway migration, dependency, security-rule or production seed changes in Phase 13.

## Last Verification

- Final Java 21 `mvn clean verify` PASSED: 96 tests, 0 failures/errors/skipped (2026-09-30).
- New homepage tests verify anonymous access, active root categories, bounded/stable newest ordering,
  active product/category visibility, optional featured/game sections, links/search and shared image/placeholder cards.
- Import tests verify opt-in/profile guards (including mixed dev + production), repeat/no-overwrite behavior,
  category conflicts, duplicate manifest keys, bad prices, missing/corrupt/traversal images and per-product rollback.
- Packaged app started twice in dev mode against PostgreSQL; Flyway V1–V7 and Hibernate validation passed.
- First run imported 15 products, 17 media and four categories. Second run created zero rows/images and retained all 15 products.
  Reports: dev-data/import-report-first-run.md and dev-data/import-report.md; source decisions: dev-data/data-review.md.
- Real-data Chromium checks passed: /, /health, /products, all 15 product details, all 17 media endpoints,
  four pictured categories, eight newest cards, three featured cards, hidden empty game section, search,
  category/product links, zero cart badge and unavailable products. Import report URL returned 404.
- Homepage at 360/768/1440px: Persian RTL, no horizontal overflow, loaded photos and touch-size search controls.
  Supplied-photo contact sheet and responsive screenshots reviewed. Unsupported hero emoji glyphs removed;
  final screenshots refreshed in dev-data/verification/ after the final successful build and browser checks.
- Browser regressions passed: admin login/logout, category/product CRUD and activation, image upload/order/removal,
  catalog search/filter/sort/pagination, product detail/gallery, cart lifecycle and no-JavaScript cart,
  checkout validation/failures/snapshots/session privacy, and admin order filtering/paging/status/cancellation.
- Temporary board-game fixtures verified the nonempty homepage game section. Full order status progression,
  cancellation/retry and database inventory assertions passed; all relevant views fit 360/768/1440px.
- Temporary 23 orders/items, 20 test products (one deleted during failure verification), four categories,
  test media and verification credentials removed; server stopped. The supplied development catalog remains:
  15 products, 17 media, four categories, zero stock and zero orders.
- Phase 13 DONE. Phase 14 NOT_STARTED. No automatic commit.

## Last Updated

2026-09-30