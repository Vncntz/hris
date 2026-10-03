# HRIS implementation state

Last updated: 2026-10-03. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 — Engineering Foundation is complete through [IMP-011](tasks/IMP-011.md).
- M1 — Workforce Foundation: [IMP-012](tasks/IMP-012.md) is complete after TASK-0031 corrective verification; [IMP-013](tasks/IMP-013.md) remains ACTIVE. TASK-0022 through TASK-0028 are Complete / `POST_MERGE_VERIFIED`; see the [task index](../tasks/README.md).
- Active work: [IMP-013](tasks/IMP-013.md) / [TASK-0029](../tasks/TASK-0029.md), recent credential re-authentication, explicitly authorized as one combined TASK/implementation PR. Status: **LOCAL_VERIFIED**; 136 ordinary and 100 MySQL integration tests passed locally. Exact-head CI and integration gates remain required.
- [IMP-012](tasks/IMP-012.md)'s corrective reopening is closed by TASK-0031, Complete / `POST_MERGE_VERIFIED`.

## Immediately relevant predecessor

[TASK-0031](../tasks/TASK-0031.md) / [PR #54](https://github.com/Vncntz/hris/pull/54)
final head `074f9fb8fd5b1cf7af58b24990f184aafbbe8a1c` passed policy/Linux/Windows
in [run 37083718233](https://github.com/Vncntz/hris/actions/runs/37083718233).
Protected merge `109fa7eb5e7c5e306904deada5c52eb199857754` passed all three jobs
in exact-merge-SHA [push run 37084121330](https://github.com/Vncntz/hris/actions/runs/37084121330).
The Agency correction closes TASK-0030's failed PR #53 / push run 37080929900 chain.
TASK-0028's Role/Permission administration, Role generation invalidation, and V8 remain integrated.

## Active durable decisions

- The operator authorized sequential IMP-088 workflow-efficiency maintenance. TASK-0019/0020/0021 are post-merge verified; later slices require separate focused authorization. Context packets never select work or establish gates.
- The external planner/reviewer handles selection, architecture reasoning, work-order preparation, and independent completion review. Codex implements authorized work and then enters the dedicated trusted-main integration procedure; development conclusions do not prove integration gates.
- Normal work uses one combined IMP/TASK + implementation PR after explicit authorization of the exact pair. No recurring planning PR or standalone closeout-only PR is required. The next combined PR reconciles verified predecessor completion. [Governance PR #19](https://github.com/Vncntz/hris/pull/19), merge `ea880d1ada243deec86e9a3a2c3e5c167c6a44dc`, and successful exact-merge [push run 36657908607](https://github.com/Vncntz/hris/actions/runs/36657908607) established this workflow.
- The latest operator authorization permits gated automatic protected-PR merge for eligible ordinary PRs only after independently passing every [integration gate](GIT_INTEGRATION_AGENT.md). A later instruction/task can require human merge. Dependabot remains human-reviewed/merged unless separately authorized. No protection bypass is granted.
- The repository is public by explicit operator decision, overriding D-131's private baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and forbids force-push/deletion. Zero approving reviews are required in the solo-developer phase. The operator directed remote settings remain unchanged; Linux/Windows checks are mandatory repository policy even though not GitHub-required checks. Verify actual remote protection at integration; this snapshot is not enforcement evidence.
- The one-time PR #11 exception remains limited to merge `ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head policy/Linux/Windows CI plus manual CI on that merge closed its missing push gate. PR policy supplied independent frozen-plan evidence. Future PRs still require exact-merge `push` CI.
- No post-planning ADR is approved; use the [ADR index](../architecture/adr/README.md) and targeted [planning index](../planning/INDEX.md). Product ownership and architecture decisions remain unchanged.

## Blockers and open verification

No predecessor gate remains open. TASK-0029 requires all acceptance and verification gates;
completion is not yet claimed. Recovery/reset, offline TOTP MFA, password-cost qualification,
final access-management closure, `repo-policy.py`, and IMP-014 remain outside this task.

## Next action

Integrate only locally verified IMP-013 / TASK-0029 through exact-head CI, fresh trusted-main
integration, protected merge and exact-merge push verification. Stop after TASK-0029.
