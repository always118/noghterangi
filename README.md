# Noghte Rangi — نقطه رنگی

A Persian local online shop built incrementally as a simple Spring Boot modular
monolith. Current implementation: **Phase 7 — Product Images**, with protected category, product, and image administration.

## Prerequisites

- JDK 21 (`JAVA_HOME` must point to JDK 21)
- Maven 3.6.3 or later
- Docker Engine/Desktop with Compose v2 and a running Linux engine
- For WSL: enable Docker Desktop integration for this distribution

Check `java -version`, `mvn -version`, and `docker compose version`.
If Maven still reports Java 11 on WSL, set the installed JDK for your shell:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
```
Spring Boot 4.1.1 manages dependency versions. The application uses MVC,
Thymeleaf, Spring Security, Validation, JPA, the PostgreSQL JDBC driver, and Flyway (including
its PostgreSQL module). There is no frontend build pipeline.

## Local configuration

For a new checkout, copy `.env.example` to `.env` and replace both placeholder
passwords with different local passwords. Keep the populated file private;
`.gitignore` excludes it. If `.env` already exists, preserve it.
Set the required admin username and BCrypt hash as described below before startup.

Compose reads `.env` automatically. Spring Boot does **not**, so export the
settings before running Maven or the application. In Bash, for a trusted local
`.env` using shell-compatible values:

```bash
set -a
source .env
set +a
```

Quote values containing shell metacharacters. Never source an untrusted file.
The example contains no production credentials, and database passwords have
no fallback in application configuration.

| Variable | Purpose / local default |
| --- | --- |
| `DB_URL` | JDBC URL; `jdbc:postgresql://localhost:5432/noghterangi` |
| `DB_USERNAME` | Application database user; `noghterangi` |
| `DB_PASSWORD` | Required application database password |
| `DB_PORT` | Compose host port; `5432` |
| `TEST_DB_URL` | Test JDBC URL; `jdbc:postgresql://localhost:5433/noghterangi_test` |
| `TEST_DB_USERNAME` | Test database user; `noghterangi_test` |
| `TEST_DB_PASSWORD` | Required, separate test database password |
| `TEST_DB_PORT` | Test container host port; `5433` |
| `ADMIN_USERNAME` | Required single administrator username; no default |
| `ADMIN_PASSWORD_HASH` | Required BCrypt hash (cost 10 or higher); no plaintext password |
| `SESSION_COOKIE_SECURE` | Set `true` with HTTPS; default `false` supports local HTTP |
| `MEDIA_STORAGE_PATH` | Local image directory; default `uploads/media` relative to working directory |
| `SERVER_PORT` | Application HTTP port; Spring Boot defaults to `8080` |

If you change a Compose host port, update the corresponding JDBC URL too.
Compose uses fixed local database/user names as listed above. When connecting
to an externally provisioned database, supply its JDBC URL and credentials.

## Start PostgreSQL

```bash
docker compose config --quiet
docker compose up -d --wait postgres
docker compose ps
```

PostgreSQL 16.15 runs in Docker, with its port bound to `127.0.0.1`. Development
data lives in the `postgres-data` named volume and survives container recreation.
The container health check uses `pg_isready`; application tests additionally
verify an authenticated JDBC connection and actual queries.

This Compose setup is for local development. The image's `POSTGRES_USER` is a
bootstrap superuser; production must use separately provisioned, appropriately
restricted database roles. Changing `.env` does not change passwords inside an
already initialized database. Change existing database credentials deliberately;
do not delete the data volume to troubleshoot authentication.

## Build and test

Start the optional isolated PostgreSQL test service, then export `.env` as above:

```bash
docker compose --profile test up -d --wait postgres postgres-test
mvn clean verify
```

Tests activate the `test` Spring profile and use only `TEST_DB_*` connection
settings. Never point those settings at a production or shared store database.
Test data is held in a temporary container filesystem and resets when the test
container stops. The development database uses a separate service and volume.
No H2 or additional database-testing libraries are used.

Four integration checks cover:

- `/health` returning HTTP 200 and plain text `OK`.
- The Persian RTL homepage rendered by Thymeleaf as UTF-8 HTML.
- A real PostgreSQL JDBC connection and a JPA query against the migrated schema.
- Flyway validation, the applied foundation migration, and a repeat migration
  run making no changes.

The test server uses a random HTTP port. A database connection or migration
failure must fail the build; tests are not silently skipped without PostgreSQL.

## Run the application

With the development database healthy and `.env` exported:

