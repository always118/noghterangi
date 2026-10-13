# Catalog Management Guide

This guide is for the local `dev` or isolated `test` environment only. It describes how to reset the catalog and load the canonical category tree before importing channel products.

## Safety rules

- Never run the reset endpoint against `prod` or `production`.
- Take a database and media backup before deleting anything.
- The reset removes products, product/category media metadata, media files and categories.
- Orders are retained; their historical product snapshots are not deleted.
- The management API is disabled by default and is not registered under a production profile.

## 1. Enable the development API

Add this line to the local, untracked `.env` file:

```env
CATALOG_MANAGEMENT_ENABLED=true
```

Do not enable this setting in a production environment.

## 2. Start the development environment

```bash
docker compose config --quiet
docker compose up -d --build postgres app
docker compose ps
curl -fsS http://localhost:8080/health
```

The target is the development database `noghterangi` on host port `5432`. The test database is not used by these commands.

## 3. Authenticate as administrator

The cookie jar and CSRF token must come from the same session. The following Bash flow logs in with curl. It never prints the password:

```bash
export ADMIN_USER='your-admin-username'
read -r -s ADMIN_PASSWORD
printf '\n'

curl -fsS -c admin.cookies http://localhost:8080/admin/login -o admin-login.html
LOGIN_CSRF=$(grep -oP 'name="_csrf"[^>]*value="\K[^"]+' admin-login.html | head -1)

curl -fsS -L \
  -b admin.cookies -c admin.cookies \
  --data-urlencode "username=$ADMIN_USER" \
  --data-urlencode "password=$ADMIN_PASSWORD" \
  --data-urlencode "_csrf=$LOGIN_CSRF" \
  http://localhost:8080/admin/login \
  -o /dev/null

curl -fsS -b admin.cookies -c admin.cookies \
  http://localhost:8080/admin/categories/new -o admin-category.html
export CSRF_TOKEN=$(grep -oP 'name="_csrf"[^>]*value="\K[^"]+' admin-category.html | head -1)
test -n "$CSRF_TOKEN" || { echo 'CSRF token was not found'; exit 1; }
```

If you use the browser instead, use **Copy as cURL** for an authenticated request. Do not combine a cookie jar from one login with a CSRF token copied from another session.

After login, the simpler browser workflow is to open `http://localhost:8080/admin/catalog`. The page contains forms for importing categories, importing the channel manifest and resetting the catalog. Thymeleaf supplies the CSRF token automatically.

## 4. Delete products

This removes products and product media only. Categories, category media and orders remain. Confirm that the target is `dev` before running it:

```bash
curl -i -b admin.cookies \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -X POST \
  'http://localhost:8080/admin/catalog/products/clear?confirmation=DELETE_ALL_CATALOG'
```

The response reports `deletedProducts` and `deletedMedia`. A wrong or missing confirmation token is rejected.

The same CSRF token can also be sent as the `_csrf` query parameter:

```bash
curl -i -b admin.cookies \
  -X POST \
  "http://localhost:8080/admin/catalog/products/clear?confirmation=DELETE_ALL_CATALOG&_csrf=$CSRF_TOKEN"
```

## 5. Delete categories

Products must be deleted first. This removes categories and category media only:

```bash
curl -i -b admin.cookies \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -X POST \
  'http://localhost:8080/admin/catalog/categories/clear?confirmation=DELETE_ALL_CATALOG'
```

If any product remains, the operation is rejected without deleting categories.

## 6. Import the canonical categories

Use the reviewed eight-root/49-child catalog:

```bash
curl -i -b admin.cookies \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -F 'catalog=@dev-data/category-catalog.json;type=application/json' \
  http://localhost:8080/admin/catalog/categories/import
```

The response reports created and already-existing categories. Existing categories are not renamed. A child with a different existing parent is rejected.

## 7. Verify the category database state

If `psql` is available inside the PostgreSQL container:

```bash
docker compose exec -T postgres psql -U noghterangi -d noghterangi \
  -c 'SELECT COUNT(*) AS categories FROM noghterangi.category;' \
  -c 'SELECT COUNT(*) AS roots FROM noghterangi.category WHERE parent_id IS NULL;' \
  -c 'SELECT COUNT(*) AS children FROM noghterangi.category WHERE parent_id IS NOT NULL;' \
  -c 'SELECT COUNT(*) AS products FROM noghterangi.product;'
```

Expected category counts after a clean import are 57 total, 8 roots and 49 children. Product count should be zero until the channel manifest is imported.

## 8. Import channel products

Only after the category verification succeeds, follow the channel import section in [`DEVELOPMENT_RUNBOOK.md`](DEVELOPMENT_RUNBOOK.md). The channel importer resolves each `categorySlug` against the target database and does not create categories from the product manifest.

## Test environment

For tests, use the separate `postgres-test` service and `TEST_DB_*` settings. Never point test settings at the development database. The reset operation is not part of the normal test suite and must not be used to prepare shared test fixtures.

## Implementation references

- API: `src/main/java/com/noghterangi/catalog/AdminCatalogManagementController.java`
- Service: `src/main/java/com/noghterangi/catalog/CatalogManagementService.java`
- Canonical catalog: `dev-data/category-catalog.json`
- Environment boundaries: `ENVIRONMENT_GUIDE.md`
