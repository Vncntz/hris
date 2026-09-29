# HRIS PLANNING STATE

State Version: 12  
Last Updated: 2026-09-29

Current Phase: PLANNING COMPLETE — FROZEN  
Last Completed Phase: P11  
Next Planning Phase: NONE

Planning Artifact Status: FROZEN — P1–P11 COMPLETE  
Compilation Status: COMPLETE — Sections 1–65 consolidated in `MASTER_SOFTWARE_PLAN.md`  
Implementation Status: NOT STARTED  
Next Implementation Milestone: M0 — Engineering Foundation  
First Implementation Task: IMP-001 — Create Maven multi-module repository skeleton and one deployable application  
Mutable Implementation State: NOT CREATED BY THIS ARTIFACT

## COMPLETED PHASES

### P1 — Business Model, Target Agency Profile, Scale Tiers, Active-Employee Definition

Status: COMPLETE

P1 established the target agency profile, preliminary scale tiers, the authoritative active-employee definition for licensing/capacity planning, and the initial Fixed-Decision Risk Review.

### P2 — Hardware and OS Baseline

Status: COMPLETE

P2 finalized practical hardware, storage, operating-system, and app/database topology baselines for all four scale tiers. It also established the smallest initial Windows/Linux production support matrix, clarified which resilience hardware is recommended rather than mandatory, and converted the P1 scale model into concrete minimum/recommended deployment profiles.

### P3 — On-Premises Deployment, Licensing Mechanism, Support Model

Status: COMPLETE

P3 defined the low-touch Windows/Linux packaging model, bundled runtime approach, OS-native service behavior, filesystem/configuration layout, HRIS-managed MySQL installation path, signed offline-capable licensing mechanism, soft employee-band enforcement, diagnostic/support model, and application-version support policy. It also established administrator-initiated upgrades, no forced updates, and a privacy-minimized diagnostic bundle. Setup/professional-service pricing was deferred to P4 and is now resolved by D-046.

### P4 — Commercial Model and Initial Pricing

Status: COMPLETE

P4 established the initial provisional perpetual-license price bands, annual-maintenance model, commercial employee-band true-up rules, founding-pilot terms, professional-service pricing, feature-request pricing, payment/cash-flow rules, and initial commercial economics assumptions. Year 1 remains intentionally pilot-first with one agency rather than a multi-customer profit-maximization target. P10 later superseded the original P4 license, maintenance, deployment, founding-pilot-discount, and feature-price levels with an affordability-first launch model while preserving the core perpetual-license/maintenance/paid-services structure.

### P5 — Domain Model and Major Workflows

Status: COMPLETE

P5 defined the core staffing-agency domain model, lifecycle boundaries, module ownership, effective-dating rules, payroll/attendance/billing immutability boundaries, recruitment-to-worker handoff, deployment/transfer/bench/separation behavior, retroactive-correction model, and cross-module write discipline. C9 is RESOLVED. Detailed statutory and privacy-rule content remains for P6 and must be verified rather than inferred.

### P6 — Philippine Compliance, Rule-Pack Architecture, Data Privacy

Status: COMPLETE

P6 established the signed, immutable, effective-dated Compliance Rule Pack architecture; declarative statutory-rule boundary; deterministic rule resolution and safe activation/rollback behavior; historical payroll reproducibility; statutory-output versioning and Compliance Register; and the initial Philippine privacy-role/data-model posture. C10 and C11a are RESOLVED at the architecture/data-model level. Specific statutory values, non-overridable legal requirements, government formats, retention mandates, and legal interpretations remain subject to current official-source or Philippine-professional verification and must never be guessed.

### P7 — Application Architecture and Scalability

Status: COMPLETE

P7 defined the implementation architecture for payroll, attendance, MySQL, Vaadin, reporting, time reliability, and application-level Windows/Linux portability. It retained the modular-monolith model, selected Maven and a modern Java/Spring/Vaadin/MySQL baseline, adopted durable database-backed batch processing for heavy work, set initial unbenchmarked scale/performance budgets, and kept partitioning, replicas, distributed queues, and similar complexity evidence-driven rather than baseline requirements.


### P8 — AI Development Workflow and Repository Governance

Status: COMPLETE

P8 defined the repository as the durable development-memory layer for humans and AI agents; selected private GitHub plus GitHub Actions as the collaboration/CI baseline; established protected-main and agent autonomy boundaries; defined task, ADR, bug, feature-request, module-documentation, and performance-baseline governance; prohibited routine use of customer production data in development/AI contexts; defined deterministic synthetic datasets through 100,000 active employees; and established layered CI plus controlled self-hosted performance qualification for the P7 scalability targets. C19 and C20 are RESOLVED at the planning/design level. Actual benchmarks remain verification work and all unmeasured targets remain TARGET — NOT YET BENCHMARKED.

### P9 — Security, Backup, Recovery, Updates, Operations

Status: COMPLETE

P9 defined the production operating model for LAN security, authentication/session controls, data-at-rest and backup protection, privacy incident handling, MySQL recovery/health operations, scheduled recovery sets, restore verification/drills, application/schema upgrade recovery, independent Rule Pack updates, legacy-data import, and clock-health thresholds. It established target RPO/RTO/backup/restore budgets by tier, adopted restore-based rollback for failed schema/application upgrades, and kept core operation independent of internet availability. C11b and C21–C25 are RESOLVED at the planning/design level; legal/privacy details and operational performance values remain verification work where explicitly identified.

### P10 — MVP Scope, Delivery Roadmap, Pricing Revisit

Status: COMPLETE

P10 converted the completed architecture into an implementation-ready solo-developer MVP plan. It defined the production MVP and explicit non-goals, M0–M6 dependency-ordered implementation milestones, milestone Definition of Done, first-production release/go-live gates, founding-pilot onboarding and parallel-payroll cutover, and a prioritized product-delivery risk register. P10 also completed the planning-level commercial revisit: affordability is the primary launch-pricing objective, the original P4 price levels were superseded by lower license/maintenance/deployment/feature estimates, and a mandatory evidence-based post-pilot repricing remains under V-008 before the second normal commercial customer. C8b and C26 are RESOLVED.

### P11 — Client Portal Add-on

Status: COMPLETE

P11 resolved the optional Client Portal as a deliberately small, separately hosted add-on that never becomes authoritative for HR/payroll data and never creates an inbound internet path to the agency installation. It established per-agency cloud isolation, separate portal identities, minimized cloud projections, outbound-only durable synchronization, idempotency/conflict/staleness rules, security/audit/retention boundaries, supported-version coupling, subscription pricing estimates, termination behavior, and a no-SLA initial operating posture. C27 is RESOLVED. The portal remains outside the core MVP and may be implemented only after the on-premises core is stable.

## ACTIVE DECISIONS

### D-001 — 100,000 Employees Is the Upper Technical Capacity Target

Status: DECIDED

The product retains the fixed supported range of 500–100,000 active employees per installation.

The primary commercial/customer-design range is 500–30,000 active employees.  
100,000 active employees is the upper supported technical-capacity target rather than the normal target-customer size.

This does not reduce the F3 scale commitment. The system must still be designed and tested so that the 100,000 tier is viable without forcing unnecessary complexity onto smaller installations.

### D-002 — Active Employee Definition

Status: DECIDED

For licensing and capacity planning, an **Active Employee** is a unique worker who:

1. has commenced at least one actual agency employment/deployment assignment; and
2. has not reached a final separation state.

The worker counts as active while any of the following applies:

- actively deployed to a client;
- retained by the agency between client assignments and eligible for redeployment;
- on approved leave;
- temporarily inactive but still employed/retained with an expected return or redeployment path;
- a seasonal/project worker during an active season/project or while still retained for the current engagement.

The following do **not** count as active employees:

- applicants;
- candidates still in recruitment/pre-employment processing;
- hired workers who have never yet commenced an actual deployment/assignment;
- resigned workers after the effective separation date;
- terminated workers after the effective separation date;
- archived historical worker records.

Rehires count again when reactivated into a new qualifying employment/deployment period.

Counting rules:

- count unique workers, not deployments, client assignments, payroll rows, or attendance records;
- one worker assigned to multiple clients/assignments still counts once;
- historical employee-record volume is never the active-license metric;
- commercial true-up cadence and license-band mechanics remain for P4, but they must use this definition as the underlying employee count.

### D-003 — General Manpower/Staffing Agency Is the Core Vertical

Status: DECIDED

The core product targets general Philippine manpower/staffing agencies operating multi-client workforce deployment.

Core design priority includes:

- recruitment;
- worker master data;
- client management;
- deployment;
- scheduling;
- attendance;
- payroll;
- billing;
- compliance;
- reporting;
- identity/access and operations.

Specialized vertical workflows such as security-agency guard operations, construction-specific project controls, or highly specialized BPO workflows are not part of the generic core unless later demand justifies configurable extensions or add-ons.

No customer-specific source-code forks are permitted.

### D-004 — Windows-First Production Preference with Ubuntu Budget Alternative

Status: DECIDED

Windows is the normal/default operating-system family for agencies prioritizing familiar administration. Ubuntu Server is the supported lower-cost production alternative for agencies seeking to minimize OS licensing cost.

The product must continue to support both Windows and Linux from one application codebase.

### D-005 — Windows Client OS Is Limited to S/M Production Tiers

Status: DECIDED

- S and M tiers may run production on Windows 11 Pro when the machine satisfies the tier hardware baseline.
- L and XL production installations use Windows Server or Ubuntu Server rather than Windows client editions.
- Windows 10 is legacy/migration-only and is not supported for new production installations.

### D-006 — ECC Memory Is Optional at Every Tier

Status: DECIDED

ECC memory is not required at any tier.

Standard non-ECC RAM is supported where the machine satisfies the applicable CPU/RAM/storage baseline. ECC may be used where available and remains a reliability enhancement, not an installation prerequisite.

### D-007 — Application/MySQL Topology by Tier

Status: DECIDED

- S: application and MySQL share one host.
- M: application and MySQL share one host.
- L: application and MySQL share one sufficiently sized host by default; a dedicated DB host is optional when measured workload or customer requirements justify it.
- XL: separate application and MySQL hosts are the baseline topology.

This preserves simple deployment through the primary 500–30,000 employee market while giving the 30,001–100,000 tier independent app/database resources.

### D-008 — RAID/Mirrored Primary Storage Is Recommended, Not Required

Status: DECIDED

RAID1, mirrored NVMe/SSD, or equivalent redundant primary storage is recommended but is not a condition for installation or support at any tier.

A customer may run on a single supported SSD/NVMe device if it accepts the increased downtime and recovery risk from device failure.

Hardware-failure responsibility, warranty exclusions, and contractual risk allocation belong to later deployment/support/commercial planning and are not defined by P2.

### D-009 — Separate Backup Storage Is Recommended, Not Required

Status: DECIDED

A backup location/device separate from the production storage is strongly recommended but is not an installation prerequisite.

The HRIS may be installed and operated without separate backup media if the customer accepts that recovery may be impossible after storage failure, corruption, ransomware, accidental deletion, or loss of the host.

Backup functionality, restore procedures, RPO/RTO, and customer/support responsibilities remain for P9 and related deployment/support planning.

### D-010 — UPS Is Recommended, Not Required

Status: DECIDED

A UPS is recommended for every production installation and strongly recommended for L/XL, but it is not an installation or operation prerequisite.

Customers operating without a UPS accept increased risk from abrupt power loss, host shutdown, storage problems, database recovery, and downtime.

### D-011 — Tier Hardware Minimums Are Real Support Baselines

Status: DECIDED

CPU, RAM, and primary-storage minimums are actual supported-hardware baselines rather than suggestions.

Installations below the applicable tier minimum are outside the normal performance/support baseline even if the software can technically start.

All sizing values remain:

**TARGET — NOT YET BENCHMARKED**

#### P2 Hardware Baseline

| Tier | Minimum CPU | Minimum RAM | Recommended CPU | Recommended RAM | Minimum Primary Storage | Server Class |
|---|---:|---:|---:|---:|---:|---|
| S | 4 modern x86-64 cores | 16 GB | 6+ modern x86-64 cores | 32 GB | 500 GB SSD/NVMe | Business-class PC/workstation acceptable |
| M | 6 modern x86-64 cores | 32 GB | 8+ modern x86-64 cores | 64 GB | 1 TB SSD/NVMe | High-spec business workstation or entry server |
| L | 8 modern x86-64 cores | 64 GB | 12–16 modern x86-64 cores | 128 GB | 2 TB NVMe preferred | Server-class hardware preferred |
| XL App Host | 12 modern x86-64 cores | 64 GB | 16–24 modern x86-64 cores | 128 GB | 1 TB+ SSD/NVMe | Server-class host |
| XL DB Host | 16 modern x86-64 cores | 128 GB | 24+ modern x86-64 cores | 256 GB+ | 4 TB NVMe preferred | Server-class host |

Notes:

- Core counts are planning baselines, not guarantees for any CPU generation.
- SSD is acceptable where stated; NVMe is preferred as workload increases.
- ECC is optional under D-006.
- RAID is optional under D-008.
- Separate backup media is optional under D-009.
- UPS is optional under D-010.

### D-012 — Dedicated Hosts Are Preferred, but Shared Customer Usage Must Be Expected

Status: DECIDED

- M/L/XL should use a host dedicated primarily to HRIS/server duties.
- S may use a Windows 11 Pro business PC that also performs ordinary office work.
- The product/support model must realistically expect that some customers will still use the HRIS host for ordinary office work even when a dedicated host is recommended.
- HRIS/MySQL services must run independently of the interactive desktop user.
- Normal office activity must not corrupt or disable HRIS operation.
- Heavy unrelated software may reduce performance and is outside the HRIS performance baseline.
- Diagnostics should later capture CPU, memory, disk, and competing-process pressure so support can identify host contention.

### D-013 — Fixed Minimum Storage Capacity by Tier

Status: DECIDED

Minimum primary-storage capacity is intentionally generous rather than tightly sized to an immature data-growth model:

- S: 500 GB minimum.
- M: 1 TB minimum.
- L: 2 TB minimum.
- XL database host: 4 TB minimum.

These are supported-hardware baselines, not measured database-growth claims.

### D-014 — Initial Production OS Support Matrix

Status: DECIDED

The initial production support matrix is intentionally small:

| Tier | Default / Preferred Windows | Supported Lower-Cost Alternative | Windows Client OS Allowed? |
|---|---|---|---|
| S | Windows 11 Pro, currently Microsoft-supported and HRIS-validated release | Ubuntu Server 26.04 LTS | Yes |
| M | Windows 11 Pro, currently Microsoft-supported and HRIS-validated release | Ubuntu Server 26.04 LTS | Yes |
| L | Windows Server 2025 Standard | Ubuntu Server 26.04 LTS | No |
| XL | Windows Server 2025 Standard | Ubuntu Server 26.04 LTS | No |

Initial architecture target is x86-64/AMD64. ARM Windows/Linux production support is outside the initial matrix unless later demand justifies validation.

For Windows 11 Pro, support is defined by vendor-supported feature releases plus HRIS validation rather than by permanently pinning one feature version. As of P2 completion, Windows 11 Pro 25H2 is the practical baseline for existing x86-64 devices. Windows 11 26H1 is not the baseline because Microsoft positions it for select new devices rather than as the normal in-place upgrade from 24H2/25H2.

No additional Linux distributions or Windows Server editions are part of the initial matrix unless later customer demand justifies the support cost.


### D-015 — Employee-Band Licensing Uses Soft Enforcement

Status: DECIDED

Employee-band enforcement uses D-002's Active Employee definition and is deliberately non-destructive.

The application must:

- warn administrators as the installation approaches or exceeds its licensed active-employee band;
- record the first over-band date and current overage status;
- expose the condition in health/diagnostic output;
- preserve an audit trail sufficient for commercial/support follow-up.

Licensing enforcement must never interrupt an active payroll run, block legally required records, or block export of customer-owned data. Core HR, attendance, payroll, statutory processing, reporting, and data-export operations remain available even when the installation is over-band or otherwise has a license problem.

### D-016 — Maintenance Controls Update and Standard-Support Entitlement, Not Continued Use

Status: DECIDED

A perpetual license remains usable after annual maintenance lapses. Maintenance expiry does not deactivate the installed software.

Active maintenance is required for normal entitlement to:

- new application releases;
- new Compliance Rule Packs;
- security updates;
- standard technical support.

The customer may continue running the last application and Rule Pack versions to which it was entitled. Commercial pricing, renewal, catch-up, and reinstatement terms remain for P4.

### D-017 — Employee-Band Overage Grace Period Is 30 Days

Status: DECIDED

When the D-002 active-employee count exceeds the licensed band, the installation enters a 30-day over-band grace period.

Warnings begin immediately and should escalate as the grace period approaches expiry. After 30 days, the installation is marked persistently license-noncompliant for support/commercial follow-up, but core operation remains available under D-015.

### D-018 — Normal Application Support Covers Two Release Lines

Status: DECIDED

Normal support covers:

- the current application release line; and
- the immediately previous application release line.

Older release lines may continue to run under a perpetual license but are outside normal support. Customers on older versions must use a documented supported upgrade path before receiving normal support, except for paid/best-effort recovery assistance where feasible.

The exact release-numbering convention may be finalized during implementation; this decision defines the support window rather than a specific semantic-versioning scheme.

### D-019 — Offline License Binding Uses a Soft Composite Machine Fingerprint

Status: DECIDED

The license is bound to the agency installation plus a composite machine fingerprint derived from multiple reasonably stable host attributes rather than a single disk, NIC, or motherboard identifier.

The fingerprint mechanism must tolerate limited legitimate hardware drift. Material host replacement or migration uses a simple license reactivation/reissue process rather than forcing a software reinstall.

Hardware changes or fingerprint problems must not create a core-payroll shutdown path.

### D-020 — No Permanent Unattended Remote-Support Agent by Default

Status: DECIDED

Standard support does not require a permanently installed unattended remote-access agent.

Support priority is:

1. system health screen and one-click diagnostic bundle;
2. customer-guided troubleshooting;
3. customer-initiated, time-limited outbound remote-support session when necessary.

No inbound firewall exposure is required for routine vendor support.

### D-021 — Vendor-Issued Signed License File Is the Authoritative License

Status: DECIDED

The authoritative installation license is a vendor-issued, cryptographically signed local license file.

Two activation paths are supported:

- preferred online request/response where internet access is available; and
- fully offline activation using an exported activation-request file and an imported vendor-issued signed license file.

After activation, normal license validation is local. There is no mandatory cloud heartbeat and prolonged internet loss must not affect core operation.

The vendor signing private key must never be present at customer installations.

### D-022 — Production Application Upgrades Are Administrator-Initiated

Status: DECIDED

Production application upgrades are never silently or automatically installed.

The administration/update tooling may detect, download, stage, or import an available release, but applying it requires an explicit administrator maintenance action.

An upgrade must refuse to begin while protected batch work such as payroll is active. P9 will define the pre-upgrade backup, database migration, recovery, and rollback mechanics.

### D-023 — Standard Deployments Use an HRIS-Managed MySQL Installation Path

Status: DECIDED

For normal deployments, the HRIS installer/deployment tooling provides a managed MySQL installation/configuration path covering service setup, database/user creation, required configuration, connectivity, and health checks.

Advanced L/XL installations may connect to a separately managed supported MySQL instance when customer requirements or measured architecture justify it.

The HRIS must not require a customer administrator to manually construct a database configuration for a routine S/M/L installation.

### D-024 — Default Diagnostic Bundles Exclude Customer Personal and Payroll Data

Status: DECIDED

One-click diagnostic bundles contain technical metadata by default, including as applicable:

- application/build and Rule Pack versions;
- OS/runtime/environment summary;
- service state and uptime;
- configuration with secrets redacted;
- sanitized application logs and recent error identifiers;
- database connectivity/health metadata rather than row contents;
- CPU, memory, disk-space, and competing-process pressure;
- backup status metadata when the P9 backup subsystem exists;
- license/support status without exposing private signing material.

Default diagnostic bundles must not include employee names, government identifiers, payroll amounts, attendance detail, database dumps, uploaded documents, or secrets.

Any deeper support-data collection requires a separate explicit administrator action and must clearly identify what additional data will be included.

### D-025 — Vendor-Assisted Initial Deployment Is Recommended and Paid, but Optional

Status: DECIDED

Vendor-assisted initial deployment is the recommended/default professional service, but it is not mandatory.

Competent customer IT staff may self-install using documented tooling. A self-installed environment remains within normal technical support only when it passes the supported-environment/preflight checks for OS, hardware, topology, service state, database connectivity, permissions, and required configuration.

The amount and commercial structure of the setup fee belong to P4.

### D-026 — Unsupported Application or Rule Pack Versions Lose Normal Payroll-Compliance Support

Status: DECIDED

An unsupported application or Compliance Rule Pack version may continue to run under the perpetual license, but the vendor does not provide normal payroll-compliance troubleshooting or represent the installed Rule Pack as current until the customer returns to a supported entitlement/version.

The application should prominently warn administrators that compliance status may be outdated. It must not block payroll solely because the version is unsupported.

### D-027 — Critical Security or Compliance Updates Are Never Force-Installed

Status: DECIDED

No application, security, or compliance update is force-installed in production.

Critical releases should be prominently flagged with repeated administrator warnings and support guidance, but installation remains an explicit maintenance-window action under D-022.

The vendor may require an upgrade before providing further normal support where the old version materially prevents safe diagnosis or correction.

### D-028 — One Application Build with Thin OS-Specific Packaging and a Bundled Java Runtime

Status: DECIDED

The product uses one application codebase and one primary application build. Windows and Ubuntu packaging add only the smallest practical OS-specific wrapper/install layer.

The production package includes the Java runtime required by the application so customers do not have to install, select, or maintain a system-wide Java runtime for HRIS operation.

Exact Java LTS/runtime versions remain subject to current-version verification under V-006.

Packaging baseline:

- Windows: signed installer package suitable for Windows 11 Pro and Windows Server within D-014;
- Ubuntu: native package/bootstrap suitable for the supported Ubuntu Server LTS baseline;
- both: identical application behavior and configuration model except where OS service/filesystem integration necessarily differs.

### D-029 — Standardized OS-Specific Directory and Service-Identity Layout

Status: DECIDED

Windows layout uses the following logical separation:

- application/runtime binaries under `%ProgramFiles%\HRIS`;
- mutable machine-wide configuration, license material, logs, and operational state under `%ProgramData%\HRIS`;
- MySQL data under the supported MySQL-managed data location rather than inside the application binary directory.

Ubuntu layout uses:

- application/runtime binaries under `/opt/hris`;
- configuration and protected secrets under `/etc/hris`;
- mutable application state under `/var/lib/hris`;
- logs under `/var/log/hris`;
- MySQL data under the supported MySQL-managed data location.

The final product/service directory name may change, but the separation of binaries, configuration/secrets, mutable state, logs, and database data is mandatory.

Services run under a dedicated non-interactive least-privilege service identity. Configuration and secret files are restricted to administrators/root and the service identity as required.

### D-030 — OS-Native Service Startup, Bounded Restart, Firewall, and Logging Model

Status: DECIDED

The Windows package installs an OS-native Windows service. The Ubuntu package installs a systemd service.

Both service definitions must:

- start automatically after normal reboot/power recovery;
- run independently of an interactive desktop/login session;
- restart after unexpected application failure using bounded delay/backoff;
- avoid an endless rapid restart loop after repeated startup failure;
- preserve deliberate administrator stop/maintenance behavior.

Firewall setup is installer-assisted and least-exposure by default:

- open only the HRIS application listener needed for LAN clients;
- do not create inbound vendor-support exposure;
- do not expose MySQL to the general LAN for single-host deployments;
- for separate app/DB topology, permit the database listener only between the approved application and DB hosts where practical.

P9 determines the final LAN HTTPS/certificate operating model.

Application logs are written to the standardized OS log directory, rotated automatically, and retained using configurable limits so logs cannot consume the production volume without bound.

### D-031 — Repair/Reinstall Preserves Customer Data by Default

Status: DECIDED

Repair, reinstall, and ordinary application uninstall actions preserve the customer database, license, configuration, and support-relevant operational data by default.

A destructive purge of customer data is a separate explicit action and must not be the default uninstall path. It requires strong confirmation and should warn about backup/export consequences.

Reinstall tooling should detect an existing supported database/configuration, validate compatibility, and reuse it rather than recreating customer data.

### D-032 — License Validation Is Local, Periodic, and Cryptographically Tamper-Evident

Status: DECIDED

Local license validation occurs at minimum:

- application/service startup;
- license import/replacement;
- at least once per day while the application remains running; and
- when license-sensitive facts such as active-employee band status materially change.

Validation checks the signed license contents, installation identity/fingerprint, employee-band entitlement, and maintenance/support metadata.

The application embeds only the public verification material needed to validate vendor signatures. Vendor private signing material remains outside customer installations.

The licensing subsystem should detect obvious local tampering such as modified license content or significant system-clock rollback and surface warnings/audit evidence, but anti-tamper detection must not create a path that violates D-015.

Band upgrades, maintenance renewals, and legitimate machine migrations are applied by importing a replacement signed license file; application reinstall is not required.

### D-033 — Built-In System Health Screen Is the First-Line Support Surface

Status: DECIDED

The administration area includes a system health screen covering at least:

- application/build version and support-window status;
- Compliance Rule Pack version/status;
- OS/runtime/environment summary;
- service uptime and recent restart/failure state;
- license band, maintenance entitlement, and overage/grace status;
- MySQL connectivity and basic health/status;
- free disk space and storage warnings;
- CPU and memory pressure indicators;
- backup last-success/status metadata once the P9 backup subsystem is defined;
- recent application errors/job failures;
- time/time-zone summary sufficient to diagnose obvious clock problems.

