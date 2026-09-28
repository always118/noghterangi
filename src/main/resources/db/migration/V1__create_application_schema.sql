-- Keep store tables separate from PostgreSQL's public schema.
-- Flyway's schema history remains in public; feature tables arrive later.
CREATE SCHEMA noghterangi;
