# HRIS implementation state

Last updated: 2026-10-03. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 — Engineering Foundation is complete through [IMP-011](tasks/IMP-011.md).
- M1 — Workforce Foundation: [IMP-012](tasks/IMP-012.md) is complete after TASK-0031 corrective verification; [IMP-013](tasks/IMP-013.md) remains ACTIVE. TASK-0022 through TASK-0028 are Complete / `POST_MERGE_VERIFIED`; see the [task index](../tasks/README.md).
- Active work: [IMP-013](tasks/IMP-013.md) / [TASK-0034](../tasks/TASK-0034.md), authenticated administrative credential reset. Status: **LOCAL_VERIFIED; integration and exact-merge verification pending** under explicit operator authorization for one combined work-order and implementation PR.
- [TASK-0033](../tasks/TASK-0033.md) is Complete / `POST_MERGE_VERIFIED` after PR #58.
- [TASK-0032](../tasks/TASK-0032.md) is Complete / `POST_MERGE_VERIFIED` and corrects [TASK-0029](../tasks/TASK-0029.md)'s recent-proof lockout finding. TASK-0029 is complete after that correction.
- [IMP-012](tasks/IMP-012.md)'s corrective reopening is closed by TASK-0031, Complete / `POST_MERGE_VERIFIED`.

## Immediately relevant predecessor

TASK-0033 [PR #58](https://github.com/Vncntz/hris/pull/58), final head
`2aa441ef18a3cf920c21ce55b3a6e6a9ad16521f`, merged as
`c1717ea649e1fc6c9cca0bd2bae03a8406a927e1`. Exact-head pull-request
[run 37107413022](https://github.com/Vncntz/hris/actions/runs/37107413022) and exact-merge
main push [run 37107875827](https://github.com/Vncntz/hris/actions/runs/37107875827)
passed policy/Linux/Windows. GitHub metadata and actual logs independently establish
predecessor completion. Current refreshed main is that merge. TASK-0032 remains complete
and closes TASK-0029's lockout finding; historical evidence remains in linked TASKs.

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

TASK-0034 local verification passed: Windows 151 ordinary; Linux 151 ordinary and 123
real-MySQL tests, including 17 reset tests; focused MySQL/HTTP 32 and tooling 42. Actual
XML reports confirm zero failures/errors/skips/flaky results. All 153 implementation inputs
match isolated build sources. See the TASK for commands, security and concurrency evidence.
Exact-final-head CI, trusted-main integration, protected merge and exact-merge push CI remain
required. Offline TOTP MFA and safe recovery/admin procedures, total loss of administrator
access recovery, supported-hardware password-cost qualification, final access-management
closure, `repo-policy.py` and IMP-014 remain deferred.

## Next action

Integrate only the locally verified TASK-0034 combined PR, independently evaluating every
trusted-main integration gate. Do not call TASK-0034 complete before exact-merge push CI passes.
IMP-013 remains ACTIVE; no successor TASK is allocated.