```bash
mvn spring-boot:run
```

Or, after a successful build:

```bash
java -jar target/noghte-rangi-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080/ and http://localhost:8080/health.
Stop the application with Ctrl+C. Set `SERVER_PORT` to change its HTTP port.
If your shell defines `DEBUG`, Spring Boot may enable verbose logging; unset it
for normal local logging. Public errors omit messages, binding errors, and traces.

## Database migrations

Flyway runs `src/main/resources/db/migration` automatically before JPA starts.
`V1__create_application_schema.sql` creates the empty `noghterangi` schema.
Flyway records applied versions and checksums in `public.flyway_schema_history`.
Hibernate's default schema is `noghterangi`, `ddl-auto=validate`, and Open Session
in View is disabled. SQL script initialization is disabled; Flyway owns DDL.

`V2__create_category.sql` adds `noghterangi.category`, including a unique slug
constraint, a lowercase slug format check, and creation/update timestamps.
`V3__add_category_parent.sql` adds the optional category parent.
`V4__create_product.sql` adds products, integer Toman prices, slug/SKU uniqueness,
category foreign key, and numeric/game-field checks. Hibernate validates both
Category, Product, and Media mappings against PostgreSQL. Do not modify an
already applied migration; add a new version. Flyway clean is disabled.
`V5__create_media.sql` adds ordered product image metadata, a unique generated
filename, and a restrictive product foreign key. Product hard deletion remains unavailable.

## Manual verification

After starting the app:

```bash
curl -i http://localhost:8080/health
curl -i http://localhost:8080/
docker compose exec -T postgres psql -U noghterangi -d noghterangi -c 'SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;'
docker compose exec -T postgres psql -U noghterangi -d noghterangi -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'noghterangi';"
```

Expect HTTP 200 for both routes, migration versions `1` through `5` with `success = t`, and
schema `noghterangi`. Restart the app: Flyway should report that it is up to date.
The `/health` route remains a simple web liveness check, not a live database probe.

To stop the local database services while preserving development data:

```bash
docker compose --profile test down
```

Do not add `--volumes` unless you intend to erase development database data.
There is no application container yet; that belongs to Phase 15.

## Scope and next phase

Java code lives under `com.noghterangi`, with categories in the `category` feature
package. Phase 5 Categories was completed early; Phase 4 now secures all admin routes.
Phase 6 adds the Product domain and protected administration; Phase 7 adds local product images.
Public catalog, cart, orders, customer accounts, and content are not implemented.
Phase 8 Product Catalog requires explicit approval; no Git commit is made automatically.

## UI foundation (Phase 3)

`templates/fragments/layout.html` is the shared Persian (`lang="fa"`, `dir="rtl"`)
page shell. New templates can reuse it with:

```html
<html lang="fa" dir="rtl" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: page('عنوان صفحه', ~{::content})}">
<body><section th:fragment="content">محتوای صفحه</section></body>
</html>
```

The shell includes head, header/navigation, optional `successMessage` and
`errorMessage` alerts, and footer fragments. Messages use escaped text.
Design variables and decorative CSS illustrations live in `static/css/main.css`.
Bootstrap 5.3.8 RTL CSS and JavaScript are served locally under `static/vendor/bootstrap`
with the upstream license; no CDN connection is needed at runtime.
The homepage uses real active categories; product illustrations remain demos
and search is disabled. Category navigation links target homepage category cards
until the catalog phase adds product browsing.

For browser verification, open `/` at 360px, 768px, and 1440px widths. Check
Persian text, RTL order, card wrapping, and absence of horizontal scrolling.
On mobile, open and close the navigation with mouse and keyboard. Tab from the
address bar to test the skip link and visible focus. Check `/health` returns `OK`.

## Categories (Phase 5 — implemented early)

Open `/admin/categories` to list categories, `/admin/categories/new` to create one,
and `/admin/categories/{id}/edit` to edit it. Saving and activation changes use
POST followed by a redirect with a Persian confirmation message. No delete action
is provided. Sign in at `/admin/login` first; all category management requires an
authenticated administrator. Existing POST forms automatically include CSRF tokens.

Names are required (maximum 120 characters). Slugs are required, trimmed, and
converted to lowercase; use English letters, numbers, and single hyphens between
words, up to 120 characters. Slugs remain unique across active and inactive
categories. Descriptions are optional, up to 2000 characters. The service checks
uniqueness, and the database constraint also prevents concurrent duplicates.
Creation/update timestamps are stored as instants; creation time is preserved on edit.

Manual checks:

1. Create an active category; confirm its name/description appears on `/` and its
   link appears under the public category navigation menu.
2. Submit a blank name, invalid slug, and existing slug; confirm field-level
   Persian errors and preserved form inputs.
3. Edit the category, retaining its slug; confirm the changes persist.
4. Deactivate it; confirm it remains in the admin list but disappears from both
   public cards and navigation. Activate it again and confirm it returns.
5. Check the admin list/form and homepage at mobile and desktop widths.
6. Confirm `/health` returns HTTP 200 and all Flyway migrations have succeeded.

Category integration tests use the isolated test database and remove only their
own uniquely prefixed records. They exercise service validation, PostgreSQL
constraints, timestamps, public visibility, HTML escaping, and real HTTP admin flows.

### Category parent compatibility correction

A category may have no parent (a root) or one root parent. The admin form offers
root categories; self-parenting, selecting a child as parent, and moving a category
that already has children under another root are rejected. This keeps two practical
levels without recursive tree management. Removing a parent returns the category
to the root level. Each category's active flag remains independent; public display
continues to filter by that category's own active flag.

## Admin security (Phase 4)

One administrator is configured through `ADMIN_USERNAME` and `ADMIN_PASSWORD_HASH`.
Spring Security keeps that user in memory; no user table or new migration is needed.
There is no default account, registration, remember-me, or password reset. Missing,
malformed, plaintext, or low-cost hashes cause startup to fail with a generic
configuration message that does not include credential values.

Generate a BCrypt hash locally at cost 12 (minimum accepted: 10). For example,
if Apache `htpasswd` is installed, it prompts without echoing the password and
can write the result to a private file instead of the terminal:

```bash
umask 077
htpasswd -nBC 12 'your-admin-username' > /tmp/noghterangi-admin-credential
```

Set `ADMIN_USERNAME` in your private `.env`. Copy only the BCrypt hash after the
colon in that private file into `ADMIN_PASSWORD_HASH`, enclosed in single quotes
so Bash preserves the dollar signs. Delete the temporary file after configuration.
Do not commit `.env`, print hashes, or place real credentials in this README.
Export `.env` using the existing instructions, then start the application.
For HTTPS deployments, set `SESSION_COOKIE_SECURE=true`.
Changing credentials requires an application restart, which also clears sessions.

`GET /admin` and every `/admin/**` route except `/admin/login` require the ADMIN
role. Successful form login always redirects to `/admin`; invalid credentials
show a generic Persian message. Public homepage, category cards, health, and
static resources remain anonymous. Spring Security manages session fixation
protection. Session cookies are HttpOnly and SameSite=Lax; URL session IDs are disabled.

Logout is a CSRF-protected POST to `/admin/logout`. The admin navigation includes
its form on the landing page and category pages. Logout clears authentication,
invalidates the session, deletes the session cookie, and redirects to the login page.
Thymeleaf `th:action` POST forms automatically include the CSRF token; keep using
that pattern for future admin forms. CSRF is enabled globally.

Verification:

1. Without cookies, `/` and `/health` return 200; `/admin` and `/admin/categories`
   redirect to `/admin/login`. The Persian RTL login page and assets return 200.
2. Invalid credentials show the generic error. Valid credentials redirect to `/admin`.
3. While signed in, list/create/edit/activate/deactivate categories; check public
   active-category visibility separately in an anonymous browser session.
4. Submit an admin POST without its CSRF token: expect 403 and no mutation.
5. Use the logout button, then revisit `/admin` and `/admin/categories`: both
   must require login again. GET alone must not log out.

Tests generate isolated, temporary administrator credentials at runtime and use
real HTTP sessions and form CSRF tokens. They do not read production admin secrets
or disable authentication/CSRF. No real admin credentials are stored in test files.

## Products (Phase 6)

After signing in, open `/admin/products` for all products in display-order/ID order,
`/admin/products/new` to create one, and `/admin/products/{id}/edit` to edit it.
The same form manages name, slug, SKU, brand, descriptions, category, product type,
price, stock, active/featured flags, and display order. Activation uses POST;
there is no hard-delete action. Product types and categories are separate concepts.

Prices are whole Toman stored as Java `Long` / PostgreSQL `BIGINT`, not fractional
or floating-point amounts. Price, stock, and display order must be nonnegative.
Slugs are trimmed and lowercase; SKUs are trimmed and uppercase. Both are unique
across active and inactive products. Name/slug allow 160 characters, SKU 64,
brand 120, short description 500, and description 10000.

New category assignments require an active category. If a product's existing
category is later deactivated, editing may retain that category; it is explicitly
labeled inactive in the selector. A missing category is rejected with a form error.
There is no automatic activation/deactivation inheritance.

Types: STATIONERY, EDUCATIONAL, BOARD_GAME, BOOK, OTHER. For BOARD_GAME, age,
player-count ranges, and playing time are nullable. Supplied ages must be
nonnegative, player counts/time positive, and maxima at least their minima.
Changing away from BOARD_GAME clears those five fields. They remain visible in
the form with explanatory text; no dynamic attributes or custom JavaScript is used.

Manual checks:

1. Anonymous `/admin/products` must redirect to login; sign in and open the list.
2. Create a product with an active category and whole Toman price; verify its
   name, SKU, category, type, price, stock, and state in the list.
3. Edit price, stock, featured state, and order; verify persistence after reload.
4. Submit duplicate slug and SKU, negative price/stock, or invalid board-game
   ranges; expect clear Persian errors and no saved changes.
5. Deactivate/reactivate a product. Check existing category create/edit/activation,
   optional parent selection, public active-category display, `/`, and `/health`.
6. Check product list/form at 360px, 768px, and 1440px. Image management is described below; the public catalog remains deferred.

Product integration tests use real PostgreSQL and authenticated HTTP requests with
CSRF tokens, and clean up only their uniquely prefixed products/categories.


## Product images (Phase 7)

Save the product first, then choose **تصاویر** in the product list or **مدیریت تصاویر محصول**
on its edit page. `/admin/products/{id}/images` supports multiple upload, numeric
ordering, and removal. Save pending product edits before following that link.
The first image ordered by `sortOrder`, then ID, is primary. Products without
images show a text placeholder. No public catalog or product deletion is added.

`Media` holds the product relationship, original filename metadata, generated
filename, MIME type, byte size, order, and creation time. `MediaService` manages
records; the small `MediaStorageService` interface isolates physical storage in
`LocalMediaStorageService`. Product business services do not handle files.

Configure `MEDIA_STORAGE_PATH` in your private `.env` and export it before startup.
The default `uploads/media` is outside the classpath and ignored by Git. Use a
persistent directory owned by the application account; do not make it writable
by other users or serve it directly from a web server. Back up that directory and
the database together. Keep the same working directory when using the relative default.

Uploads accept JPEG (`.jpg`/`.jpeg`, `image/jpeg`) and PNG (`.png`, `image/png`),
up to 5 MiB each, 10 files per batch, 10 million pixels, and 8192 pixels per side.
The request limit is 55 MiB. The JDK decoder validates actual content and its match
to MIME/extension before writing; no image processing dependencies are added.
WebP and SVG are unsupported. Images are not resized, re-encoded, or stripped of
metadata. Original basenames are escaped display metadata only. Generated UUID
filenames, strict filename validation, and no-follow reads prevent path traversal.

`GET /media/{id}` anonymously serves the matching file with its image content type;
missing records or files return 404. Image URLs remain public even when their
product is inactive. Admin upload/order/delete require the existing admin session
and CSRF protection. The multipart form places the CSRF token in its action query
to support multipart CSRF verification without an upload filter before security, following the
[Spring Security multipart guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-considerations-multipart).
Avoid logging query strings for these upload routes. Other forms keep hidden CSRF fields.

A failed upload batch rolls back its database records and cleans up new files.
Removal commits the record deletion before deleting that generated file; missing
files are harmless. Filesystem and database operations are not a distributed
transaction: process crashes or filesystem cleanup failures can leave orphan
files. Cleanup failures log the generated filename for manual reconciliation;
there is no background cleanup job. Never remove files referenced by other records.

Manual checks:

1. Sign in, save a product, open its images page, and upload one JPEG and one PNG.
2. Confirm both images display, then change order and check the product list's primary image.
3. Submit SVG/disguised content and a file larger than 5 MiB; expect Persian errors and no saved upload.
4. Remove one image and confirm the other remains. Remove the last image and check the placeholder.
5. Check anonymous image retrieval, blocked anonymous admin mutations, and CSRF rejection.
6. Check product/category edits and activation, `/`, `/health`, and RTL at 360/768/1440px.

Tests use a temporary image directory, real PostgreSQL, and real HTTP multipart
requests. They cover content/size/path checks, image ownership, ordering, retrieval,
removal, rollback cleanup, authentication, and CSRF alongside existing regressions.
