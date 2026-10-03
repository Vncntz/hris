# HRIS implementation state

Last updated: 2026-10-03. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 — Engineering Foundation is complete through [IMP-011](tasks/IMP-011.md).
- M1 — Workforce Foundation: [IMP-012](tasks/IMP-012.md) is complete after TASK-0031 corrective verification; [IMP-013](tasks/IMP-013.md) remains ACTIVE. TASK-0022 through TASK-0028 are Complete / `POST_MERGE_VERIFIED`; see the [task index](../tasks/README.md).
- Active work: [IMP-013](tasks/IMP-013.md) / [TASK-0032](../tasks/TASK-0032.md), corrective recent re-authentication lockout implementation. Status: **LOCAL_VERIFIED; integration and exact-merge verification pending**. The planning gate passed and the operator separately authorized only this correction. Local Windows verification passed 141 ordinary tests; full Linux verification passed 141 ordinary and 105 MySQL tests, confirmed from XML reports. Tooling and preservation checks passed; see TASK-0032.
- [TASK-0029](../tasks/TASK-0029.md) is **REOPENED - security correction required**. Valid exact-head and exact-merge CI does not establish full completion after the independently confirmed lockout finding.
- [IMP-012](tasks/IMP-012.md)'s corrective reopening is closed by TASK-0031, Complete / `POST_MERGE_VERIFIED`.

## Immediately relevant predecessor

[TASK-0029](../tasks/TASK-0029.md) / [PR #55](https://github.com/Vncntz/hris/pull/55)
final head `b33cee888d0a15a57201bd69fefbb9e06b8f872e` passed policy/Linux/Windows
in exact-head `pull_request` [run 37091776395](https://github.com/Vncntz/hris/actions/runs/37091776395).
Protected merge `6b6a9d0f433352f36a8b7be7b360a26c5f9296d1` passed all three jobs
in exact-merge main `push` [run 37092203330](https://github.com/Vncntz/hris/actions/runs/37092203330).
Actual logs confirm 136 ordinary tests per platform, 100 Linux MySQL integration tests
and 42 tooling tests, with zero failures/errors/skips. Head and merge trees are identical.
The unresolved
[PR #55 finding](https://github.com/Vncntz/hris/pull/55#discussion_r4171478811)
is confirmed: recent proof bypasses active temporary locks and failed-attempt bookkeeping.
TASK-0029's CI/integration chain is valid, but security completion requires TASK-0032.
TASK-0031 remains complete and closes TASK-0030's failed chain / IMP-012 reopening.

TASK-0032 planning [PR #56](https://github.com/Vncntz/hris/pull/56) merged as
`64a225df2f6f295db9864fd39e3908cc51176d52`, matching refreshed main at implementation start.
Exact-merge main `push` [run 37097947565](https://github.com/Vncntz/hris/actions/runs/37097947565)
passed policy/Linux/Windows. Actual logs confirm 42 tooling tests, 136 ordinary tests per
platform and 100 Linux MySQL tests, with zero failures/errors/skips. The six-file planning
package is present and has no superseding main commit at implementation start.

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

The predecessor CI/integration gates passed, but TASK-0029's lockout security finding
remains open until TASK-0032 corrective implementation is independently integrated and
post-merge verified. TASK-0032 implementation requires exact-final-head policy/Linux/Windows,
every trusted-main integration gate, protected merge and exact-merge main push CI.
Recovery/reset, offline TOTP MFA, password-cost qualification, final access-management
closure, `repo-policy.py` and IMP-014 remain deferred.

## Next action

Integrate only the locally verified IMP-013 / TASK-0032 under the current operator
authorization. Do not mark TASK-0032 complete or TASK-0029 corrected before exact implementation
merge-SHA push CI passes. After verification, review remaining IMP-013 scope without allocating
TASK-0033 or starting recovery/reset, TOTP MFA, password-cost qualification or IMP-014.
