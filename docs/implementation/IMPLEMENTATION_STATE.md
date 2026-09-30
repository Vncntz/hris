# HRIS implementation state

Last updated: 2026-09-30. Planning and compilation are complete; implementation is in progress.

## Current milestone and task

M0 — Engineering Foundation. [IMP-006](tasks/IMP-006.md) / [TASK-0009](../tasks/TASK-0009.md) is `POST_MERGE_VERIFIED` / complete. [IMP-007](tasks/IMP-007.md) / [TASK-0010](../tasks/TASK-0010.md) is the active combined work-order and implementation PR #18; local implementation verification has passed, and final-head PR CI, integration, and post-merge CI remain pending.

Last completed work order: [TASK-0009](../tasks/TASK-0009.md) under completed [IMP-006](tasks/IMP-006.md).

## Active implementation decisions

- The latest explicit operator instruction authorizes gated automatic merge for ordinary eligible PRs. A later explicit instruction or task can require human merge.
- The operator authorized a single combined PR for each complete local IMP/TASK pair and its implementation after an explicit implementation prompt. Codex commits the work orders with the code; a separate planning PR is not required. The next task PR reconciles verified predecessor closeout.
- [Governance PR #19](https://github.com/Vncntz/hris/pull/19) merged as `ea880d1ada243deec86e9a3a2c3e5c167c6a44dc`; [post-merge `push` run 36657908607](https://github.com/Vncntz/hris/actions/runs/36657908607) passed policy, Linux, and Windows on that exact SHA, making the combined-PR workflow effective.
- The repository is public by the user's explicit decision, overriding the frozen private-repository baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and disallows force-push and deletion. The user directed that protection remain unchanged after IMP-003; `ci / build-linux` and `ci / build-windows` are policy checks but are not GitHub-required status checks. Do not change remote settings without a separate explicit decision.
- The dedicated Git Integration Agent may merge only after independently passing all current [integration gates](GIT_INTEGRATION_AGENT.md). This does not grant protection bypass. Dependabot PRs remain human-reviewed unless separately authorized later.
- The operator accepted a one-time exception for PR #11 / merge commit `ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head PR policy/Linux/Windows CI and manual CI on that merge SHA close its missing post-merge `push` run gate. The PR policy job, rather than the manual policy self-comparison, supplies independent frozen-plan evidence. Future PRs retain the normal post-merge `push` CI requirement.

## Blockers and open verification

- TASK-0010 combined PR #18 requires exact-head policy/Linux/Windows CI after adding the work orders, protected-main integration, and successful exact-merge-SHA `push` CI before completion.
- Official Spring Boot/Testcontainers/MySQL image sources were checked. Boot 4.1.1 manages Testcontainers 2.0.5 and Connector/J 9.7.0; the test uses the official `mysql:8.4.11` image tag.
- IMP-011 owns architecture tests; TASK-0007 established the first real-MySQL integration test and verified it locally and in Ubuntu CI.
- No approved ADRs or open product/architecture decisions are recorded.

## Completed evidence

- [IMP-001](tasks/IMP-001.md) records the Maven skeleton, full local reactor, application startup, HTTP, shutdown, and wrapper verification.
- [IMP-002](tasks/IMP-002.md) / [TASK-0001](../tasks/TASK-0001.md) record repository governance and branch-protection evidence.
- [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md) record the Linux/Windows CI baseline and passing post-merge `main` CI.
- [IMP-087](tasks/IMP-087.md) is complete; [TASK-0003](../tasks/TASK-0003.md) through [TASK-0006](../tasks/TASK-0006.md) merged in order with required post-merge verification.
- [IMP-004](tasks/IMP-004.md) / [TASK-0007](../tasks/TASK-0007.md) established disposable MySQL 8.4.11 testing. [PR #12](https://github.com/Vncntz/hris/pull/12) exact head `847228f5f67b9212ae77d5459e73728d560666ff` passed policy/Linux/Windows [PR CI](https://github.com/Vncntz/hris/actions/runs/36592276188). It merged as `00b59954db6c13a031d78434ea36ed8f0c744425`, and [post-merge `push` run 36592772489](https://github.com/Vncntz/hris/actions/runs/36592772489) passed `ci / policy`, `ci / build-linux`, and `ci / build-windows` on that merge SHA. Linux ran `./mvnw -B -Pmysql-it clean verify` and `MySqlDatasourceIT` against `mysql:8.4.11` with zero failures/errors/skips; Windows ran ordinary `.\mvnw.cmd -B clean verify` and passed the HTTP smoke test. This satisfies IMP-004's acceptance direction and closes the predecessor without a standalone closeout PR.
- [IMP-005](tasks/IMP-005.md) / [TASK-0008](../tasks/TASK-0008.md) established the global Flyway migration stream and technical V1 baseline. [Implementation PR #14](https://github.com/Vncntz/hris/pull/14) exact head `f11eb3663bcc0cc2b386849931b163291faa0c93` passed policy/Linux/Windows in [PR run 36647193876](https://github.com/Vncntz/hris/actions/runs/36647193876). It merged as `024143ee931c63a0c1847e1b4cf5fa4ca1288f98`; [post-merge `push` run 36647440772](https://github.com/Vncntz/hris/actions/runs/36647440772) on `main` at that SHA passed all three required jobs. IMP-005 / TASK-0008 is `POST_MERGE_VERIFIED` / complete.
- [IMP-006](tasks/IMP-006.md) / [TASK-0009](../tasks/TASK-0009.md) planning [PR #15](https://github.com/Vncntz/hris/pull/15) merged as `a1f0b52b490ad7e4c34870db29a9514b18b827ad`, with passing exact-SHA [post-merge `push` run 36650227495](https://github.com/Vncntz/hris/actions/runs/36650227495). Local implementation verification: `.\mvnw.cmd -B -pl shared-kernel test` passed 7 tests; `.\mvnw.cmd -B clean verify` passed all 17 modules, with 7 shared-kernel and 1 app smoke test in Surefire reports; `.\mvnw.cmd -B -pl shared-kernel dependency:tree` showed only test-scope JUnit dependencies; `python tools/plan-index.py --check` passed. [Implementation PR #16](https://github.com/Vncntz/hris/pull/16) exact head `745ac47b2b2e82b49e32fb76106de1c3e953f1df` passed policy/Linux/Windows [PR run 36651069582](https://github.com/Vncntz/hris/actions/runs/36651069582), merged as `2b95e6d609333b09605d392e6d141dc168de1f2f`, and passed [post-merge `push` run 36651279934](https://github.com/Vncntz/hris/actions/runs/36651279934) on that exact SHA. IMP-006 / TASK-0009 is `POST_MERGE_VERIFIED` / complete.

## Next action

Complete TASK-0010 PR #18's final-head verification and independent integration gates, merge through protected `main` when eligible, then verify required `push` CI on its exact merge SHA.
