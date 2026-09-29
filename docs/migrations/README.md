# Global database migrations

The deployable `hris-app` owns the one Flyway stream at
`hris-app/src/main/resources/db/migration`. Every module contributes SQL there
through its implementation PR. Do not create module-local Flyway locations or
schema-history tables. Spring Boot migrates the configured MySQL `DataSource`
on startup and records applications in `flyway_schema_history`.

## Allocate a version

Use `V<N>__<short_snake_case_description>.sql`, with a positive, consecutive
integer `N` shared across the entire application. `V1__create_technical_baseline_view.sql`
is the first migration. Before adding one, refresh `main`, inspect this
directory, and take the next integer after the highest version on `main`.
Keep the migration in the PR for the owning IMP/TASK and describe its purpose.
If concurrent work takes the same version, the later PR must refresh from
`main`, rename its unreleased migration to the next free version, and rerun
the MySQL migration tests before merge. Flyway's duplicate-version detection
is a failure, not an allocation mechanism.

After a migration is released on `main`, its filename and contents are
immutable. Correct defects with a later forward migration; do not rewrite or
repair released history to conceal a checksum conflict. Application startup
validates applied history, and a mismatch must be investigated before any
upgrade proceeds.

`V1` creates only a constant technical view. It proves application, history,
and repeat validation without introducing business schema or data.

## Verify locally

- Ordinary reactor and database-independent application smoke test: on Windows,
  `.\mvnw.cmd -B clean verify`; on Unix, `./mvnw -B clean verify`.
- Disposable MySQL 8.4.11 migration and invalid-history tests: on Windows,
  `.\mvnw.cmd -B -Pmysql-it clean verify`; on Unix,
  `./mvnw -B -Pmysql-it clean verify`. This requires a working Docker-compatible
  container runtime. No persistent local database is required.

This foundation does not implement production upgrade sequencing, maintenance
mode, backup, restore, or recovery qualification. Those remain later work under
[D-157](../planning/INDEX.md).

## Compatibility sources checked for TASK-0008

- [Spring Boot 4.1.1 database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html): the Flyway starter, separate `org.flywaydb:flyway-mysql`, default `classpath:db/migration`, and startup `Flyway.migrate()` behavior.
- [Spring Boot 4.1.1 managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html): Boot manages `flyway-core` and `flyway-mysql`; the checked-in Boot 4.1.1 dependency POM resolves both to Flyway 12.4.0.
- [Spring Boot 4.1.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html): Java 25 is within its Java 17 through 26 compatibility range.
- [Redgate Flyway MySQL reference](https://documentation.red-gate.com/fd/mysql-277579322.html): MySQL support needs the separate MySQL module. MySQL 8.4.11 behavior is additionally qualified by this repository's real-MySQL integration test; the upstream page does not individually list 8.4 as a verified version.