The same information should be exportable in the privacy-minimized diagnostic bundle defined by D-024.

### D-034 — Supported Upgrade Paths Favor Current-Minus-One to Current

Status: DECIDED

The normal supported direct application upgrade path is:

- patch/update within a supported release line; or
- immediately previous supported release line to the current release line.

Installations older than the immediately previous release line use documented intermediate upgrades or vendor-assisted migration rather than assuming arbitrary direct jumps.

Upgrade preflight must check at least supported OS/environment, adequate disk headroom, package integrity, protected batch activity, database connectivity, and version compatibility. Maintenance entitlement is checked for entitlement to the target release but failure of entitlement does not damage the currently installed system.

Database schema downgrade is not treated as a normal in-place operation. P9 must define pre-upgrade backup, migration execution, failure recovery, and restore-based rollback.

### D-035 — Self-Installed Environments Must Pass a Supportability Preflight

Status: DECIDED

Self-installation is supported under D-025, but routine support assumes the environment passes the same automated preflight used by vendor-assisted deployment.

The preflight should identify at least:

- supported OS/release family under D-014;
- tier hardware minimums under D-011/D-013;
- expected app/database topology under D-007;
- service identity/permissions;
- application and database service state;
- required LAN listener/firewall state;
- writable data/log locations and disk headroom;
- license presence/status;
- obvious unsupported local modifications.

A failed preflight does not erase or block customer data, but support may require remediation of the unsupported environment before performance or reliability issues are treated as product defects.

### D-036 — Internal Professional-Services Rate Baseline

Status: DECIDED

The internal commercial planning baseline for the developer's professional time is:

- ₱2,500 per hour; or
- ₱20,000 per 8-hour day.

This is the economic baseline for implementation, configuration, training, data migration, integrations, paid support, consulting, and feature work. Customer-facing fixed quotes may include contingency and therefore do not have to equal the internal hourly calculation exactly.

### D-037 — Normal Support Burden Must Remain Below a 24-Hour Annual Planning Ceiling

Status: DECIDED

Commercial packaging and support processes must be designed so that normal vendor effort averages materially below 24 developer-hours per customer per year.

The 24-hour figure is a planning ceiling, not a promise of 24 included support hours. Contractually included support hours are lower under D-042 so there is capacity for diagnosis, release coordination, defects, and operational variance.

### D-038 — Commercial Positioning Is Specialist Mid-Market Staffing HRIS

Status: DECIDED

The product is positioned as a specialist mid-market Philippine staffing/manpower-agency HRIS rather than bargain payroll software or a heavyweight enterprise suite.

Commercial messaging should emphasize:

- manpower/staffing-specific workflows;
- payroll/compliance correctness;
- perpetual ownership of the licensed version;
- on-premises control and offline-capable core operation;
- direct specialist support;
- simpler deployment and administration than heavyweight enterprise platforms.

### D-039 — Year 1 Is a One-Agency Founding-Pilot Year

Status: DECIDED

Year 1 is not planned around four production customers or a fixed ₱3,000,000 owner-income/profit objective.

The Year-1 commercial objective is one real founding-pilot agency used to validate the product through actual implementation, payroll cycles, operational use, support incidents, and structured feedback.

The financial objective is practical development-cost recovery and cash-flow neutrality where possible rather than maximizing Year-1 profit. The pilot must still pay enough to demonstrate commitment and contribute toward real delivery cost.

### D-040 — Provisional Perpetual License Bands and Standard List Prices

Status: SUPERSEDED

P4 originally set provisional perpetual-license list prices of S ₱250,000 / M ₱500,000 / L ₱1,000,000 / XL ₱2,000,000.

P10 supersedes these price levels with D-167 because affordability became the primary launch-pricing objective. D-040 remains only as historical traceability and must not be used for current quotes.


### D-041 — Founding Pilot Receives a 45% License Price and First-Year Maintenance Included

Status: SUPERSEDED

P4 originally priced the founding-pilot software license at 45% of the then-current standard list price and included the first 12 months of maintenance.

P10 supersedes the 45% license-discount mechanism with D-168. The founding pilot now pays the normal affordability-first license price for its band while still receiving first-year maintenance included. D-041 remains historical only.


### D-042 — Annual Maintenance Is 20% of Standard List Price with Tiered Included Support

Status: SUPERSEDED

P4 originally tied annual maintenance to 20% of standard list price, with a ₱60,000 S-tier minimum and larger included support-hour allowances.

P10 supersedes those maintenance amounts and support allowances with the affordability-first fixed maintenance schedule in D-169. The entitlement principle from D-016 remains unchanged.


### D-043 — Commercial True-Up Uses the Existing 30-Day Grace Period and Band Upgrade Difference

Status: DECIDED

D-017's 30-day over-band grace period is also the commercial temporary-spike allowance. There is no separate burst-license product in the initial commercial model.

Rules:

- warnings begin immediately when the active count exceeds the licensed band;
- if the active count returns within the licensed band before the 30-day grace period expires, no license charge is due;
- if the installation remains over-band after 30 days, the customer is commercially required to upgrade to the appropriate band, while core operation remains non-blocking under D-015;
- no retroactive per-employee overage fee is charged for the grace period;
- the permanent band-upgrade fee is the difference between the then-current standard list prices of the old and new bands;
- the customer's existing band receives full current-list-value credit for this calculation even if its original license was discounted;
- the annual-maintenance amount moves to the new band, with a prorated maintenance difference for the remaining full months of the current maintenance term;
- there is no automatic refund if employee count later falls. Any requested downgrade is handled at renewal as a commercial reissue and does not refund previously paid perpetual-license fees.

### D-044 — Maintenance Reinstatement Avoids Multi-Year Back-Payment

Status: DECIDED

A lapsed-maintenance customer retains perpetual use rights under D-016 but loses normal update, Rule Pack, security-update, and support entitlement.

To reinstate normal maintenance, the customer pays:

- the current annual-maintenance amount for the licensed band; plus
- a 25% reinstatement surcharge on that annual-maintenance amount.

The vendor does not require payment of every missed maintenance year.

If the installed application/Rule Pack is outside the supported upgrade path, the customer must also complete the required supported upgrade or migration. Vendor-assisted catch-up, recovery, or migration work is separately billable professional service. A long-lapsed or materially modified environment may require a paid assessment before support resumes.

### D-045 — Paid Feature Work Uses Size Bands and a Sponsored-Roadmap Option

Status: SUPERSEDED

P4 originally used feature-price guidance of Small ₱25,000–₱75,000, Medium ₱100,000–₱250,000, and Large from ₱275,000.

P10 supersedes these price levels with D-171/D-172 so paid reusable feature work is consistent with the lower affordability-first software-license ladder. The no-customer-specific-source-fork rule and reusable sponsored-roadmap concept remain active.


### D-046 — Provisional Professional-Service Price Schedule

Status: SUPERSEDED

P4 originally set vendor-assisted deployment/setup at S ₱40,000 / M ₱60,000 / L ₱100,000 / XL ₱160,000, with separate training, migration, consulting, support, and integration charges.

P10 supersedes the full schedule with D-170 so the deployment entry cost matches the affordability-first commercial strategy while preserving the established ₱2,500/hour / ₱20,000/day professional-time baseline and separate billing for non-standard work.


### D-047 — Commercial Cash-Flow and Payment Terms Favor Upfront Commitment

Status: DECIDED

Initial payment structure:

- standard perpetual license: 50% upon signed order/contract and 50% before production activation;
- founding-pilot license: 50% at pilot kickoff and 50% before production go-live;
- standard first-year maintenance: mandatory and payable no later than production activation;
- annual maintenance renewals: payable in advance for the next 12-month term;
- deployment/professional services: normally 50% to schedule/start and 50% at delivery/handover;
- paid feature/integration work: normally 50% deposit and 50% on accepted delivery, with milestone billing permitted for larger projects.

Quotes may require different milestone terms when third-party costs, travel, hardware, or unusually large migration/integration work creates material upfront cost.

Unless a written quote states otherwise, prices exclude applicable taxes and pass-through third-party/travel charges. Exact invoice/tax wording depends on the vendor's actual Philippine business/tax registration and must not be assumed from this planning file.


### D-048 — Workers May Have Multiple Concurrent Active Deployments

Status: DECIDED

A Worker/Employment may have multiple concurrent active Deployments, including across different clients or sites. Each attendance/payable work segment must resolve to one deployment context so schedule, rate, client/site attribution, payroll inputs, and billing remain deterministic. Licensing still counts the worker once under D-002.

### D-049 — Finalized Payroll Results Are Immutable

Status: DECIDED

Finalized payroll results cannot be directly edited or reopened. Corrections to finalized payroll are represented through traceable adjustment/reversal entries in a later regular or off-cycle Payroll Run.

### D-050 — Transfers Create Successor Deployments

Status: DECIDED

A material transfer between client, site, or position closes or future-ends the prior Deployment and creates a new effective-dated successor Deployment. Minor correction of erroneous metadata may be an audited edit where it does not represent a real business transfer.

### D-051 — Bench/Undeployed Is an Explicit Employment State

Status: DECIDED

A retained employed worker with no active client Deployment is represented as active but undeployed/bench at the Employment/Engagement level. The system must not invent an internal/fake client Deployment merely to represent bench status.

### D-052 — Applicant and Worker Are Distinct Linked Concepts

Status: DECIDED

Recruitment history remains in Applicant/Application records. Hiring creates or links a durable Worker master identity instead of converting the Applicant record into the Worker record. Duplicate-person detection should prevent accidental duplicate Worker identities.

### D-053 — Rehire Creates a New Employment/Engagement Period

Status: DECIDED

Worker is the durable person/workforce identity. Separation closes an Employment/Engagement period. A later rehire creates a new Employment/Engagement under the same Worker rather than reopening or overwriting the prior engagement.

### D-054 — Deployment-Specific Scheduling Takes Precedence

Status: DECIDED

Schedule assignment is primarily deployment-specific. A Worker/Employment may have a fallback/default schedule only when no applicable deployment-specific schedule exists. Schedule resolution must remain effective-dated and deterministic.

### D-055 — Attendance Corrections Preserve Original Evidence

Status: DECIDED

Raw/source attendance evidence is never destructively overwritten by ordinary corrections. Human corrections are recorded as separate auditable Attendance Adjustments with reason, actor, timestamp, and before/after effect on the interpreted attendance outcome.

### D-056 — Payroll Runs Preserve Frozen Input Snapshots

Status: DECIDED

Each Payroll Run preserves a versioned/frozen snapshot of the effective inputs used for calculation, including relevant attendance, leave, rate, adjustment, and rule references. A deliberate pre-finalization recalculation may refresh inputs. Finalized runs retain the original input basis permanently.

### D-057 — Compensation Rules and Billing Rules Are Independent

Status: DECIDED

Worker compensation/pay rules and client billing rules are separate effective-dated histories. A business action may change both together, but the domain must never assume that changing worker pay automatically changes client billing or vice versa.

### D-058 — Payroll Run Uses a Controlled State Machine

Status: DECIDED

Payroll Run uses an explicit controlled lifecycle conceptually equivalent to Draft/Preparing -> Calculated -> Under Review -> Approved -> Finalized. Only authorized transitions are allowed. Recalculation is permitted before finalization; Finalized cannot transition back to an editable state.

### D-059 — Payroll Records the Exact Compliance Rule Version Used

Status: DECIDED

Each finalized payroll calculation records the exact Compliance Rule-Pack/rule version applied. New payroll resolves applicable rules by effective date. Historical finalized payroll is never silently recalculated merely because a newer Rule Pack is installed.

### D-060 — Requirement Status and Document Evidence Are Separate

Status: DECIDED

Requirement Definition describes what is required in a context. Requirement Status records applicant/worker-specific satisfaction state. Document artifacts are evidence and do not automatically equal requirement satisfaction. A validated explicit relationship links documents to satisfied requirements.

### D-061 — Leave Is an Independent Business Lifecycle

Status: DECIDED

Leave has its own dates/units, classification, request/approval state, and history. Approved leave influences Scheduling, Attendance, and Payroll through references or derived inputs and must not rewrite original attendance punches.

### D-062 — Payroll Group, Period, and Run Have Distinct Roles

Status: DECIDED

Payroll Group owns recurring payroll-cycle configuration and membership rules. Payroll Period is one concrete dated cutoff/pay-date occurrence for a Payroll Group. Payroll Run is a processing execution for a Payroll Period; reruns and off-cycle runs are explicitly distinguished.

### D-063 — Billing Uses Frozen Cycle-Specific Support Data

Status: DECIDED

Billing produces traceable cycle-specific support snapshots containing quantities, rule/version references, rates, amounts, and source references. Finalized billing support is not recomputed silently from current operational data. Corrections are explicit adjustments.

### D-064 — User Identity Is Separate from Worker Identity

Status: DECIDED

Identity & Access User is independent of Worker. A User may optionally reference a Worker, but application-account lifecycle must not be coupled to employment lifecycle, and historical actor identity must survive worker separation or account disablement.

### D-065 — Audit Event Is Append-Only and Cross-Cutting

Status: DECIDED

A common append-only audit facility records material actions such as actor, time, action, target, reason, and context. Audit Event complements but does not replace authoritative domain history such as effective-dated Deployments, Attendance Adjustments, Payroll Results, or Billing Adjustments.

### D-066 — Position Is Reusable; Client/Site Conditions Belong to Assignment Terms

Status: DECIDED

Position/Job is a reusable agency-level definition. Client/site-specific job conditions are represented through effective-dated Assignment Terms rather than duplicate Position records. Assignment Terms may reference applicable compensation, billing, scheduling, and deployment requirements.

### D-067 — One Applicant May Have Multiple Applications/Pipelines

Status: DECIDED

Applicant is the durable recruitment identity. A person may have multiple historical or simultaneous Application/Pipeline instances for different opportunities. Each instance has its own target opportunity, stages, requirements, status, and outcome.

### D-068 — Effective Separation Closes Employment and Bounds Dependent Activity

Status: DECIDED

When separation becomes effective, the Employment/Engagement closes and no Deployment or schedule may remain active beyond that date. Historical records remain preserved. A later return uses a new Employment/Engagement under D-053.

### D-069 — Requirements Support Severity and Controlled Override

Status: DECIDED

Requirement definitions may be informational, warning-level, or blocking. Blocking requirements may support a tightly permissioned override with mandatory reason and audit trail unless P6 establishes through verified authority that a specific requirement is legally non-overridable.

### D-070 — Payroll Result Owns Immutable Financial Lines

Status: DECIDED

Payroll Result owns its earning, deduction, statutory contribution/withholding, and net-pay components with source/calculation references. Before finalization they may be recalculated through controlled inputs. After finalization they are immutable and corrected only through later adjustments under D-049.

### D-071 — Payslip Is a Projection of Finalized Payroll Result

Status: DECIDED

The finalized Payroll Result is the authoritative financial result. Payslip generation renders that result and records issuance/regeneration metadata. Regenerating a payslip must not alter payroll values.

### D-072 — Attendance Day Is the Authoritative Interpreted Daily Record

Status: DECIDED

Attendance Punch is source evidence, Attendance Adjustment records corrections, and Attendance Day is the interpreted worker/deployment/work-date outcome derived from punches, schedule, leave, and adjustments. Attendance Day has controlled review/approval/payroll-lock states before Payroll consumes it.

### D-073 — Modules Cannot Directly Write Another Module's Owned Aggregates

Status: DECIDED

Each module owns writes to its aggregates/repositories. Cross-module workflows use explicit application-service commands, stable IDs, and exposed read/query services. Database relationships may use stable identifiers, but hidden repository-level cross-module writes are prohibited.

### D-074 — Client Management Owns Client Company and Client Site

Status: DECIDED

Client Management exclusively owns Client Company and Client Site/Location lifecycle. Deployment, Scheduling, Attendance, Payroll, Billing, Recruitment, and Reporting may reference these identities/configuration but cannot modify the client master directly. Deactivation preserves historical references.

### D-075 — Retroactive Corrections Start at the Originating Domain

Status: DECIDED

Retroactive correction follows the rule: correct the originating domain, then propagate explicit downstream adjustments. Open downstream periods may be recalculated deliberately. Finalized payroll or billing remains immutable and receives explicit adjustment entries in a later eligible cycle.

### D-076 — Worker Master Data Owns Worker and Employment/Engagement

Status: DECIDED

Worker Master Data owns Worker and Employment/Engagement lifecycle. Client Deployment owns Deployment/Assignment and Assignment Terms. Separation, transfer, bench/deployed transitions, and rehire are coordinated explicitly across those owners rather than through shared mutable records.

### D-077 — Compliance Rules Owns Rule Packs and Statutory Rule Versions

Status: DECIDED

Compliance Rules exclusively owns Compliance Rule Packs, effective-dated statutory rule definitions, and rule-version metadata. Payroll consumes resolved rules and records exact versions used but must not duplicate or independently edit statutory definitions.

### D-078 — One Payroll Result per Worker/Employment per Payroll Run

Status: DECIDED

For an applicable Payroll Run, one Worker/Employment receives one Payroll Result even when multiple Deployments contribute earnings. Result/input lines retain deployment/client/site attribution where relevant, while employee-level deductions, statutory calculations, net pay, and payslip remain consolidated.

### D-079 — One Effective Payroll Group per Employment/Engagement

Status: DECIDED

An active Employment/Engagement has exactly one effective Payroll Group at a time. Payroll-group membership is effective-dated and independent of the number of concurrent Deployments. A future transfer may schedule a future payroll-group change.

### D-080 — Reporting Is Read-Oriented and Owns No Authoritative Transactions

Status: DECIDED

Reporting consumes read-only projections/query services from source modules. It may later maintain optimized reporting projections or caches if P7 justifies them, but authoritative transactional records remain with their owning modules.

### D-081 — Agency Is the Single Installation-Level Root

Status: DECIDED

Platform/Operations owns one Agency configuration/business root per installation. Because one installation equals one agency and one database, the core domain does not introduce SaaS-style multi-agency tenancy keys into every aggregate. Stable/public identifiers remain appropriate for integration/portal readiness.

### D-082 — Scheduling Owns Shift and Schedule Assignment

Status: DECIDED

Scheduling exclusively owns reusable Shift definitions, rotations/patterns, effective-dated schedule assignments, and schedule conflict validation. Deployment supplies assignment context; Attendance consumes the resolved schedule and does not modify scheduling records.

### D-083 — Attendance Owns Punch, Attendance Day, and Attendance Adjustment

Status: DECIDED

Attendance exclusively owns attendance evidence, interpretation, correction, approval, and payroll-lock state. Payroll consumes approved/frozen attendance outputs and never directly modifies Attendance records.

### D-084 — Recruitment Owns Applicant/Application and Orchestrates Hiring Handoff

Status: DECIDED

Recruitment owns Applicant, Application/Pipeline, recruitment stages, recruitment outcomes, and pre-hire requirement status. Hiring invokes Worker Master Data to create or link Worker and establish Employment/Engagement, then Recruitment records the hiring outcome and resulting references without transferring repository ownership.

### D-085 — Payroll Owns the Complete Payroll Processing Aggregate Family

Status: DECIDED

Payroll exclusively owns Payroll Group, Payroll Period, Payroll Run, Payroll Result, earning/deduction/statutory result lines, and Payslip issuance metadata. Other modules supply inputs/references but cannot directly alter Payroll-owned financial records.

### D-086 — Billing Owns Billing Rules and Billing/Invoice Support Data

Status: DECIDED

Billing exclusively owns effective-dated Billing Rules, billing cycles/support snapshots, billing adjustments, and invoice-support outputs. It references Client, Deployment, Attendance, and other approved source data but cannot modify the source aggregates.

### D-087 — Leave Is Initially Owned by Worker Master Data

Status: DECIDED

Leave remains a distinct business lifecycle under D-061 but is initially owned inside Worker Master Data rather than introducing a standalone Leave module. Scheduling, Attendance, and Payroll consume approved leave effects. A separate module is introduced only if later complexity materially justifies it.

### D-088 — Shared Person Documents Capability Owns Document Artifacts

Status: DECIDED

A small shared Person Documents capability owns document artifacts and document metadata. Recruitment and Worker Master Data own their respective requirement definitions/statuses and may reference the same document IDs, allowing hiring to link existing evidence instead of copying files.

### D-089 — Identity & Access Exclusively Owns Security Principals and Authorization

Status: DECIDED

Identity & Access exclusively owns User, Role, Permission, authentication state, and authorization assignments. Business modules request authorization checks and record actor IDs but do not maintain private role/permission models.

### D-090 — Client Deployment Owns Assignment Compensation Terms

Status: DECIDED

Client Deployment owns effective-dated Assignment Compensation Terms such as base rates and assignment-specific allowance/rate parameters. Payroll owns generic earning/deduction calculation semantics and consumes resolved assignment terms without rewriting the assignment agreement.

### D-091 — Employment Deployed/Undeployed State Is Derived from Deployments

Status: DECIDED

Employment/Engagement distinguishes active deployed, active undeployed/bench, and separated business states. Deployed/undeployed status is derived from active Deployment relationships rather than duplicated editable flags that can contradict assignment history.

### D-092 — Deployment Activation Requires Valid Active Prerequisites

Status: DECIDED

A Deployment may become operationally active only while its Employment/Engagement is active and its referenced Client Company, Client Site, and Position are valid for the effective period. Future-dated Deployments may be prepared in advance but cannot activate outside the engagement period.

### D-093 — Effective-Dated Single-Value Rules Cannot Overlap Ambiguously

Status: DECIDED

Where exactly one rule/value must apply for a given scope and time, overlapping effective periods are invalid unless that rule type explicitly defines combination or priority semantics. The system must reject ambiguous configuration rather than silently pick a value.

### D-094 — Recruitment Pipeline Stages Are Agency-Configurable

Status: DECIDED

Recruitment owns configurable ordered pipeline stage definitions. Stable system-level outcome semantics such as active, hired, rejected/withdrawn, and closed remain available. Each Application preserves stage-transition history instead of storing only the current stage.

### D-095 — Client Manpower Request / Job Requisition Is a First-Class Domain Concept

Status: DECIDED

Client Management owns Client Manpower Request / Job Requisition as the representation of client demand, including client/site, Position, requested headcount, target dates, and fulfillment state. Recruitment consumes open requests and Deployment may reference the fulfilled request. A Deployment is not required to originate from a formal request.

### D-096 — Client Deployment Owns the Reusable Position / Job Catalog

Status: DECIDED

Client Deployment owns the agency-level Position/Job catalog. Recruitment and Client Management reference Position through stable IDs. Client-specific conditions remain in Assignment Terms under D-066 instead of duplicating Position records.

### D-097 — Client Manpower Request Uses a Controlled Fulfillment Lifecycle

Status: DECIDED

Client Manpower Request uses a lifecycle conceptually equivalent to Draft -> Open -> Partially Filled -> Filled/Closed/Cancelled. Requested headcount is versioned business input. Fulfilled headcount is derived from qualifying linked outcomes/deployments rather than manually maintained. Authorized users may close or cancel a request before full fulfillment.


### D-098 — Compliance Rule Packs Are Signed, Immutable, Effective-Dated Artifacts

Status: DECIDED

Compliance Rule Packs are separately versioned artifacts owned by Compliance Rules. A released pack is immutable and contains source/provenance metadata, effective-date metadata, compatibility metadata, and integrity/signature information.

Statutory rates, tables, thresholds, calendars, and other time-sensitive values must not be embedded as ordinary Java constants when they can be represented safely as rule data. Installed historical packs remain available so historical calculations can be reproduced.

### D-099 — Rule Packs Are Declarative and Cannot Execute Arbitrary Code

Status: DECIDED

Rule Packs may carry validated declarative data such as tables, thresholds, percentages, classifications, calendars, parameters, mapping definitions, and versioned statutory-output definitions.

Rule Packs do not contain arbitrary executable scripts, expressions with unrestricted code execution, plugins, JARs, or dynamically loaded customer/vendor code. New calculation semantics that cannot be represented by the supported declarative rule model require an application release with normal code review and testing.

This deliberately limits flexibility in exchange for auditability, security, deterministic behavior, and solo-developer maintainability.

### D-100 — Statutory Rule Resolution Is Explicit, Effective-Dated, and Conflict-Rejecting

Status: DECIDED

Each statutory rule family must declare the date/basis used to determine applicability, such as work date, payroll/payment date, covered month, reporting period, or another verified statutory basis. The application must not assume one universal effective-date rule for every statutory calculation.

For a given rule key/scope/date, unresolved overlapping definitions are invalid. Where multiple official instruments must be combined or one supersedes another, the Rule Pack must encode the verified relationship explicitly and retain source provenance.

A statutory requirement is not configurable as an override merely because the HRIS technically supports overrides elsewhere. Any legally mandatory or non-overridable requirement must be constrained according to verified authority.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL

### D-101 — Rule Pack Activation Uses Validation, Regression Tests, Preview, and Controlled Approval

Status: DECIDED

Rule Pack lifecycle is conceptually:

Draft/Imported -> Signature/Integrity Verified -> Schema/Compatibility Validated -> Regression Tested -> Previewed -> Approved/Activated -> Superseded/Deactivated.

Before activation, the HRIS must validate at least package integrity/signature, schema, effective-date consistency, unresolved overlaps, application compatibility, required source metadata, and rule-family-specific regression fixtures.

Regression fixtures should cover representative normal cases, boundary values, effective-date transitions, known exception cases, and historical examples where reliable official examples exist.

