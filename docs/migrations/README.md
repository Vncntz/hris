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

`V2__create_audit_event.sql` creates Platform/Operations audit storage and
MySQL triggers rejecting ordinary UPDATE and DELETE statements. It introduces
no data transformation. `DATETIME(6)` holds UTC wall time by explicit
application conversion; `BINARY(16)` holds the stable public event UUID.
On MySQL with binary logging enabled, creating V2's triggers requires a migration
account with the server's required elevated privilege or a DBA-approved
`log_bin_trust_function_creators=1` setting. The disposable integration server
uses that setting solely to exercise the trigger migration and enforcement.
See the [MySQL 8.4 trigger privilege rules](https://dev.mysql.com/doc/refman/8.4/en/create-trigger.html)
and [binary-log option](https://dev.mysql.com/doc/refman/8.4/en/replication-options-binary-log.html).

`V3__create_identity_access_authentication.sql` creates the minimal Identity & Access account table. It stores a compact internal key, binary public UUID, canonical unique ASCII login, versioned password hash, enabled state, failure count, UTC lock and security timestamps, and an optimistic row version. It creates no account, role, permission, MFA, Worker link, or production-data transformation. Clean migrations through V3 still run V2, including its binary-logging trigger-creation privilege prerequisite above.

`V4__create_agency_configuration.sql` creates Platform/Operations' single Agency configuration row structure. A fixed key constrained to `1` enforces one row per database; the table also stores a unique binary public UUID, bounded display name, explicit IANA zone name, UTC creation/update wall times, and an optimistic row version. V4 creates no Agency row or production default. Application commands supply both a display name and explicit time zone during initialization.

`V5__create_identity_authorization.sql` adds Identity & Access roles, permission authority keys, account-role membership, and role-permission membership. Unique role UUIDs/names and authority keys, canonical ASCII CHECK constraints, composite membership primary keys, and explicit RESTRICT foreign keys protect relational integrity. Membership primary keys support authentication lookup; reverse indexes support referenced-key checks. V5 seeds no account, role, permission, membership, credential, or customer value and does not rewrite prior data. The real-MySQL tests cover a clean migration and an upgrade from V4 preserving an existing synthetic account. Released V1–V4 are unchanged; later evolution requires forward migrations. The canonical checks use MySQL 8.4's [CHECK constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html) and [case-sensitive regular expression matching](https://dev.mysql.com/doc/refman/8.4/en/regexp.html).

## Verify locally

`V6__create_identity_bootstrap_state.sql` adds Identity & Access's technical singleton
coordination/completion row for [TASK-0023](../tasks/TASK-0023.md). It seeds only that
technical row, with no account, role, permission, assignment or credential. CHECK
constraints enforce its fixed identity and Boolean state; triggers reject ordinary
deletion and changes to completed state. Successful provisioning closes it atomically
with identity state and audit evidence. Existing populated installations are never
adopted or elevated. The [operator runbook](../deployment/first-administrator.md)
describes fail-closed reconciliation and the absence of reset/recovery. V6 requires
the same trigger-creation privileges described for V2. V1–V5 remain immutable.

`V7__add_identity_authentication_generation.sql` adds only the non-secret BIGINT NOT NULL
DEFAULT 0 authentication_generation on identity_account, with a nonnegative CHECK constraint.
Existing accounts receive zero. Authentication carries the value transiently; password and
lifecycle mutations advance it independently of credential timestamps. TASK-0027 advances it
for each committed Account-to-Role membership change using existing V5/V7 schema; no migration
is added and released V1-V7 remain unchanged.
Real MySQL verification compares an existing V6 account's original columns before/after upgrade,
validates history, and compares upgraded/clean V7 schema. It seeds no identity/security data.
Application restart discards pre-upgrade local servlet sessions as before.

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


`V8__add_identity_role_authorization_generation.sql` adds only identity_role.authorization_generation,
a non-secret BIGINT NOT NULL DEFAULT 0 with a nonnegative CHECK constraint. Existing Roles receive
zero. No authorization or customer data is seeded or rewritten; released V1-V7 remain immutable.
TASK-0028 advances this scalar for Role enable/disable and Permission membership mutation.
Role-generation migration coverage compares a populated V7 upgrade with clean V8 schema while
preserving synthetic Account/Role/Permission and both membership tables. Existing V6-to-V7
regression remains explicitly pinned to V7; current clean-history assertions now expect V8.

`V9__add_identity_offline_mfa.sql` adds optional encrypted factor state to Identity Accounts,
with coherent-state CHECK constraints and no secret/identity seed. Released V1-V8 are unchanged.
Current clean-history assertions expect V9; historical V7/V8 regressions remain version-pinned.
