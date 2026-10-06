# Noghte Rangi — Phase Status

| Phase | Name | Status | Verification |
|------:|------|--------|--------------|
| 0 | Repository Inspection | DONE | Empty repository inspected |
| 1 | Spring Boot Foundation | DONE | Build + tests + / + /health passed |
| 2 | PostgreSQL and Flyway | DONE | DB + JPA + Flyway + tests passed |
| 3 | Base UI, RTL and Design System | DONE | RTL + Bootstrap + fragments; Chromium 360/768/1440px; mvn clean verify (4 tests); / and /health HTTP 200 |
| 4 | Admin Security | DONE | Spring Security + BCrypt + sessions + CSRF; 20 tests; browser login/logout + category regression passed |
| 5 | Categories | DONE | Optional parent corrected in V3; two-level validation + admin forms; 22 tests passed |
| 6 | Products | DONE | V4 + Product admin + validation; 32 tests; browser CRUD/security + RTL 360/768/1440px; / and /health HTTP 200 |
| 7 | Product Images | DONE | V5 + local JPEG/PNG storage + protected image management; 43 tests; packaged app/browser upload/order/removal/security + RTL 360/768/1440px; / and /health HTTP 200 |
| 8 | Product Catalog | DONE | V6 normalization + PostgreSQL filters/sorts/pagination; 53 tests; Chromium catalog/admin regressions + RTL 360/768/1440px; /, /products, /health HTTP 200 |
| 9 | Product Detail | DONE | Public slug detail + gallery + safe descriptions/specifications; 60 tests; Chromium keyboard/quantity/404 + RTL 360/768/1440px and admin/catalog regressions passed |
| 10 | Shopping Cart | DONE | Session cart + current prices/availability + CSRF; Java 21 mvn clean verify (67 tests); Chromium cart/no-JS/admin/catalog/detail/image regressions; RTL 360/768/1440px passed |
| 11 | Checkout and Orders | DONE | V7 orders/snapshots + atomic stock locks + configured payment; Java 21 mvn clean verify (77 tests); rollback/concurrency/CSRF; Chromium checkout/failures/DB/regressions and RTL 360/768/1440px passed |
| 12 | Admin Orders | DONE | Protected paginated list/detail + explicit transitions + atomic once-only restock; Java 21 mvn clean verify (87 tests); concurrent/rollback/CSRF tests; Chromium status/cancel/DB/regressions + RTL 360/768/1440px passed |
| 13 | Real Homepage | DONE | Database-driven sections, original branding, 15 supplied products/17 media/4 categories with authorized sample prices; safe opt-in dev import/repeat; Java 21 mvn clean verify (96 tests); Chromium real-data homepage and full regressions at 360/768/1440px passed |
| 14 | UX Polish | DONE | Persistent mobile cart, keyboard/no-JS navigation, accessible forms, RTL cards and compact checkout; Java 21 mvn clean verify (96 tests); real-data and full journey/admin regressions at 360/768/1440px passed; imported data/photos unchanged |
| 15 | Docker | DONE | Java 21 multi-stage image; Compose app/PostgreSQL stack; health, Flyway V1–V7, endpoint, restart, recreate, and named-volume persistence checks passed |
| 16 | Full V1 Testing | DONE | Java 21 Maven suite: 96 tests passed; Docker stack, public routes, search/detail, restart/recreate, Flyway, persistence, security, and existing customer/admin/order regression coverage passed |
| 17 | V1 Release Preparation | IN_PROGRESS | Vazirmatn typography and product-image crop/compression in progress |
| 18 | Content Module / V1.1 | NOT_STARTED | — |
