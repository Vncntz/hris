# HRIS implementation state

Last updated: 2026-09-29. Planning and compilation are complete; implementation is in progress.

## Current milestone and task

M0 — Engineering Foundation. [IMP-004](tasks/IMP-004.md) is the active planned implementation item. [TASK-0007](../tasks/TASK-0007.md) defines its first focused work order. MySQL/Testcontainers implementation has not started.

Last completed work order: [TASK-0006](../tasks/TASK-0006.md) under completed [IMP-087](tasks/IMP-087.md). The last completed normal implementation item is [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md).

## Active implementation decisions

- The latest explicit operator instruction authorizes gated automatic merge for ordinary eligible PRs. A later explicit instruction or task can require human merge.
- The repository is public by the user's explicit decision, overriding the frozen private-repository baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and disallows force-push and deletion. The user directed that protection remain unchanged after IMP-003; `ci / build-linux` and `ci / build-windows` are policy checks but are not GitHub-required status checks. Do not change remote settings without a separate explicit decision.
- The dedicated Git Integration Agent may merge only after independently passing all current [integration gates](GIT_INTEGRATION_AGENT.md). This does not grant protection bypass. Dependabot PRs remain human-reviewed unless separately authorized later.

## Blockers and open verification

- No current blocker is known for TASK-0007.
- Before implementation pins or adds database-test dependencies, verify the current Testcontainers version/integration convention, exact MySQL 8.4 LTS-line image tag, MySQL JDBC version when not BOM-managed, and Spring Boot 4.1.x Testcontainers conventions from official sources.
- No current architecture-test or MySQL integration suite exists; IMP-011 owns architecture tests and TASK-0007 will establish the first real-MySQL integration baseline.
- No approved ADRs or open product/architecture decisions are recorded.

## Completed evidence

- [IMP-001](tasks/IMP-001.md) records the Maven skeleton, full local reactor, application startup, HTTP, shutdown, and wrapper verification.
- [IMP-002](tasks/IMP-002.md) / [TASK-0001](../tasks/TASK-0001.md) record repository governance and branch-protection evidence.
- [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md) record the Linux/Windows CI baseline and passing post-merge `main` CI.
- [IMP-087](tasks/IMP-087.md) is complete; [TASK-0003](../tasks/TASK-0003.md) through [TASK-0006](../tasks/TASK-0006.md) merged in order with required post-merge verification.

## Next action

Execute [TASK-0007](../tasks/TASK-0007.md) in a separate implementation task/chat. Do not implement it as part of this planning change.