Vendor-issued packs are signed. Any permitted local/manual statutory configuration change requires privileged authorization, reason, before/after audit evidence, and clear identification as locally modified. Dual approval is configurable for high-risk compliance changes rather than mandatory for every installation.

### D-102 — Historical and Retroactive Payroll Uses the Correct Historical Rule Version

Status: DECIDED

Payroll continues to record the exact Rule Pack/rule version used under D-059. Reproduction or recalculation of an historical period resolves the statutory rule applicable to the original legally relevant date/basis, not simply the newest currently installed rule.

Finalized Payroll Results remain immutable under D-049. Installing a corrected or newer Rule Pack never silently changes finalized payroll. Required corrections flow through explicit later adjustments/reversals while retaining references to both original and correction rule versions.

### D-103 — Rule Pack Rollback Affects Open/Future Processing, Not Finalized History

Status: DECIDED

A faulty newly activated Rule Pack may be deactivated and the previously approved compatible pack may be restored for eligible open/future processing when legally appropriate.

Rollback never rewrites finalized payroll, finalized billing support, issued audit history, or the recorded rule version used by historical results. If a bad pack affected finalized financial results, correction follows D-049/D-075 through explicit adjustments after the correct rule is verified.

The system must preserve activation, deactivation, rollback, actor, reason, timestamp, and affected-version audit history.

### D-104 — Statutory Outputs Are Versioned and Backed by a Compliance Register

Status: DECIDED

Government report/export layouts and mappings are versioned separately where practical from the underlying calculation rules so a format change does not unnecessarily require changing unrelated statutory calculations.

The Compliance Rules module maintains a Compliance Register containing at least:

- rule/output family;
- responsible government authority;
- official issuance/source reference;
- source publication/effectivity information where known;
- HRIS Rule Pack/rule/output version;
- applicability/effective-date basis;
- verification date/status;
- regression-test coverage/status;
- implementation notes and supersession relationship.

Any government file/export definition not verified against the current authoritative specification is labeled:

VERIFY AGAINST CURRENT OFFICIAL FORMAT

### D-105 — Customer Agency Is Normally PIC for HRIS Workforce Data; Vendor PIP Role Is Conditional on Actual Processing

Status: DECIDED

For the normal on-premises operating model, the customer staffing agency is modeled as the Personal Information Controller for workforce/applicant/customer operational personal data whose collection, purpose, and use it controls.

The HRIS vendor is modeled as a Personal Information Processor only when it actually processes customer-controlled personal data on the agency's instructions, such as an explicitly authorized migration, diagnostic investigation, or support activity involving personal data. The vendor may separately act as a controller for its own independent business records where it determines their purposes and means.

Exact contractual role allocation must reflect the real processing activity and be reviewed for the customer's deployment rather than assumed solely from the software relationship.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL

### D-106 — Privacy Data Model Uses Purpose Limitation, Data Minimization, Least Privilege, and Category-Specific Retention

Status: DECIDED

The HRIS privacy model must support:

- identification/classification of personal and sensitive personal data categories;
- declared business/legal processing purpose where needed;
- least-privilege access through Identity & Access;
- auditable material access/change activity;
- minimum-data collection and diagnostic exposure;
- retention categories/policies rather than one universal retention period;
- secure deletion/anonymization workflow where applicable after the controlling retention basis expires;
- preservation where law, legitimate business purpose, legal claims, or other verified obligations require continued retention.

Operational encryption, backup handling, breach-response mechanics, and secure deletion implementation details remain for P9 under C11b and related operations/security requirements.

Specific legal retention periods are never inferred from this decision.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL

### D-107 — Vendor Support Has No Routine Production-Personal-Data Access

Status: DECIDED

Normal technical support uses D-024 privacy-minimized diagnostics and D-020 customer-initiated support access. The vendor has no standing/default access to customer production personal or payroll data.

When deeper customer-data access is necessary, it requires an explicit authorized support action with defined purpose, minimum necessary scope, time limitation where technically practical, identity/accountability of the support actor, and audit evidence. Exported support datasets must be minimized and handled as customer-controlled data under the applicable privacy role/contract.


### D-108 — P7 Technology Baseline Uses Java 25 LTS, Spring Boot 4.1, Vaadin 25.3, MySQL 8.4 LTS, and Maven

Status: DECIDED — delegated

The P7 implementation baseline is:

- Java 25 LTS language/runtime level;
- Spring Boot 4.1.x;
- Vaadin 25.3.x;
- MySQL 8.4 LTS;
- Maven with a checked-in Maven Wrapper.

The application remains a Spring Boot executable JAR with the bundled runtime model from D-028. Dependency versions are governed by the Spring Boot/Vaadin BOMs where practical instead of individually pinning every transitive library.

Exact patch versions and the chosen distributable OpenJDK build remain release-time verification items so a production package never relies on stale version or licensing assumptions.

### D-109 — The Modular Monolith Is Implemented as a Maven Multi-Module Build with One Deployable Application

Status: DECIDED — delegated

The repository uses a Maven multi-module structure that mirrors the major business modules while producing one deployable Spring Boot application.

Rules:

- one repository and one deployable application artifact;
- domain modules are separate Maven modules/packages where that materially improves ownership and AI-agent isolation;
- a small shared-kernel/common module may contain only truly generic primitives such as IDs, money/time helpers, common audit contracts, and neutral infrastructure abstractions;
- business concepts do not migrate into the shared module merely to avoid a dependency;
- cyclic module dependencies are prohibited;
- Java Platform Module System (JPMS) is not required initially;
- ArchUnit or equivalent automated architecture tests enforce allowed dependencies.

### D-110 — Cross-Module Interaction Uses Application Services, Queries, and Limited In-Process Events

Status: DECIDED — delegated

D-073 remains authoritative. Cross-module writes occur only through explicit application-service commands owned by the target module. Cross-module reads use query services/projections or stable IDs.

In-process domain/application events may be used for secondary reactions that do not own the originating transaction. Events that must be guaranteed after commit use an explicit persisted handoff. A general transactional outbox is DESIGN NOW / IMPLEMENT LATER and is introduced only when a real external/portal/integration consumer needs guaranteed delivery.

No Kafka, broker, distributed event bus, or service-mesh infrastructure is part of the baseline.

### D-111 — JPA Owns Normal Aggregate Persistence; JDBC Is Allowed for Proven High-Volume Paths

Status: DECIDED — delegated

Spring Data JPA/Hibernate remains the default persistence approach for normal aggregate lifecycle and transactional business operations.

For high-volume paths where ORM object materialization would create unnecessary cost, a module may use module-owned JDBC/JdbcTemplate-style repositories for:

- attendance bulk ingestion;
- payroll result-line batch writes;
- large read projections/exports;
- archival or maintenance operations.

Bulk SQL must not bypass module ownership, audit requirements, lifecycle rules, or finalized-record immutability.

Internal relational primary keys use compact numeric IDs where practical. Externally stable/public identifiers are stored separately, normally as UUID values in a compact database representation, so future APIs do not expose sequential database keys.

### D-112 — Transactions Are Short, Explicit, and Do Not Depend on Long Database Snapshots

Status: DECIDED — delegated

Application-service command boundaries define normal Spring transactions. Long-running UI operations, file generation, payroll-wide processing, and report generation must not hold one database transaction for their entire duration.

Baseline application isolation is READ COMMITTED. Business invariants are protected through:

- database unique/foreign-key/check constraints where supported and appropriate;
- optimistic versioning for ordinary concurrent edits;
- targeted pessimistic row locking for critical state transitions such as payroll finalization;
- idempotency keys for retryable ingestion/batch operations.

Deadlocks and transient lock timeouts may receive a small bounded retry at the infrastructure boundary. Business validation failures are never blindly retried.

### D-113 — Durable Heavy Background Work Uses Spring Batch with a JDBC Job Repository

Status: DECIDED — delegated

Payroll calculation, large attendance imports, bulk payslip/report generation, and other business-important long-running jobs use Spring Batch or an equivalent Spring-managed durable batch abstraction backed by MySQL job metadata.

Requirements:

- restartable/resumable execution where the business action permits it;
- chunk-level transaction boundaries;
- persisted job/step status and progress;
- bounded worker pools;
- administrator-visible cancellation/failure state;
- no dependence on the Vaadin request thread or browser session remaining connected.

Spring scheduling may be used for lightweight maintenance triggers. Quartz is not a baseline dependency. Distributed job infrastructure is not introduced unless a later demonstrated multi-node requirement justifies it.

### D-114 — Payroll Calculation Uses a Frozen Snapshot, Deterministic Worker Calculation, and Idempotent Chunk Writes

Status: DECIDED — delegated

A payroll calculation execution follows this shape:

1. validate the Payroll Run and freeze/identify the input snapshot under D-056;
2. resolve the exact applicable Rule Pack/rule versions under D-059/D-102;
3. enumerate the applicable Worker/Employment set;
4. calculate workers in bounded chunks;
5. persist pre-finalization results idempotently;
6. expose exceptions for review;
7. permit controlled pre-finalization recalculation;
8. finalize only after completeness and authorization checks pass.

Each worker calculation should behave as a deterministic function of its frozen inputs, effective compensation terms, and resolved compliance rule versions.

The initial processing target is roughly 100–500 workers per transactional chunk, starting near 250 and tuned by benchmark. This is a tuning target rather than a business invariant.

### D-115 — Payroll Parallelism Is Bounded and Finalization Is Run-Level Serialized

Status: DECIDED — delegated

Payroll may parallelize independent worker chunks, but concurrency is deliberately bounded so database saturation does not make the HRIS unusable.

Rules:

- only one active calculation/recalculation execution may mutate a given Payroll Run at a time;
- finalization obtains an explicit run-level/state-transition lock and refuses to finalize while calculation work is incomplete or unresolved failures remain;
- independent Payroll Runs may execute concurrently when their business constraints permit it;
- worker/result uniqueness constraints prevent duplicate result creation on retries;
- payroll executors receive higher resource priority than report/export executors during cutoff contention;
- finalized Payroll Results remain immutable under D-049/D-070.

### D-116 — Initial Payroll Performance Budgets Are Tier Targets, Not Benchmarks

Status: DECIDED — delegated

On the tier's recommended hardware and a representative synthetic workload, the target elapsed time from starting a full regular payroll calculation to all results being available for review is:

| Tier | Population Budget | Payroll Calculation Target |
|---|---:|---:|
| S | up to 2,000 | <= 5 minutes |
| M | up to 10,000 | <= 15 minutes |
| L | up to 30,000 | <= 30 minutes |
| XL | up to 100,000 | <= 90 minutes |

The budgets exclude human correction/review time and bulk report/PDF generation. Normal interactive HRIS use should remain practical while the batch runs.

**TARGET — NOT YET BENCHMARKED**

### D-117 — Attendance Ingestion Uses a Canonical Import Pipeline and Immutable Source Evidence

Status: DECIDED — delegated

Biometric/device integrations, files, manual entry, and future APIs feed a canonical Attendance ingestion service rather than writing Attendance tables directly.

The ingestion pipeline preserves source evidence and records at least source, import/batch identity, device/site context where available, original timestamp/value, ingestion time, and validation outcome.

Duplicate handling uses the strongest available source key. When a source provides a stable external punch ID, it is unique within that source/device scope. When no stable external ID exists, the adapter creates a deterministic deduplication fingerprint from the available source fields and records any ambiguity for review.

Late imports or corrections never overwrite raw Punch evidence. They trigger controlled re-interpretation of affected Attendance Days only while downstream locks permit it; otherwise they surface an exception/adjustment workflow under D-055/D-075.

### D-118 — Attendance Tables Start Indexed and Partition-Friendly; Partitioning Is Not the Default

Status: DECIDED — delegated

The initial MySQL design uses normal InnoDB tables with deliberate composite indexes for the actual access paths, including worker/time-range lookup, source deduplication, site/import lookup, and Attendance Day work-date/payroll lookup.

Large attendance queries must use bounded date ranges and indexed predicates. Full-history scans in normal UI paths are prohibited.

Table partitioning is DESIGN NOW / IMPLEMENT ONLY IF MEASURED. The schema should avoid choices that make later date-based partitioning unnecessarily difficult, but the product does not take on partition administration until V-014 or equivalent measurements show that indexed non-partitioned tables are inadequate at the required retention horizon.

Archival/purge behavior must obey the category-specific retention rules from P6/P9 and may never silently destroy legally or operationally required evidence.

### D-119 — MySQL Tuning Favors Query/Index Discipline Before Exotic Infrastructure

Status: DECIDED — delegated

MySQL scalability uses:

- explicit indexes tied to observed access paths;
- `EXPLAIN`/`EXPLAIN ANALYZE` review for hot or unexpectedly slow queries;
- projections/keyset pagination for large reads;
- JDBC batching for bulk writes;
- bounded HikariCP connection pools rather than one connection per user;
- InnoDB as the transactional engine;
- no Hibernate second-level entity cache by default;
- no read replica, sharding, or separate reporting database in the baseline.

Initial InnoDB buffer-pool planning targets are approximately 25–40% of RAM on shared application/database hosts and 60–70% on a dedicated XL database host, with enough headroom reserved for the JVM, OS, connection buffers, and maintenance work.

**TARGET — NOT YET BENCHMARKED**

Exact connection-pool and buffer-pool settings are installation-tier configuration and must be validated by V-003/V-015 rather than hardcoded universally.

### D-120 — Vaadin Screens Must Be Lazy, Bounded, and Session-Light

Status: DECIDED — delegated

Vaadin UI design rules are:

- Grid/DataProvider access is lazy and server-side filtered/sorted;
- ordinary screens never load complete employee/attendance/payroll tables into memory;
- use bounded page sizes and keyset/seek pagination for very deep/high-volume browsing where offset pagination becomes costly;
- UI state stores identifiers, filters, and small view models rather than large entity graphs or generated files;
- expensive counts are avoided when a precise total is not required;
- report/export generation never runs on the UI/request thread.

Initial active-session memory targets are <= 15 MB average and <= 30 MB p95 per concurrent user under representative screens.

**TARGET — NOT YET BENCHMARKED**

### D-121 — Long-Running Vaadin Operations Use Persisted Job Progress and Polling by Default

Status: DECIDED — delegated

When a user launches payroll, imports, bulk reports, or other long work, the UI creates/starts the durable job and immediately returns a job/progress view.

The default progress UX uses modest polling only while a relevant progress view is open. Global server push/WebSocket infrastructure is not required for the initial product. Push may be added later if measured UX need justifies the additional connection/session complexity.

Refreshing or closing the browser must not stop the background job.

### D-122 — Reporting Uses Background Generation, Streaming Output, and Workload Throttling

Status: DECIDED — delegated

Reporting remains read-oriented under D-080. Large payslip batches, statutory exports, year-end reports, BIR-related outputs, large operational exports, and similar high-volume outputs execute as background jobs.

Reporting rules:

- stream rows/output rather than building the whole report in memory;
- use read projections and frozen payroll/billing snapshots where authoritative history is required;
- persist job status, output metadata, failure details, and expiration/cleanup state;
- limit concurrent heavy report workers;
- throttle or queue non-critical report generation when payroll processing needs resources;
- do not introduce a reporting replica/database until measurements demonstrate a real need.

Single lightweight views or small on-demand documents may remain synchronous when they are demonstrably fast and bounded.

### D-123 — One Installation Has One Authoritative IANA Business Time Zone; Technical Instants Are Stored in UTC

Status: DECIDED — delegated

Platform/Operations owns one installation-level IANA `ZoneId`. The Philippine default is `Asia/Manila`, but the zone remains explicit rather than being inferred from the Windows/Linux host setting.

Time model:

- audit/event instants are stored as UTC instants with sufficient precision;
- legally/operationally meaningful local dates such as work date, payroll period dates, leave dates, and holiday dates are stored as local business dates, not reconstructed later from UTC alone;
- local shift start/end rules retain the business-zone context needed for overnight work;
- domain code receives an injectable Clock/TimeService instead of calling the system clock arbitrarily;
- payroll cutoff/rule resolution uses explicit period/work/payment dates, not `now()` as a hidden calculation input.

The Philippines currently has no DST transition requirement for normal operations, but using an IANA zone keeps the model correct if another supported zone later has offset changes.

### D-124 — Clock Health Is Monitored but Core Offline Processing Does Not Depend on Internet Time

Status: DECIDED — delegated

Production hosts should use OS-supported time synchronization when available. Loss of internet/NTP does not stop core HRIS operation.

The health subsystem must detect and surface material clock problems through at least:

- comparison of application and database host time where they are separate;
- detection of significant backward/forward wall-clock jumps where practical;
- last-known synchronization/clock-health metadata when available from the OS;
- prominent warnings in System Health and diagnostics.

Exact warning/blocking thresholds are operational policy for P9 and must be tested. Deterministic payroll calculations remain based on explicit business dates so a temporary clock problem does not silently change historical payroll logic.

### D-125 — Caching Is Conservative and Version-Aware

Status: DECIDED — delegated

The baseline has no general Hibernate second-level cache and no Redis/distributed cache.

Local in-process caching is limited to small, read-mostly or immutable/version-keyed data where invalidation is simple, such as parsed immutable Rule Pack metadata, stable reference dictionaries, or expensive read-only configuration projections.

Mutable attendance/payroll financial state is not cached as an alternate source of truth. Database indexes and query design are optimized before introducing broader caches.

### D-126 — Error Handling Separates User/Business Errors from Infrastructure Failures

Status: DECIDED — delegated

Business validation errors are returned in user-readable form without stack traces. Infrastructure errors receive a correlation/error ID, are logged with technical detail, and are surfaced to the UI as a safe recoverable failure message.

Batch jobs persist failed item/step context sufficient for diagnosis and controlled retry without exposing personal/payroll data in ordinary logs. Automatic retry is limited to recognized transient failures and is bounded.

### D-127 — Observability Is Local-First and Privacy-Minimized

Status: DECIDED — delegated

Spring Boot Actuator/Micrometer-style health and metrics hooks are used for local operations and diagnostics without requiring external telemetry.

Important metrics/log context include:

- JVM heap/GC/thread pressure;
- active Vaadin sessions;
- DB pool utilization and wait time;
- background-job queue/running/failure counts;
- rows/items processed and job duration;
- payroll/report/import execution IDs;
- disk/free-space and existing D-033 health signals;
- correlation IDs for requests and background work.

External telemetry/SaaS monitoring is optional and not required for production operation.

### D-128 — API Readiness Is Achieved Through Stable IDs and Application Contracts, Not by Building the Portal Early

Status: DECIDED — delegated

Application services expose DTO/command/query contracts that do not leak JPA entities. Stable public identifiers are retained for future integrations/portal use.

The MVP does not build a general public REST API merely for architectural purity. When a real integration endpoint is added, it should call existing application services and respect the same authorization, audit, and module-ownership rules.

### D-129 — The Same Application Architecture Serves All Tiers; Scale Changes Tuning, Not Product Topology

Status: DECIDED — delegated

S, M, L, and XL use the same deployable codebase, domain model, module boundaries, and job architecture.

Tier differences are limited primarily to:

- hardware sizing;
- application/database host placement already defined by D-007;
- connection/buffer/job-worker tuning;
- report/batch concurrency limits;
- benchmarked operational settings.

No customer tier receives a separate code branch, microservice split, sharded database, or special distributed architecture by default.

### D-130 — Application-Level Windows/Linux Portability Must Be Explicit and Testable

Status: DECIDED — delegated

Application code must behave consistently across the supported Windows and Ubuntu baselines:

- use Java `Path`/filesystem APIs and configuration-driven directories rather than hardcoded separators;
- never rely on case-insensitive filenames or case-only filename distinctions;
- use UTF-8 internally unless an authoritative statutory/import format requires another encoding;
- accept common CRLF/LF input where the file format permits it, while generating explicit line endings required by each target format;
- use explicit locale/formatting rules rather than the host default locale;
- use `BigDecimal`/database `DECIMAL` for money/rates rather than floating point;
- generated PDFs must use controlled bundled/embedded redistributable fonts rather than depending on fonts installed on the customer OS;
- sanitize filenames for Windows-invalid characters and avoid assumptions that require POSIX-only permissions, links, or rename semantics;
- use temporary-file-plus-finalize patterns for generated files so partial outputs are not presented as complete.

Government/statutory file details remain subject to V-010 and must override generic encoding/line-ending defaults when the official format requires it.


### D-131 — Private GitHub and GitHub Actions Are the Development Collaboration Baseline

Status: DECIDED

The canonical source repository is a private GitHub repository. GitHub Actions is the normal hosted CI platform.

Repository governance uses:

- a protected `main` branch representing production-quality integrated code;
- short-lived feature/fix/task branches or isolated worktrees;
- pull requests for significant integration changes, including solo-development work where review/history materially helps;
- required CI checks before normal merge;
- ordinary Maven, shell, and PowerShell commands as the authoritative build/test interface so the project is not operationally dependent on GitHub-specific build semantics.

The deployed HRIS has no runtime dependency on GitHub or GitHub Actions.

### D-132 — Real Customer Production Data Is Prohibited from Normal Development, CI, AI Context, and Performance Datasets

Status: DECIDED

Routine development and testing must not copy real customer/production personal, payroll, applicant, attendance, government-identifier, document, or other operational data into:

- Git repositories;
- CI artifacts or fixtures;
- ordinary developer databases;
- AI-agent prompts/context or coding-agent workspaces;
- synthetic/performance benchmark datasets.

Development uses deterministic synthetic data. When a real production incident reveals an edge case, the normal response is to create a sanitized synthetic regression scenario reproducing the behavior without retaining the customer's records.

Explicitly authorized support, migration, recovery, or forensic work may process customer data only under the P6/P9 privacy/support controls and does not convert that data into ordinary development/test material.

### D-133 — Performance Validation Uses Separate PR, Scheduled/On-Demand, and Release-Gate Layers

Status: DECIDED

Full S/M/L/XL performance qualification is not part of every pull-request run.

Validation layers are:

1. **PR blocking:** compile, unit, architecture, MySQL integration, migration, payroll golden/regression, selected dual-OS portability checks, and small deterministic performance smoke tests.
2. **Scheduled/on-demand performance qualification:** representative S/M/L/XL workloads, including the 100,000-active-employee technical tier, on controlled benchmark infrastructure.
3. **Release qualification:** relevant full regression, upgrade/restore qualification as applicable, and performance suites before production release.

A performance threshold becomes a hard blocking regression gate only after a sufficiently repeatable measured baseline exists. Until then, P7 performance numbers remain targets.

**TARGET — NOT YET BENCHMARKED**

### D-134 — AI Agents May Prepare Changes but May Not Merge Protected Main Without Human Approval

Status: DECIDED

AI coding agents may, within granted repository credentials and task scope:

- create branches or worktrees;
- read repository documentation and code;
- edit code, tests, migrations, and documentation;
- commit and push task-branch changes;
- run CI;
- open or update pull requests.

Agents may not bypass branch protections or merge into protected `main` without explicit human approval. Payroll/compliance correctness, database migration safety, and cross-module boundaries therefore retain a final human integration checkpoint.

### D-135 — Meaningful AI-Assisted Coding Work Uses Checked-In Task Specifications

Status: DECIDED

Meaningful implementation work uses a lightweight checked-in task file, normally `docs/tasks/TASK-####.md`. The task is durable repository context rather than a substitute for source code or ADRs.

A task file contains at minimum:

- goal;
- explicit non-goals/scope boundaries;
- affected modules;
- relevant Decisions/ADRs/domain documents;
- acceptance criteria;
- required tests;
- documentation implications;
- migration/deployment implications where applicable.

Trivial mechanical edits do not require a task file. Completed task files are normally retained or archived in Git for traceability rather than deleted.

### D-136 — Authoritative Performance Qualification Uses Controlled Self-Hosted Benchmark Hardware

Status: DECIDED

GitHub-hosted runners remain suitable for ordinary CI, portability checks, and small smoke measurements, but they are not the authoritative source for product performance qualification.

Authoritative benchmark runs execute on controlled self-hosted benchmark machines whose relevant environment is recorded, including:

- CPU and memory;
- storage;
- network topology where app/DB are separated;
- OS and patch level;
- JDK/JVM build and flags;
- MySQL version and configuration;
- application configuration;
- dataset profile, seed, and generator version.

The project does not initially require one physical machine per S/M/L/XL tier. Benchmark infrastructure expands only when representative qualification or a production-support requirement proves tier-specific hardware is necessary.

### D-137 — Approved Performance Baselines Are Version-Controlled Repository Artifacts

Status: DECIDED

Compact approved performance baselines are stored under repository documentation, normally `docs/performance/baselines/`, keyed by benchmark profile and qualified environment.

Each accepted baseline records enough metadata to determine whether a later result is comparable. Large raw datasets and bulky transient logs are not committed to Git.

Changing an approved baseline requires review and a reason such as optimization, workload-model change, environment change, or an explicitly accepted regression. Measurements from materially different environments must not be presented as direct like-for-like regressions.

### D-138 — Repository Structure Mirrors Business Ownership and Keeps Cross-Cutting Governance Explicit

Status: DECIDED — delegated

The repository remains one Maven multi-module build under D-109. Business source/test code is organized by the existing module ownership model rather than by technical layer across the whole application. A practical baseline is:

```text
/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── AGENTS.md
├── .github/workflows/
├── .cursor/rules/
├── hris-app/
├── shared-kernel/
├── modules/
│   ├── platform-operations/
│   ├── identity-access/
│   ├── person-documents/
│   ├── client-management/
│   ├── recruitment/
│   ├── worker-master/
│   ├── client-deployment/
│   ├── scheduling/
│   ├── attendance/
│   ├── payroll/
│   ├── billing/
│   ├── compliance-rules/
│   └── reporting/
├── tools/
│   └── synthetic-data/
└── docs/
```

