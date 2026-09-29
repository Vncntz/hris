# HRIS IMPLEMENTATION STATE

State Version: 1
Last Updated: 2026-09-29

## STATUS

Planning: COMPLETE
Compilation: COMPLETE
Implementation: IN PROGRESS

Current Milestone: M0 — Engineering Foundation
Current Task: None — IMP-003 awaits integration; IMP-004 is blocked
Last Completed Task: IMP-003

## AUTHORITATIVE PLANNING SOURCES

- `docs/planning/MASTER_SOFTWARE_PLAN.md`
- `docs/planning/FINAL_PLANNING_STATE.md`

These planning artifacts are frozen and must not be modified during normal implementation.

## CURRENT TASK

### Blocked next: IMP-004 — MySQL/Testcontainers development and test environment

Status: BLOCKED — not started; predecessor PR integration and post-merge `main` CI verification pending

Objective:

Create the MySQL/Testcontainers development and test environment.

Detailed task specification:

The IMP-004 control file and focused TASK work order have not yet been created. The [completed IMP-003 control file](tasks/IMP-003.md) and [TASK-0002 work order](../tasks/TASK-0002.md) record the CI baseline and the user's instruction to leave `main` protection unchanged.

## COMPLETED IMPLEMENTATION TASKS

IMP-001 — Maven multi-module repository skeleton and one deployable application (2026-09-29).
IMP-002 — Repository governance and documentation foundation; protected `main` under the approved solo-developer model (2026-09-29).
IMP-003 — Ubuntu/Windows Maven CI baseline and verified named build checks on its task branch; `main` protection unchanged by explicit user instruction (2026-09-29).

## ACTIVE IMPLEMENTATION DECISIONS

User-approved solo-developer governance (2026-09-29): normal changes to `main` require a PR with 0 required approving reviews while the repository has one human owner. Protection applies to administrators; force-push and deletion are disabled; no normal-development bypass is intentional. The human owner reviews the PR and verification evidence and makes the final merge decision. Agents may prepare branches and PRs but may not merge `main`, bypass protection, or change protection settings without explicit user authorization. A second human reviewer or separate non-admin agent credential is not required for this phase. Add required named CI checks only after IMP-003 creates and verifies them. If a genuine collaborator joins later, one required approval is recommended hardening.

The user explicitly made the repository public to enable protection, overriding the frozen private-repository baseline for the current repository. The frozen planning files remain unchanged.

User decision (2026-09-29): leave `main` branch protection unchanged after IMP-003 verified `ci / build-linux` and `ci / build-windows`; neither check is required. This overrides the planned D-131 required-check gate for the current repository until a later explicit decision. Three draft PRs were authorized for IMP-001, IMP-002, and IMP-003; the human owner retains the merge decision.

New implementation-specific decisions must be recorded here or through an approved ADR when appropriate.

## APPROVED ADRS

None.

## OPEN BLOCKERS

IMP-003 implementation and task-branch verification are complete, but draft PRs #1, #2, and #3 remain pending human review and integration into `main` in order. IMP-004 must not begin until all three predecessor PRs are integrated and the first post-merge `push`-to-`main` CI run for IMP-003 passes on Ubuntu and Windows. The user directed that `main` branch protection remain unchanged; neither build check is required. Architecture tests await IMP-011; MySQL integration coverage awaits IMP-004 and is not covered by the passing IMP-003 build checks.

The earlier IMP-002 private-repository protection and reviewer/credential-path blockers were resolved by the explicit public-repository and solo-developer decisions. Their historical evidence remains in `docs/implementation/tasks/IMP-002.md`.

## OPEN IMPLEMENTATION QUESTIONS

None.

## IMPLEMENTATION VERIFICATION ITEMS

