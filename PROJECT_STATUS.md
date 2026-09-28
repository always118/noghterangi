# Noghte Rangi — Project Status

## Current Phase

Phase 3 — Base UI and RTL

Status: DONE

## Completed Phases

- Phase 0 — Repository Inspection
- Phase 1 — Spring Boot Foundation
- Phase 2 — PostgreSQL and Flyway
- Phase 3 — Base UI and RTL

## Current Work

Phase 3 complete: reusable Persian RTL fragments, local Bootstrap 5.3.8, shared CSS,
and responsive demo homepage. Awaiting approval for Phase 4.

## Next Phase

Phase 4 — Categories (requires explicit approval)

## Important Technical Decisions

- Single Spring Boot application
- Server-side rendered UI with Thymeleaf
- Local Bootstrap 5.3.8 RTL; one custom stylesheet; no frontend build pipeline
- PostgreSQL as the database
- Flyway manages database schema changes
- Modular monolith
- Customer accounts are not required in V1
- Admin will use session-based authentication
- Cart will be simple and server-side/session-based
- Payment in V1 is manual card-to-card

## Known Issues / Notes

- Java 21 and Maven are installed in WSL; set JAVA_HOME to Java 21 for Maven.
- Maven commands require Java 21.
- No business entities exist yet.
- Application itself is not containerized yet.
- Docker Compose currently runs PostgreSQL.

## Last Verification

- Build: `mvn clean verify` PASSED with WSL Maven 3.6.3 and Java 21.0.7.
- Tests: 4 passed, 0 failures/errors/skipped; homepage and local assets included.
- Application startup: packaged JAR started successfully on port 8081.
- Database: PostgreSQL healthy; JPA/JDBC and Flyway validated; only V1 applied;
  zero tables in the noghterangi schema; no business entities or migrations added.
- Main endpoints: GET / and /health HTTP 200; health body OK.
- UI: Chromium checks at 360/768/1440px passed without horizontal overflow;
  Persian/RTL, Bootstrap, fragments, mobile menu, and keyboard skip link verified.
- Visual review: mobile and desktop screenshots inspected; other browsers untested.

## Last Updated

2026-09-29
