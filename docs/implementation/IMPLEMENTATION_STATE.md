# HRIS implementation state

Last updated: 2026-09-30. Planning and compilation are complete; implementation is in progress.

## Current milestone and task

M0 — Engineering Foundation. [IMP-004](tasks/IMP-004.md) / [TASK-0007](../tasks/TASK-0007.md) is `POST_MERGE_VERIFIED` / complete. [IMP-005](tasks/IMP-005.md) / [TASK-0008](../tasks/TASK-0008.md) planning PR #13 merged and passed required post-merge CI; implementation is in progress.

Last completed work order: [TASK-0007](../tasks/TASK-0007.md) under completed [IMP-004](tasks/IMP-004.md).

## Active implementation decisions

- The latest explicit operator instruction authorizes gated automatic merge for ordinary eligible PRs. A later explicit instruction or task can require human merge.
- The repository is public by the user's explicit decision, overriding the frozen private-repository baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and disallows force-push and deletion. The user directed that protection remain unchanged after IMP-003; `ci / build-linux` and `ci / build-windows` are policy checks but are not GitHub-required status checks. Do not change remote settings without a separate explicit decision.
- The dedicated Git Integration Agent may merge only after independently passing all current [integration gates](GIT_INTEGRATION_AGENT.md). This does not grant protection bypass. Dependabot PRs remain human-reviewed unless separately authorized later.
- The operator accepted a one-time exception for PR #11 / merge commit `ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head PR policy/Linux/Windows CI and manual CI on that merge SHA close its missing post-merge `push` run gate. The PR policy job, rather than the manual policy self-comparison, supplies independent frozen-plan evidence. Future PRs retain the normal post-merge `push` CI requirement.

## Blockers and open verification

- TASK-0008 implementation requires local verification, exact-head PR CI, trusted-main integration gates, merge, and normal post-merge `push` CI before completion.
- Official Spring Boot/Testcontainers/MySQL image sources were checked. Boot 4.1.1 manages Testcontainers 2.0.5 and Connector/J 9.7.0; the test uses the official `mysql:8.4.11` image tag.
- IMP-011 owns architecture tests; TASK-0007 established the first real-MySQL integration test and verified it locally and in Ubuntu CI.
- No approved ADRs or open product/architecture decisions are recorded.

## Completed evidence

- [IMP-001](tasks/IMP-001.md) records the Maven skeleton, full local reactor, application startup, HTTP, shutdown, and wrapper verification.
- [IMP-002](tasks/IMP-002.md) / [TASK-0001](../tasks/TASK-0001.md) record repository governance and branch-protection evidence.
- [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md) record the Linux/Windows CI baseline and passing post-merge `main` CI.
- [IMP-087](tasks/IMP-087.md) is complete; [TASK-0003](../tasks/TASK-0003.md) through [TASK-0006](../tasks/TASK-0006.md) merged in order with required post-merge verification.
- [IMP-004](tasks/IMP-004.md) / [TASK-0007](../tasks/TASK-0007.md) established disposable MySQL 8.4.11 testing. [PR #12](https://github.com/Vncntz/hris/pull/12) exact head `847228f5f67b9212ae77d5459e73728d560666ff` passed policy/Linux/Windows [PR CI](https://github.com/Vncntz/hris/actions/runs/36592276188). It merged as `00b59954db6c13a031d78434ea36ed8f0c744425`, and [post-merge `push` run 36592772489](https://github.com/Vncntz/hris/actions/runs/36592772489) passed `ci / policy`, `ci / build-linux`, and `ci / build-windows` on that merge SHA. Linux ran `./mvnw -B -Pmysql-it clean verify` and `MySqlDatasourceIT` against `mysql:8.4.11` with zero failures/errors/skips; Windows ran ordinary `.\mvnw.cmd -B clean verify` and passed the HTTP smoke test. This satisfies IMP-004's acceptance direction and closes the predecessor without a standalone closeout PR.

## Next action

Finish TASK-0008 implementation on its focused branch and PR, then verify protected-main integration and post-merge `push` CI before marking the task complete.
