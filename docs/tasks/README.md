# Task work orders

The [IMP-### control files](../implementation/tasks/IMP-002.md) track implementation backlog scope, status, and completion evidence. A focused `TASK-####.md` here is a checked-in work order linked to one parent IMP item. One IMP item may have multiple TASK work orders; these are separate levels of record and do not replace each other. Keep existing IMP files in place.

Allocate the next unused four-digit TASK ID after checking this directory and Git history; never reuse or renumber an allocated ID. Cross-link each TASK and parent IMP file and list the TASK below. Prospectively, a TASK records status, goal, scope/owning module, references and relevant decisions/ADRs, acceptance criteria, non-goals, security/privacy/audit implications, migration/deployment implications, required verification, and concise completion evidence. IMP files remain stable parent/backlog controls: objective, authority, dependencies, scope, boundaries, task links/status, and completion direction. Keep transient debugging chronology in PR discussion/evidence unless it establishes a durable product or operational fact. Apply these rules to new work; do not mass-rewrite completed records. Keep completed records in Git for traceability rather than deleting them.

| ID | Parent | Status | Work order |
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
| [TASK-0029](TASK-0029.md) | [IMP-013](../implementation/tasks/IMP-013.md) | REOPENED; TASK-0032 security correction required | Recent credential re-authentication |
| [TASK-0030](TASK-0030.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete after TASK-0031 correction | Simplify AI workflow and execution-state context |
| [TASK-0031](TASK-0031.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete; POST_MERGE_VERIFIED | Correct Agency concurrent initialization regression |
| [TASK-0032](TASK-0032.md) | [IMP-013](../implementation/tasks/IMP-013.md) | PLANNED - corrective | Enforce lockout policy during recent credential re-authentication |

IMP-001 predates this convention and has no corresponding `TASK-####` file. The frozen backlog reserves IMP-001 through IMP-086; IMP-087 and IMP-088 are separately authorized maintenance parents. Current execution routing and verified predecessor evidence live in [implementation state](../implementation/IMPLEMENTATION_STATE.md).

TASK-0029 has valid PR #55 exact-head/exact-merge CI but is reopened for its confirmed security finding.
TASK-0032 is the only authorized successor: this cycle explicitly requires the six-file planning PR
and verified planning merge-SHA push CI before separately authorized corrective implementation.
TASK-0031's verified correction closes TASK-0030's failed post-merge chain and IMP-012 reopening.
`repo-policy.py` remains deferred without a work-order allocation.

Use the normal [combined work-order + implementation PR workflow](../implementation/EXECUTION_RULES.md#local-work-order-authorization-and-single-pr-integration); recurring planning PRs and standalone closeout-only PRs are not required. The next combined PR reconciles its verified predecessor. Operator prompts supply authorization and constraints; Codex reads authoritative rules directly.

## Task context packets

From a trusted checkout, run `python tools/task-context.py TASK-0020` for the first manifest-backed packet, or `python tools/task-context.py TASK-0010 D-140` for an existing task without a manifest. Additional positional `D-###` IDs request targeted excerpts; repeated IDs are deduplicated and sorted. The tool never chooses a task. Successful output is one JSON object; invalid inputs produce an error on stderr, a nonzero exit, and no JSON. Redirect output outside the repository if saving a cache.

An optional adjacent `TASK-####.context.json` has exactly `version` (integer 1), `task`, `imp`, `decisions` (ID array), and `references` (repository-relative path array). See [TASK-0020's manifest](TASK-0020.context.json). Manifest decisions must be mentioned in the TASK or IMP; references must already be linked there or belong to the fixed governance set. Explicit CLI decisions allow additional investigation. Canonical relative paths, file existence, repository containment, task/parent/backlink identity, and the indexed planning excerpts are validated before output. Duplicate JSON keys and unknown fields fail; repeated decision/reference array entries are deduplicated. A routing manifest cannot change scope or replace reading the work order.

The packet includes task/IMP text, selected decision text, and references to the manifest, planning index, governance, and routed files. Each reference has a source path, one-based inclusive line range, byte count, and SHA-256 of the exact file or range bytes. Empty files have an empty range (`start_line: 1`, `end_line: 0`). Decision `indexed_sha256` separately describes the LF-normalized excerpt checked by the existing `plan-get.py`; on CRLF checkouts it can differ from the raw-byte `sha256`. HEAD, branch (`HEAD` when detached), and dirty status identify the Git snapshot; dirty means tracked or untracked changes excluding ignored files and submodule contents. No timestamps are added, so identical inputs produce identical JSON.

Rebuild after changing a source, manifest, index, branch, commit, or worktree state. Read the referenced governance; follow work-order links to applicable module, domain, ADR, compliance, source, and test context. If a dependency or conflict appears, retrieve the additional decision or source and apply the execution rules' precedence. The packet is a derived cache, never authority, semantic review, authorization, CI evidence, or merge permission.

During trusted-main integration, invoke the absolute path to the trusted `main` tool with `--repo <review-worktree>` when needed. It imports only its own trusted sibling `plan-get.py`, reads the other checkout as data, and disables Git filesystem monitors and configured clean/process filters without changing repository settings. Unsupported filter-key spellings fail closed. Dirty status may conservatively report changes that an enabled filter would normalize away. Never run the PR's copy of either tool. The packet does not certify trust or validate remote gates. Source changes detected while building cause failure; as with any cache, revalidate before acting.
