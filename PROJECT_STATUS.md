# Noghte Rangi — Project Status

## Current Phase

Phase 4 — Admin Security

Status: DONE

## Completed Phases

- Phase 0 — Repository Inspection
- Phase 1 — Spring Boot Foundation
- Phase 2 — PostgreSQL and Flyway
- Phase 3 — Base UI, RTL and Design System
- Phase 4 — Admin Security
- Phase 5 — Categories

Note:
Phase 5 Categories was implemented before Phase 4 because the project
phase order was updated after category development.
Admin Security now protects the existing category routes and future admin routes.

## Current Work

Admin Security complete: Persian form login, BCrypt environment credentials,
HTTP sessions, protected /admin/**, CSRF-protected category forms, and logout.
Awaiting explicit approval for Phase 6.

## Next Phase

Phase 6 — Products (NOT_STARTED; explicit approval required)

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
- Category domain and admin management
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
- Prices will use Long and Toman
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

- Build: `mvn clean verify` PASSED using Java 21.
- Tests: 20 passed, 0 failures/errors/skipped; security and category regressions.
- Startup: packaged application passed; missing credentials correctly refuse startup.
- PostgreSQL/Flyway/Hibernate: passed; V1/V2 unchanged; no security migration/table.
- Anonymous /, /health, login, and static resources: HTTP 200.
- Anonymous /admin and category admin pages: redirect to login.
- Valid/invalid login, session rotation, authenticated category CRUD, and CSRF: passed.
- Logout clears access; replay of the invalidated session remains blocked.
- Public active-category display and RTL: passed; Chromium 360/768/1440px checked.

## Last Updated

2026-09-29