# Task work orders

The [IMP-### control files](../implementation/tasks/IMP-002.md) track implementation backlog scope, status, and completion evidence. A focused `TASK-####.md` here is a checked-in work order linked to one parent IMP item. One IMP item may have multiple TASK work orders; these are separate levels of record and do not replace each other. Keep existing IMP files in place.

Allocate the next unused four-digit TASK ID after checking this directory and Git history; never reuse or renumber an allocated ID. Cross-link each TASK and parent IMP file and list the TASK below. A work order states its status, goal, non-goals, affected modules, relevant decisions/ADRs, acceptance criteria, required tests, documentation, security/audit, and migration/deployment implications. Keep completed records in Git for traceability rather than deleting them.

| ID | Parent | Status | Work order |
| --- | --- | --- | --- |
| [TASK-0001](TASK-0001.md) | [IMP-002](../implementation/tasks/IMP-002.md) | In progress; review and credential path blocked | Repository governance foundation |

IMP-001 predates this convention and has no corresponding `TASK-####` file. This historical drift is left intact.
