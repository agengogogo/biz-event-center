-- Baseline migration for the local (SQLite) profile.
-- Schema tables are introduced by later migrations; this marker ensures Flyway
-- has a managed history from the first start.
SELECT 1;
