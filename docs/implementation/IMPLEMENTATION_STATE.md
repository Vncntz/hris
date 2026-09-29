# HRIS implementation state

Last updated: 2026-09-29. Planning and compilation are complete; implementation is in progress.

## Current milestone and task

M0 — Engineering Foundation. [IMP-087](tasks/IMP-087.md) is an authorized four-PR maintenance initiative that temporarily precedes IMP-004. [TASK-0004](../tasks/TASK-0004.md) is active on `chore/imp-087-task-0004-context-state`. [TASK-0005](../tasks/TASK-0005.md) and [TASK-0006](../tasks/TASK-0006.md) must run sequentially after their predecessors merge and required post-merge verification passes.

Last completed work order: [TASK-0003](../tasks/TASK-0003.md) under active IMP-087. The last completed normal implementation item is [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md). IMP-004 remains ready for planning only and has not started.

## Active implementation decisions

- The latest explicit operator instruction authorizes gated automatic merge for all four IMP-087 PRs and future ordinary eligible PRs. It supersedes the earlier human merge requirement for TASK-0003 through TASK-0005. Each task still uses a separate branch and initially Draft PR. A later explicit instruction or task can require human merge. Resume normal implementation at IMP-004 after all four integrate; do not begin it automatically.
- The repository is public by the user's explicit decision, overriding the frozen private-repository baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and disallows force-push and deletion. The user directed that protection remain unchanged after IMP-003; `ci / build-linux` and `ci / build-windows` are currently policy checks but are not GitHub-required status checks. Do not change remote settings without a separate explicit decision.
- The dedicated Git Integration Agent may merge only after independently passing all current [integration gates](GIT_INTEGRATION_AGENT.md). The current operator session is the trust root for this bootstrap. This does not grant protection bypass. Dependabot PRs remain human-reviewed unless separately authorized later.

## Blockers and open verification

- TASK-0005 and TASK-0006 retain the sequential dependencies in [IMP-087](tasks/IMP-087.md). IMP-004 waits for this maintenance initiative.
- TASK-0004 local tooling and documentation verification passed; Draft PR, current-head CI, merge gates, and post-merge CI remain open. No current architecture-test or MySQL integration suite exists; those belong to IMP-011 and IMP-004 respectively.
- No approved ADRs or open implementation questions are recorded.

## Completed evidence

- [IMP-001](tasks/IMP-001.md) records the Maven skeleton, full local reactor, application startup, HTTP, shutdown, and wrapper verification.
- [IMP-002](tasks/IMP-002.md) / [TASK-0001](../tasks/TASK-0001.md) record repository governance, branch-protection readback, historical decisions, and [PR #2](https://github.com/Vncntz/hris/pull/2). IMP-001 merged via [PR #1](https://github.com/Vncntz/hris/pull/1).
- [IMP-003](tasks/IMP-003.md) / [TASK-0002](../tasks/TASK-0002.md) record local and Linux/Windows CI, [PR #3](https://github.com/Vncntz/hris/pull/3), governance [PR #4](https://github.com/Vncntz/hris/pull/4), and the passing [first `push`-to-`main` run](https://github.com/Vncntz/hris/actions/runs/36553948635). The IMP-003 post-merge CI gate passed.
- [TASK-0003](../tasks/TASK-0003.md) records governance hardening, [PR #6](https://github.com/Vncntz/hris/pull/6) merged as `179c2e8`, and passing [post-merge `main` CI](https://github.com/Vncntz/hris/actions/runs/36569966460).

## Next action

Commit TASK-0004 and open its focused Draft PR. Recheck every integration gate before merge and require passing post-merge `main` CI before TASK-0005.