Exact artifact IDs/directories may be adjusted during implementation without changing ownership semantics. Database migrations may use one deterministic globally ordered migration stream even though each migration identifies its owning module, because schema evolution requires a single unambiguous application upgrade order.

Generated build outputs, large generated datasets, and transient benchmark logs are not committed except for deliberately approved compact fixtures/baselines.

### D-139 — Root AGENTS.md Is the Short Global Agent Contract; Module Rules Are Selective

Status: DECIDED — delegated

Root `AGENTS.md` stays intentionally short and points to canonical detailed documents. It defines at least:

- mandatory context-reading order;
- global architecture and module-write constraints;
- prohibited shortcuts;
- testing expectations;
- privacy/data rules including D-132;
- migration and compliance caution points;
- definition-of-done requirements;
- pointers to relevant docs/indexes.

Before editing, an agent reads in this order:

1. root `AGENTS.md`;
2. applicable module `AGENTS.md` if present;
3. the active `TASK-####.md`;
4. referenced ADR/domain/module/compliance documents;
5. relevant code and tests.

Module-level `AGENTS.md` files are created only where additional invariants materially reduce errors. Initial candidates are Payroll, Attendance, Compliance Rules, and database migration/upgrade work. Do not duplicate root rules into every module.

Cursor rules are thin operational pointers/guards and must not become a competing architecture specification. Canonical policy lives in normal repository Markdown and source/tests.

### D-140 — Architecture Decisions and Documentation Use Small Indexed Markdown Artifacts

Status: DECIDED — delegated

Repository documentation is deliberately split into small indexed files rather than enormous agent-context documents. The baseline taxonomy is:

```text
docs/
├── README.md                 # documentation index
├── architecture/adr/
├── domain/
├── modules/
├── compliance/
├── deployment/
├── migrations/
├── releases/
├── support/
├── os/
├── performance/
├── bugs/
├── features/
├── tasks/
└── known-issues/
```

ADRs use stable IDs such as `ADR-####`. An accepted ADR is not rewritten to conceal a later architectural change; a newer ADR supersedes it and links the history.

Domain/module docs explain invariants and ownership rather than duplicating implementation. The P6 Compliance Register remains a separately maintained indexed artifact with source/effectivity/test status. Deployment, migration, release, support-matrix, OS-specific, performance-budget, and known-issue documentation is versioned with the code that it governs.

### D-141 — Bug Registry Uses Stable BUG IDs and One Durable Record per Material Defect

Status: DECIDED — delegated

Material product defects use stable IDs such as `BUG-####`, with an index plus one Markdown record per bug under `docs/bugs/`. GitHub Issues may link to the record but do not replace the repository record where durable release/migration knowledge is required.

The bug record contains at minimum:

- ID and title;
- status;
- first affected version;
- fixed version when known;
- affected OS/platform;
- affected scale tier/workload where relevant;
- owning module;
- symptoms/impact;
- deterministic reproduction or evidence;
- root cause when established;
- fix summary;
- regression-test reference;
- migration/data-repair implications;
- customer-specific occurrence vs general product defect;
- related task/PR/ADR/release-note references where useful.

Sensitive customer data is never copied into the registry under D-132.

### D-142 — Feature Requests Use Stable FR IDs and Preserve Commercial/Product Classification

Status: DECIDED — delegated

Material feature requests use stable IDs such as `FR-####`, with an index plus one Markdown record per request under `docs/features/`.

The record contains at minimum:

- ID and title;
- requesting customer/reference where disclosure is appropriate internally;
- capability requested;
- business reason;
- owning/affected module;
- estimated size;
- quote/commercial reference when applicable;
- approval state;
- development status;
- target release when committed;
- core-roadmap/sponsored-roadmap/customer-configuration classification;
- configuration/feature-flag treatment where applicable;
- related task/PR/ADR references.

This registry must preserve the no-customer-specific-source-fork rule and the D-045 distinction among configuration, sponsored reusable roadmap work, and general roadmap work.

### D-143 — Automated Tests Are Layered and MySQL Behavior Is Tested Against Real MySQL

Status: DECIDED — delegated

Testing uses the smallest layer that proves the behavior, with broader integration where database/framework behavior matters:

- pure unit tests for deterministic calculations and small components;
- domain/application-service integration tests for transactional workflows and module contracts;
- repository/database integration tests;
- Testcontainers or equivalent ephemeral **MySQL 8.4 LTS-line** instances for SQL/JPA/locking/index/migration behavior;
- migration tests from supported prior schema/application states;
- payroll golden/regression fixtures tied to explicit input and Rule Pack versions;
- upgrade tests for supported release transitions;
- backup/restore qualification tests once P9 defines the backup mechanism;
- Windows/Ubuntu portability tests;
- synthetic load/performance tests under D-133/D-136.

H2 or another database may be used only for narrow tests whose behavior is explicitly database-independent; it is not accepted as a substitute for MySQL compatibility or concurrency semantics.

### D-144 — CI Separates Fast Required Checks from Heavy Qualification Work

Status: DECIDED — delegated

Normal pull-request CI prioritizes fast feedback while still protecting architecture and payroll correctness. Required PR checks normally include:

- Maven build/compile;
- unit tests;
- architecture/module-boundary tests;
- selected service/repository integration tests using ephemeral MySQL;
- migration validation;
- payroll golden/regression tests relevant to changed code;
- static quality/security checks selected during implementation;
- selected Windows and Ubuntu portability checks;
- small deterministic performance smoke tests where stable enough.

Scheduled/on-demand/release workflows perform the heavier integration, installer, upgrade, restore, large synthetic-data, concurrency, and performance suites. CI jobs must be reproducible from repository commands and should not hide critical behavior inside proprietary workflow-only scripts.

### D-145 — Synthetic Data Generator Is Deterministic, Versioned, Scenario-Based, and Supports the XL Tier

Status: DECIDED — delegated

`tools/synthetic-data/` provides a versioned generator capable of producing at least 100,000 active employees plus realistic historical/operational data needed by C20. Dataset identity is defined by at least:

- generator version;
- named profile;
- deterministic random seed;
- scenario/options manifest.

Named baseline profiles correspond to S/M/L/XL workload scales and can generate:

- active and historical Workers/Employments;
- Applicants/Applications where relevant to scenario coverage;
- Client Companies/Sites and manpower requests;
- concurrent and historical Deployments/Assignment Terms;
- Positions and effective-dated compensation/billing terms;
- schedules including overnight/rotating/split cases;
- attendance punches, duplicates, late imports, missing punches, adjustments, and Attendance Days;
- payroll groups/periods/runs/results/history;
- billing support history where needed for cross-workload tests;
- compliance/statutory rule references and result structures using test Rule Packs/fixtures rather than guessed current legal values;
- audit/history volumes sufficient for realistic query pressure.

Large datasets are generated on demand or restored from controlled test artifacts; they are not normally committed as giant database dumps. Re-running the same compatible generator version/profile/seed must reproduce equivalent logical data.

Synthetic values must be visibly fictional and must not intentionally imitate a real customer's workforce dataset.

### D-146 — Performance Measurements Require Comparable Metadata and Explicit Regression Rules

Status: DECIDED — delegated

Every authoritative benchmark result records at least:

- source commit/release;
- benchmark/test version;
- dataset generator version/profile/seed;
- active and historical population sizes;
- OS/hardware/storage/network topology;
- JDK/JVM configuration;
- MySQL version/configuration and connection-pool settings;
- application worker/concurrency settings;
- warm-up/repetition methodology where applicable;
- measured result and relevant variance.

Performance budgets from P7 are compared only against compatible environments/workloads. Regression thresholds should include tolerance for normal variance and become blocking only after baseline repeatability is established.

Raw measurements may be retained as CI artifacts or external build artifacts; compact approved baselines and conclusions remain in Git under D-137.

### D-147 — Architecture Rules Are Executable CI Constraints, Not Documentation Alone

Status: DECIDED — delegated

ArchUnit or equivalent architecture tests plus Maven dependency structure enforce the key P5/P7 module rules, including:

- no cyclic business-module dependencies;
- no direct writes/repository access into another module's owned aggregate;
- JPA entities do not become cross-module API contracts;
- cross-module calls use allowed application/query contracts;
- shared-kernel dependencies remain generic rather than accumulating business ownership;
- forbidden infrastructure dependencies such as Kafka/Redis are not casually introduced without an accepted architecture decision.

Where a rule cannot be mechanically proven, the corresponding `AGENTS.md`/ADR/task review requirement remains mandatory.

### D-148 — Definition of Done Includes Tests, Documentation, and Operational Consequences

Status: DECIDED — delegated

A meaningful task is not complete solely because code compiles. As applicable, done requires:

- acceptance criteria satisfied;
- relevant unit/integration/golden/architecture tests added or updated;
- migrations and migration tests for schema/data changes;
- documentation/module/domain/ADR updates when behavior or architecture changed;
- bug/feature/task register updates;
- release/migration/known-issue notes when customer operation is affected;
- performance evidence when a performance-sensitive path changes materially;
- no new use of real customer data under D-132.

Specialized agent roles are used only where they reduce cross-module risk. Initial useful specializations are Payroll/Compliance, Attendance, database migration/upgrade, and performance-validation work. A separate persona/agent for every module is unnecessary.

### D-149 — Dual-OS CI Uses Risk-Based Coverage Rather Than Running Every Heavy Test Twice on Every PR

Status: DECIDED — delegated

Because application-level Windows/Linux portability is mandatory under D-130, CI includes both Windows and Ubuntu coverage.

- PRs run selected compile/unit/integration/file/path/locale/encoding portability tests on both OS families where the code path is OS-sensitive.
- Broad business-domain tests may run primarily once when they are demonstrably OS-neutral.
- installer/service/firewall/upgrade/package qualification is executed in dedicated Windows and Ubuntu release/scheduled workflows.
- PDF/file portability tests compare controlled outputs on both supported OS families where practical.

This limits duplicated CI cost while preserving explicit dual-OS validation.

### D-150 — Normal LAN Access Uses HTTPS with Customer-Local Trust

Status: DECIDED

Normal browser access to the HRIS over the customer LAN uses HTTPS.

The standard deployment path provides an agency-local certificate-authority/server-certificate mechanism and installation guidance/tooling for establishing trust on authorized customer workstations. A customer-provided certificate may be used when it satisfies the supported configuration.

Plain HTTP is not the normal LAN operating mode. It may be available only for tightly controlled loopback/recovery diagnostics where exposing it beyond the local host is prevented.

Existing firewall rules remain authoritative: MySQL is not exposed to the general LAN on single-host deployments, and separate app/DB deployments restrict database access to the approved application host where practical.

### D-151 — Authentication Uses Adaptive Password Hashing, Bounded Sessions, and Optional Offline MFA

Status: DECIDED

Identity & Access remains the sole owner of users, roles, permissions, authentication state, and authorization under D-089.

Authentication baseline:

- passwords are stored only through a modern adaptive one-way password-hashing function; Argon2id is the preferred implementation target where the selected Spring Security/runtime combination supports and validates it;
- the password work factor is benchmarked on supported hardware rather than blindly accepting a universal cost parameter;
- minimum password length is 12 characters by default and may be raised by agency policy;
- default/vendor-known passwords are prohibited;
- forced periodic password changes are not a baseline requirement absent a customer/legal policy requiring them;
- repeated failed authentication attempts are rate-limited and may trigger a bounded temporary lock;
- default authenticated-session idle timeout is 30 minutes and is administrator-configurable within supported bounds;
- privileged/high-risk operations may require recent re-authentication;
- offline-capable TOTP MFA should be supported for privileged accounts without making internet connectivity a dependency.

Exact password/MFA policy parameters remain configurable operational policy and must be tested against usability, hardware, and customer requirements.

### D-152 — Data-at-Rest Protection Uses OS Volume Encryption Plus Strong Backup Encryption

Status: DECIDED

The baseline does not attempt application-level field encryption for every HR/payroll column.

Instead:

- Windows deployments should use supported BitLocker/full-volume encryption where operationally feasible;
- Ubuntu deployments should use supported LUKS/full-volume encryption where operationally feasible;
- application secrets, private credentials, and recovery material receive restricted storage/permissions beyond ordinary user-readable configuration;
- removable-media, external-drive, portable, and offsite backup copies containing customer data must use strong authenticated encryption;
- backup encryption/recovery keys require a documented separately protected recovery copy so encryption does not create an unrecoverable installation.

Disk encryption remains recommended rather than an installation prerequisite unless customer policy or verified law requires it. Backup encryption is mandatory for removable/offsite copies containing customer data.

### D-153 — Privacy/Security Incidents Use a Documented Containment and Breach-Assessment Runbook

Status: DECIDED

Operational incident handling follows a documented lifecycle:

1. detect and record the incident;
2. contain further exposure or damage;
3. preserve relevant technical evidence;
4. assess affected systems/data and likely scope;
5. recover/restore secure operation;
6. perform post-incident review and corrective action;
7. support any required notification/reporting through the actual PIC/PIP responsibility chain.

Normal diagnostics remain privacy-minimized under D-024/D-107. Vendor access to deeper production data remains explicit, purpose-limited, time-bounded where practical, least-privilege, and audited.

The customer agency/PIC and its DPO or responsible privacy personnel own the customer-side breach determination and notification obligations. The vendor assists according to the actual processing role and contract.

Current Philippine breach-notification thresholds, timing, reporting channel, required contents, postponement rules, and incident-reporting obligations must be verified against current NPC authority when implementing/updating the operational runbook.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL

### D-154 — MySQL Recovery Prefers Built-In InnoDB Recovery and Restore over Ad-Hoc Data-File Repair

Status: DECIDED

InnoDB remains the authoritative transactional engine.

After an unexpected process termination or sudden power loss, the normal first recovery path is MySQL/InnoDB crash recovery followed by application/database health validation.

If MySQL cannot recover or start cleanly:

- capture the relevant service/MySQL error state and logs;
- diagnose storage/capacity/configuration causes;
- avoid unsupported manual editing/copying of live InnoDB data files as a normal repair technique;
- escalate to the documented restore/recovery procedure when the database cannot be returned to a known-good supported state.

Database/application logs are rotated and capacity-bounded. System Health includes database connectivity/service state, error/job state, backup status, disk headroom, and relevant storage/runtime signals.

HRIS-managed deployments own the supported MySQL configuration baseline. Customer-managed MySQL deployments remain supported only when they satisfy the approved version/configuration/supportability requirements.

### D-155 — Standard Backups Are Versioned Recovery Sets with Separate Copies and Tiered RPO/RTO Targets

Status: DECIDED

The standard backup unit is a versioned **Recovery Set**, not an isolated database file.

A Recovery Set contains as applicable:

- a transactionally consistent MySQL backup produced through the supported MySQL backup/export tooling;
- database backup metadata needed for recovery and point-in-time continuation where enabled;
- HRIS configuration required to reconstruct the installation, with secrets handled securely;
- the installed license/installation state required for supported restoration/reactivation;
- installed Compliance Rule Packs and operational metadata needed to reproduce the installation state;
- person-document/file storage owned outside MySQL when present;
- a manifest containing versions, creation time, installation identity, included components, checksums/integrity information, and encryption metadata without exposing keys.

Standard storage pattern:

1. local/staging backup on the production environment where adequate headroom exists;
2. copy to a separate external device, NAS, or other physically/logically separate customer-controlled storage;
3. optional encrypted offsite copy when the customer has suitable connectivity/storage.

Default backup-generation retention is 7 daily + 4 weekly + 12 monthly Recovery Sets, configurable by installation policy. This backup retention policy does not override legal/business retention requirements for the underlying authoritative HR/payroll/attendance/document records.

Initial operational targets on recommended tier hardware and representative data are:

| Tier | Full Backup Target | RPO Target | Restore Duration Target | RTO Target* |
|---|---:|---:|---:|---:|
| S | <= 30 minutes | <= 24 hours | <= 1 hour | <= 4 hours |
| M | <= 60 minutes | <= 12 hours | <= 2 hours | <= 6 hours |
| L | <= 2 hours | <= 4 hours | <= 4 hours | <= 8 hours |
| XL | <= 4 hours | <= 1 hour | <= 8 hours | <= 12 hours |

`*` RTO begins when supported replacement host/storage resources needed for recovery are available; procurement/shipping of replacement hardware is excluded.

**TARGET — NOT YET BENCHMARKED**

For L/XL, the baseline design enables archived MySQL binary logs and protected copies sufficient to pursue the tighter RPO through point-in-time recovery after the latest suitable full Recovery Set. S/M may enable the same mechanism when customer risk tolerance requires it.

### D-156 — A Backup Is Not Proven Until It Is Verified and Restore-Tested

Status: DECIDED

A successful file-copy operation alone is not treated as proof that a backup can restore the HRIS.

Every Recovery Set must receive automated manifest/checksum/integrity validation and record a clear success/failure state visible to operations/health diagnostics.

Restore qualification includes:

- initial restore validation before production acceptance/go-live;
- restore testing after a material backup-format/tooling change;
- at least semi-annual isolated restore drills for S/M;
- at least quarterly isolated restore drills for L/XL;
- release qualification tests for backup/restore and supported upgrade/recovery paths.

A restore drill uses an isolated environment and verifies more than database startup: application schema compatibility, configuration, Rule Pack availability, document/file references where applicable, basic authentication/authorization, and representative business-data reads are validated before the drill is considered successful.

### D-157 — Application Upgrades Use Flyway Forward Migrations and Restore-Based Rollback

Status: DECIDED

Flyway is the schema migration framework for the baseline implementation.

Production application upgrade sequence is:

1. verify target package integrity/signature and entitlement/support compatibility;
2. run the supported-environment/version/disk/database/protected-job preflight;
3. create and verify a pre-upgrade Recovery Set;
4. enter explicit maintenance mode and prevent new conflicting business operations;
5. apply ordered immutable forward Flyway migrations;
6. validate the resulting schema/application compatibility;
7. start the new application and execute health/smoke validation;
8. either complete the upgrade or invoke documented restore-based recovery.

Released/versioned Flyway migrations are immutable. A mistake is corrected by a later migration, not by silently rewriting already-released migration history.

Database schema downgrade is not a normal in-place rollback mechanism. If an application/schema upgrade fails and safe forward repair is not appropriate, recovery restores the complete verified pre-upgrade Recovery Set and the compatible prior application version.

Large-table migrations must be designed to bound locks, log growth, free-space demand, and maintenance-window risk, using expand/backfill/contract, chunked backfill, or another measured strategy rather than one uncontrolled blocking transaction.

Migration/upgrade durations are release-qualification targets and must be measured on representative tier datasets before a release is approved for materially large schema changes.

### D-158 — Compliance Rule Pack Updates Remain Operationally Independent from Application Upgrades

Status: DECIDED

A Compliance Rule Pack may be imported/validated/activated independently of an application release when its declared compatibility permits it.

D-098 through D-104 remain authoritative for Rule Pack signing, immutability, effective dating, resolution, activation, rollback, output versioning, and historical reproducibility.

A bad newly activated Rule Pack is handled by controlled deactivation/rollback to a previously approved compatible pack for eligible open/future work. It never triggers silent modification of finalized Payroll Results or finalized historical financial outputs.

Application upgrades and Rule Pack updates therefore share package-integrity, compatibility, audit, and operational-health principles but remain separate deployment lifecycles.

### D-159 — Legacy Import Uses Staging, Dry Run, Reconciliation, Provenance, and Idempotent Commit

Status: DECIDED

Supported migration/import inputs initially include:

- Excel/XLSX;
- CSV;
- mapped exports from a customer's prior HRIS or payroll system where a documented mapping can be created.

Legacy data never writes directly into authoritative domain tables from raw source files.

The import workflow is:

1. create an Import Batch and preserve source-file metadata/checksum;
2. map source fields/codes to the HRIS import model;
3. normalize and stage candidate records;
4. validate required values, references, dates, code mappings, and business invariants;
5. perform duplicate/identity detection and expose ambiguous matches;
6. run a dry-run transformation and reconciliation report;
7. require administrator approval for the reviewed batch;
8. create a verified pre-import Recovery Set for production commits;
9. commit through module-owned application services/import APIs in restartable/idempotent chunks;
10. record per-record provenance, success/failure, and resulting HRIS IDs;
11. produce a post-import reconciliation/error report.

Correction normally occurs by fixing the staged mapping/source issue and rerunning an idempotent or replacement batch, not by manually editing database rows.

Historical employment and client/deployment history should be preserved where source data is sufficiently reliable. Opening balances must be explicitly reconciled. Historical finalized payroll imported from legacy systems should normally be retained as historical financial evidence/snapshots rather than silently recalculated when original statutory inputs/rule versions cannot be proven.

### D-160 — Clock Health Uses Explicit Warning/Critical Thresholds Without Making NTP a Core Dependency

Status: DECIDED

P7's explicit installation business time zone and deterministic business-date rules remain authoritative.

Initial operational clock-health thresholds are:

- app/DB host time drift above 30 seconds: warning;
- app/DB host time drift above 2 minutes: critical health condition;
- detected backward wall-clock jump above 5 minutes: critical and audited;
- detected forward wall-clock jump above 15 minutes: critical and audited.

These thresholds drive health warnings, diagnostics, and operator guidance. They do not silently alter payroll/work dates and do not by themselves disable core offline payroll processing.

Security-sensitive actions may reject or require review when an obviously invalid clock would make signatures, audit ordering, licensing evidence, or certificates unreliable, but such protection must not violate D-015/D-124 or block customer-owned data export.

The thresholds remain subject to controlled validation under V-017.

### D-161 — Disaster Recovery Uses Simple Documented Paths per Failure Class

Status: DECIDED

The baseline recovery matrix is:

- **Sudden power loss / unexpected application stop:** restart services; allow InnoDB crash recovery; validate MySQL and application health before normal operation resumes.
- **Corrupted application installation:** repair/reinstall the compatible application/runtime while preserving customer database/configuration under D-031; restore application files from package if needed.
- **Database service fails to start:** diagnose service/configuration/storage/error logs; correct supported environmental causes; restore a Recovery Set when the database cannot be returned safely to a known-good state.
- **Disk almost full:** raise escalating health warnings; stop/reject nonessential space-consuming jobs such as large reports/imports/upgrades/backups before critical exhaustion where practical; preserve payroll/database safety; expand/free storage before resuming blocked maintenance work.
- **Bad application update or schema migration:** use the verified pre-upgrade Recovery Set and prior compatible application package when safe forward recovery is not appropriate.
- **Bad Rule Pack update:** deactivate/rollback the pack for eligible open/future work under D-103/D-158; never rewrite finalized history.
- **Lost server/storage:** build a supported replacement host, install the compatible HRIS/MySQL baseline, restore the latest appropriate Recovery Set plus protected binary logs where applicable, validate, and perform license migration/reactivation under D-019/D-021.
- **Restore to replacement hardware:** validate OS/hardware/topology supportability, restore, rebind/reissue license as needed, then execute recovery smoke tests before reopening production.
- **Wrong system clock:** correct the host clock/time-zone source, audit the event, review security/licensing/certificate effects, and preserve deterministic payroll dates under D-123/D-124/D-160.
- **Extended internet outage:** core recruitment, attendance, payroll, statutory processing, reporting, local licensing validation, and local/external backups continue. Online update checks, offsite backup, and outbound support wait until connectivity returns.

Recovery documentation must be written as executable operator runbooks and exercised through the restore/upgrade qualification defined by D-156/D-157 rather than remaining aspirational prose.


### D-162 — Production MVP Is a Narrow End-to-End Staffing and Payroll Vertical Slice

Status: DECIDED

The first production MVP must support a real staffing agency from demand/recruitment through worker/deployment, time/attendance, payroll/compliance, billing support, reporting, and production operations. The product reduces MVP depth rather than removing payroll-critical dependency modules.

`MVP REQUIRED` capabilities are:

- Platform/Operations: agency configuration, audit, health/diagnostics, licensing, configuration and operational foundations;
- Identity & Access: authentication, users, roles, permissions, session/security controls;
- Client Management: clients, sites, and basic manpower requests;
- Recruitment: configurable basic pipeline, applicant/application handling, requirements and hiring handoff;
- Person Documents: document artifacts/metadata needed by applicant/worker workflows;
- Worker Master Data: Worker, Employment/Engagement, payroll-required leave and post-hire requirement state;
- Client Deployment: Position, Deployment, Assignment Terms and compensation terms required by payroll/billing;
- Scheduling: shifts and effective-dated schedule assignments needed for attendance/payroll;
- Attendance: canonical import/manual entry, immutable punches, interpretation, correction, approval and payroll locking;
- Payroll: groups, periods, runs, frozen inputs, deterministic calculation, earnings/deductions/statutory lines, review/approval/finalization, adjustments and payslips;
- Compliance Rules: Rule Pack import/validation/activation, exact rule-version resolution, pilot-required statutory calculations and verified outputs;
- Billing: basic billing-support snapshots, traceability and adjustments, not a full accounts-receivable/accounting suite;
- Reporting: essential operational, payroll, compliance, reconciliation and export outputs;
- Legacy import: XLSX/CSV staging, dry run, reconciliation and controlled commit;
- Production operation: installer/service setup, HTTPS, diagnostics, Recovery Sets/restore, supported upgrades and supportability preflight.

`DESIGN NOW / IMPLEMENT LATER` includes direct biometric-device integrations beyond generic adapters/imports, workflow/rules designers, advanced recruitment automation, schedule optimization, generalized report/BI designers, advanced AR/accounting, SSO/Active Directory, a general public API, generalized third-party integration infrastructure, global server push, reporting replicas, table partitioning, and generalized outbox/event infrastructure unless an earlier demonstrated requirement makes one necessary.