IMP-003 verification (2026-09-29): local Windows `.\mvnw.cmd -B clean verify` passed all 17 reactor projects and the single smoke test with 0 failures, 0 errors, and 0 skips. GitHub Actions push runs `36546049750` and `36546445927` and PR run `36546891168` passed both `ci / build-linux` and `ci / build-windows`; logs from the first show `BUILD SUCCESS` and the same smoke-test result. The workflow was parsed and executed remotely. Draft PRs #1/#2/#3 were created for IMP-001/002/003; #3 is based on the IMP-002 branch and is mergeable. GitHub API readback still reports `required_status_checks: null` on `main`; the user directed no protection change. The run does not contain architecture or MySQL integration tests because those suites do not yet exist.

Latest IMP-003 correction verification (2026-09-29): commit `2df1553` added workflow `push` validation for `main` and concurrency that cancels older runs for the same PR while giving non-PR runs unique groups. GitHub Actions PR #3 run `36548541831` successfully executed the updated workflow; `ci / build-linux` and `ci / build-windows` both passed. The concurrency configuration was accepted and executed, though overlapping-run cancellation was not separately observed. Post-merge `main` CI remains pending. IMP-004 remains blocked until PRs #1–#3 are integrated and the first resulting `main` CI run passes on both jobs.

IMP-001 verification: `.\mvnw.cmd -B clean verify` passed with all 17 reactor projects successful; `HrisApplicationSmokeTest` ran once with 0 failures, 0 errors, and 0 skips. The packaged JAR started on port `18080`; logs confirmed Tomcat startup, Vaadin production mode, and `HrisApplication` Started. `/` returned HTTP 200 with `text/html;charset=utf-8` and Vaadin bootstrap HTML. Browser rendering of the static route text was unavailable for independent verification. Spring graceful shutdown completed, the process exited, and no port `18080` listener remained. Git mode for `mvnw` is `100755`.

IMP-002: GitHub REST independently read back `main` as protected and the classic rule as requiring PR integration with 0 approving reviews, administrator enforcement enabled, force-push and deletion disabled, and `required_status_checks: null`. GraphQL independently confirmed pattern `main`, approving reviews required with count 0, admin enforcement, no force-push/deletion, and no required status checks. No ruleset was added. The collaborators endpoint listed only owner `Vncntz`. Local verification resolved 55 relative links across 26 tracked Markdown files, checked TASK/ADR/index and PR workflow/template requirements, and passed `git diff --check` and full changed-path/scope review. No Maven/source/build or CI files changed, so no Maven rerun was required. Required CI/status checks are the IMP-003 handoff.

## M0 — ENGINEERING FOUNDATION

| ID | Task | Status |
|---|---|---|
| IMP-001 | Maven multi-module repository skeleton and one deployable application | COMPLETE |
| IMP-002 | AGENTS.md, task/ADR/docs structure, branch protection, PR conventions | COMPLETE |
| IMP-003 | CI baseline | COMPLETE |
| IMP-004 | MySQL/Testcontainers development and test environment | BLOCKED BY PR INTEGRATION AND POST-MERGE MAIN CI |
| IMP-005 | Flyway and migration-order conventions | BLOCKED BY IMP-004 |
| IMP-006 | UUID/public-ID, money, business-date, UTC instant, timezone primitives | READY |
| IMP-007 | Configuration/secrets/environment conventions | READY |
| IMP-008 | Append-only audit foundation | BLOCKED BY IMP-006 |
| IMP-009 | Authentication/authorization foundation | BLOCKED BY IMP-006 AND IMP-007 |
| IMP-010 | Deterministic synthetic-data generator skeleton | BLOCKED BY IMP-004 AND IMP-006 |
| IMP-011 | Executable architecture/module-boundary rules | READY |

## NEXT ACTION

Human owner reviews and merges draft PRs #1, #2, and #3 in order, with PR #2 and then PR #3 retargeted to `main` and PR #3 CI passing against the updated base before merge. Verify the first post-merge `main` push CI run passes both named jobs. Only then begin IMP-004 — MySQL/Testcontainers development and test environment. `main` protection stays unchanged per the user's explicit instruction. IMP-011 owns executable architecture tests; do not claim that suite currently passes.
