# HRIS implementation state

Last updated: 2026-10-03. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 is complete through [IMP-011](tasks/IMP-011.md).
- M1 - Workforce Foundation: [IMP-012](tasks/IMP-012.md) and [IMP-013](tasks/IMP-013.md) are complete after verified closeout and fresh completion review.
- Active work: [IMP-014](tasks/IMP-014.md) / [TASK-0036](../tasks/TASK-0036.md), Client Company/Site master-data lifecycle foundation. Status: LOCAL_VERIFIED; integration and exact-merge verification pending under exact operator authorization for one combined work-order and implementation PR.
- TASK-0035 is Complete / POST_MERGE_VERIFIED. Historical evidence remains in the [task index](../tasks/README.md).

## Immediately relevant predecessor

TASK-0035 [PR #60](https://github.com/Vncntz/hris/pull/60), final head
`33020a466bd320940dd2c52cdf9c87c87c97db0c`, merged as
`b8668b422c9651375c1f856be896f322729ecd13`. Exact-head pull-request
[run 37123541949](https://github.com/Vncntz/hris/actions/runs/37123541949) and exact-merge
main push [run 37124138201](https://github.com/Vncntz/hris/actions/runs/37124138201)
passed policy/Linux/Windows. Independent metadata, steps and logs establish completion:
156 ordinary tests per platform, 139 Linux MySQL tests and 42 tooling tests, zero failures/errors/skips.
Fresh IMP-013 review confirms the functional completion boundary; operational qualification
and broad administration adapters remain later scope. Refreshed main matches the reviewed base.

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

TASK-0036 local verification passed: Windows 171 ordinary; Linux 171 ordinary / 153
real-MySQL tests; final affected MySQL 18, including 14 Client; Client unit tests 14;
architecture tests 9; tooling tests 42. Actual 26 Surefire / 19 Failsafe reports have
zero failures/errors/skips/flaky/rerun results, and all 180 implementation input hashes
match both isolated builds. See the TASK for commands and security/migration evidence.
Exact-final-head CI, trusted-main review and exact-merge push CI remain required.
No predecessor blocker remains.
Total administrator access loss, supported-hardware password-cost qualification, key
rotation tooling, broad Client UI and `repo-policy.py` remain deferred.

## Next action

Integrate only the locally verified IMP-014 / TASK-0036 combined PR, independently evaluating
every trusted-main gate before protected integration. Do not call it complete before exact-merge push CI passes.
After completion, review IMP-014 afresh. Do not allocate TASK-0037 or start IMP-015.
