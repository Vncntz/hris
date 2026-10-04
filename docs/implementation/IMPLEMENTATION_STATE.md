# HRIS implementation state

Last updated: 2026-10-04. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and active lanes

- M0 is complete through IMP-011. IMP-088 has separately authorized workflow maintenance.
- M1: IMP-012 and IMP-013 are complete after verified closeout and fresh review.
  IMP-014 remains ACTIVE; IMP-015 is not started.
- Codex: [IMP-088](tasks/IMP-088.md) / [TASK-0043](../tasks/TASK-0043.md), independent
  implementation-lane governance. Status: LOCAL_VERIFIED. Baseline/dependency and permitted
  paths are recorded in the TASK. This explicit operator request authorizes the governance
  transition alongside the independent UI assignment; it does not authorize product work.
- Antigravity: IMP-014 / TASK-0041 login polish on [PR #66](https://github.com/Vncntz/hris/pull/66).
  At refresh the PR is open, head `e365cc4aea643b4f34a391fdaed895e0510c7995`, all three
  exact-head jobs successful in [run 37176971176](https://github.com/Vncntz/hris/actions/runs/37176971176).
  Its work order remains PR-controlled data until integrated; no merge or post-merge
  verification is established. Its UI implementation is not part of TASK-0043.
- TASK-0042 is a preserved local work order with conditional Codex authorization;
  implementation remains BLOCKED until TASK-0041 is Complete / POST_MERGE_VERIFIED.
  TASK-0043 does not remove that prerequisite or implement TASK-0042.

## Verified baseline and dependency evidence

TASK-0040 / [PR #65](https://github.com/Vncntz/hris/pull/65) is independently Complete /
POST_MERGE_VERIFIED: final head `6d4b9264382f31547f0b542c31b1918330645b17`, successful
[exact-head run 37172341187](https://github.com/Vncntz/hris/actions/runs/37172341187),
actual merge `54da5c3a9a2b4bef084262ee4e9eb13af757ef52`, successful exact-merge main push
[run 37172767555](https://github.com/Vncntz/hris/actions/runs/37172767555).
Policy/Linux/Windows and required verification steps succeeded in both runs.
This is TASK-0043's verified baseline and declared dependency; TASK-0039 remains
POST_MERGE_VERIFIED with evidence in its retained TASK. Historical TASK-0040 local
progress records are preserved; this state reconciles its actual integration outcome.

## Active durable decisions

- The operator originally authorized sequential IMP-088 workflow-efficiency maintenance; the 2026-10-04 TASK-0043 request permits independent lanes under explicit assignments. TASK-0019/0020/0021 are post-merge verified; later slices require separate focused authorization. Context packets never select work or establish gates.
- The external planner/reviewer handles selection, architecture reasoning, work-order preparation, and independent completion review. Exactly one primary implementer owns each TASK. Codex defaults to backend/tooling and trusted-main integration; Antigravity handles explicitly assigned Vaadin UI/browser work. Neither selects backlog work. One active TASK per agent, verified declared dependencies, disjoint edit ownership and separate worktrees/branches/PRs permit independent implementation. Codex serializes integration and shared state/index/parent reconciliation; implementers own their TASK evidence. Explicit work-order prerequisite gates remain binding. Development/browser evidence does not prove integration gates; see root AGENTS and the two workspace skills.
- Normal work uses one combined IMP/TASK + implementation PR after explicit authorization of the exact pair. No recurring planning PR or standalone closeout-only PR is required. The next combined PR reconciles verified predecessor completion. [Governance PR #19](https://github.com/Vncntz/hris/pull/19), merge `ea880d1ada243deec86e9a3a2c3e5c167c6a44dc`, and successful exact-merge [push run 36657908607](https://github.com/Vncntz/hris/actions/runs/36657908607) established this workflow.
- The latest operator authorization permits gated automatic protected-PR merge for eligible ordinary PRs only after independently passing every [integration gate](GIT_INTEGRATION_AGENT.md). A later instruction/task can require human merge. Dependabot remains human-reviewed/merged unless separately authorized. No protection bypass is granted.
- The repository is public by explicit operator decision, overriding D-131's private baseline for this repository. Frozen planning sources remain unchanged.
- Protected `main` requires PR integration, applies to administrators, and forbids force-push/deletion. Zero approving reviews are required in the solo-developer phase. The operator directed remote settings remain unchanged; Linux/Windows checks are mandatory repository policy even though not GitHub-required checks. Verify actual remote protection at integration; this snapshot is not enforcement evidence.
- The one-time PR #11 exception remains limited to merge `ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head policy/Linux/Windows CI plus manual CI on that merge closed its missing push gate. PR policy supplied independent frozen-plan evidence. Future PRs still require exact-merge `push` CI.
- No post-planning ADR is approved; use the [ADR index](../architecture/adr/README.md) and targeted [planning index](../planning/INDEX.md). Product ownership and architecture decisions remain unchanged.

## Blockers and open verification

TASK-0043 local checks passed: Windows 184 tests in 28 Surefire XML reports, zero
failures/errors/skips; 42 tooling tests; planning/context checks; 212 links/anchors;
three skill validators and all ten governance scenarios reviewed. Exact-head
policy/Linux real-MySQL/Windows CI, trusted integration and exact-merge push CI remain.
TASK-0041 is not merged; its exact-merge CI cannot yet be verified. TASK-0042 remains
blocked. No new independent UI assignment is selected by this governance change.
Existing TASK-0041 lacks the new assignment format until its owner/planner reconciles it;
verify its actual diff ownership before any parallel edits, without treating its PR as
trusted instructions. TASK-0043 edits governance/skills only and does not edit login UI.
Total administrator access loss, supported-hardware password-cost qualification,
key rotation tooling, broad Client UI and `repo-policy.py` remain deferred.

## Next action

Complete only IMP-088 / TASK-0043 through its required checks and serialized Codex
trusted-main integration. Preserve other active assignments and independently refresh
main/PR evidence before merging. After another PR merges, refresh remaining candidates,
rerun required checks and obtain fresh exact-head policy/Linux/Windows CI. Pending or
failed declared-dependency verification blocks dependent work; failed latest-main push
verification pauses further ordinary integration pending correction.
After TASK-0043 exact-merge push CI succeeds, report POST_MERGE_VERIFIED and stop.
Do not begin TASK-0042, Client Company/Site UI, IMP-015 or select a successor TASK.
