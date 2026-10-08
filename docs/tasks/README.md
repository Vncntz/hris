# Task work orders

An IMP is a stable parent capability contract; a TASK is one focused checked-in work
order linked to it. Keep existing IMP files in place. Allocate the next unused
four-digit TASK ID only under explicit planner/operator assignment after checking
the directory and Git history. Never reuse or renumber an allocated identity.
Cross-link each TASK/IMP and list the TASK here. The table is a routing/historical
view, not live execution truth; legacy statuses may lag Git/GitHub. Follow
[authority and lifecycle rules](../implementation/EXECUTION_RULES.md), not table
prose, to reconstruct current state. Do not mass-rewrite completed records.

| ID | Parent | Recorded status (derived/historical) | Work order |
| --- | --- | --- | --- |
| [TASK-0001](TASK-0001.md) | [IMP-002](../implementation/tasks/IMP-002.md) | Complete | Repository governance foundation |
| [TASK-0002](TASK-0002.md) | [IMP-003](../implementation/tasks/IMP-003.md) | Complete | CI baseline |
| [TASK-0003](TASK-0003.md) | [IMP-087](../implementation/tasks/IMP-087.md) | Complete | Governance and security hardening |
| [TASK-0004](TASK-0004.md) | [IMP-087](../implementation/tasks/IMP-087.md) | Complete | Lazy context retrieval and local agent state |
| [TASK-0005](TASK-0005.md) | [IMP-087](../implementation/tasks/IMP-087.md) | Complete | CI deduplication and mechanical gates |
| [TASK-0006](TASK-0006.md) | [IMP-087](../implementation/tasks/IMP-087.md) | Complete | Java codebase polish |
| [TASK-0007](TASK-0007.md) | [IMP-004](../implementation/tasks/IMP-004.md) | Complete | Real MySQL integration-test baseline |
| [TASK-0008](TASK-0008.md) | [IMP-005](../implementation/tasks/IMP-005.md) | Complete | Flyway foundation and global migration ordering |
| [TASK-0009](TASK-0009.md) | [IMP-006](../implementation/tasks/IMP-006.md) | Complete; post-merge CI passed | Shared identifier, money, and business-time primitives |
| [TASK-0010](TASK-0010.md) | [IMP-007](../implementation/tasks/IMP-007.md) | Complete; post-merge CI passed | Configuration, secrets, and environment conventions |
| [TASK-0011](TASK-0011.md) | [IMP-008](../implementation/tasks/IMP-008.md) | Complete; post-merge CI passed | Append-only audit persistence foundation |
| [TASK-0012](TASK-0012.md) | [IMP-009](../implementation/tasks/IMP-009.md) | Complete; post-merge CI passed | Password authentication, session, and authorization foundation |
| [TASK-0013](TASK-0013.md) | [IMP-010](../implementation/tasks/IMP-010.md) | Complete; post-merge CI passed | Deterministic synthetic-data generator core |
| [TASK-0014](TASK-0014.md) | [IMP-011](../implementation/tasks/IMP-011.md) | Complete; post-merge CI passed | Executable modular-monolith architecture guardrails |
| [TASK-0015](TASK-0015.md) | [IMP-011](../implementation/tasks/IMP-011.md) | Complete; post-merge CI passed | Architecture-test production-module coverage hardening |
| [TASK-0016](TASK-0016.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete; post-merge CI passed | Agency/platform configuration root |
| [TASK-0017](TASK-0017.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete; post-merge CI passed | Agency migration no-seed verification hardening |
| [TASK-0018](TASK-0018.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete; post-merge CI passed | Agency initialization contention hardening |
| [TASK-0019](TASK-0019.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete; post-merge CI passed | Deterministic PR gate hardening |
| [TASK-0020](TASK-0020.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete; post-merge verified after TASK-0021 | Compact task context packet |
| [TASK-0021](TASK-0021.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete; post-merge verified | Fix task-context identity heading regression |
| [TASK-0022](TASK-0022.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Persist authorization roles/permissions and load runtime authorities |
| [TASK-0023](TASK-0023.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Secure first-administrator provisioning |
| [TASK-0024](TASK-0024.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Authenticated account creation boundary |
| [TASK-0025](TASK-0025.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Authenticated password change and session revocation foundation |
| [TASK-0026](TASK-0026.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Administrative account enable/disable lifecycle and stale-authentication invalidation |
| [TASK-0027](TASK-0027.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Account-to-Role assignment administration and stale-authority session invalidation |
| [TASK-0028](TASK-0028.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Administer Roles and Role-to-Permission assignments with authority-generation invalidation |
| [TASK-0029](TASK-0029.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; corrected by TASK-0032 / POST_MERGE_VERIFIED | Recent credential re-authentication |
| [TASK-0030](TASK-0030.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete after TASK-0031 correction | Simplify AI workflow and execution-state context |
| [TASK-0031](TASK-0031.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete; POST_MERGE_VERIFIED | Correct Agency concurrent initialization regression |
| [TASK-0032](TASK-0032.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Enforce lockout policy during recent credential re-authentication |
| [TASK-0033](TASK-0033.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Correct ordinary form-login failed-attempt overcounting |
| [TASK-0034](TASK-0034.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Authenticated administrative credential reset |
| [TASK-0035](TASK-0035.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete; POST_MERGE_VERIFIED | Privileged offline TOTP MFA and recovery controls |
| [TASK-0036](TASK-0036.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Client Company and Client Site master-data lifecycle |
| [TASK-0037](TASK-0037.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Isolate Client reference queries from ambient transactions |
| [TASK-0038](TASK-0038.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete; POST_MERGE_VERIFIED | Onboard Antigravity as Vaadin UI specialist |
| [TASK-0039](TASK-0039.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Client administration browse/query backend contract |
| [TASK-0040](TASK-0040.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Authenticated Vaadin application shell foundation |
| [TASK-0041](TASK-0041.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Polish responsive HRIS login experience |
| [TASK-0042](TASK-0042.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Align Vaadin route authorization with persisted permission authorities |
| [TASK-0043](TASK-0043.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete; POST_MERGE_VERIFIED | Independent Codex and Antigravity implementation lanes |
| [TASK-0045](TASK-0045.md) | [IMP-088](../implementation/tasks/IMP-088.md) | COMPLETE closeout carried by TASK-0047; effective on reconciliation merge | Establish the core AIDD repository constitution |
| [TASK-0046](TASK-0046.md) | [IMP-014](../implementation/tasks/IMP-014.md) | Complete; POST_MERGE_VERIFIED | Stabilize Vaadin request ownership in route-authority test harness |
| [TASK-0047](TASK-0047.md) | [IMP-088](../implementation/tasks/IMP-088.md) | PR_REVIEW; independent review required | Implement TASK schema-v2 parsing and compatibility validation |

IMP-001 predates TASK convention. Frozen backlog parents are IMP-001 through IMP-086;
IMP-087/088 are separately authorized maintenance parents. Use
[operator state](../implementation/IMPLEMENTATION_STATE.md) for active assignments,
manual blockers and next action; use Git/GitHub for PR/CI/merge facts. The normal
[combined PR workflow](../implementation/EXECUTION_RULES.md#local-work-order-authorization-and-single-pr-integration)
needs no recurring planning PR or standalone closeout-only PR. Preserve historical
TASK records and unique evidence; transient debugging chronology belongs in PR
evidence unless it establishes a durable product/operational fact.

TASK-0046 verified closeout and TASK-0045 baseline refresh are preserved in the
[authorized refresh record](TASK-0045.md#authorized-baseline-refresh-2026-10-05);
its earlier handoff text remains historical.

## TASK schema version 2 (prospective specification)

Phase 3 specifies the contract; [TASK-0047](TASK-0047.md) implements the first
prospective Phase-4 parsing/compatibility slice in the existing `task-context.py`.
Keep the compatible identity heading/parent-link syntax below. TASK-0045 remains
legacy-compatible. Assignment/dependency-eligibility/scope automation and Phase-5
CI/PR/trusted-main enforcement are separately authorized future work.

Machine-readable metadata is deliberately small:

| Field | Contract |
| --- | --- |
| Schema | Integer `2` for an explicitly v2 work order. |
| TASK ID | One allocated `TASK-####`. |
| Parent IMP | One `IMP-###`, linked to its existing parent file. |
| Title | Focused work-order title. |
| Primary owner | Exactly one of `CODEX` or `ANTIGRAVITY`; a new role needs future human approval. |
| Baseline main SHA | Exact full protected-main commit defining the assignment baseline. |
| Dependencies | Explicit TASK IDs/contract prerequisites or explicit `none`; numbering is not dependency. |

Serialization/parser details are defined below. Required semantic sections are:

| Section | Required meaning |
| --- | --- |
| Objective | Outcome and approved business/architecture authority. |
| In scope | Work to perform. |
| Out of scope | Non-goals and later work excluded. |
| Invariants | Ownership, architecture, data, security and other constraints that remain true. |
| Permitted repository scope | Explicit files or directory prefixes/modules the owner may edit. |
| Forbidden repository scope | Protected/excluded paths and behaviors. |
| Observable acceptance criteria | Reviewable outcomes; no vague 'works' claim. |
| Required verification | Native commands/checks and expected evidence proportionate to risk; retain existing mandatory gates. |
| Evidence / handoff requirements | Actual results, scope, blockers, distinct review-context certification and Git/GitHub evidence routing. |

Semantic assignment details include lane, separate branch/worktree/PR plan, shared
documentation coordinator, applicable references/Decision IDs and risk/security/
privacy/audit/schema/deployment effects. These do not enlarge machine metadata.
Declare dependencies and independently inspect their required head/merge verification
in Git/GitHub; evidence links are historical references, not moving fields.

Do not require manual schema maintenance of current PR head, CI run ID, merge SHA,
mergeability or current CI outcome. Git/GitHub own those facts. Reports/certification
records identify the observed exact SHA without becoming mutable TASK truth.

`schema v2 -> strict validation prospectively`

`legacy TASK -> compatibility/best-effort parsing`

Legacy work orders remain readable and valid under their applicable contracts;
historical records become v2 only by explicit migration. No mass rewrite or
retroactive v2 gates. Active-candidate grandfathering is recorded in
[TASK-0045 transition compatibility](TASK-0045.md#transition-compatibility).

### V2 serialization and local validation

A v2 TASK retains its first-line `# TASK-#### - Title` identity and exactly one
`Parent implementation item: [IMP-###](../implementation/tasks/IMP-###.md)` line.
For v2, this declaration must be active, outside HTML comments and fenced examples.
Duplicate or ambiguous active declarations fail; the canonical link, metadata IMP
and linked parent identity must agree. Legacy extraction remains compatible.
After the identity heading, one blank line and one unindented fenced block with
the exact opening marker `task-schema-v2` carry the following JSON object.
[TASK-0047](TASK-0047.md) is the first actual work order in this format.

````text
# TASK-0047 - Implement TASK schema-v2 parsing and compatibility validation

```task-schema-v2
{
  "schema": 2,
  "task": "TASK-0047",
  "imp": "IMP-088",
  "title": "Implement TASK schema-v2 parsing and compatibility validation",
  "owner": "CODEX",
  "baseline_main_sha": "14018990bc15fc62814412a3be821a3822b762ab",
  "dependencies": ["TASK-0045"]
}
```

Parent implementation item: [IMP-088](../implementation/tasks/IMP-088.md)
````

The block closes with an unindented line of exactly three backticks. The reserved
`task-schema` fence namespace opts into strict parsing: unsupported/misplaced/
duplicate/malformed markers never fall back to legacy. Do not put additional
schema-fenced examples in a TASK. Ordinary legacy prose mentioning schema v2 does
not opt in. Legacy TASKs without such a fence retain compatibility parsing.

The object has exactly the seven fields shown; field order/JSON indentation are
irrelevant. Duplicate keys at any object level, unknown fields, malformed JSON and
wrong types fail. `schema` is integer 2, identities use ASCII digits of exactly
four/three places and match heading/parent, and title is a nonempty single-line
string matching the heading. Owner is one string, exactly CODEX or ANTIGRAVITY.
Baseline is exactly 40 lowercase hexadecimal characters for this Git SHA-1
repository; syntax validation does not establish protected-main provenance.

Dependencies are either the string `"none"` or a nonempty JSON array. Entries are
exact `TASK-####` strings or objects with exactly `contract` and `requirement`:
for example `{"contract": "docs/tasks/README.md", "requirement": "Approved Phase-3 TASK contract"}`.
Contract paths use canonical repository-relative forward-slash spelling without
traversal, query or fragment; requirement is a nonempty single-line string.
Duplicate TASK IDs/contract paths and self-dependencies fail. Do not mix `none`
with entries. Array order is preserved; no predecessor is inferred from numbering.
Dependency syntax does not prove existence, approval, baseline containment or CI;
independent inspection under execution Section 3 still establishes eligibility.

Each required semantic section in the table is a unique level-2 heading with
nonempty content. Heading names match case-insensitively; fenced/commented examples
cannot supply a required heading, and comments/subheadings alone are not content.
The parser verifies presence, not semantic adequacy, authorization or scope.

```text
python tools/task-context.py TASK-0047 --validate-only
python tools/task-context.py TASK-0047
python tools/task-context.py TASK-0045 --validate-only
```

Both commands validate identity, linked existing parent/backlink and optional
routing manifest/context sources using the same read-only snapshot checks.
`--validate-only` emits one compact JSON syntax result with task/imp/schema/metadata;
legacy reports `schema: "legacy"` and `metadata: null`. Failure produces a nonzero
exit, stderr diagnostic and no JSON. Default context-packet shape/version/hashes
are unchanged for legacy and v2. No new GitHub checks or enforcement are wired.

## IMP contract

An IMP contains its stable ID, capability/objective, business/architecture authority,
dependencies, scope/boundaries, child TASK links and completion direction. It does
not duplicate every child acceptance criterion or mutable PR/CI status. Child TASKs
own focused scope/verification; actual completion is reconstructed under the
[Definition of Done](../implementation/EXECUTION_RULES.md#6-definition-of-done).
Preserve historical evidence while keeping future parents stable and concise.

## Concurrent assignment records

Apply [assignment eligibility](../implementation/EXECUTION_RULES.md#3-one-active-task-per-agent).
Parallelize analysis aggressively; parallelize overlapping writes conservatively.
One owner per TASK, verified dependencies, disjoint scopes, separate branch/worktree/PR
and no conflicting shared-document ownership are required. Shared governance writes
are serialized by Codex; an explicit governance assignment can authorize those
writes before integration. Each implementer owns its TASK evidence. Unmerged
declared backend dependencies block UI implementation. Skill discovery grants no
assignment authority. Existing explicit predecessor gates remain binding.

## Task context packets

From a trusted checkout, run `python tools/task-context.py TASK-0020` for the first manifest-backed packet, or `python tools/task-context.py TASK-0010 D-140` for an existing task without a manifest. Additional positional `D-###` IDs request targeted excerpts; repeated IDs are deduplicated and sorted. The tool never chooses a task. Successful output is one JSON object; invalid inputs produce an error on stderr, a nonzero exit, and no JSON. Redirect output outside the repository if saving a cache.

An optional adjacent `TASK-####.context.json` has exactly `version` (integer 1), `task`, `imp`, `decisions` (ID array), and `references` (repository-relative path array). See [TASK-0020's manifest](TASK-0020.context.json). Manifest decisions must be mentioned in the TASK or IMP; references must already be linked there or belong to the fixed governance set. Explicit CLI decisions allow additional investigation. Canonical relative paths, file existence, repository containment, task/parent/backlink identity, and the indexed planning excerpts are validated before output. Duplicate JSON keys and unknown fields fail; repeated decision/reference array entries are deduplicated. A routing manifest cannot change scope or replace reading the work order.

The packet includes task/IMP text, selected decision text, and references to the manifest, planning index, governance, and routed files. Each reference has a source path, one-based inclusive line range, byte count, and SHA-256 of the exact file or range bytes. Empty files have an empty range (`start_line: 1`, `end_line: 0`). Decision `indexed_sha256` separately describes the LF-normalized excerpt checked by the existing `plan-get.py`; on CRLF checkouts it can differ from the raw-byte `sha256`. HEAD, branch (`HEAD` when detached), and dirty status identify the Git snapshot; dirty means tracked or untracked changes excluding ignored files and submodule contents. No timestamps are added, so identical inputs produce identical JSON.

Rebuild after changing a source, manifest, index, branch, commit, or worktree state. Read the referenced governance; follow work-order links to applicable module, domain, ADR, compliance, source, and test context. If a dependency or conflict appears, retrieve the additional decision or source and apply the execution rules' precedence. The packet is a derived cache, never authority, semantic review, authorization, CI evidence, or merge permission.

During trusted-main integration, invoke the absolute path to the trusted `main` tool with `--repo <review-worktree>` when needed. It imports only its own trusted sibling `plan-get.py`, reads the other checkout as data, and disables Git filesystem monitors and configured clean/process filters without changing repository settings. Unsupported filter-key spellings fail closed. Dirty status may conservatively report changes that an enabled filter would normalize away. Never run the PR's copy of either tool. The packet does not certify trust or validate remote gates. Source changes detected while building cause failure; as with any cache, revalidate before acting.
