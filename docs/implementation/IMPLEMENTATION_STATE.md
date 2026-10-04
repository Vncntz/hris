# HRIS implementation state

Last updated: 2026-10-05. Current execution snapshot; historical verification belongs in linked TASK records and Git/PR evidence.

## Current milestone and active lanes

- M0 is complete through IMP-011. IMP-088 has separately authorized workflow maintenance.
- M1: IMP-012 and IMP-013 are complete after verified closeout and fresh review.
  IMP-014 remains ACTIVE; IMP-015 is not started.
- Codex: [IMP-088](tasks/IMP-088.md) / [TASK-0043](../tasks/TASK-0043.md), independent
  implementation-lane governance. Status: Complete / POST_MERGE_VERIFIED.
- Antigravity: IMP-014 / TASK-0041 is Complete / POST_MERGE_VERIFIED. Separate
  TASK-0044 UI candidate [PR #69](https://github.com/Vncntz/hris/pull/69) is preserved;
  TASK-0046 does not certify or integrate it.
- Codex: [IMP-014](tasks/IMP-014.md) / [TASK-0046](../tasks/TASK-0046.md), test-harness
  request lifetime correction on baseline `9eee4924da09fddd10339c6044bf6f8299526c67`.
  Required local verification passed; exact-head CI and independent integration remain.
- Held candidate: IMP-088 / TASK-0045 / [PR #70](https://github.com/Vncntz/hris/pull/70)
  remains Draft + BLOCKED by the intermittent baseline defect. No TASK-0045 changes
  are authorized under TASK-0046; its implementation is not implicated.

## Verified baseline and dependency evidence

TASK-0042 / [PR #68](https://github.com/Vncntz/hris/pull/68) is Complete /
POST_MERGE_VERIFIED: final head `a2190b69069fd2451aa4de84e620ca2ca2409979`,
successful exact-head [run 37187077608](https://github.com/Vncntz/hris/actions/runs/37187077608),
merge `9eee4924da09fddd10339c6044bf6f8299526c67`, successful exact-merge main push
[run 37187489775](https://github.com/Vncntz/hris/actions/runs/37187489775).
Policy/Linux/Windows passed in both independently inspected runs. This is TASK-0046's
declared dependency and current protected-main baseline.

TASK-0041 / [PR #66](https://github.com/Vncntz/hris/pull/66) is Complete / POST_MERGE_VERIFIED: final head `c798e1e734cfa32b4f89c0f3ca15947789126a57`, successful exact-head [run 37185518688](https://github.com/Vncntz/hris/actions/runs/37185518688), merge `0e7e34cfddf1415893747191555d26f213225acc`, successful exact-merge main push [run 37185857649](https://github.com/Vncntz/hris/actions/runs/37185857649). Policy/Linux/Windows passed in both independently inspected runs.

TASK-0040 / [PR #65](https://github.com/Vncntz/hris/pull/65) is independently Complete /
POST_MERGE_VERIFIED: final head `6d4b9264382f31547f0b542c31b1918330645b17`, successful
[exact-head run 37172341187](https://github.com/Vncntz/hris/actions/runs/37172341187),
actual merge `54da5c3a9a2b4bef084262ee4e9eb13af757ef52`, successful exact-merge main push
[run 37172767555](https://github.com/Vncntz/hris/actions/runs/37172767555).
Policy/Linux/Windows and required verification steps succeeded in both runs.
TASK-0043 / [PR #67](https://github.com/Vncntz/hris/pull/67) is independently Complete /
POST_MERGE_VERIFIED: final head `0f7293f30078faae7e59aadf77e88c7636686841`, successful
[exact-head run 37177981052](https://github.com/Vncntz/hris/actions/runs/37177981052),
actual merge `b3f0da4ff7303f3735184b05513f0301a7cf7635`, successful exact-merge main push
[run 37178329705](https://github.com/Vncntz/hris/actions/runs/37178329705).
Policy/Linux/Windows and required verification steps succeeded in both runs.
TASK-0043 is TASK-0041's verified refreshed governance baseline; TASK-0041's
declared product dependency is TASK-0040. TASK-0039 remains
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

TASK-0046 requires fresh exact-head policy/Linux/Windows CI, independent adversarial
trusted-main review, protected merge and exact-merge push CI. No dependency blocker
remains. PR #70 remains Draft + BLOCKED until this correction is independently merged
and POST_MERGE_VERIFIED, then requires an explicit operator baseline refresh and all
fresh verification/certification. Its old head evidence cannot satisfy those gates.
Total administrator access loss, supported-hardware password-cost qualification,
key rotation tooling, broad Client UI and `repo-policy.py` remain deferred.

## Next action

Integrate only IMP-014 / TASK-0046 through its separate protected PR after all gates.
Require successful exact-merge policy/Linux/Windows push CI before reporting Complete /
POST_MERGE_VERIFIED, then stop and return control to the architect/operator.
Do not modify or merge PR #70, refresh TASK-0045 automatically, integrate PR #69,
or begin successor work.