`DEFERRED FROM CORE MVP`: Client Portal only. P11 planning is now resolved by D-179–D-198; implementation remains post-core.

### D-163 — P10 Sets Planning-Final Launch Price Estimates and Requires Post-Pilot Repricing

Status: DECIDED

P10 establishes commercially usable launch-price estimates from the known MVP scope and support/delivery model. These amounts are product decisions and estimates, not external market facts or measured profitability results.

Because no measured founding-pilot production evidence was available at P10 close, V-008 remains mandatory: after the founding pilot and before the second normal commercial customer, re-evaluate license, maintenance, deployment, migration, support, feature and related economics using actual measured evidence.

### D-164 — MVP Delivery Uses M0–M6 Dependency-Ordered Milestones

Status: DECIDED

The implementation order is:

1. M0 — Engineering/Foundation;
2. M1 — Workforce Foundation;
3. M2 — Time & Attendance;
4. M3 — Payroll & Compliance;
5. M4 — Billing & Reporting;
6. M5 — Production Hardening;
7. M6 — Founding-Pilot Qualification and Go-Live.

Testing, auditing, security, documentation, migrations, synthetic data and operations are developed continuously where relevant rather than deferred wholesale until M5. No calendar delivery promise is made until implementation velocity and qualification effort are measured.

### D-165 — First Production Go-Live Has a Non-Negotiable Qualification Gate

Status: DECIDED

The first production activation requires all applicable MVP acceptance criteria plus, at minimum:

- representative end-to-end payroll/UAT completed;
- Compliance Rule Packs and statutory outputs used by the pilot verified for the actual applicable use cases;
- no unresolved Severity-1 payroll, data-integrity, security, restore or production-blocking product defect;
- production-like legacy-import dry run and reconciliation completed where migration is required;
- verified Recovery Set plus successful isolated restore qualification;
- supported application/schema upgrade and recovery path qualified;
- licensing, System Health and privacy-minimized diagnostic behavior validated;
- HTTPS, RBAC/authentication and other applicable security controls validated;
- administrator/payroll-user handover completed;
- Windows and Ubuntu release qualification completed for the supported product release, while the individual pilot installation only deploys its selected supported OS package;
- D-173 parallel-payroll/cutover requirements satisfied.

Unmeasured P7/P9 performance values remain `TARGET — NOT YET BENCHMARKED` and are not misrepresented as measured release results.

### D-166 — Affordability Is the Primary Launch-Pricing Objective

Status: DECIDED

The launch commercial strategy prioritizes affordability for Philippine staffing/manpower agencies while retaining perpetual licensing, annual maintenance, paid professional services and paid feature work.

Lower software pricing must not be achieved by weakening payroll/compliance correctness, security, recoverability, supported-environment requirements or the no-customer-specific-source-fork rule.

### D-167 — P10 Affordability-First Perpetual License Ladder

Status: DECIDED

**P10 launch pricing estimate as of 2026-09-29.**

| Band | Licensed Active Employees | Perpetual License Estimate |
|---|---:|---:|
| S | Up to 2,000 | ₱40,000 |
| M | 2,001–10,000 | ₱80,000 |
| L | 10,001–30,000 | ₱160,000 |
| XL | 30,001–100,000 | ₱320,000 |

D-002 remains the employee-count definition. Below 500 active employees remains outside the initial target market and does not create a micro-tier below S.

The approximate doubling between bands is intentionally simple to quote and administer. These are estimates subject to V-008 after the founding pilot.

### D-168 — Founding Pilot Pays the Normal Launch License Price with First-Year Maintenance Included

Status: DECIDED

The founding pilot no longer receives a percentage discount from list price. It pays the applicable D-167 launch license estimate and receives the first 12 months of maintenance included from production activation.

Deployment, migration, extra training, travel, integrations, unusual configuration and separately requested feature work remain separately billable when applicable.

The pilot is still expected to provide structured operational feedback and reasonable issue-reproduction/validation cooperation subject to privacy and support-access rules.

### D-169 — Affordability-First Annual Maintenance Uses Fixed Tier Amounts

Status: DECIDED

**P10 annual-maintenance estimate as of 2026-09-29.**

| Band | Annual Maintenance Estimate | Included Remote Support / Year |
|---|---:|---:|
| S | ₱15,000 | 2 hours |
| M | ₱25,000 | 4 hours |
| L | ₱50,000 | 6 hours |
| XL | ₱100,000 | 8 hours |

Maintenance is payable in advance for the maintenance term. First-year maintenance is included for the founding pilot under D-168; ordinary new-license terms continue to require active first-year maintenance unless a written commercial exception is approved.

D-016 remains authoritative: maintenance lapse never deactivates the perpetual license, but active maintenance is required for normal entitlement to new application releases, new Compliance Rule Packs, security updates and standard support.

Included support hours do not roll over. Additional/out-of-scope support remains separately billable under D-170. V-008 must validate whether these amounts are sustainable after real pilot support evidence exists.

### D-170 — P10 Professional-Service and Deployment Price Schedule

Status: DECIDED

**P10 professional-service pricing estimate as of 2026-09-29.**

Vendor-assisted deployment remains optional under D-025. The current schedule is:

| Service | P10 Price Estimate |
|---|---:|
| S deployment/setup package | ₱15,000 |
| M deployment/setup package | ₱25,000 |
| L deployment/setup package | ₱40,000 |
| XL deployment/setup package | ₱60,000 |
| Training — half day | ₱10,000 |
| Training — full day | ₱20,000 |
| Extended configuration/consulting | ₱2,500/hour or ₱20,000/day |
| Data migration | ₱2,500/hour or ₱20,000/day; ₱40,000 minimum after source-data assessment |
| Hardware/environment consultation | ₱10,000 remote review or ₱20,000/day onsite, plus travel where applicable |
| Extra support outside maintenance allowance | ₱2,500/hour, 2-hour minimum |
| Integration work | Written quote using D-036 baseline; ₱40,000 minimum for a separately delivered integration |

Self-installation carries no deployment-service fee when competent customer IT performs the work, but normal support still requires the D-035 supportability preflight to pass.

The deployment package retains the normal P3 scope: supported installation, bundled runtime, HRIS-managed MySQL or approved supported connection, service/firewall/LAN configuration, license activation/import, supportability preflight, health validation and administrator handover. Migration, data cleanup, additional training, travel, complex integration and unusual environment/configuration work remain outside the base package unless explicitly quoted.

### D-171 — Affordability Also Applies to Paid Reusable Feature Work

Status: DECIDED

Paid feature pricing must remain proportionate to the affordability-first license model rather than using the much higher P4 feature-price bands.

This does not make arbitrary customization cheap or mandatory. Requests that create disproportionate complexity, legal/compliance risk, large migration impact, major integration effort or weeks of development may be deferred, declined or custom-quoted.

### D-172 — P10 Paid Feature Price Bands

Status: DECIDED

**P10 paid-feature pricing estimate as of 2026-09-29.**

| Size | Typical Scope | Price Estimate |
|---|---|---:|
| Minor | Very small reusable enhancement/configurable behavior | ₱5,000–₱10,000 |
| Small | Focused reusable feature | ₱10,000–₱20,000 |
| Medium | Meaningful multi-screen/workflow enhancement | ₱25,000–₱50,000 |
| Large | Substantial module/workflow capability | From ₱60,000; written quote |

A generally reusable sponsored-roadmap feature may receive approximately a 20–30% sponsorship discount because the vendor retains the feature in the common product for reuse.

Normal product bug fixes and vendor-selected roadmap improvements are not converted into customer feature charges merely because the product uses low license pricing.

No customer-specific source-code fork is allowed. Customer-specific configuration continues to use existing product capability and is billed as professional service where appropriate.

### D-173 — Founding-Pilot Payroll Cutover Requires Parallel Payroll and Reconciliation

Status: DECIDED

Before the HRIS becomes the authoritative payroll process for the founding pilot:

1. required customer data is migrated and reconciled;
2. representative UAT is completed;
3. at least one complete payroll cycle is processed in parallel/shadow mode against the customer's trusted existing process;
4. every material discrepancy is investigated and reconciled;
5. another parallel cycle is required when material discrepancies remain unresolved;
6. authorized customer payroll personnel sign off before authoritative production cutover.

A controlled payroll group/population may be used first where operationally practical before expanding to the full eligible population.

### D-174 — Every MVP Milestone Uses a Production-Oriented Definition of Done

Status: DECIDED

M0–M6 milestone work is not complete merely because screens compile or the happy path runs. For applicable work, completion requires:

- acceptance criteria satisfied;
- appropriate unit/integration/architecture/golden tests passing;
- real-MySQL behavior tested where database/framework semantics matter;
- authorization/security/audit requirements enforced;
- database migration and recovery implications handled;
- affected documentation/ADRs/module/domain/task/bug/feature records updated;
- no known unresolved Severity-1 defect in the delivered milestone scope;
- material limitations/known issues recorded;
- relevant Windows/Linux portability checks passing;
- payroll/compliance behavior backed by versioned verified regression fixtures where applicable;
- D-148 repository Definition of Done satisfied.

### D-175 — Lower Price Levels Do Not Change the Existing Payment and Band-Upgrade Mechanics

Status: DECIDED

The P4 commercial mechanics remain unless explicitly superseded by P10 amounts:

- software license: normally 50% on signed order/contract and 50% before production activation;
- founding-pilot license: normally 50% at pilot kickoff and 50% before production go-live;
- annual maintenance renewals: payable in advance;
- deployment/professional services: normally 50% to schedule/start and 50% at delivery/handover;
- feature/integration work: normally 50% deposit and 50% on accepted delivery, with milestone billing permitted for larger work;
- the 30-day D-017/D-043 over-band grace period remains;
- persistent over-band status requires upgrade to the appropriate band without blocking core operation;
- permanent band-upgrade fee remains the difference between the then-current list prices of the old and new bands, with maintenance adjusted reasonably for the new band/remaining term;
- later headcount reduction does not create an automatic refund of perpetual-license fees.

### D-176 — P10 Product-Delivery Risks Are Prioritized by Payroll/Data Safety First

Status: DECIDED

Risk prioritization is:

**Critical:** payroll correctness; statutory-rule correctness/change management; backup/restore failure; bad data migration/cutover.

**High:** solo-developer continuity; scale/performance at L/XL; poor customer hardware/environment quality; excessive support burden.

**Moderate:** Windows/Linux support overhead; licensing/commercial disputes.

Mitigations must use existing architecture/governance rather than adding unnecessary infrastructure: deterministic/golden payroll tests, verified Rule Packs/Compliance Register, parallel payroll, Recovery Sets/restore drills, staged/reconciled imports, synthetic performance qualification, supportability preflight, health/diagnostics, repository documentation/runbooks, narrow MVP scope, explicit support boundaries and non-destructive licensing.

### D-177 — M0–M6 Milestone Scope Is Implementation-Ready

Status: DECIDED

The milestone contents are:

- **M0 — Engineering Foundation:** repository/Maven module skeleton, CI, architecture tests, MySQL/Testcontainers baseline, Flyway, common IDs/time/money primitives, audit foundation, configuration/secrets conventions, synthetic-data generator skeleton, task/ADR/documentation structure.
- **M1 — Workforce Foundation:** Agency/platform basics, Identity/RBAC, Client/Site/Manpower Request, basic Recruitment pipeline and hiring handoff, Person Documents, Worker/Employment, Position/Deployment/Assignment Terms and compensation terms.
- **M2 — Time & Attendance:** Shift/schedule assignment, payroll-required Leave, attendance ingestion/import/manual entry, punch evidence, Attendance Day interpretation, corrections, approval/review/payroll-lock behavior.
- **M3 — Payroll & Compliance:** Payroll Group/Period/Run/Result, frozen snapshots, deterministic calculation, earnings/deductions/statutory lines, Compliance Rule Packs/resolution, review/approval/finalization, later adjustments and payslips.
- **M4 — Billing & Reporting:** basic billing-support cycles/snapshots/adjustments plus essential operational, payroll, compliance, reconciliation and export/report outputs.
- **M5 — Production Hardening:** Windows/Ubuntu packaging/service behavior, HTTPS/security qualification, licensing, System Health/diagnostics, backup/restore, update/Flyway recovery qualification, legacy migration tooling, release/installer qualification and operator runbooks.
- **M6 — Founding-Pilot Qualification and Go-Live:** customer migration dry run/reconciliation, UAT, D-173 parallel payroll, discrepancy closure, restore drill, administrator/payroll-user training, supported-environment final preflight, go-live checklist/sign-off and production activation.

Each milestone is split into focused `TASK-####` work and reviewable PRs/release increments; a milestone is not implemented as one giant pull request.

### D-178 — Founding-Pilot Onboarding Is a Structured Controlled Process

Status: DECIDED

The standard founding-pilot onboarding sequence is:

1. scope and supported-environment assessment;
2. customer source-data/migration assessment;
3. installation/supportability preflight;
4. configured test/UAT environment;
5. migration dry run and reconciliation;
6. administrator and payroll-user training;
7. D-173 parallel payroll and discrepancy resolution;
8. signed go-live checklist/acceptance;
9. production activation;
10. defined stabilization period followed by normal maintenance/support rules.

Major data cleanup, bespoke integration, additional training, onsite travel, unusual environment work and new feature development are outside the standard onboarding scope unless separately quoted.

### D-179 — Client Portal V1 Is a Deliberately Small Optional Add-on

Status: DECIDED

The first Client Portal version is outside the core MVP and includes only:

- deployed headcount/status visibility by permitted client/site scope;
- timesheet review with approve/reject/comment actions;
- read-only billing summaries.

Initial portal scope excludes payroll results/payslips, statutory details, recruitment, employee self-service, personnel documents, invoice/payment processing, schedule editing, employee editing, and attendance-punch editing.

The portal is implemented only after the on-premises core is stable.

### D-180 — On-Premises HRIS Remains Authoritative and Portal Failure Cannot Block Payroll

Status: DECIDED

The on-premises HRIS/database remains the authoritative system of record for every HR, attendance, payroll, deployment, billing-support, and related business record.

Cloud portal data consists only of minimized projections plus pending client actions. A portal action becomes authoritative only after the on-premises installation retrieves, validates, and commits it through the owning application service.

Portal/internet failure must never stop core HRIS operation or payroll. Portal approval is an additional approval channel, not the only recovery path. Authorized HRIS users retain an audited local review/approval or override path when portal connectivity is unavailable.

### D-181 — Portal Cloud Isolation Uses One Isolated Stack per Agency and a Separate Deployable Application

Status: DECIDED

Each agency portal uses an isolated cloud deployment boundary generated from the same supported application image/infrastructure template. The baseline provides dedicated agency portal data storage/database, tenant credentials/secrets, hostname/tenant identity, and isolated backup namespace rather than a shared authoritative multi-agency operational database.

The public Client Portal is a separate cloud-deployed application from the on-premises HRIS. It may remain in the Java/Spring ecosystem. Shared code/artifacts are limited to versioned protocol DTO/contracts and neutral primitives; the portal must not import on-premises JPA entities or business repositories.

No customer-specific portal source-code fork is permitted.

### D-182 — Portal Identities Are Separate and Agency-Provisioned

Status: DECIDED

Client Portal identities are distinct from on-premises HRIS User identities. They may carry stable references needed for audit/integration but do not share on-premises passwords or authentication state.

Initial portal roles are:

- Client Viewer;
- Timesheet Approver;
- Billing Viewer.

Agency administrators provision/invite portal users and scope them to allowed Client Company/Site records. Public self-registration is prohibited. Client-company delegated user administration is DESIGN NOW / IMPLEMENT LATER.

For the internet-facing portal, prefer a managed standards-based OIDC identity service with MFA capabilities rather than building full public account recovery and identity-security infrastructure from scratch. Exact provider, region, contractual terms, and current security capabilities remain implementation-time verification items.

### D-183 — Portal Synchronizes Only the Minimum Data Required for V1

Status: DECIDED

Portal synchronization is allow-list based. V1 may synchronize only fields necessary for the approved portal capabilities, such as:

- client-facing worker display name/identifier;
- client/site/deployment public identifiers and permitted status/headcount data;
- work date;
- summarized time/hours classifications required for review;
- timesheet version and approval state;
- billing-period identifiers and client billing-summary amounts;
- public UUIDs and synchronization/version metadata.

Do not synchronize government identifiers, bank data, home addresses, unnecessary personal contacts, payroll earnings/deductions, statutory contribution/tax details, applicant data, personnel documents, payslips, or raw biometric punches merely for convenience.

A future portal feature that needs additional fields requires an explicit synchronization-contract expansion and privacy/security review.

### D-184 — Portal Timesheet Actions Are Approve, Reject, and Comment; Clients Do Not Edit Authoritative Hours

Status: DECIDED

Portal clients may approve, reject, and comment on the specific synchronized timesheet version presented to them. They do not directly edit authoritative attendance hours in V1.

A rejection returns the business item to the agency. Corrections occur in the authoritative Attendance workflow under the existing evidence/history rules and a corrected version is synchronized again for review where required.

### D-185 — Portal Synchronization Is Outbound-Only over HTTPS with a Durable Local Outbox

Status: DECIDED

The agency installation accepts no inbound internet connection for portal operation.

The on-premises `portal-sync` capability initiates outbound HTTPS communication. Portal-relevant committed changes are recorded in a durable transactional/persisted outbox or equivalent module-owned handoff. The sync worker uploads pending projection changes and, in the same outbound communication pattern, polls/downloads pending portal commands for local validation and commit.

No inbound callback, public on-premises API, reverse tunnel, VPN requirement, Kafka, distributed broker, or cloud-to-LAN connection is part of the baseline portal design.

This portal requirement activates the persisted-handoff/outbox mechanism previously kept DESIGN NOW / IMPLEMENT LATER under D-110, but only for portal/integration delivery that requires guaranteed retry after commit.

### D-186 — Portal Sync Is Durable, Idempotent, Retryable, and Eventually Consistent

Status: DECIDED

Every portal sync event/command carries at least:

- immutable event/command UUID;
- installation/agency portal identity;
- affected entity public ID;
- entity/projection version;
- creation time;
- idempotency/deduplication identity.

The cloud retains receipt/inbox metadata needed to make repeated deliveries harmless. The on-premises installation retains outbox state plus a processed-command ledger/receipt sufficient to prevent duplicate business effects.

Normal planning default is approximately one-minute polling while healthy when portal work is active, with bounded/exponential retry backoff during failures and durable catch-up after extended outage. The interval is configurable and is not a contractual latency guarantee.

### D-187 — Portal Conflicts Reject Stale Writes and the UI Must Expose Staleness

Status: DECIDED

Portal command handling uses optimistic version validation. Last-write-wins is prohibited for approval/business commands.

If a portal user approves/rejects version N but the authoritative on-premises item has advanced to version N+1, the stale command is rejected/recorded without overwriting the newer state; the latest projection is synchronized and fresh review is required when applicable.

The portal displays the last successful synchronization timestamp. Initial product behavior should show a prominent stale-data warning after approximately 15 minutes without successful synchronization, or sooner when a known synchronization failure exists. The threshold is configurable.

### D-188 — Portal Cloud Retention Is Intentionally Short and Configurable

Status: DECIDED

Initial product defaults are:

- detailed timesheet projections: 90 days;
- billing-summary projections: 12 months;
- completed portal-action/audit metadata: 12 months;
- pending commands: retained until successfully processed plus a short diagnostic retention period.

The authoritative long-term business record remains on-premises. These are product data-minimization defaults, not assertions of Philippine statutory retention periods, and they may be changed where verified legal/contract/customer policy requires a different period.

### D-189 — Portal Security Uses Internet-Facing Least-Privilege Controls and Encryption

Status: DECIDED

Portal baseline security includes:

- HTTPS only;
- encryption at rest for portal data storage and backups;
- secrets/keys outside source code and ordinary readable configuration;
- least-privilege service identities;
- per-agency isolation under D-181;
- MFA capability for privileged portal administration;
- bounded authentication/session controls;
- maintained security/dependency patching;
- privacy-minimized logs;
- no public database listener;
- no reuse of on-premises HRIS credentials.

Portal security incidents must not create an inbound technical path to the on-premises installation.

### D-190 — Portal Actions Are Audited in Cloud and Authoritatively Recorded On-Premises

Status: DECIDED

The cloud temporarily records sufficient audit metadata for portal operation and investigation, including portal actor identity, action, timestamp, affected public identifier/version, and processing result.

After synchronization, the authoritative HRIS audit trail records the accepted/rejected portal command, portal actor/reference, version, processing actor/system context, and resulting business effect where applicable.

Cloud audit retention does not replace the existing on-premises append-only audit requirements.

### D-191 — Portal Compatibility Follows the Supported HRIS Release Window and Requires Active Maintenance

Status: DECIDED

Portal protocol contracts are explicitly versioned and support capability/version negotiation.

Normal portal compatibility follows the application support window from D-018: current release line plus immediately previous supported release line. An incompatible/unsupported installation suspends portal synchronization with a clear administrator warning but does not block local HRIS operation or customer data access.

Use of the hosted portal requires:

- an active portal subscription;
- active on-premises annual maintenance; and
- a supported HRIS application/protocol version.

Lapse of maintenance or portal entitlement never deactivates the perpetual on-premises HRIS license under D-016.

### D-192 — Portal Hosting Uses One Supported Nearby Region without an Initial Philippine-Only Promise

Status: DECIDED

The initial portal offering does not promise Philippine-only hosting. Select one supported Southeast Asian or otherwise suitably nearby region/provider deployment that meets required privacy/security/operations needs and disclose the relevant data-location posture contractually.

Exact cloud provider, available region, service terms, resilience characteristics, managed identity service, and data-transfer/storage economics must be verified before portal implementation/launch.

### D-193 — Portal V1 Avoids SMS, Personnel Documents, and Behavioral Analytics

Status: DECIDED

V1 may use email only for account/security/invitation functions required to operate the portal. Workflow reminder email is DESIGN NOW / IMPLEMENT LATER. SMS is not part of V1.

The portal is not a personnel-document repository and does not synchronize HR documents or payslips. Billing summary display/export may be generated from the minimized synchronized billing-summary projection.

Third-party advertising/behavioral analytics are not embedded by default. Operational/security telemetry is limited to what is needed for service health, synchronization diagnosis, security, capacity, and support.

### D-194 — Portal Is a Separate Annual Subscription Using the Existing S/M/L/XL Bands

Status: DECIDED

The Client Portal is commercially separate from the perpetual on-premises license and is sold as an annual subscription per agency installation.

**P11 portal subscription planning estimate as of 2026-09-29:**

| Band | Annual Portal Subscription Estimate | Approx. Monthly Equivalent |
|---|---:|---:|
| S | ₱24,000/year | ₱2,000/month |
| M | ₱36,000/year | ₱3,000/month |
| L | ₱60,000/year | ₱5,000/month |
| XL | ₱96,000/year | ₱8,000/month |

The subscription covers normal portal hosting, portal software updates, cloud backup/operations, and ordinary portal support within the supported service scope. Major migration, unusual configuration, integration, travel, and separately requested feature work remain separately billable.

These are product pricing estimates, not external market facts or measured hosting-profitability evidence, and must be revisited under V-028 after real operating data exists.

### D-195 — Initial Portal Pricing Has No Per-User or Per-Client-Company Meter

Status: DECIDED

Portal pricing initially uses the agency installation's S/M/L/XL active-employee band only. There is no per-seat, per-client-company, or per-login charge in the baseline model.

Reasonable portal-user volumes are included. Abnormally large or unusual usage may be handled through a written enterprise/custom operating quote if measured hosting/support cost justifies it.

### D-196 — Portal Expiry and Termination Never Affect the Core HRIS

Status: DECIDED

Portal subscription expiry uses a 30-day commercial grace period unless a written agreement states otherwise. After grace, portal synchronization/access may be disabled, but the on-premises HRIS remains fully usable.

The agency must retain the ability to export appropriate portal administration/audit information before final disposal where needed.

Initial product default is deletion of remaining tenant cloud projection data approximately 30 days after final portal termination, subject to contractual, incident, dispute, legal-hold, privacy, or other verified retention obligations. Reactivation after deletion may require rebuilding projections from the authoritative on-premises HRIS.

### D-197 — Initial Portal Launch Has No Contractual Uptime SLA

Status: DECIDED

The initial portal offering does not promise a contractual uptime SLA. The first operating objective is approximately 99.5% monthly portal availability excluding planned maintenance and agreed exclusions.

**TARGET — NOT YET BENCHMARKED**

The service still requires monitoring, backups, incident handling, and customer communication appropriate to a paid hosted add-on. A formal SLA/premium service tier is introduced only after measured operating evidence and support capacity justify it.

### D-198 — Portal Tenant Provisioning Is Vendor-Assisted and Automated, Not Public Self-Service

Status: DECIDED

Initial portal tenant activation is vendor-assisted through repeatable automated deployment/provisioning tooling. Public customer self-service tenant creation/billing provisioning is not part of V1.

After activation, agency administrators manage normal portal-user invitations and access within D-182. This keeps initial abuse prevention, billing automation, domain verification, and cloud provisioning complexity within solo-developer support capacity.

## P5 DOMAIN MODEL SUMMARY

### Core Module Ownership

