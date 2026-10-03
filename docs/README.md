# Documentation index

Use this file for routing only. Current execution state lives in [implementation state](implementation/IMPLEMENTATION_STATE.md); detailed implementation evidence lives in TASK records and GitHub PR/CI history.

| Area | Entry point |
| --- | --- |
| Frozen architecture authority | [Master Software Plan](planning/MASTER_SOFTWARE_PLAN.md) and [Final Planning State](planning/FINAL_PLANNING_STATE.md) |
| Targeted frozen-decision retrieval | [Planning index](planning/INDEX.md) and `tools/plan-get.py` |
| Agent entry point | [Root AGENTS.md](../AGENTS.md) |
| Execution rules | [Execution rules](implementation/EXECUTION_RULES.md) |
| Current execution snapshot | [Implementation state](implementation/IMPLEMENTATION_STATE.md) |
| Repository/PR workflow | [Repository workflow](implementation/REPOSITORY_WORKFLOW.md) |
| Integration gates | [Git Integration Agent](implementation/GIT_INTEGRATION_AGENT.md) |
| Implementation parents | [Implementation task controls](implementation/tasks/) |
| Task work orders | [Task index](tasks/README.md) |
| Architecture | [ADR index](architecture/adr/README.md) and [executable rules](architecture/EXECUTABLE_RULES.md) |
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
| Known issues | [Known issues index](known-issues/README.md) |

For an explicitly assigned work order, use the [task context packet](tasks/README.md#task-context-packets) to obtain hashed routing references and targeted frozen-decision excerpts. For mechanical documentation/work-order checks, run `python tools/repo-policy.py`. Neither tool selects work, grants authority, or replaces semantic/integration review.
