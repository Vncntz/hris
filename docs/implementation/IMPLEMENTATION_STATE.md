# HRIS implementation state

Last updated: 2026-10-04. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and task

- M0 is complete through [IMP-011](tasks/IMP-011.md).
- M1 - Workforce Foundation: [IMP-012](tasks/IMP-012.md) and [IMP-013](tasks/IMP-013.md) are complete after verified closeout and fresh completion review.
- Active work: [IMP-014](tasks/IMP-014.md) / [TASK-0041](../tasks/TASK-0041.md), polish responsive HRIS login experience. Owner: ANTIGRAVITY. Status: LOCAL_VERIFIED; exact-head CI, trusted integration and exact-merge push CI pending.
- TASK-0040 is Complete / POST_MERGE_VERIFIED. IMP-014 remains ACTIVE.

## Immediately relevant predecessor

TASK-0040 [PR #65](https://github.com/Vncntz/hris/pull/65), final head
`6d4b9264382f31547f0b542c31b1918330645b17`, merged as
`54da5c3a9a2b4bef084262ee4e9eb13af757ef52`. Independently refreshed exact-head
[run 37172341187](https://github.com/Vncntz/hris/actions/runs/37172341187) and exact-merge
main push [run 37172767555](https://github.com/Vncntz/hris/actions/runs/37172767555)
passed policy/Linux/Windows, including their required verification steps.
TASK-0040 is Complete / POST_MERGE_VERIFIED; no predecessor CI blocker remains.

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

TASK-0041 local verification passed: clean Maven verify across modules; 5 LoginView unit tests passed;
4 LoginViewBrowserVerificationTest browser tests passed in headless Edge across desktop (1920x1080),
laptop (1366x768), narrow mobile (500x800), and generic error state; planning index check passed,
42 tooling tests passed, diff clean. Exact-final-head CI, trusted-main gates and exact-merge push CI
remain required. Total administrator access loss, supported-hardware password-cost qualification,
key rotation tooling, broad Client UI and `repo-policy.py` remain deferred.

## Next action

Integrate only IMP-014 / TASK-0041 through Codex trusted-main integration gates. After successful
exact-merge push CI, report POST_MERGE_VERIFIED and stop. Do not begin Client Company/Site CRUD,
IMP-015 or any successor TASK.
