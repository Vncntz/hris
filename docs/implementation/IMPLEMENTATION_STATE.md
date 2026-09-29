# HRIS implementation state

Last updated: 2026-09-29. Planning and compilation are complete; implementation is in progress.

## Current milestone and task

M0 — Engineering Foundation. [IMP-004](tasks/IMP-004.md) / [TASK-0007](../tasks/TASK-0007.md) is the active implementation work. The real MySQL baseline passed local verification; PR CI and integration verification are pending.

Last completed work order: [TASK-0006](../tasks/TASK-0006.md) under completed [IMP-087](tasks/IMP-087.md). The last completed normal implementation item is [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md).

## Active implementation decisions

- The latest explicit operator instruction authorizes gated automatic merge for ordinary eligible PRs. A later explicit instruction or task can require human merge.
- The repository is public by the user's explicit decision, overriding the frozen private-repository baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and disallows force-push and deletion. The user directed that protection remain unchanged after IMP-003; `ci / build-linux` and `ci / build-windows` are policy checks but are not GitHub-required status checks. Do not change remote settings without a separate explicit decision.
- The dedicated Git Integration Agent may merge only after independently passing all current [integration gates](GIT_INTEGRATION_AGENT.md). This does not grant protection bypass. Dependabot PRs remain human-reviewed unless separately authorized later.
- The operator accepted a one-time exception for PR #11 / merge commit `ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head PR policy/Linux/Windows CI and manual CI on that merge SHA close its missing post-merge `push` run gate. The PR policy job, rather than the manual policy self-comparison, supplies independent frozen-plan evidence. Future PRs retain the normal post-merge `push` CI requirement.

## Blockers and open verification

- TASK-0007 local verification passed; exact-head PR CI, trusted-main integration gates, merge, and normal post-merge `push` CI remain open.
- Official Spring Boot/Testcontainers/MySQL image sources were checked. Boot 4.1.1 manages Testcontainers 2.0.5 and Connector/J 9.7.0; the test uses the official `mysql:8.4.11` image tag.
- IMP-011 owns architecture tests; TASK-0007 has established the first real-MySQL integration test locally, pending PR CI and integration.
- No approved ADRs or open product/architecture decisions are recorded.

## Completed evidence

- [IMP-001](tasks/IMP-001.md) records the Maven skeleton, full local reactor, application startup, HTTP, shutdown, and wrapper verification.
- [IMP-002](tasks/IMP-002.md) / [TASK-0001](../tasks/TASK-0001.md) record repository governance and branch-protection evidence.
- [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md) record the Linux/Windows CI baseline and passing post-merge `main` CI.
- [IMP-087](tasks/IMP-087.md) is complete; [TASK-0003](../tasks/TASK-0003.md) through [TASK-0006](../tasks/TASK-0006.md) merged in order with required post-merge verification.

## Next action

Open the focused TASK-0007 PR, verify exact-head policy/Linux/Windows CI and the Linux MySQL test, then run the protected-main integration gates and normal post-merge `push` CI.
