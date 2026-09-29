-- Isolated checksum-conflict fixture. Never change the production V1 migration.
CREATE VIEW hris_migration_baseline AS SELECT 2 AS baseline_version;
