# Task work orders

The [IMP control files](../implementation/tasks/IMP-002.md) track parent backlog scope and completion direction. A focused `TASK-####.md` here is a checked-in work order linked to one parent IMP. One IMP may have multiple TASK work orders; keep both levels of record.

Allocate the next unused four-digit TASK ID only after current predecessor gates are verified. Cross-link each TASK and parent IMP file and list the TASK below. Use the concise [TASK template](TASK_TEMPLATE.md) for future work orders. Keep acceptance criteria and durable completion evidence in the TASK; transient debug/test narratives belong in PR/log evidence unless they establish a durable defect or rule.

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
| [TASK-0009](TASK-0009.md) | [IMP-006](../implementation/tasks/IMP-006.md) | Complete / `POST_MERGE_VERIFIED` | Shared identifier, money, and business-time primitives |
| [TASK-0010](TASK-0010.md) | [IMP-007](../implementation/tasks/IMP-007.md) | Complete / `POST_MERGE_VERIFIED` | Configuration, secrets, and environment conventions |
| [TASK-0011](TASK-0011.md) | [IMP-008](../implementation/tasks/IMP-008.md) | Complete / `POST_MERGE_VERIFIED` | Append-only audit persistence foundation |
| [TASK-0012](TASK-0012.md) | [IMP-009](../implementation/tasks/IMP-009.md) | Complete / `POST_MERGE_VERIFIED` | Password authentication, session, and authorization foundation |
| [TASK-0013](TASK-0013.md) | [IMP-010](../implementation/tasks/IMP-010.md) | Complete / `POST_MERGE_VERIFIED` | Deterministic synthetic-data generator core |
| [TASK-0014](TASK-0014.md) | [IMP-011](../implementation/tasks/IMP-011.md) | Complete / `POST_MERGE_VERIFIED` | Executable modular-monolith architecture guardrails |
| [TASK-0015](TASK-0015.md) | [IMP-011](../implementation/tasks/IMP-011.md) | Complete / `POST_MERGE_VERIFIED` | Architecture-test production-module coverage hardening |
| [TASK-0016](TASK-0016.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete / `POST_MERGE_VERIFIED` | Agency/platform configuration root |
| [TASK-0017](TASK-0017.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete / `POST_MERGE_VERIFIED` | Agency migration no-seed verification hardening |
| [TASK-0018](TASK-0018.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Complete / `POST_MERGE_VERIFIED` | Agency initialization contention hardening |
| [TASK-0019](TASK-0019.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete / `POST_MERGE_VERIFIED` | Deterministic PR gate hardening |
| [TASK-0020](TASK-0020.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete / `POST_MERGE_VERIFIED` | Compact task context packet |
| [TASK-0021](TASK-0021.md) | [IMP-088](../implementation/tasks/IMP-088.md) | Complete / `POST_MERGE_VERIFIED` | Fix task-context identity heading regression |
| [TASK-0022](TASK-0022.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Persist authorization roles/permissions and load runtime authorities |
| [TASK-0023](TASK-0023.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Secure first-administrator provisioning |
| [TASK-0024](TASK-0024.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Authenticated account creation boundary |
| [TASK-0025](TASK-0025.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Authenticated password change and session revocation foundation |
| [TASK-0026](TASK-0026.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Administrative account lifecycle and stale-authentication invalidation |
| [TASK-0027](TASK-0027.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Account-to-Role assignment administration and stale-authority invalidation |
| [TASK-0028](TASK-0028.md) | [IMP-013](../implementation/tasks/IMP-013.md) | Complete / `POST_MERGE_VERIFIED` | Role and Role-to-Permission administration with authority-generation invalidation |
| [TASK-0029](TASK-0029.md) | [IMP-088](../implementation/tasks/IMP-088.md) | IN PROGRESS | Streamline execution context and automate repository policy checks |

IMP-001 predates this convention and has no corresponding `TASK-####` file; preserve that historical exception.

## Task context packets

From a trusted checkout, run `python tools/task-context.py TASK-####` for an explicitly assigned work order. Additional positional `D-###` IDs request targeted frozen-decision excerpts; repeated IDs are deduplicated and sorted. The tool never chooses a task. Successful output is one JSON object; invalid inputs produce an error on stderr, a nonzero exit, and no JSON. Redirect output outside the repository if saving a cache.

An optional adjacent `TASK-####.context.json` has exactly `version` (integer 1), `task`, `imp`, `decisions` (ID array), and `references` (repository-relative path array). [TASK-0020's manifest](TASK-0020.context.json) is the first example. Manifest decisions must be mentioned in the TASK or IMP; references must already be linked there or belong to the fixed governance set. Explicit CLI decisions allow additional investigation. Canonical relative paths, file existence, repository containment, task/parent/backlink identity, and indexed planning excerpts are validated before output. A routing manifest cannot change scope or replace reading the work order.

The packet includes TASK/IMP text, selected decision text, and hashed references. HEAD, branch, and dirty status identify the Git snapshot. Rebuild after changing a source, manifest, index, branch, commit, or worktree state. Read referenced governance and expand context only when dependencies or conflicts require it.

During trusted-main integration, invoke the trusted `main` copy of the tool with `--repo <review-worktree>` when needed; never execute the PR head's copy as trusted code. The packet is a derived cache, not authority, semantic review, authorization, CI evidence, or merge permission.

## Deterministic repository policy

Run `python tools/repo-policy.py` to validate repository-local Markdown links/anchors, canonical TASK identity, parent IMP links/backlinks, and task-index registration. CI runs the same check. Fix the source record rather than adding temporary one-off validation scripts.
