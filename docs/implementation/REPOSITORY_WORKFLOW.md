# Repository workflow

## Assignment and branch

Use one explicitly assigned IMP/TASK, one primary owner, one separate worktree and
short-lived branch, and one focused PR containing its work order and implementation.
Follow [AGENTS](../../AGENTS.md), [execution rules](EXECUTION_RULES.md) and the assigned
contracts. Independent implementation follows the
[assignment eligibility rules](EXECUTION_RULES.md#3-one-active-task-per-agent):
dependency-eligible verified predecessors, disjoint writes and coordinated shared
documents. Eligibility applies to serial and concurrent work: protected merge at a
known actual SHA in the verified baseline, successful required exact-merge/post-merge
verification, satisfied substantive acceptance, no unresolved security/privacy,
schema/data-integrity or relied-on application/service/API/contract blocker, passed
explicit stronger gates and unambiguous identity/evidence. Only factual durable
closeout may remain; eligibility does not require COMPLETE unless explicitly gated.
Never share another agent's active checkout/branch. Suggested names are
`feat/imp-###-short-description`, `fix/imp-###-short-description` and
`chore/imp-###-short-description`. Do not commit directly to protected main.

Complete consistent local IMP/TASK files and explicit operator authorization permit
the [combined work-order + implementation PR](EXECUTION_RULES.md#local-work-order-authorization-and-single-pr-integration).
Preserve originals outside the PR and compare final contracts before integration.
A separate planning PR is not mandatory. Verified dependency/latest integrated
durable closeout may accompany an authorized PR under serialized Codex coordination;
it does not authorize predecessor implementation. No standalone closeout-only PR is
mandatory, but required durable closure must finish before COMPLETE. A dependency-
eligible predecessor may remain POST_MERGE_VERIFIED while dependent work is assigned
and implemented. That dependent TASK's authorized combined PR may carry only factual
predecessor closure without changing verified behavior, contracts, schema, security
semantics or the dependency interface; predecessor COMPLETE is not an authorization
prerequisite for that PR. Substantive predecessor changes require their own authority.

## Candidate and handoff

Review the full diff for scope and customer data; run actual task-required local
checks with ordinary Maven/shell/PowerShell commands. Use the checked-in wrapper:
`.\mvnw.cmd -B clean verify` on Windows; `./mvnw -B clean verify` on Unix/Linux,
with `-Pmysql-it` for required real-MySQL verification. Inspect reports, not only exit
claims. Required CI remains policy/Linux/Windows, including Linux real MySQL.

The [PR template](../../.github/pull_request_template.md) prompts for IDs/scope,
actual commands/results, security/privacy/audit, migration/deployment effects,
documentation and unresolved issues. Explain why a field is not applicable.
Handoff identifies exact candidate SHA, baseline, branch, changed scope, evidence,
blockers and next action. Git/GitHub own moving PR/head/run/merge facts; summaries
are derived and must identify the SHA observed. Do not maintain those as TASK schema
truth. Status is PR_REVIEW until a distinct reviewer context establishes readiness.

## Independent review and protected integration

Apply [lifecycle, exact-SHA and certification rules](EXECUTION_RULES.md#6-definition-of-done).
Implementation does not certify itself. Review requires an execution context
distinct from implementation, reconstructing acceptance from contracts, diff and
evidence at the exact candidate SHA. A shared GitHub identity is allowed and is not
proof of independence. A second human or separate non-admin credential is not
universally required; Human retains architecture/risk/protected-main authority.

The [Git Integration Agent](GIT_INTEGRATION_AGENT.md), guided by the
[integration skill](../../.codex/skills/hris-git-integration/SKILL.md), starts from
fresh clean current trusted main and independently verifies all gates. A new checkout
or phase declaration in the implementer's existing conversation is insufficient.
The implementer prepares/pushes/opens the PR but has no standing merge authority.
Integration adds no unrelated product functionality or UX redesign. Corrections
return to the assigned branch, then require fresh certification and every final-head
gate. No agent bypasses protection or changes remote policy without specific human
authorization. A task-specific stop/human-only boundary overrides ordinary merge
authorization.

Codex integrates one PR at a time through protected main. Actual merge enters
MERGED_PENDING_VERIFY; exact-merge push CI success establishes POST_MERGE_VERIFIED.
COMPLETE additionally requires cleared acceptance/security/schema blockers and
required durable reconciliation. Pending/failed dependency CI blocks dependent
implementation. Pending/failed latest-main push verification pauses ordinary
integration until success or a corrective protected PR. Independent disjoint work
may continue on a verified baseline.

## Current enforcement and approved targets

The durable operator decision permits public repository operation to enable branch
protection, overriding D-131's private baseline for this repository; frozen sources
remain unchanged. Verify actual visibility/protection in GitHub when relevant.
The current protection contract requires PR integration, application to admins,
zero required approving reviews in the solo-developer phase, and no force-push or
deletion. Existing ordinary-PR merge authorization is governed by the integration
procedure; Dependabot remains human-reviewed/merged unless separately authorized.
Prose is not enforcement evidence; inspect effective GitHub settings at integration.

Current CI uses PRs, main pushes and manual dispatch. Policy checks generated
planning/frozen-source paths and tooling tests before Linux/Windows reactor builds.
Newer runs may cancel older runs for the same PR; main push runs do not intentionally
cancel one another. Repository policy requires successful `ci / policy`,
`ci / build-linux` and `ci / build-windows` even where remote protection does not
require those checks. The prior operator decision keeps remote settings unchanged
until a separately scoped cutover.

Approved targets are GitHub-required policy/Linux/Windows checks and resolved review
conversations, secret scanning, push protection where available and dependency
security alerts. These are targets, not claims about live enforcement. Phase-5
workflow/remote cutover requires its own assigned work. Phase 3 specifies contracts;
Phase 4 implements prospective parsing/assignment validation. Neither is implicitly
authorized by documenting the target. Legacy transition details belong in the
assigned [migration work order](../tasks/TASK-0045.md#transition-compatibility), not
the permanent root constitution.

### Preserved historical exception

The one-time PR #11 exception is limited to merge
`ca8ca7e5d54cc490d6723e9f7fbe22b85fe717d1`: exact-head policy/Linux/Windows plus
manual CI on that merge closed its missing push gate; PR policy supplied independent
frozen-plan evidence. This preserved historical fact grants no future exception.
Future merges still require actual-merge push CI. The combined-PR workflow was
established by [PR #19](https://github.com/Vncntz/hris/pull/19), merge
`ea880d1ada243deec86e9a3a2c3e5c167c6a44dc`, successful exact-merge
[push run 36657908607](https://github.com/Vncntz/hris/actions/runs/36657908607).

## Concurrent development and serialized integration

Parallelize analysis aggressively; parallelize overlapping writes conservatively.
One primary implementer per TASK, Section 3 dependency eligibility, disjoint permitted/actual
writes, separate branch/worktree/PR and no shared-document collision are mandatory.
TASK order does not imply dependency; explicit work-order gates do. UI must consume
approved merged/verified owned application contracts when they are dependencies.
Missing/overlapping ownership or unfinished required contracts blocks affected work.

Implementers own TASK evidence. Codex serializes shared governance/state/index/parent
reconciliation, preserving active lanes; explicitly assigned governance slices may
own shared edits before integration. Check actual ownership when scope/main changes.
After another PR merges, refresh candidates against current main without rewriting
published history, resolve only authorized conflicts, review the full result, rerun
required verification and obtain all three jobs at the new exact head. Every SHA
change requires fresh independent certification. An exact-baseline-sensitive work
order may require operator refresh before this step. Material conflicts return to
the operator/planner for reassignment. Automated collision detection is future work.
