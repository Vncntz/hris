# HRIS implementation state

Last updated: 2026-10-04. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 is complete through [IMP-011](tasks/IMP-011.md).
- M1 - Workforce Foundation: [IMP-012](tasks/IMP-012.md) and [IMP-013](tasks/IMP-013.md) are complete after verified closeout and fresh completion review.
- Active work: [IMP-014](tasks/IMP-014.md) / [TASK-0037](../tasks/TASK-0037.md), correction of Client reference reads joining ambient transactions. Status: LOCAL_VERIFIED; integration and exact-merge verification pending under exact operator authorization for one combined work-order and implementation PR.
- IMP-014 remains ACTIVE pending fresh post-TASK-0037 review. IMP-015 is not started.

## Immediately relevant predecessor

TASK-0036 [PR #61](https://github.com/Vncntz/hris/pull/61), final head
`308ddbfb2a9506e9cdc6c08f388f524f5c673339`, merged as
`b7f834f2b1ea560f13d43b1c0589b46c336b16da`. Exact-head pull-request
[run 37130865630](https://github.com/Vncntz/hris/actions/runs/37130865630) and exact-merge
main push [run 37131439302](https://github.com/Vncntz/hris/actions/runs/37131439302)
passed policy/Linux/Windows; job metadata and successful build steps independently verified.
TASK-0036 is Complete / POST_MERGE_VERIFIED. Fresh review found the ambient stale-snapshot
reference-read gap; TASK-0037 corrects it. Refreshed main matches the reviewed baseline.

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

TASK-0037 local verification passed: Windows 171 ordinary; Linux 171 ordinary / 154
real-MySQL tests; focused Client unit tests 14 and focused MySQL tests 15;
architecture tests 9; tooling tests 42. Final 26 Surefire / 19 Failsafe reports
have zero failures/errors/skips/flaky/rerun results. All 180 implementation input
hashes match the Linux and Windows sources. The negative control reproduces the
original stale Company activity with only the propagation fix removed.
See TASK-0037 for commands and evidence. Exact-head CI, trusted-main integration and
exact-merge push CI remain required.
No predecessor CI blocker remains.
Total administrator access loss, supported-hardware password-cost qualification, key
rotation tooling, broad Client UI and `repo-policy.py` remain deferred.

## Next action

Integrate only the locally verified IMP-014 / TASK-0037 combined PR, independently
evaluating every trusted-main gate before protected integration. Completion requires exact-merge push CI.
Keep IMP-014 ACTIVE pending fresh post-TASK-0037 review; do not start IMP-015.
