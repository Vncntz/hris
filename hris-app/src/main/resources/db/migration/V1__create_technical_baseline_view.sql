-- Technical marker for the first global migration; contains no business data.
CREATE VIEW hris_migration_baseline AS SELECT 1 AS baseline_version;
