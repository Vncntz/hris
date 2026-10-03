# HRIS implementation state

Last updated: 2026-10-03. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 — Engineering Foundation is complete through [IMP-011](tasks/IMP-011.md).
- M1 — Workforce Foundation: [IMP-012](tasks/IMP-012.md) was complete after fresh-cycle closure review and is now reopened for TASK-0031; [IMP-013](tasks/IMP-013.md) remains ACTIVE. TASK-0022 through TASK-0028 are Complete / `POST_MERGE_VERIFIED`; see the [task index](../tasks/README.md).
- Active work: corrective [IMP-012](tasks/IMP-012.md) / [TASK-0031](../tasks/TASK-0031.md), correct Agency concurrent initialization. Status: **LOCAL_VERIFIED**; exact-head CI, independent integration, and exact-merge push verification remain required. IMP-012 is reopened for this regression; historical closure and verified tasks remain preserved.
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

TASK-0030 merged through [PR #53](https://github.com/Vncntz/hris/pull/53): final head `af75040063c0184b745bdfc987f58750647e314c` passed all three jobs in [run 37080447796](https://github.com/Vncntz/hris/actions/runs/37080447796); actual merge `a647975fa7fd4a6ef2e6d0919fea70eacf7ee6b6` failed Linux in exact-merge [push run 37080929900](https://github.com/Vncntz/hris/actions/runs/37080929900), while policy and Windows passed. Agency concurrent initialization returned two successes. TASK-0030 is **post-merge blocked**, not POST_MERGE_VERIFIED. The operator explicitly authorized TASK-0031 as the corrective slice despite this failed predecessor; later work remains blocked until corrective exact-merge push CI passes.

IMP-013 remains open for its remaining secure-access obligations. TASK-0031 authorizes no IMP-013 behavior or IMP-014 advancement. TASK-0029 stays reserved and untouched. `repo-policy.py` remains deferred without a TASK allocation.

## Next action

Integrate only IMP-012 / TASK-0031 through exact-head CI, fresh trusted-main review, protected merge, and exact-merge push CI. Reconcile the TASK-0030 failed chain only after successful corrective post-merge verification; the next authorized task PR carries tracked completion metadata under normal governance. Stop after TASK-0031.
