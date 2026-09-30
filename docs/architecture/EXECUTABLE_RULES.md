# Executable modular-monolith rules

[IMP-011](../implementation/tasks/IMP-011.md) / [TASK-0014](../tasks/TASK-0014.md) places ordinary JUnit/ArchUnit tests in `hris-app/src/test/java/io/github/vncntz/hris/architecture`. The app is the composition root and has the production modules on its test classpath. The importer excludes test classes. Normal Maven `test` and `verify` execute the rules; ArchUnit is test-scoped and adds no production runtime component.

## Mechanical checks

- Top-level `io.github.vncntz.hris` package slices cannot form dependency cycles. The app may depend inward on modules.
- JPA `@Entity` classes and types assignable to Spring Data `Repository` cannot be public. Module-owned persistence types stay internal.
- `shared-kernel` classes cannot depend on other HRIS packages. Other HRIS production packages cannot depend on `hris-app` classes.
- Vaadin class dependencies cannot appear outside the `hris-app` package.
- Root Maven Enforcer rejects direct or transitive dependencies from the Kafka, Spring Kafka, Spring Data Redis, Spring Boot Redis starter, Lettuce, Jedis, and Redisson families during `validate`, even if no application class uses them.

These rules inspect the production classes on the app's classpath. The separate `tools/synthetic-data` build tool is outside that classpath and is not covered by these ArchUnit rules. A class rule cannot detect an unused Maven dependency, which is why Enforcer handles the prohibited infrastructure families.

## Review obligations

Cross-module writes must call the target module's application command contract. Reads must use query services, projections, or stable IDs. Direct access to another module's owned persistence or aggregate remains prohibited. Current placeholder modules have no stable command/query package structure, so a general mechanical allowlist would invent contracts that do not yet exist. Review each new cross-module edge under D-110 and D-147. Likewise, review whether additions to `shared-kernel` are genuinely neutral; dependency direction alone cannot prove semantic neutrality. Reflection and externally configured class names require review when introduced.

When an approved task or ADR changes a boundary or permits an infrastructure family, update the affected rule and this document in that same task, then run the normal reactor verification on Windows and Linux CI. Do not weaken a rule merely to admit an accidental dependency.
