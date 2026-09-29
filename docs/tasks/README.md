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
| [TASK-0007](TASK-0007.md) | [IMP-004](../implementation/tasks/IMP-004.md) | Ready | Real MySQL integration-test baseline |

IMP-001 predates this convention and has no corresponding `TASK-####` file. This historical drift is left intact.

The frozen backlog reserves IMP-001 through IMP-086. [IMP-087](../implementation/tasks/IMP-087.md) is the separately authorized completed maintenance parent. Normal implementation has resumed at [IMP-004](../implementation/tasks/IMP-004.md), with TASK-0007 defined but not yet implemented.
