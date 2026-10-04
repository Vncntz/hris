# HRIS implementation state

Last updated: 2026-10-04. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 is complete through [IMP-011](tasks/IMP-011.md).
- M1 - Workforce Foundation: [IMP-012](tasks/IMP-012.md) and [IMP-013](tasks/IMP-013.md) are complete after verified closeout and fresh completion review.
- Active work: [IMP-014](tasks/IMP-014.md) / [TASK-0039](../tasks/TASK-0039.md), Client administration browse/query backend contract. Owner: CODEX. Status: LOCAL_VERIFIED; exact-head CI, trusted integration and exact-merge push CI pending.
- TASK-0037 remains Complete / POST_MERGE_VERIFIED. Fresh external review supplied with TASK-0039 found its foundation sound and selected bounded administrator browsing as the remaining backend gap. IMP-014 remains ACTIVE pending fresh external review after TASK-0039.

## Immediately relevant predecessor

TASK-0038 [PR #63](https://github.com/Vncntz/hris/pull/63), final head
`6ddb5af2b095542f396f13516c8f478a715ee536`, merged as
`babf78ea2cb38dd5ba74f87f2bca6bec46057cea`. Independently refreshed exact-head
[run 37165002074](https://github.com/Vncntz/hris/actions/runs/37165002074) and exact-merge
main push [run 37165401785](https://github.com/Vncntz/hris/actions/runs/37165401785)
passed policy/Linux/Windows, including their required verification steps.
TASK-0038 is Complete / POST_MERGE_VERIFIED; no predecessor CI blocker remains.

## Active durable decisions

- The operator authorized sequential IMP-088 workflow-efficiency maintenance. TASK-0019/0020/0021 are post-merge verified; later slices require separate focused authorization. Context packets never select work or establish gates.
- The external planner/reviewer handles selection, architecture reasoning, work-order preparation, and independent completion review. Exactly one primary implementer owns each TASK. Codex defaults to backend/tooling and trusted-main integration; Antigravity handles explicitly assigned Vaadin UI/browser work. Neither selects backlog work. Development/browser evidence does not prove integration gates; see root AGENTS and the two workspace skills.
- Normal work uses one combined IMP/TASK + implementation PR after explicit authorization of the exact pair. No recurring planning PR or standalone closeout-only PR is required. The next combined PR reconciles verified predecessor completion. [Governance PR #19](https://github.com/Vncntz/hris/pull/19), merge `ea880d1ada243deec86e9a3a2c3e5c167c6a44dc`, and successful exact-merge [push run 36657908607](https://github.com/Vncntz/hris/actions/runs/36657908607) established this workflow.
- The latest operator authorization permits gated automatic protected-PR merge for eligible ordinary PRs only after independently passing every [integration gate](GIT_INTEGRATION_AGENT.md). A later instruction/task can require human merge. Dependabot remains human-reviewed/merged unless separately authorized. No protection bypass is granted.
- The repository is public by explicit operator decision, overriding D-131's private baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and forbids force-push/deletion. Zero approving reviews are required in the solo-developer phase. The operator directed remote settings remain unchanged; Linux/Windows checks are mandatory repository policy even though not GitHub-required checks. Verify actual remote protection at integration; this snapshot is not enforcement evidence.
- The one-time PR #11 exception remains limited to merge `ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head policy/Linux/Windows CI plus manual CI on that merge closed its missing push gate. PR policy supplied independent frozen-plan evidence. Future PRs still require exact-merge `push` CI.
- No post-planning ADR is approved; use the [ADR index](../architecture/adr/README.md) and targeted [planning index](../planning/INDEX.md). Product ownership and architecture decisions remain unchanged.

## Blockers and open verification

TASK-0039 local verification passed: Windows 178 ordinary and Linux 178 ordinary / 158
MySQL tests, all final reports inspected with zero failures/errors/skips/flaky/reruns;
focused Client 21 unit / 18 MySQL tests, architecture checks, 42 tooling tests,
planning/task routing, local documentation links/anchors, whitespace, released-migration
preservation and full-diff/security/privacy review passed. Exact-final-head CI,
trusted-main gates and exact-merge push CI remain required. Total administrator access loss,
supported-hardware password-cost qualification, key rotation tooling, broad Client UI
and `repo-policy.py` remain deferred.

## Next action

Complete only IMP-014 / TASK-0039 and its combined work-order/implementation PR through
all required verification and protected integration gates. After successful exact-merge push CI,
report POST_MERGE_VERIFIED and stop. Fresh external review selects any later work or IMP closure.
No frontend implementation, TASK-0040, IMP-015, Position or other successor work is authorized.