| Module / Capability | Authoritative Concepts |
|---|---|
| Platform / Operations | Agency installation-level root; common operational configuration; common audit facility ownership |
| Client Management | Client Company; Client Site/Location; Client Manpower Request / Job Requisition |
| Recruitment | Applicant; Application/Pipeline; configurable Stage definitions; recruitment outcomes; pre-hire requirement status |
| Person Documents (shared capability) | Document artifacts and document metadata |
| Worker Master Data | Worker; Employment/Engagement; Leave; post-hire requirement status |
| Client Deployment | Position/Job; Deployment/Assignment; Assignment Terms; Assignment Compensation Terms; transfer history |
| Scheduling | Shift; rotation/pattern; effective-dated schedule assignments; schedule conflicts |
| Attendance | Attendance Punch; Attendance Day; Attendance Adjustment; attendance review/approval/payroll-lock state |
| Payroll | Payroll Group; Payroll Period; Payroll Run; Payroll Result; earning/deduction/statutory result lines; Payslip issuance metadata |
| Billing | Billing Rule; billing cycle/support snapshot; billing adjustment; invoice-support data |
| Compliance Rules | Compliance Rule Packs; effective-dated statutory rules; rule-version metadata |
| Identity & Access | User; Role; Permission; authentication state; authorization assignments |
| Reporting | Read-only cross-module queries/projections; no authoritative transactional ownership |

### Major Workflow Invariants

- Applicant and Application history survive hiring; hiring creates/links Worker and a new Employment/Engagement.
- Rehire reuses Worker identity but creates a new Employment/Engagement period.
- Employment may be active deployed or active undeployed/bench; deployment state derives from actual active assignments.
- A Worker/Employment may have multiple concurrent Deployments, but each payable attendance segment resolves to one deployment context.
- Material client/site/position transfer creates a successor Deployment rather than rewriting assignment history.
- Effective separation closes Employment/Engagement and prevents Deployments/schedules beyond the separation date.
- Deployment activation requires a valid active Employment/Engagement and effective Client/Site/Position prerequisites.
- Scheduling resolves deployment-specific schedules first; Attendance consumes the resolved schedule.
- Raw Attendance Punches are preserved; corrections are Attendance Adjustments; Attendance Day is the authoritative interpreted daily outcome.
- Payroll consumes approved/frozen attendance outputs and a frozen/versioned payroll-input snapshot.
- Payroll Run progresses through controlled preparation/calculation/review/approval/finalization states.
- Finalized Payroll Result and its financial lines are immutable; retroactive corrections use later adjustments/reversals.
- One Employment has one effective Payroll Group at a time; one Payroll Run produces one consolidated Payroll Result per Worker/Employment.
- Payslip is a projection of finalized Payroll Result, not an editable financial source.
- Compensation and Billing Rules are independent effective-dated histories.
- Billing freezes cycle-specific support data; finalized billing support is corrected by explicit adjustment, not silent recomputation.
- Retroactive corrections begin in the owning source domain; open downstream periods may recalculate, finalized downstream financial records receive explicit adjustments.
- Compliance rules are resolved by effective date and finalized Payroll records retain the exact Rule-Pack/rule version used.
- Single-value effective-dated rules cannot overlap ambiguously unless explicit priority/combination semantics exist.
- Cross-module workflows use explicit service operations and stable IDs; modules never directly write another module's aggregates.
- Reporting remains read-oriented and cannot become a second source of transactional truth.

## P6 COMPLIANCE AND PRIVACY SUMMARY

### Compliance Rule-Pack Boundary

- Application code owns stable calculation semantics, validation framework, supported declarative rule types, signature verification, rule resolution, audit behavior, and payroll integration.
- Rule Packs own changeable statutory values/configuration such as contribution/withholding tables, thresholds, calendars, effective dates, classifications, parameters, and versioned output mappings where the supported declarative model is sufficient.
- Arbitrary executable code is never accepted from a Rule Pack.
- A genuinely new statutory calculation method that exceeds the supported rule model requires an application release rather than unsafe scripting.
- Statutory resolution is based on the verified applicability basis for that rule family; there is no single assumed date rule for every statute.
- Finalized Payroll Results permanently retain the exact rule version used and are never silently recomputed after a Rule Pack update.

### Compliance Register — Authoritative Source Categories

| Rule / Output Family | Primary Authoritative Source Category | P6 Status |
|---|---|---|
| SSS | SSS law, official SSS circulars, contribution schedules and employer guidance | Architecture RESOLVED; current values/requirements VERIFY per release |
| PhilHealth | Applicable law/IRR, official PhilHealth circulars/advisories and employer guidance | Architecture RESOLVED; current values/requirements VERIFY per release |
| Pag-IBIG | Applicable law/rules and official Pag-IBIG Fund circulars/guidelines | Architecture RESOLVED; current values/requirements VERIFY per release |
| BIR withholding | Tax law plus current BIR Revenue Regulations, Revenue Memorandum issuances, official withholding tables, forms and file specifications | Architecture RESOLVED; current values/formats VERIFY per release |
| Holiday/premium pay | Applicable labor law/rules, Official Gazette proclamations where relevant, and current DOLE Labor Advisories/issuances | Architecture RESOLVED; each calendar/pay rule VERIFY per applicable period |
| Other statutory payroll parameters | Controlling statute/regulation/official agency issuance for the specific rule | VERIFY individually before implementation/release |
| Data privacy | Republic Act No. 10173, its IRR, and current National Privacy Commission issuances | Data-model posture RESOLVED; deployment/legal interpretation VERIFY as needed |

### Privacy Role/Data-Model Boundary

- Customer agency normally controls the purpose/use of workforce data and is modeled as PIC for that processing.
- Vendor is not automatically a PIP merely because it licenses/supports on-premises software; the PIP role attaches when the vendor actually processes customer personal data under customer instructions.
- Support defaults to technical metadata without employee/payroll contents.
- Personal-data access is least-privilege, purposeful, minimized, and auditable.
- Retention is category/basis-specific; no universal statutory retention period is assumed.
- P9 completes operational security, encryption, backup, breach-response, and deletion mechanics.


## P7 APPLICATION ARCHITECTURE SUMMARY

### Core Runtime Shape

- One Maven-built Spring Boot modular monolith produces one executable application artifact.
- JPA/Hibernate is the ordinary persistence layer; module-owned JDBC is permitted for measured high-volume paths.
- Spring Batch with a JDBC JobRepository provides durable/restartable processing for payroll, large imports, and large reports.
- MySQL remains the only operational database. No broker, Redis, reporting replica, sharding, or distributed cache is required initially.
- S/M/L normally remain single app+DB host deployments under D-007; XL uses the existing separate app/DB baseline without changing the application architecture.

### Payroll Scalability

- Payroll freezes/identifies a versioned input snapshot, resolves exact Rule Pack versions, then calculates workers in restartable chunks.
- Worker calculations are deterministic from frozen inputs and versioned rules.
- Chunk persistence is idempotent and bounded; no payroll-wide database transaction is permitted.
- Finalization is run-level serialized and cannot proceed with incomplete or failed work.
- Initial full-run targets range from <=5 minutes at S to <=90 minutes at XL on recommended hardware.

**TARGET — NOT YET BENCHMARKED**

### Attendance Scalability

- All attendance sources enter through a canonical ingestion pipeline.
- Raw Punch evidence remains immutable; duplicates use stable source IDs or deterministic fingerprints.
- Late imports recalculate only eligible open Attendance Days or create controlled downstream exceptions/adjustments.
- Normal indexed InnoDB tables are the baseline; partitioning remains evidence-driven.

### MySQL and Vaadin Scalability

- Query/index discipline, projections, batching, bounded connection pools, and measured InnoDB sizing come before topology complexity.
- Vaadin grids are lazy and bounded; no full-table loading is allowed.
- Long-running work leaves the UI thread and exposes persisted progress.
- Polling is the initial progress mechanism; push remains optional.
- Vaadin session-memory budgets are explicit performance targets for P8 validation.

### Reporting and Time Reliability

- Large reports/exports/PDF batches are durable background jobs with streaming generation and concurrency throttling.
- Payroll workloads receive priority during contention.
- One installation-level IANA business time zone is authoritative; UTC instants and local business dates are stored according to their meaning.
- NTP is recommended, not required for offline operation; clock problems are health conditions, while payroll calculations use explicit business dates rather than hidden wall-clock dependence.

### Application-Level OS Portability

- Paths, filename casing, locale, text encoding, line endings, fonts, and generated-file behavior are controlled by application code/configuration rather than host defaults.
- PDFs use bundled/embedded redistributable fonts for deterministic Windows/Linux output.
- Statutory formats remain authoritative over generic file defaults and are still verified under V-010.


## P8 AI DEVELOPMENT WORKFLOW AND REPOSITORY GOVERNANCE SUMMARY

### Repository as Durable Memory

- Private GitHub is the canonical source repository; GitHub Actions provides ordinary CI, while Maven/local scripts remain the reproducible build/test interface.
- Protected `main` is human-merge-controlled. AI agents work through branches/worktrees and pull requests.
- Root `AGENTS.md` is a short global contract. Module-level agent guidance is selective and initially most useful for Payroll, Attendance, Compliance Rules, and migrations.
- Meaningful work uses durable `TASK-####.md` files. Architecture changes use immutable/superseding `ADR-####` records.
- Documentation is split into indexed domain/module/compliance/deployment/migration/release/support/OS/performance/bug/feature/task/known-issue areas rather than one giant context file.

### Registries and Traceability

- Material defects use `BUG-####` records with affected/fixed versions, OS/tier/module, symptoms, reproduction, root cause, fix, regression test, migration implications, and customer-specific/general classification.
- Material requests use `FR-####` records with customer/business reason, capability/module, size/quote/approval, development/target-release state, roadmap classification, and configuration/feature-flag treatment.
- GitHub Issues and PRs may provide workflow convenience but do not replace repository records that carry durable product/release knowledge.

### Testing and CI

- Tests are layered from unit through service/domain integration, real-MySQL repository/integration, migration, payroll golden/regression, upgrade, backup/restore once P9 defines it, dual-OS portability, and load/performance qualification.
- Testcontainers/ephemeral MySQL is the normal way to validate MySQL-specific behavior; H2 is not treated as MySQL-compatible proof.
- PR CI remains bounded and fast enough for solo development. Heavy XL, installer, upgrade/restore, and full performance suites run scheduled/on-demand and before release.
- Architecture rules are executable through Maven/ArchUnit-style tests rather than being documentation-only.

### Synthetic Data and Performance Regression

- Development/CI/AI context does not use real production/customer data. Real incidents become sanitized synthetic regression scenarios.
- The synthetic-data generator is versioned and deterministic by generator version + named S/M/L/XL profile + seed + scenario manifest. It supports at least 100,000 active employees plus historical workers, clients/deployments, schedules, attendance, payroll groups/history, billing/compliance structures, and audit volume.
- Large data is generated/restored on demand, not committed as giant dumps.
- Authoritative performance qualification runs on controlled self-hosted hardware with full environment metadata.
- Approved comparable performance baselines are stored in Git; hard regression gates are introduced only after repeatable measured baselines exist.

**TARGET — NOT YET BENCHMARKED**


## P9 SECURITY, BACKUP, RECOVERY, UPDATES, AND OPERATIONS SUMMARY

### Security and Operational Privacy

- Normal LAN access uses HTTPS with customer-local trust; plain HTTP is not the general LAN mode.
- Identity & Access retains RBAC ownership. Password storage uses a benchmarked adaptive one-way hash, bounded login/session controls, and optional offline-capable TOTP for privileged accounts.
- OS full-volume encryption is recommended where practical; removable/offsite customer-data backups must be strongly encrypted and recoverable through protected key escrow/recovery material.
- Vendor support keeps privacy-minimized diagnostics as the default and has no standing customer-personal-data access.
- Security/privacy incidents use a documented containment, evidence, assessment, recovery, review, and legally verified notification workflow.

### Database, Backup, and Recovery

- InnoDB crash recovery is the first response to normal unclean shutdown; unsupported ad-hoc data-file repair is not a routine recovery method.
- Backups are complete versioned Recovery Sets covering MySQL plus configuration, Rule Packs, external document/file storage, license/recovery metadata, and integrity manifests as applicable.
- Default backup-generation retention is 7 daily + 4 weekly + 12 monthly, independent from legal/business record-retention requirements.
- Separate external/NAS storage is the standard second copy; encrypted offsite backup remains optional.
- L/XL use protected binary logs for tighter point-in-time-recovery objectives; S/M may enable the same mechanism.
- Backup integrity is checked automatically and isolated restore drills are mandatory on a recurring schedule.

### Recovery Targets

| Tier | Full Backup Target | RPO Target | Restore Duration Target | RTO Target* |
|---|---:|---:|---:|---:|
| S | <= 30 minutes | <= 24 hours | <= 1 hour | <= 4 hours |
| M | <= 60 minutes | <= 12 hours | <= 2 hours | <= 6 hours |
| L | <= 2 hours | <= 4 hours | <= 4 hours | <= 8 hours |
| XL | <= 4 hours | <= 1 hour | <= 8 hours | <= 12 hours |

`*` Hardware procurement/replacement lead time is outside the RTO measurement.

**TARGET — NOT YET BENCHMARKED**

### Updates, Migrations, and Legacy Import

- Flyway is the schema-migration baseline; released migrations are forward and immutable.
- Every application/schema upgrade requires package validation, preflight, a verified pre-upgrade Recovery Set, maintenance mode, migration/schema validation, and post-upgrade smoke checks.
- Rollback of a failed application/schema upgrade is restore-based rather than an assumed in-place schema downgrade.
- Large-table migrations use bounded/chunked or expand/backfill/contract techniques when required by measured scale.
- Rule Pack updates remain independent from application releases while preserving signature/compatibility/activation/rollback controls.
- Legacy Excel/CSV/old-HRIS imports use staging, field mapping, validation, duplicate detection, dry run, reconciliation, approval, idempotent chunked commit, provenance, and recovery protection.

### Clock and Disaster Operations

- Initial clock-health thresholds are 30-second warning / 2-minute critical app-vs-DB drift, >5-minute backward jump critical, and >15-minute forward jump critical.
- NTP/internet loss does not stop core operation; explicit business dates remain authoritative for payroll.
- Power loss, application corruption, DB startup failure, disk exhaustion, bad app/schema/Rule Pack updates, server loss, hardware replacement, wrong clocks, and prolonged internet loss all have explicit recovery paths under D-161.


## P10 MVP, ROADMAP, COMMERCIAL AND DELIVERY-RISK SUMMARY

### MVP Maturity Boundary

- `MVP REQUIRED`: the complete narrow staffing/payroll vertical slice in D-162, including production operations and basic billing support.
- `DESIGN NOW / IMPLEMENT LATER`: convenience/automation/integration/scale complexity that is not required for first-customer payroll correctness or production safety.
- `DEFERRED FROM CORE MVP`: Client Portal only; P11 planning is complete and implementation remains post-core.
- The MVP intentionally limits depth rather than removing payroll-critical modules.

### Delivery Roadmap

| Milestone | Primary Outcome |
|---|---|
| M0 | Engineering/build/data/test foundation |
| M1 | Client, recruitment, worker and deployment foundation |
| M2 | Scheduling, leave and attendance ready for payroll |
| M3 | Deterministic payroll + Compliance Rule Pack processing |
| M4 | Basic billing support + essential reporting |
| M5 | Deployable, secure, recoverable, supportable Windows/Linux product |
| M6 | Qualified founding-pilot migration, parallel payroll and production go-live |

No calendar-duration commitment is recorded. Milestones are decomposed into small task files/PRs and close only under D-174.

### First-Production Release Gate

Before first production use, the release/customer deployment must satisfy D-165 and D-173, including payroll/UAT, current applicable compliance verification, migration reconciliation, isolated restore, upgrade/recovery qualification, security/licensing/diagnostics validation, administrator handover and at least one reconciled parallel payroll cycle.

### P10 Launch Pricing — ESTIMATES

| Item | S | M | L | XL |
|---|---:|---:|---:|---:|
| Perpetual license | ₱40,000 | ₱80,000 | ₱160,000 | ₱320,000 |
| Annual maintenance | ₱15,000 | ₱25,000 | ₱50,000 | ₱100,000 |
| Included support/year | 2h | 4h | 6h | 8h |
| Assisted deployment | ₱15,000 | ₱25,000 | ₱40,000 | ₱60,000 |

Founding pilot pays the applicable normal launch license price with first-year maintenance included. Self-installation has no deployment-service charge when the supported-environment preflight passes. Feature-price bands are defined by D-172. Professional time remains based on D-036/D-170.

All monetary values above are planning/commercial estimates, not external market facts or measured profitability evidence. V-008 requires post-pilot repricing before the second normal commercial customer.

### Prioritized Product-Delivery Risk Register

| Priority | Risk | Concrete Mitigation |
|---|---|---|
| Critical | Payroll correctness | Frozen inputs, deterministic calculation, golden/regression fixtures, controlled finalization, parallel payroll and customer sign-off |
| Critical | Statutory changes/correctness | Signed effective-dated Rule Packs, Compliance Register, official-source verification, regression tests, independent update lifecycle |
| Critical | Backup/restore failure | Complete Recovery Sets, integrity checks, isolated restore drills, pre-upgrade/pre-import protection, first-go-live restore gate |
| Critical | Data migration/cutover | Staging, dry run, duplicate review, reconciliation, provenance, idempotent commit, Recovery Set, parallel payroll |
| High | Solo-developer continuity | Repository as durable memory, task/ADR/module/runbook docs, reproducible build/test/release processes, explicit operational handover |
| High | Scale/performance | Deterministic S/M/L/XL synthetic workloads, controlled benchmark qualification, measured tuning; targets remain unbenchmarked until proven |
| High | Customer hardware/environment quality | Tier minimums, preflight, System Health/diagnostics, supported topology/OS matrix, explicit supportability boundary |
| High | Excessive support load | Narrow MVP, diagnostics-first support, limited included support, separately billed out-of-scope work, no customer source forks |
| Moderate | Windows/Linux support burden | Small OS matrix, one codebase/build, thin OS wrappers, risk-based dual-OS CI and release qualification |
| Moderate | Licensing disputes | Precise D-002 counting, signed local license, 30-day grace, audit trail, difference-based upgrades and non-destructive enforcement |
## P11 CLIENT PORTAL ADD-ON SUMMARY

### Scope and Authority

- Client Portal remains optional and outside the core MVP.
- V1 is limited to deployed headcount/status visibility, timesheet approve/reject/comment, and read-only billing summaries.
- The on-premises HRIS/database remains authoritative; cloud records are minimized projections and pending client actions only.
- Portal/internet failure never blocks core HRIS or payroll. Authorized local audited fallback remains available.
- Client users do not edit authoritative attendance hours in the portal.

### Cloud and Synchronization Architecture

- The public portal is a separate deployable application with one isolated cloud stack/data boundary per agency, produced from common vendor-managed templates/images.
- The agency installation accepts no inbound internet connection for portal operation.
- On-premises sync initiates outbound HTTPS, publishes committed changes through a durable persisted outbox, and polls/downloads pending portal commands for local validation/commit.
- Delivery is idempotent and retryable using immutable command/event IDs, public entity IDs, versions, and processed-command receipts.
- Stale client commands are rejected through optimistic version checks; last-write-wins is prohibited for approval actions.
- Portal shows last successful synchronization time and warns when data is stale.

### Identity, Privacy, Security, and Retention

- Portal identities are separate from on-premises HRIS Users; agency administrators provision scoped client access.
- Prefer a managed standards-based OIDC identity provider with MFA capability for internet-facing authentication; exact provider/region is verified before implementation.
- Cloud synchronization is allow-list/minimum-data based and excludes payroll details, government IDs, bank data, personnel documents, applicant data, payslips, and raw biometric evidence from V1.
- Portal uses HTTPS, encrypted data/backups, least-privilege service identities, isolated secrets, privacy-minimized logs, and no public database listener.
- Portal actions are temporarily audited in cloud and authoritatively recorded in the on-premises audit trail after processing.
- Cloud retention defaults are deliberately short and configurable; authoritative history remains on-premises.

### Compatibility and Commercial Model

- Portal protocol is versioned and normally supports the current and immediately previous HRIS release lines.
- Portal use requires an active portal subscription, active on-premises maintenance, and a supported HRIS/protocol version. Expiry never deactivates the perpetual local HRIS.
- Initial portal hosting does not promise Philippine-only data residency; one suitable nearby supported region is chosen and disclosed after verification.
- Portal is sold separately as an annual per-agency subscription using the existing S/M/L/XL active-employee bands: S ₱24,000; M ₱36,000; L ₱60,000; XL ₱96,000 per year.
- There is no initial per-user or per-client-company meter.
- Initial launch has no contractual uptime SLA; 99.5% monthly availability is an internal target only.
- Tenant provisioning is vendor-assisted/automated rather than public self-service.

**TARGET — NOT YET BENCHMARKED** applies to the unmeasured availability objective.

## ASSUMPTIONS

### A-001 — Preliminary Scale Tiers

Status: ACTIVE

The following four tiers are used for planning. These are workload assumptions and capacity targets, not measured benchmarks.

| Tier | Active Employees | Indicative Client Companies | Expected Concurrent HR/Payroll Users | Approx. Attendance Punches/Day | Payroll Workload Model | Cutoff-Day Peak Model | Reporting Intensity |
|---|---:|---:|---:|---:|---|---|---|
| S | 500–2,000 | 2–15 | 3–10 | 1,000–6,000 | Usually one or a few payroll groups per cycle; full-tier recalculation must be practical | Attendance consolidation/corrections plus payroll for up to the full active population in a business day | Low–Moderate |
| M | 2,001–10,000 | 5–50 | 8–30 | 4,000–30,000 | Multiple client/pay groups; overlapping cutoff processing expected | Multiple imports, validation, correction and payroll runs may overlap for a substantial share of the tier | Moderate |
| L | 10,001–30,000 | 15–120 | 20–75 | 20,000–90,000 | Many client/pay groups with overlapping cycles; reruns and exceptions expected | Large same-day attendance consolidation, payroll calculation, exception handling and operational reports | High |
| XL | 30,001–100,000 | 30–300+ | 50–200 | 60,000–300,000 | Very large multi-client batch workload; full-population or near-full-population processing must remain viable | Heavy overlapping cutoff activity, reruns, statutory/report generation and management reporting | Very High |

All numeric performance/capacity values above are:

**TARGET — NOT YET BENCHMARKED**

C1 is now RESOLVED for planning baseline purposes because P2 adds hardware, topology, OS, storage, database-growth assumptions, and qualitative performance expectations. Measured validation remains in later phases.

### A-002 — Payroll Frequency Model

Status: ACTIVE

Capacity planning will assume semi-monthly payroll as the primary baseline workload pattern, while the product must support weekly, monthly, and client-specific payroll cycles where configured.

The architecture must not assume that all clients share one cutoff calendar.

### A-003 — Target Agency Operating Profile

Status: ACTIVE

The normal target customer is expected to have:

- multiple client companies and deployment sites;
- frequent worker onboarding, separation, transfer, and redeployment;
- workers moving between client assignments;
- multiple rate structures and client-specific compensation/billing rules;
- rotating, overnight, split, and client-specific schedules;
- attendance arriving from multiple operational sources;
- corrections and exceptions near payroll cutoff;
- payroll, billing, statutory, and management reporting peaks near cutoff dates.

### A-004 — Attendance Complexity

Status: ACTIVE

Attendance design must assume:

- multiple punches per worker per day are possible;
- multi-site ingestion;
- late or corrected attendance;
- overnight shifts crossing calendar dates;
- missing/duplicate punches and manual corrections;
- bursts of ingestion around cutoff periods.

The P1 punch/day figures are planning ranges only.

### A-005 — HRIS User Profile

Status: ACTIVE

Typical users may include:

- recruiters;
- HR/personnel staff;
- deployment/coordinator staff;
- timekeepers;
- payroll staff;
- billing/accounting staff;
- compliance/reporting staff;
- supervisors/managers;
- system administrators.

Concurrency assumptions refer to simultaneous authenticated HRIS users, not the total number of named user accounts.

### A-006 — Reporting Profile

Status: ACTIVE

Reporting intensity increases with tier size and cutoff activity.

The product should expect:

- operational lists and exception reports during normal days;
- heavier payroll, billing, compliance, and reconciliation reports around cutoff;
- very large exports/reports at the upper technical tier.

Detailed reporting architecture belongs to P7.

### A-007 — Core Database Growth Planning Ranges

Status: ACTIVE

For storage planning only, assume approximate annual growth of the core transactional MySQL database as follows:

| Tier | Planning Range for Core DB Growth / Year |
|---|---:|
| S | 5–25 GB/year |
| M | 15–75 GB/year |
| L | 50–200 GB/year |
| XL | 150–600 GB/year |

These ranges are broad planning assumptions derived from P1 attendance volumes plus payroll, audit, employee, deployment, billing, and operational history.

They exclude:

- database backups;
- application logs;
- generated report/export files;
- operating-system files;
- large binary/document repositories unless a later design deliberately stores them in MySQL.

All values are:

**TARGET — NOT YET BENCHMARKED**

The fixed storage minimums in D-013 intentionally provide additional headroom because real retention policies and data models are not yet finalized.

### A-008 — Minimum Operational Host Assumptions

Status: ACTIVE

Production planning assumes:

- 64-bit x86-64/AMD64 hardware for the initial support matrix;
- supported and security-patched operating system;
- stable wired LAN connectivity to the HRIS host where practical;
- at least 1 GbE server-side LAN connectivity as the normal baseline;
- sufficient free disk space above the minimum required to permit database growth, temporary files, updates, and maintenance operations;
- local administrator/root access available during installation/maintenance;
- HRIS services configured to start independently of an interactive user session.

Network throughput values are planning baselines only and may be revised by measured testing.

### A-009 — Performance Expectations by Tier

Status: ACTIVE

Performance expectations are qualitative until P7/P8 benchmarking:

