# Task work orders

The [IMP-### control files](../implementation/tasks/IMP-002.md) track implementation backlog scope, status, and completion evidence. A focused `TASK-####.md` here is a checked-in work order linked to one parent IMP item. One IMP item may have multiple TASK work orders; these are separate levels of record and do not replace each other. Keep existing IMP files in place.

Allocate the next unused four-digit TASK ID after checking this directory and Git history; never reuse or renumber an allocated ID. Cross-link each TASK and parent IMP file and list the TASK below. A work order states its status, goal, non-goals, affected modules, relevant decisions/ADRs, acceptance criteria, required tests, documentation, security/audit, and migration/deployment implications. Keep completed records in Git for traceability rather than deleting them.

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
| [TASK-0017](TASK-0017.md) | [IMP-012](../implementation/tasks/IMP-012.md) | Planned | Agency migration no-seed verification hardening |

IMP-001 predates this convention and has no corresponding `TASK-####` file. This historical drift is left intact.

The frozen backlog reserves IMP-001 through IMP-086. [IMP-087](../implementation/tasks/IMP-087.md) is the separately authorized completed maintenance parent. [IMP-004](../implementation/tasks/IMP-004.md) / TASK-0007 reached `POST_MERGE_VERIFIED` on PR #12's passing post-merge `push` CI. [IMP-005](../implementation/tasks/IMP-005.md) / TASK-0008 reached `POST_MERGE_VERIFIED` on PR #14's passing post-merge `push` CI. [IMP-006](../implementation/tasks/IMP-006.md) / TASK-0009 reached `POST_MERGE_VERIFIED` on PR #16's passing post-merge `push` CI. IMP-007 / TASK-0010 reached `POST_MERGE_VERIFIED` on PR #18's passing post-merge `push` CI. IMP-008 / TASK-0011 reached `POST_MERGE_VERIFIED` on PR #20's passing post-merge `push` CI. IMP-009 / TASK-0012 reached `POST_MERGE_VERIFIED` on PR #21's passing post-merge `push` CI. IMP-010 / TASK-0013 reached `POST_MERGE_VERIFIED` on PR #22's passing post-merge `push` CI. TASK-0014 reached `POST_MERGE_VERIFIED` on PR #24's passing post-merge `push` CI. IMP-011 / TASK-0015 reached `POST_MERGE_VERIFIED` on PR #26's passing exact-merge-SHA `push` CI. IMP-012 / TASK-0016 reached `POST_MERGE_VERIFIED` on PR #28's passing exact-merge-SHA `push` CI; TASK-0017 is the planned review follow-up.
