# Reviewed development catalog

This directory is **not packaged as a static resource or migration**. Nothing imports by default.
Use a local development database, Java 21, existing DB/admin environment configuration, and:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--app.dev-data.enabled=true --app.dev-data.directory=dev-data"
```

For the packaged application:

```sh
java -jar target/noghte-rangi-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --app.dev-data.enabled=true
```

The importer requires `dev` or `test` **and** explicit `app.dev-data.enabled=true`.
It is excluded when either `prod` or `production` is active, even alongside `dev`/`test`.
Use the development database only; a Spring profile cannot determine whether a supplied database URL is production.
`products.json` is the small reviewed manifest, not a general chat-file importer.
`images.json` preserves the original photo index for provenance; only paths accepted in `products.json` are imported.
Each product and its media are one transaction through ProductService and MediaService. A missing/rejected referenced
image skips that product; a deliberately empty image list uses the normal placeholder. Bad records do not stop later ones.
Existing slug, SKU or exact case-insensitive product name is skipped; existing records and images are never overwritten.
Duplicate keys inside a manifest reject all affected rows. An existing category with a conflicting name is not reused.
`import-report.md` records the latest run, including imported, skipped, duplicate, missing and rejected counts.
Read `data-review.md` for the source review, authorized sample prices and unresolved records.

Before real sales: confirm prices, inventory and product facts in admin; replace `DEV-P13-*` identifiers with business SKUs.
Re-running import never applies price edits to existing data; use the existing admin editor for deliberate changes.
