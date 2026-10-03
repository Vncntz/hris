# HRIS implementation state

Last updated: 2026-10-03. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 — Engineering Foundation is complete through [IMP-011](tasks/IMP-011.md).
- M1 — Workforce Foundation: [IMP-012](tasks/IMP-012.md) is complete after fresh-cycle closure review; [IMP-013](tasks/IMP-013.md) remains ACTIVE. TASK-0022 through TASK-0028 are Complete / `POST_MERGE_VERIFIED`; see the [task index](../tasks/README.md).
- Active work: maintenance [IMP-088](tasks/IMP-088.md) / [TASK-0030](../tasks/TASK-0030.md), simplify AI workflow and execution-state context. Status: **LOCAL_VERIFIED**; exact-head, integration, and exact-merge push gates remain required before completion. Local evidence is in its work order.
- TASK-0029 is reserved by the operator for IMP-013 recent credential re-authentication. Its existing untracked local draft is preserved outside this PR; this maintenance task grants no product implementation authority.

## Immediately relevant predecessor

[TASK-0028](../tasks/TASK-0028.md) / [PR #51](https://github.com/Vncntz/hris/pull/51) is Complete / `POST_MERGE_VERIFIED`. Final head `237251ced7faa8c422dd66af75f7fb5715d2e3af` passed `ci / policy`, `ci / build-linux`, and `ci / build-windows` in [run 37022286478](https://github.com/Vncntz/hris/actions/runs/37022286478). Merge `5abd75b168bc517a7e8f828d1b1a66208f237b65` passed the same three jobs in exact-merge-SHA [push run 37023143564](https://github.com/Vncntz/hris/actions/runs/37023143564). Refreshed `origin/main` equals that merge at TASK-0030 allocation; no predecessor gate remains open.

Role/Permission administration, Role authority-generation invalidation, affected-session expiry, self-admin protection, and V8 are integrated. Detailed behavior, tests, and historical diagnostics remain in TASK-0028 and its PR; no product behavior changes in TASK-0030.

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

No predecessor blocker remains. TASK-0030's exact-head CI, protected integration, and post-merge verification remain pending until independently evidenced. Failed/pending required checks block completion and any next task.

IMP-013 remains open for its remaining secure-access obligations; consult its parent control record and prepare any future work order through fresh review. TASK-0030 does not authorize IMP-013 behavior or IMP-014 advancement. `repo-policy.py` is deferred to separate maintenance, conceptually TASK-0031 only if a fresh post-TASK-0030 review confirms ID availability and receives authorization.

## Next action

Verify and integrate only IMP-088 / TASK-0030 through its combined PR, exact-head CI, fresh trusted-main review, protected merge, and exact-merge push CI. Stop after TASK-0030. The next authorized task PR will reconcile its completion from GitHub evidence; do not pre-mark it complete.
