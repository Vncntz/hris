# Documentation index

The frozen [Master Software Plan](planning/MASTER_SOFTWARE_PLAN.md) and [Final Planning State](planning/FINAL_PLANNING_STATE.md) govern architecture. Use the generated [planning decision index](planning/INDEX.md) for targeted retrieval; it is not authoritative. [Execution rules](implementation/EXECUTION_RULES.md), [implementation state](implementation/IMPLEMENTATION_STATE.md), [repository workflow](implementation/REPOSITORY_WORKFLOW.md), and [Git Integration Agent procedure](implementation/GIT_INTEGRATION_AGENT.md) govern execution. The completed [IMP-004 control file](implementation/tasks/IMP-004.md) records the MySQL baseline; the completed [IMP-005 control file](implementation/tasks/IMP-005.md) records the Flyway foundation; the completed [IMP-006 control file](implementation/tasks/IMP-006.md) records the shared-primitives work; the completed [IMP-007 control file](implementation/tasks/IMP-007.md) records the configuration work; the completed [IMP-008 control file](implementation/tasks/IMP-008.md) records the audit foundation; the completed [IMP-009 control file](implementation/tasks/IMP-009.md) records the authentication foundation; the completed [IMP-010 control file](implementation/tasks/IMP-010.md) records the synthetic-data generator foundation; the completed [IMP-011 control file](implementation/tasks/IMP-011.md) records the architecture-guardrail work and [TASK-0015 coverage follow-up](tasks/TASK-0015.md). Use [implementation state](implementation/IMPLEMENTATION_STATE.md) for the current work order. [IMP-012](implementation/tasks/IMP-012.md) awaits closure review after TASK-0018; [IMP-088](implementation/tasks/IMP-088.md) has [TASK-0020](tasks/TASK-0020.md) pending a post-merge regression correction under planned [TASK-0021](tasks/TASK-0021.md). [TASK work orders](tasks/README.md) map focused execution to IMP items.

| Area | Entry point |
| --- | --- |
| Architecture decisions | [ADR index](architecture/adr/README.md) and [executable rules](architecture/EXECUTABLE_RULES.md) |
| Domain | [Domain index](domain/README.md) |
| Modules | [Module index](modules/README.md) |
| Compliance | [Compliance index](compliance/README.md) |
| Deployment | [Deployment index](deployment/README.md) |
| Migrations | [Migration index](migrations/README.md) |
| Releases | [Release index](releases/README.md) |
| Support | [Support index](support/README.md) |
| OS | [OS index](os/README.md) |
| Performance | [Performance index](performance/README.md) |
| Bugs | [Bug index](bugs/README.md) |
| Features | [Feature index](features/README.md) |
| Tasks | [Task index](tasks/README.md) |
| Known issues | [Known issues index](known-issues/README.md) |

These indexes are entry points. Add focused records as the relevant work is implemented and verified.

For an explicitly assigned TASK, the [task context packet](tasks/README.md#task-context-packets) provides hashed routing references and targeted frozen-decision excerpts. It is a read-only derived cache; source review and integration gates remain authoritative.