- S: routine HRIS use and normal payroll/cutoff processing should remain practical on a single supported host.
- M: multiple imports, corrections, reporting, and payroll work may overlap without making normal HRIS interaction unusable.
- L: heavy cutoff processing and operational reporting must remain viable on one properly sized host, with optional app/DB separation if measured contention warrants it.
- XL: full or near-full population batch processing, reruns, reporting, and large attendance volumes must remain viable on separate app/DB hosts.
- All tiers: heavy unrelated workloads on a shared customer machine may reduce performance and are outside the baseline.

All performance expectations are:

**TARGET — NOT YET BENCHMARKED**

### A-010 — Dual-OS Support Burden

Status: ACTIVE

Supporting Windows and Ubuntu adds real packaging, installation, update, service-management, diagnostics, filesystem, permissions, antivirus/security-tool interaction, and test-matrix cost for one developer.

The support burden is considered acceptable only because the matrix is intentionally constrained to:

- Windows 11 Pro for S/M;
- Windows Server 2025 Standard for server-tier Windows deployments;
- Ubuntu Server 26.04 LTS as the Linux alternative;
- one application codebase;
- one database engine;
- no additional Linux distributions in the initial matrix.

P3 must preserve this small matrix when defining installation and support tooling.

### A-011 — P4 Commercial Economics Planning Assumptions

Status: SUPERSEDED BY P10

The P4 economics used the former license/maintenance schedule and an illustrative approximately 12 developer-hours/customer/year support assumption. Those calculations are no longer authoritative after the affordability-first P10 repricing in D-167–D-172.

Historical P4 calculations may be consulted only for traceability. Current launch-price sustainability is explicitly unverified and must be measured under V-008.


### A-012 — Customer Value Must Be Estimated from the Customer's Own Labor and Rework Inputs

Status: ACTIVE

Do not claim a universal ROI or fabricated competitor savings. Estimate customer value from visible customer-specific assumptions such as administrative hours saved, payroll rework avoided, billing-cycle improvements, and support/maintenance displacement.

Illustrative example only: if a customer values HR/payroll administrative labor at ₱250/hour and the HRIS saves 120 staff-hours/month across attendance consolidation, payroll preparation, corrections, reporting, and related administration, direct labor capacity value would be approximately ₱360,000/year.

That example is not a market fact and does not include unquantified value from reduced errors, faster billing, compliance risk reduction, or management visibility. P10 should replace illustrative inputs with pilot measurements where possible.

### A-013 — P10 Launch Pricing Has No Measured Founding-Pilot Economics Yet

Status: ACTIVE

At P10 close, no measured production-pilot evidence has been supplied for actual deployment days, migration effort, support hours, upgrade/Rule Pack maintenance effort, willingness-to-pay, realized customer savings, or sales-cycle friction.

The D-167–D-172 amounts are therefore affordability-driven launch estimates. Their sustainability assumes strong automation/diagnostics, narrow included-support allowances, and separate billing of out-of-scope migration/configuration/training/integration work. V-008 is the mandatory evidence gate.

## VERIFIED EXTERNAL FACTS — P2

Verified as of 2026-09-29 from official vendor sources.

### EF-P2-001 — Windows 10 General Support Ended

Microsoft states that Windows 10 general support ended on 2025-10-14. Standard Windows 10 is therefore not a new-production baseline for this product.

Official sources:

- https://support.microsoft.com/en-us/windows/deployment/updates-lifecycle/windows-10-support-has-ended-on-october-14-2025
- https://learn.microsoft.com/en-us/lifecycle/announcements/windows-10-end-of-support

### EF-P2-002 — Windows 11 Pro Uses the Modern Lifecycle

Microsoft lists Windows 11 Home/Pro as in support and services Pro feature releases for 24 months. As of verification, Windows 11 Pro 25H2 is supported through October 2027. Windows 11 26H1 is a select-new-device release and is not the normal in-place feature update path from 24H2/25H2.

Official sources:

- https://learn.microsoft.com/en-us/windows/release-health/windows11-release-information
- https://learn.microsoft.com/lifecycle/products/windows-11-home-and-pro

### EF-P2-003 — Windows Server 2025 Lifecycle

Windows Server 2025 Standard is covered by Microsoft's fixed lifecycle, with mainstream support through November 2029 and extended support through November 2034.

Official source:

- https://learn.microsoft.com/en-us/lifecycle/products/windows-server-2025

### EF-P2-004 — Ubuntu 26.04 LTS Lifecycle

Canonical lists Ubuntu 26.04 LTS as released in April 2026 with standard security maintenance through May 2031.

Official source:

- https://ubuntu.com/about/release-cycle?product=ubuntu&release=ubuntu&version=26.04+LTS

## VERIFIED EXTERNAL FACTS — P6

Verified as of 2026-09-29 from official Philippine government sources.

### EF-P6-001 — PIC and PIP Roles Depend on Control and Outsourced Processing

The Data Privacy Act defines a Personal Information Controller as the person/organization controlling collection, holding, processing, or use of personal information, including one instructing another to process on its behalf. It defines a Personal Information Processor as a person/entity to whom a controller may outsource personal-data processing.

Official source:

- https://privacy.gov.ph/data-privacy-act/

### EF-P6-002 — Philippine Privacy Rules Require Purpose Limitation, Proportionality, Security, and Limited Retention

The Data Privacy Act IRR requires transparency, legitimate purpose, proportionality, appropriate privacy/security safeguards, and retention only as long as necessary subject to legal/business bases and other lawful exceptions. It also requires reasonable and appropriate organizational, physical, and technical security measures for controllers and processors.

Official sources:

- https://privacy.gov.ph/implementing-rules-regulations-data-privacy-act-2012/
- https://privacy.gov.ph/data-security/

### EF-P6-003 — Statutory Payroll Inputs and Outputs Are Issuance- and Effective-Date-Dependent

Official sources demonstrate that payroll/compliance inputs are published through dated schedules, circulars, regulations, forms, annexes, and labor advisories rather than being timeless application constants. Examples verified during P6 include an SSS contribution schedule effective January 2025, BIR withholding tables with explicit effectivity, PhilHealth contribution schedules by year, Pag-IBIG contribution guidance, and DOLE holiday-pay advisories for specific 2026 dates.

Official source categories/examples:

- https://www.sss.gov.ph/sss-circulars/
- https://www.philhealth.gov.ph/circulars/
- https://www.pagibigfund.gov.ph/document/pdf/circulars/
- https://www.bir.gov.ph/ and official BIR-hosted regulations/forms/annexes
- https://bwc.dole.gov.ph/issuances/labor-advisories/

P6 intentionally does not copy these changing numeric values into architectural Decisions. Current production values must be verified when building/releasing the corresponding Rule Pack.


## VERIFIED EXTERNAL FACTS — P7

Verified as of 2026-09-29 from official vendor documentation.

### EF-P7-001 — Java 25 Is an LTS Release

Oracle's Java SE support roadmap identifies Java 25 as an LTS release, released in September 2025. Oracle's consolidated JDK 25 release notes show the 25.0.4.1 update released on 2026-08-18.

Official sources:

- https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- https://www.oracle.com/java/technologies/javase/25all-relnotes.html

### EF-P7-002 — Spring Boot 4.1.1 Is Current Stable and Supports Java 25

Spring Boot's official system-requirements page identifies Spring Boot 4.1.1 and states it supports Java versions up to and including Java 26. It supports Maven 3.6.3+ and Gradle 8.14+/9.x.

Official source:

- https://docs.spring.io/spring-boot/system-requirements.html

### EF-P7-003 — Vaadin 25 Requires Java 21+ and Spring Boot 4.1+

Vaadin's current compatibility documentation for Vaadin 25 lists Java 21 or later and Spring Boot 4.1 or later. Current upgrade documentation references the Vaadin 25.3 line.

Official sources:

- https://vaadin.com/docs/latest/compatibility
- https://vaadin.com/docs/latest/upgrading/25-2-to-25-3

### EF-P7-004 — MySQL 8.4 Is the LTS Track

Oracle MySQL documentation identifies MySQL 8.4 as an LTS series intended for environments that prioritize a stable feature set and longer support. The LTS series receives fixes while avoiding feature removals inside the LTS line.

Official source:

- https://dev.mysql.com/doc/refman/8.4/en/mysql-releases.html

### EF-P7-005 — Spring Batch Supports Chunk Transactions, Persistent Job Metadata, and Restartability

Spring Batch documentation defines chunk-oriented processing with transaction boundaries at chunk commits and documents persistent JDBC JobRepository metadata plus restartable jobs/steps. This directly supports the selected payroll/import/report background-job model.

Official sources:

- https://docs.spring.io/spring-batch/reference/step/chunk-oriented-processing.html
- https://docs.spring.io/spring-batch/reference/job/configuring-repository.html
- https://docs.spring.io/spring-batch/reference/step/chunk-oriented-processing/restart.html

## VERIFIED EXTERNAL FACTS — P9

Verified as of 2026-09-29 from official vendor and Philippine government sources.

### EF-P9-001 — NPC Rules Include a 72-Hour Breach-Notification Requirement for Breaches Meeting the Notification Criteria

The National Privacy Commission's Data Privacy Act IRR states that the Commission and affected data subjects are notified by the personal information controller within 72 hours upon knowledge of, or reasonable belief that, a personal data breach requiring notification has occurred. The notification requirement depends on the applicable breach criteria rather than every security incident automatically becoming a mandatory breach notification.

Official sources:

- https://privacy.gov.ph/implementing-rules-regulations-data-privacy-act-2012/
- https://privacy.gov.ph/pips-and-pics/breach-reporting/

P9 does not hardcode broader legal conclusions from this rule. Current reporting channel, criteria, contents, postponement rules, annual incident reporting, and customer/vendor role allocation remain subject to V-011/V-024 and professional review.

### EF-P9-002 — Spring Security Recommends Adaptive One-Way Password Hashing with a Hardware-Tuned Work Factor

Spring Security's password-storage guidance recommends adaptive one-way functions such as bcrypt, PBKDF2, scrypt, or Argon2 and recommends tuning the work factor to the actual system rather than assuming one universal setting.

Official source:

- https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html

### EF-P9-003 — MySQL Supports InnoDB Crash Recovery and Binary-Log Point-in-Time Recovery

MySQL 8.4 documentation describes InnoDB's transactional/crash-recovery behavior and documents point-in-time recovery by restoring a suitable full backup and then applying subsequent binary-log events.

Official sources:

- https://dev.mysql.com/doc/refman/8.4/en/backup-and-recovery.html
- https://dev.mysql.com/doc/refman/8.4/en/backup-types.html
- https://dev.mysql.com/doc/refman/8.4/en/point-in-time-recovery.html
- https://dev.mysql.com/doc/refman/8.4/en/binary-log.html

### EF-P9-004 — MySQL Shell Provides Consistent Parallel Dump/Load Utilities with Optional Checksums

MySQL Shell documentation provides `util.dumpInstance()`/related dump utilities and `util.loadDump()` for supported MySQL instances. The dump utilities support consistent InnoDB dumps, parallel/chunked compressed output, and checksum metadata that can be verified during loading.

Official source:

- https://dev.mysql.com/doc/mysql-shell/26.7/en/mysql-shell-utilities-dump-instance-schema.html

Exact MySQL Shell/server patch compatibility remains release-time verification under V-006/V-021.

## VERIFICATION REGISTER

### V-001 — Validate Scale-Tier Workload Assumptions

Status: TO VERIFY  
Target Phase: P7 / P8

Validate the client-count, concurrent-user, attendance-punch, payroll-peak, reporting, and P2 hardware assumptions against:

- representative customer data where available;
- synthetic load testing;
- measured application/database behavior.

Do not treat the scale-tier workload figures as benchmarks.

### V-002 — Benchmark the 100,000-Employee Technical Tier

Status: TO VERIFY  
Target Phase: P7 / P8

The 100,000-active-employee tier must be validated with representative synthetic data and repeatable performance tests covering at least:

- attendance ingestion;
- payroll calculation/reruns;
- database query/index behavior;
- concurrent Vaadin usage;
- reporting/export generation;
- app/DB network behavior;
- backup/restore implications where applicable.

Until measured, all performance expectations remain:

**TARGET — NOT YET BENCHMARKED**

### V-003 — Validate P2 Hardware and Database-Growth Baselines

Status: TO VERIFY  
Target Phase: P7 / P8

Validate:

- CPU/RAM minimums;
- recommended CPU/RAM sizing;
- single-host viability for S/M/L;
- separate-host XL topology;
- MySQL memory requirements;
- database-growth ranges;
- primary-storage headroom;
- network requirements between XL app and DB hosts.

Revise D-011/D-013 only if measured evidence proves the current support baseline materially inappropriate.

### V-004 — Revalidate OS Lifecycle Before Release/Installation

Status: TO VERIFY  
Target Phase: P3 / ongoing release management

Before shipping an HRIS release or approving a new production installation, verify that the target Windows/Ubuntu release remains vendor-supported.

Do not assume the P2 feature-version details remain current indefinitely.

### V-005 — Validate Install/Service/Repair Behavior on Every Supported OS Baseline

Status: TO VERIFY  
Target Phase: P8 / P9 / ongoing release engineering

Before production release, test the P3 packaging model on each supported OS family/release covering at least:

- clean install;
- HRIS-managed MySQL setup;
- service auto-start after reboot;
- unexpected-process restart behavior;
- firewall-rule creation/removal;
- repair/reinstall with preserved data;
- uninstall with preserved data;
- self-install preflight;
- upgrade from the immediately previous supported release line;
- diagnostic-bundle generation and redaction.

### V-006 — Revalidate Exact Java/MySQL Patch Levels and Runtime Distribution Before Packaging

Status: TO VERIFY  
Target Phase: P8 / ongoing release management

P7 verified the architecture baseline as Java 25 LTS and MySQL 8.4 LTS, with Spring Boot 4.1.x and Vaadin 25.3.x compatibility as of 2026-09-29. Before locking each production package, revalidate:

- the exact current Java 25 LTS patch/build and chosen redistributable OpenJDK distribution/license terms;
- the exact current supported MySQL 8.4 LTS patch level;
- the exact compatible Spring Boot/Vaadin patch levels;
- any relevant security/advisory constraints.

Do not infer release-time versions from this planning snapshot.

### V-007 — Philippine Legal Review of Commercial and Support Documents

Status: TO VERIFY  
Target Phase: P10 / before first binding production contract

A Philippine lawyer should review at minimum:

- perpetual software-license terms;
- warranty disclaimer;
- limitation of liability;
- payroll-output/customer-review responsibility;
- annual-maintenance and reinstatement terms;
- support scope, exclusions, and response-target wording;
- data-processing/privacy terms;
- support access and remote-support authorization;
- confidentiality;
- intellectual-property treatment for sponsored/custom work;
- pilot-specific feedback, testing, and commercial terms.

This planning phase does not make legal conclusions about enforceability.

### V-008 — Mandatory Post-Pilot Commercial Repricing Before Customer #2

Status: TO VERIFY  
Target Phase: after founding pilot / before second normal commercial customer

P10 completed the planning-level commercial revisit and established the affordability-first launch estimates in D-167–D-172. Because no measured founding-pilot production evidence existed at P10 close, those amounts must be reviewed after the pilot and before quoting the second normal commercial customer.

Revisit at minimum:

- actual deployment/setup days;
- migration/data-cleanup effort;
- training effort;
- included and non-billable support hours plus issue categories;
- separately billed support/professional-service demand;
- upgrade/Rule Pack maintenance effort;
- feature-request effort and acceptance rate;
- customer willingness-to-pay and price objections;
- realized labor/rework savings where measurable;
- sales-cycle friction;
- tooling/business overhead;
- whether S/M/L/XL price gaps and support allowances remain commercially sustainable.

Do not silently raise or lower the D-167–D-172 schedule without recording the measured basis and superseding decision(s).


### V-009 — Verify Each Implemented Statutory Rule and Non-Overridable Requirement Against Current Authority

Status: TO VERIFY  
Target Phase: P6 architecture complete; ongoing before implementation/Rule Pack release

P6 resolved the architecture but does not pretend that every Philippine statutory detail is timeless or fully captured in planning. Before implementing or releasing each affected rule family, verify from the current authoritative source:

- mandatory/non-overridable hiring, deployment, payroll, leave, premium-pay, contribution, withholding, or reporting behavior;
- the legally relevant applicability/effective-date basis;
- required classifications, records, outputs, deadlines, and retention requirements;
- whether any otherwise configurable HRIS override must be prohibited or narrowed;
- superseding issuances and transition rules.

Record the result in the Compliance Register and regression fixtures. Never infer a current rate, table, deadline, or legal conclusion from an older Rule Pack or planning document.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL

### V-010 — Verify Every Government File/Export Definition Against the Current Official Format

Status: TO VERIFY  
Target Phase: implementation / every Rule Pack or output-format release

Before shipping a government statutory file/export generator, verify the current authoritative agency specification, including field definitions, layout/schema, code lists, encoding, naming, validation rules, and version/effectivity where applicable.

Do not assume an older file format remains accepted merely because the underlying payroll calculation is unchanged.

VERIFY AGAINST CURRENT OFFICIAL FORMAT

### V-011 — Philippine Privacy/Contract Review for Deployment-Specific Role Allocation and Retention

Status: TO VERIFY  
Target Phase: P9 / P10 / before first production contract

A Philippine privacy/legal professional should review the deployment-specific allocation of PIC/PIP responsibilities, data-processing/support clauses, retention categories that depend on law or customer policy, cross-organization support-data handling, and any secure-deletion/incident obligations that depend on current NPC issuances or sector-specific requirements.

This complements V-007 and does not replace the customer's own privacy governance responsibilities.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL


### V-012 — Benchmark P7 Payroll Budgets and Safe Parallelism

Status: TO VERIFY  
Target Phase: P8

Using representative synthetic data and the tier hardware baselines, validate:

- D-116 full-run elapsed-time budgets;
- worker chunk size, starting near 250;
- safe worker parallelism by tier;
- database write rate and lock behavior;
- restart/resume after controlled failure;
- idempotent retry behavior;
- UI responsiveness while payroll is running.

All D-116 values remain:

**TARGET — NOT YET BENCHMARKED**

### V-013 — Benchmark Vaadin Session Memory and Concurrent-User Behavior

Status: TO VERIFY  
Target Phase: P8

Validate the D-120 targets using representative recruiter, HR, attendance, payroll, billing, and reporting screens across the P1 concurrency ranges.

Measure at least:

- average and p95 active-session memory;
- heap/GC behavior;
- server response latency under concurrent use;
- lazy-grid query behavior;
- progress polling overhead;
- behavior while heavy background jobs are running.

**TARGET — NOT YET BENCHMARKED**

### V-014 — Validate Attendance Indexing and the Need for Partitioning/Archival

Status: TO VERIFY  
Target Phase: P8 / P9

Generate multi-year attendance data at the L/XL workload ranges and validate:

- ingestion throughput;
- duplicate detection cost;
- worker/date and site/date query plans;
- Attendance Day rebuild/review queries;
- payroll cutoff reads;
- index size and maintenance cost;
- backup/restore implications.

Do not enable MySQL table partitioning merely because the table is large. Introduce it only if measured evidence shows a material benefit that justifies operational complexity.

### V-015 — Validate MySQL Pool and Buffer-Pool Starting Targets

Status: TO VERIFY  
Target Phase: P8 / P9

Measure HikariCP pool demand, query concurrency, InnoDB buffer-pool hit behavior, JVM/database memory pressure, and XL app/DB network behavior.

The D-119 memory percentages are starting targets only and must be tuned from measurements.

**TARGET — NOT YET BENCHMARKED**

### V-016 — Cross-Platform Application/PDF/File Behavior

Status: TO VERIFY  
Target Phase: P8 / P9

Automated release validation must compare supported Windows and Ubuntu behavior for:

- paths and case sensitivity;
- CRLF/LF imports;
- locale/date/number formatting;
- UTF-8 and required alternative encodings;
- filename sanitation;
- bundled/embedded PDF fonts and layout consistency;
- temporary/finalized file behavior;
- antivirus/file-lock retry cases where practical.

This verification is separate from statutory-format validation under V-010.

### V-017 — Validate Operational Clock-Health Thresholds

Status: TO VERIFY  
Target Phase: implementation / P9 operational qualification

Validate D-160's initial thresholds using supported Windows and Ubuntu deployments with:

- synchronized clocks under normal conditions;
- NTP/internet unavailable;
- deliberate app/DB host drift;
- deliberate backward/forward jumps;
- separate-host XL topology;
- certificate, audit, licensing, and background-job behavior around clock corrections.

Confirm that the 30-second warning, 2-minute critical drift, >5-minute backward jump, and >15-minute forward jump thresholds produce useful operator guidance without unnecessary disruption. Revise D-160 only if measured operational evidence justifies a different threshold.

Any protective response must preserve the P7 rule that deterministic payroll calculations rely on explicit business dates rather than silently changing because the system clock is wrong.


### V-018 — Validate Synthetic Dataset Determinism and Workload Representativeness

Status: TO VERIFY  
Target Phase: implementation / performance-test construction

Before relying on a named synthetic profile for qualification, verify that:

- the same compatible generator version/profile/seed produces equivalent logical data;
- the population and historical volumes match the declared profile manifest;
- attendance, payroll, deployment, schedule, and exception distributions exercise the intended access paths;
- fictional data does not inadvertently embed copied customer datasets;
- generated edge cases cover the business invariants required by P5/P6/P7.

Synthetic distributions are engineering test models, not claims about a universal Philippine staffing-agency population.

### V-019 — Establish Stable Measured Performance Baselines Before Enabling Hard Regression Gates

Status: TO VERIFY  
Target Phase: implementation / benchmark qualification

For each performance budget that will block CI/release, establish repeatability on the controlled D-136 environment using the D-145 dataset identity and D-146 metadata. Define an evidence-based tolerance that distinguishes ordinary variance from a meaningful regression.

Until that is done, P7/P8 performance values remain:

**TARGET — NOT YET BENCHMARKED**

### V-020 — Validate Repository/CI Permission Boundaries for AI Agents

Status: TO VERIFY  
Target Phase: repository setup / ongoing security review

When repository automation is implemented, verify that branch protection, token/app permissions, secrets handling, self-hosted runner permissions, and PR workflows actually enforce D-134. AI-agent credentials should receive only the repository/actions permissions required for their tasks and must not permit silent bypass of protected-main review.

### V-021 — Benchmark Backup, Restore, and Recovery Targets by Tier

Status: TO VERIFY  
Target Phase: implementation / P9 operational qualification / release qualification

Using representative S/M/L/XL synthetic datasets and the supported deployment topologies, measure:

- full Recovery Set duration and production impact;
- Recovery Set size/compression/encryption overhead;
- external-copy duration;
- checksum/integrity-validation duration;
- isolated full-restore duration;
- app/schema/Rule Pack/document validation after restore;
- L/XL binary-log archival and point-in-time-recovery behavior;
- disk-space headroom required during backup/restore;
- behavior during concurrent normal workload where backup is supported online.

Validate or revise the D-155 targets only from measured comparable evidence.

**TARGET — NOT YET BENCHMARKED**

### V-022 — Validate HTTPS, Encryption, Authentication, and Session Controls on Supported Deployments

Status: TO VERIFY  
Target Phase: implementation / security qualification

Validate D-150 through D-152 on supported Windows and Ubuntu configurations, including:

- local certificate generation/import/trust and renewal/replacement;
- customer-provided certificate configuration;
- no accidental plain-HTTP general-LAN exposure;
- Argon2id/adaptive password-hash compatibility and benchmarked work factor;
- failed-login throttling/temporary lock behavior;
- session idle timeout and privileged re-authentication;
- offline TOTP recovery/admin procedures;
- BitLocker/LUKS operational guidance;
- encrypted external/offsite backup restore with protected recovery keys.

Security defaults must remain usable for agencies with limited internal IT staff and must not create an unrecoverable installation.

### V-023 — Qualify Upgrade/Migration Recovery and Large-Table Migration Behavior

Status: TO VERIFY  
Target Phase: implementation / release qualification

Before production release, exercise supported upgrade paths from the immediately previous release line covering:

- package integrity/signature validation;
- preflight and maintenance-mode behavior;
- verified pre-upgrade Recovery Set;
- Flyway migration success and idempotent startup/schema validation;
- controlled injected migration failure and full restore-based rollback;
- large-table migration behavior at representative L/XL volumes;
- disk/log headroom and maintenance-window duration;
- preservation of license/configuration/Rule Packs/documents;
- Windows and Ubuntu package/service behavior.

Migration-time budgets for materially large changes remain release-specific targets until measured.

**TARGET — NOT YET BENCHMARKED**

### V-024 — Verify Philippine Privacy Incident, Retention, Deletion, and Backup-Handling Procedures

Status: TO VERIFY  
Target Phase: implementation / P10 contract review / before first production deployment

Verify the operational runbooks and customer/vendor responsibilities against current NPC authority and Philippine professional advice, including:

- what constitutes a security incident versus a reportable personal data breach;
- current mandatory-notification criteria, timing, channels, required contents, postponement, and follow-up reporting;
- PIC/PIP coordination and vendor support responsibilities;
- retention categories and lawful deletion/anonymization triggers;
- handling, transport, storage, access, and destruction of backup/support media containing personal data;
- incident evidence retention and access controls;
- customer-facing privacy/security contract language.

Do not treat the P9 technical design as a substitute for deployment-specific legal/privacy governance.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL


### V-025 — Execute Founding-Pilot Production-Readiness Qualification

Status: TO VERIFY  
Target Phase: implementation / M6 / before first production activation

