# Synthetic data generator

`tools/synthetic-data` owns a deterministic generator of fictional population identities for development and later workload construction. It is a tool, not a production application module. It currently has no business aggregates, persistence, MySQL loader, or benchmark runner. Business modules do not depend on it.

## Dataset identity

`GeneratorRequest` requires a `PopulationProfile`, signed 64-bit seed, lower-case ASCII scenario name, and a map of options. The current scenario is `baseline`. Scenario and option keys use `[a-z][a-z0-9-]*`; option values use `[A-Za-z0-9._-]+`. Null, empty, and ambiguous separator characters are rejected. The request copies options into immutable key order, so insertion order cannot affect identity or later mutation.

`SyntheticDataGenerator.GENERATOR_VERSION` starts at `1` and is independent of Maven's snapshot version. Increment it for any incompatible change to logical output, including canonical manifest text or record derivation, for the same profile, seed, scenario, and options.

The canonical manifest is UTF-8 text with LF (`\n`) endings and a final LF. It writes `generator-version`, `profile`, `active-population`, `seed`, and `scenario` in that order, followed by `option.<key>=<value>` lines in Java `String` key order. Numbers use Java's locale-independent decimal conversion. `DatasetManifest.fingerprint()` is lower-case hexadecimal SHA-256 of those exact UTF-8 bytes. The manifest can be stored with later benchmark metadata, but its fingerprint is a dataset identity, not a measurement.

For one-based ordinal `n`, a record ID takes the first 128 bits of SHA-256 over the manifest text followed by `record-ordinal=n\n` in UTF-8, then sets RFC 9562 UUID version 8 and variant bits. `PublicId` wraps the resulting UUID. The label is `SYNTH-` plus a six-digit, zero-padded ordinal. No name, government identifier, customer record, or external dataset is used. `records()` maps a primitive ordinal stream lazily and retains no population graph.

| Profile | Active population target |
| --- | ---: |
| S | 2,000 |
| M | 10,000 |
| L | 30,000 |
| XL | 100,000 |

These are generation targets. **TARGET — NOT YET BENCHMARKED.** Iterating 100,000 identities in a unit test does not qualify HRIS application performance at the XL tier.

## Extension boundary

Later tasks can consume the manifest and lazy identity sequence to construct scenario-specific data in the module that owns each business aggregate. Keep domain rules and persistence in those owning modules; do not move aggregates into `shared-kernel` or make this tool the owner of Worker, Applicant, Client, attendance, payroll, or billing behavior. Any future domain-specific generator must preserve visibly fictional values and deterministic inputs. Real customer production data is prohibited in normal development, CI, AI context, and performance datasets.