Verify the D-165/D-173 release and cutover gates in the real founding-pilot context, including:

- representative payroll/UAT;
- applicable Rule Pack/statutory-output verification status;
- production-like migration dry run and reconciliation;
- no unresolved Severity-1 payroll/data-integrity/security/restore blocker;
- verified Recovery Set and isolated restore;
- supported upgrade/recovery qualification;
- HTTPS/RBAC/authentication/licensing/diagnostics checks;
- at least one complete parallel payroll cycle with material discrepancies reconciled;
- customer payroll/admin sign-off and handover;
- final supported-environment preflight.

This verification establishes production readiness for the first customer; it does not convert unmeasured scale/performance targets into benchmark facts.

### V-026 — Verify Portal Cloud Provider, Region, Managed Identity, and Security Baseline

Status: TO VERIFY  
Target Phase: portal implementation / before first portal production launch

Before selecting the production portal platform, verify current authoritative provider documentation and contractual terms for:

- suitable region availability and data-location behavior;
- managed identity/OIDC/MFA capabilities;
- database/storage encryption and backup behavior;
- secret/key management;
- network exposure/private-service options;
- audit/logging and incident-support capabilities;
- service lifecycle/deprecation policy;
- data-transfer/storage/backup cost drivers;
- processor/subprocessor and privacy/security contractual terms.

Do not infer current cloud-service capabilities, prices, or region guarantees from planning assumptions.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL where Philippine privacy/legal interpretation is required.

### V-027 — Qualify Portal Sync Correctness, Offline Recovery, Idempotency, and Version Compatibility

Status: TO VERIFY  
Target Phase: portal implementation / release qualification

Test the portal protocol with representative local/cloud failures covering at least:

- committed local change followed by process/network failure before acknowledgement;
- duplicate upload and duplicate cloud command delivery;
- extended internet outage followed by catch-up;
- out-of-order and stale projection/approval versions;
- on-premises restart during pending delivery;
- portal/cloud restart during pending command processing;
- current and immediately previous supported HRIS release compatibility;
- unsupported protocol version behavior;
- local payroll/core operation while portal is unavailable;
- audit correlation from cloud actor to authoritative on-premises result.

Portal synchronization must prove that retries do not duplicate business effects and that portal failure cannot corrupt or block the core system.

### V-028 — Validate Portal Hosting Economics, Subscription Prices, Support Load, and Availability Objective

Status: TO VERIFY  
Target Phase: after first portal pilot / before normal portal scale-up

Measure and revisit:

- per-agency cloud compute/database/storage/backup/egress cost;
- managed identity and email-provider cost;
- deployment/provisioning effort;
- security/patching/incident operational effort;
- ordinary portal support hours;
- actual user/activity/sync volumes by S/M/L/XL band;
- whether no per-seat pricing remains commercially sustainable;
- achieved monthly availability and major outage causes;
- whether a contractual SLA or premium service tier is supportable.

Do not silently change D-194/D-195/D-197 without recording the measured basis and superseding decision(s).

The 99.5% availability objective remains:

**TARGET — NOT YET BENCHMARKED**

### V-029 — Verify Portal Privacy, Retention, Data-Location, Termination, and Contract Terms

Status: TO VERIFY  
Target Phase: before first binding portal contract / ongoing when service terms change

A Philippine privacy/legal professional should review the actual portal processing arrangement and contract, including:

- customer/vendor PIC/PIP or other role allocation for hosted portal processing;
- permitted data categories and minimization;
- hosting region/data-location disclosure;
- subprocessors and cross-border processing implications where applicable;
- portal audit/log/backup retention;
- D-188 projection-retention defaults;
- D-196 termination/grace/export/deletion behavior;
- breach/security-incident coordination;
- portal subscription/support/SLA disclaimers and customer responsibilities.

Product retention defaults are not substitutes for verified legal or contractual requirements.

VERIFY AGAINST CURRENT OFFICIAL ISSUANCE / PH PROFESSIONAL

## DEFERRED ITEMS

### X-001 — Client Portal

Status: RESOLVED IN P11  
Target Phase: P11

Resolved by D-179–D-198. The Client Portal remains outside the core MVP but its initial scope, authority boundary, outbound-only synchronization architecture, data minimization, security/retention posture, compatibility rules, subscription model, and launch operating constraints are now planned. Implementation remains after the on-premises core is stable.

### X-002 — Deployment/Setup Professional-Service Pricing

Status: RESOLVED IN P4  
Target Phase: P4

Resolved by D-046. Vendor-assisted deployment remains optional under D-025 and is priced separately from the perpetual software license. The tiered deployment package includes the normal P3 installation/runtime/database/service/firewall/license/preflight/health/handover scope, while migration, unusual configuration, integrations, extra training, travel, and other non-standard work remain separately billable.

## REQUIREMENTS COVERAGE

| ID | Requirement | Assigned Phase(s) | Status |
|---|---|---|---|
| C1 | Scale tiers and capacity model | P1, P2 | RESOLVED |
| C2 | Active employee definition | P1 | RESOLVED |
| C3 | Windows/Linux support matrix | P2 | RESOLVED |
| C4 | Installation and packaging | P3 | RESOLVED |
| C5a | Licensing mechanism | P3 | RESOLVED |
| C5b | Licensing commercial rules | P4 | RESOLVED |
| C6 | Support and diagnostics | P3 | RESOLVED |
| C7 | Version support policy | P3 | RESOLVED |
| C8a | Initial commercial model and pricing | P4 | RESOLVED |
| C8b | Final commercial revisit | P10 | RESOLVED |
| C9 | Domain model and major workflows | P5 | RESOLVED |
| C10 | Compliance Rule Pack architecture | P6 | RESOLVED |
| C11a | Privacy roles and data model | P6 | RESOLVED |
| C11b | Operational privacy controls | P9 | RESOLVED |
| C12 | Payroll engine scalability | P7 | RESOLVED |
| C13 | Attendance scalability | P7 | RESOLVED |
| C14 | MySQL scalability | P7 | RESOLVED |
| C15 | Vaadin scalability | P7 | RESOLVED |
| C16 | Reporting architecture | P7 | RESOLVED |
| C17 | Time reliability | P7 | RESOLVED |
| C18a | OS packaging differences | P3 | RESOLVED |
| C18b | Application-level OS differences | P7 | RESOLVED |
| C19 | AI workflow and repository governance | P8 | RESOLVED |
| C20 | Synthetic data and performance regression | P8 | RESOLVED |
| C21 | LAN security | P9 | RESOLVED |
| C22 | Database operations | P9 | RESOLVED |
| C23 | Backup and restore | P9 | RESOLVED |
| C24 | Updates and migrations | P9 | RESOLVED |
| C25 | Legacy data import | P9 | RESOLVED |
| C26 | MVP scope and delivery roadmap | P10 | RESOLVED |
| C27 | Client Portal add-on | P11 | RESOLVED |

## FIXED-DECISION RISK REVIEW

P1 review completed.

One material risk was identified for F3:

- **Risk:** Treating 100,000 active employees as the normal target-customer profile could over-engineer the product and operational model for the much more common lower tiers.
- **Resolution:** D-001 retains the full 500–100,000 supported range while defining 500–30,000 as the primary commercial/design range and 100,000 as the upper technical-capacity target.
- **Consequence:** The 100,000 tier still requires deliberate design and performance validation, but does not dictate unnecessary complexity for smaller installations.

No other Fixed Decisions F1–F10 are changed by P1.

P2 introduced no material contradiction with the Fixed Decisions. Its OS matrix preserves F5 by supporting both Windows and Linux while minimizing the number of supported production variants.

P3 introduced no material contradiction with the Fixed Decisions. Its licensing model preserves F4 by requiring no online heartbeat and preventing license failures from stopping payroll or customer-data access. Its packaging model preserves F5/F8 by using one application codebase with thin OS-specific wrappers rather than separate product variants.

P4 introduced no material contradiction with the Fixed Decisions. It implements F6 using perpetual licensing, annual maintenance, paid feature requests, and paid professional services. The one-agency founding-pilot strategy changes only the commercialization sequence and provisional price level; it does not reduce the supported scale or alter the product architecture.

P5 introduced no material contradiction with the Fixed Decisions. It refines F10 module boundaries while preserving the modular-monolith constraint, one-agency installation model, no-fork rule, offline-capable core, and solo-developer maintainability. P5 intentionally defers statutory rule content and legal conclusions to P6 rather than hardcoding or guessing them.

P6 introduced no material contradiction with the Fixed Decisions. Its signed declarative Rule Pack model preserves offline operation and the modular-monolith/solo-developer constraints while allowing statutory data to change independently where practical. Privacy decisions reinforce the existing privacy-minimized diagnostics and customer-controlled support-access model rather than requiring cloud processing or permanent vendor access.

P7 introduced no material contradiction with the Fixed Decisions. Its durable local batch model, bounded parallelism, Maven modular-monolith structure, lazy Vaadin rules, and indexed MySQL-first strategy preserve F3/F4/F5/F8/F9 while deliberately avoiding distributed infrastructure. The 100,000-employee requirement remains a measured performance target rather than a reason to split the system prematurely.

P8 introduced no material contradiction with the Fixed Decisions. Its repository-governance and AI-agent model reinforces F8/F9/F10 by keeping module ownership explicit, builds Maven-reproducible, and agent changes reviewable. Its synthetic-data/performance strategy supports F3 without using customer production data or forcing distributed infrastructure; the 100,000-employee tier remains a controlled qualification target rather than an everyday PR workload.

P9 introduced no material contradiction with the Fixed Decisions. Its HTTPS, local-first security, Recovery Set, restore-based rollback, point-in-time recovery, Flyway migration, and staged legacy-import model preserve F1/F2/F4/F5/F8/F9 by keeping production customer-owned, offline-capable, single-database, dual-OS, and operationally manageable by one developer. P9 deliberately avoids clustering, replication, cloud dependency, and enterprise backup infrastructure while still making recovery procedures testable.

P10 introduced no material contradiction with the Fixed Decisions. Its narrow end-to-end MVP preserves F3/F4/F5/F8/F10 while explicitly deferring non-essential complexity; its affordability-first commercial changes remain inside F6's perpetual-license + annual-maintenance + paid-services model. At P10 close, Client Portal was the only intentionally deferred product capability and was assigned to P11 under F7; P11 has now resolved its design while keeping implementation outside the core MVP.

P11 introduced no material contradiction with the Fixed Decisions. It preserves F1/F2/F4/F7/F8 by keeping the Client Portal optional, separately hosted, non-authoritative, outbound-only from each agency installation, tolerant of internet loss, and outside the core MVP. Per-agency portal isolation and a separate public deployable increase hosting/operations cost, but avoid turning the cloud into a shared authoritative HR/payroll system or exposing an inbound path to customer servers.

## CROSS-PHASE CONSTRAINTS

- Core recruitment, attendance, payroll, statutory processing, and reporting must work without internet.
- Payroll/compliance correctness outranks feature count.
- Licensing failure must never interrupt an active payroll run.
- Licensing failure must never block legally required records or customer data export.
- One installation represents exactly one staffing agency and one MySQL database.
- Architecture defaults to a modular monolith.
- No customer-specific source-code forks.
- Both Windows and Linux must remain supported.
- Client Portal planning is complete under P11; implementation remains outside the core MVP and is deferred until the on-premises core is stable.
- The design must remain realistic for one developer to build and support.
- Primary commercial/design optimization should focus on 500–30,000 active employees while preserving the 100,000-active-employee technical-capacity requirement.
- Licensing and capacity planning must use D-002's Active Employee definition, never total historical worker-record count.
- Scale/performance figures remain targets until benchmarked.
- S/M may use Windows 11 Pro production hosts; L/XL must use Windows Server or Ubuntu Server.
- Windows 10 is legacy/migration-only and must not be the baseline for new production installations.
- Ubuntu Server 26.04 LTS is the supported lower-cost OS alternative across tiers.
- Initial production architecture targets x86-64/AMD64; ARM is outside the initial support matrix.
- ECC, RAID/mirroring, separate backup media, and UPS are recommendations rather than installation prerequisites.
- CPU/RAM/primary-storage minimums are actual support baselines.
- M/L/XL should be dedicated primarily to HRIS, but support and diagnostics must assume some customers will run unrelated workloads on the machine.
- Heavy unrelated host usage is outside the performance baseline.
- XL baseline topology uses separate application and database hosts.
- P3 packaging/install tooling must preserve one application codebase and the deliberately small Windows/Linux support matrix.
- The authoritative production license is a vendor-issued signed local license file; normal operation requires no cloud heartbeat.
- Employee-band enforcement uses D-002 and a 30-day overage grace period, but core payroll/statutory/record/export operations are never license-blocked.
- Maintenance lapse does not deactivate a perpetual license; it removes normal entitlement to new releases, Rule Packs, security updates, and standard support.
- Normal application support covers the current release line plus the immediately previous release line.
- Production upgrades are administrator-initiated; no update is force-installed, including critical security/compliance updates.
- Standard deployments use an HRIS-managed MySQL installation/configuration path; advanced L/XL may use a separately managed supported MySQL instance.
- Vendor-assisted initial setup is recommended/default but optional; P10 deployment estimates are S ₱15,000 / M ₱25,000 / L ₱40,000 / XL ₱60,000 under D-170, while self-installation remains free of deployment-service charge if supportability preflight passes.
- Default diagnostic bundles contain technical metadata only and exclude customer personal/payroll data unless an administrator explicitly authorizes deeper collection.
- Routine support has no permanent unattended remote-access agent and requires no inbound vendor-support firewall access.
- Packaging uses one application build, thin Windows/Ubuntu wrappers, and a bundled Java runtime.
- Repair/reinstall/uninstall preserves customer data by default; destructive purge is always a separate explicit action.
- Self-installed environments remain supportable only when they pass the automated supportability preflight.
- P10 affordability-first perpetual-license estimates are S ₱40,000 / M ₱80,000 / L ₱160,000 / XL ₱320,000 under D-167.
- Year 1 remains a one-agency founding pilot, not a multi-customer profit-maximization year.
- The founding pilot pays the applicable normal P10 launch license estimate and receives first-year maintenance included; deployment/migration/other services remain separately billable as applicable.
- P10 annual-maintenance estimates are S ₱15,000 / M ₱25,000 / L ₱50,000 / XL ₱100,000 with 2/4/6/8 included remote-support hours respectively.
- Commercial temporary-spike handling uses the existing 30-day over-band grace period; persistent over-band status requires a band upgrade but never blocks core operation.
- Standard band upgrades remain priced at the current list-price difference between bands, with reasonable maintenance adjustment.
- Professional-services pricing retains the ₱2,500/hour / ₱20,000/day baseline; non-standard customer effort must remain separable from low software-license pricing.
- Paid reusable feature estimates are Minor ₱5,000–₱10,000 / Small ₱10,000–₱20,000 / Medium ₱25,000–₱50,000 / Large from ₱60,000, subject to scope and V-008; no customer-specific source-code forks are permitted.
- All P10 commercial amounts are estimates. V-008 requires evidence-based repricing after the founding pilot and before the second normal commercial customer.
- The production MVP is the narrow end-to-end vertical slice in D-162; future convenience/integration complexity remains DESIGN NOW / IMPLEMENT LATER unless evidence makes it necessary.
- MVP implementation follows M0–M6 under D-164/D-177 and uses D-174 as the milestone Definition of Done.
- First production activation requires D-165 and D-173 qualification, including migration reconciliation, restore validation and at least one reconciled parallel payroll cycle.
- Client Portal remains the only product capability intentionally outside the core MVP; P11 design is complete and implementation remains post-core.

- P5 domain ownership follows D-073: modules may reference/query other modules but may not directly write another module's owned aggregates.
- Worker is a durable identity; each true rehire creates a new Employment/Engagement period.
- Employment/deployment are separate lifecycles; active undeployed/bench workers are not represented through fake client assignments.
- Multiple concurrent Deployments are allowed, but every payable attendance segment must resolve to one deployment context.
- Material transfers create successor Deployments; effective separation bounds all active/future deployments and schedules.
- Effective-dated single-value configuration must reject ambiguous overlaps unless explicit combination/priority semantics exist.
- Attendance preserves raw evidence and uses auditable adjustments plus an interpreted Attendance Day lifecycle.
- Payroll consumes frozen/versioned inputs and finalized Payroll Results are immutable.
- Retroactive corrections originate in the owning source domain and propagate as explicit downstream adjustments once financial records are finalized.
- One Employment/Engagement has one effective Payroll Group at a time; one Payroll Run creates one consolidated Payroll Result per Worker/Employment.
- Payslips are projections of finalized Payroll Results, not independent financial records.
- Compensation rules and client billing rules are separate effective-dated histories.
- Billing freezes cycle-specific support data; finalized billing support is adjusted, not silently rewritten.
- Payroll records the exact Compliance Rule-Pack/rule version used; statutory rule ownership remains in Compliance Rules.
- Requirements/Documents remain distinct; legally mandatory/non-overridable constraints must be verified per V-009 before implementation.
- Reporting owns no authoritative transactional data.

- Compliance Rule Packs are signed, immutable after release, versioned, effective-dated, and retained for historical reproduction.
- Rule Packs contain declarative data/configuration only; arbitrary executable scripts/plugins are prohibited.
- Statutory applicability uses the verified date/basis for each rule family; unresolved overlapping rules are rejected rather than guessed.
- Rule Pack activation requires integrity/schema/compatibility validation plus regression testing and controlled approval.
- Finalized payroll is never silently recalculated by a newer or rolled-back Rule Pack; corrections remain explicit adjustments.
- Statutory report/export definitions are versioned and must be verified against the current official format before release.
- A Compliance Register must track authoritative source/provenance, effectivity, HRIS version mapping, verification status, and tests for each rule/output family.
- Customer agencies are normally modeled as PIC for workforce data they control; vendor PIP status depends on actual customer-directed processing and must match real contractual activity.
- Vendor support has no standing production-personal-data access; deeper access is explicit, minimized, purpose-limited, and audited.
- Privacy retention is category/basis-specific; no universal retention period is assumed. P9 completes operational privacy/security mechanics.


- P7 technology baseline is Java 25 LTS, Spring Boot 4.1.x, Vaadin 25.3.x, MySQL 8.4 LTS, and Maven; exact patch/runtime distribution remains release-time verification.
- The implementation is one Maven multi-module modular monolith producing one deployable Spring Boot application.
- Heavy payroll/import/report work uses durable database-backed batch jobs; browser disconnects must not cancel business processing.
- Payroll calculation is chunked, restartable, idempotent, snapshot-based, and bounded in parallelism; finalization is explicitly serialized.
- D-116 payroll times and D-120 session-memory values are unbenchmarked targets and require P8 validation.
- JPA is the default persistence model, with module-owned JDBC permitted for proven high-volume paths.
- MySQL query/index discipline and measured tuning come before replicas, sharding, distributed cache, or partitioning.
- Vaadin screens must lazy-load and must never materialize full high-volume tables into a user session.
- Large reports/exports run in the background, stream output, and yield resources to payroll during contention.
- One explicit IANA business timezone governs the installation; technical instants use UTC while business dates remain local-date concepts.
- Core operation cannot depend on NTP/internet availability, but clock-health problems must be visible and diagnosable.
- Application behavior must not depend on Windows-only case-insensitivity, host locale, host fonts, or platform-specific path/line-ending assumptions.
- A general event broker, Redis, Quartz cluster, reporting database, and general outbox are not baseline dependencies; add them only for demonstrated requirements.


- Private GitHub plus GitHub Actions is the development/CI baseline, but deployed HRIS operation and local Maven builds must not depend on GitHub availability.
- Protected `main` requires explicit human merge approval; AI agents may prepare branches/commits/PRs but may not bypass this integration gate.
- Real customer production data is prohibited from normal development repositories, CI, AI-agent context, fixtures, and performance datasets; sanitized synthetic scenarios are the normal regression mechanism.
- Meaningful coding work uses durable repository task specifications; architecture and product knowledge must remain recoverable without chat history.
- Root/module agent rules, Cursor rules, and documentation must point to one canonical repository truth rather than duplicate competing specifications.
- Architecture/module constraints must be enforced by Maven dependency structure and executable architecture tests where practical.
- MySQL-specific persistence/concurrency behavior must be validated against real MySQL, normally through ephemeral Testcontainers-style instances rather than H2 substitution.
- The synthetic-data generator must support deterministic S/M/L/XL profiles through at least 100,000 active employees plus realistic historical and operational volume.
- Ordinary PR CI stays bounded; authoritative full-scale performance qualification runs scheduled/on-demand and before release on controlled self-hosted benchmark infrastructure.
- Approved performance baselines are version-controlled and may be treated as hard gates only after repeatability is demonstrated; unmeasured budgets remain TARGET — NOT YET BENCHMARKED.
- Dual-OS CI is risk-based: both Windows and Ubuntu are explicitly validated, while expensive installer/service/upgrade qualification may run outside every PR.

- Normal HRIS LAN access uses HTTPS; plain HTTP is not a general LAN production mode.
- Identity & Access remains the exclusive security-principal/RBAC owner; password storage uses a benchmarked adaptive hash and sessions are bounded/configurable.
- Core authentication/security controls must remain functional without internet; privileged offline TOTP may be used without a cloud dependency.
- OS full-disk encryption is recommended where practical; removable/offsite backups containing customer data must be strongly encrypted and recoverable with protected key material.
- Normal vendor support remains privacy-minimized and has no standing access to customer production personal/payroll data.
- Security/privacy incident procedures must be current-NPC verified and deployment-role aware; legal notification duties are never guessed from stale planning text.
- InnoDB crash recovery is the first normal response to unclean shutdown; unsupported ad-hoc editing of live InnoDB files is not a standard repair path.
- Backups are versioned complete Recovery Sets, not database-only files, and include integrity manifests plus all non-database operational content needed to reconstruct the installation.
- Separate backup storage is still optional as an installation prerequisite under D-009, but the supported operational backup model expects a separate copy when the customer wants recoverability from host/storage loss.
- Default backup-generation retention is 7 daily + 4 weekly + 12 monthly and never overrides authoritative legal/business record-retention requirements.
- Backup success requires automated integrity validation; recurring isolated restore drills are part of supported operations.
- D-155 backup/RPO/RTO/restore values are TARGET — NOT YET BENCHMARKED and must be validated under V-021.
- L/XL baseline recovery uses protected MySQL binary logs for point-in-time recovery objectives; S/M may enable the same capability where risk requirements justify it.
- Flyway is the application-schema migration baseline. Released migrations are immutable, forward-ordered, and qualified through supported upgrade tests.
- Failed application/schema upgrades recover from a verified pre-upgrade Recovery Set when safe forward repair is inappropriate; in-place database downgrade is not a normal rollback strategy.
- Compliance Rule Pack updates remain a lifecycle separate from application/schema upgrades and never rewrite finalized payroll history.
- Legacy data import uses staging, dry run, reconciliation, duplicate review, provenance, idempotent chunked commit, and a pre-import Recovery Set for production commits.
- Clock-health thresholds are operational warnings/critical conditions, not hidden payroll calculation inputs and not a reason to make NTP/internet mandatory.
- Disaster-recovery procedures must be executable runbooks exercised through restore/upgrade qualification, covering power loss, corrupted app install, DB startup failure, disk exhaustion, bad updates/migrations/Rule Packs, server loss/replacement, wrong clocks, and extended internet outage.

### P11 Portal Constraints

- Client Portal is optional and outside the core MVP; implementation starts only after the on-premises core is stable enough to justify the add-on.
- No portal design may require inbound internet connectivity to the agency server or expose MySQL/on-premises application listeners publicly.
- The on-premises HRIS/database remains authoritative. Cloud projections/actions never become a second HR/payroll system of record.
- Portal outage, subscription lapse, protocol incompatibility, or internet loss must never stop core recruitment, attendance, payroll, compliance, reporting, or customer data export.
- Cloud synchronization is data-minimized, allow-list based, versioned, idempotent, retryable, and auditable.
- The portal-specific durable outbox/persisted handoff is justified for guaranteed post-commit delivery; it must not be generalized into unnecessary distributed messaging infrastructure.
- Portal client actions use public stable IDs and optimistic versions. Stale approvals are rejected rather than merged through last-write-wins.
- Portal identities/authentication remain separate from on-premises HRIS credentials and must be scoped to the permitted Client Company/Site context.
- Cloud retention is intentionally shorter than authoritative on-premises history and remains subject to verified privacy/contract requirements.
- Portal compatibility is bounded by the supported HRIS release window; unsupported portal sync is disabled safely without affecting the local perpetual application.
- Portal subscription is commercially separate from the on-premises perpetual license. Active maintenance may be required for portal compatibility/entitlement without changing D-016's perpetual-use rule.
- Portal hosting/provider/region/identity-service choices must be verified at implementation; no stale provider capability, pricing, or data-residency assumption may become a release fact.

## FINAL IMPLEMENTATION HANDOFF STATUS

Status: **FROZEN — PLANNING AND COMPILATION COMPLETE**

Planning phases P1–P11 are COMPLETE. Requirements coverage C1–C27 is RESOLVED at planning/design level. Open `V-###` items remain implementation/release/pilot/external-fact verification obligations and are not unresolved product-design decisions.

This file is a frozen planning artifact and must not become the mutable implementation-progress state.

Implementation has **not** started.

Next implementation milestone:

**M0 — Engineering Foundation**

First implementation task:

**IMP-001 — Create Maven multi-module repository skeleton and one deployable application**

The authoritative implementation specification is:

`MASTER_SOFTWARE_PLAN.md`

When implementation begins in a later session, create a separate mutable implementation-state mechanism rather than editing this frozen planning snapshot.
