# Documentation index

Use this index to find authoritative sources; current execution status and predecessor evidence live in [implementation state](implementation/IMPLEMENTATION_STATE.md), not here.

| Authority / execution | Entry point |
| --- | --- |
| Architecture baseline | Frozen [Master Software Plan](planning/MASTER_SOFTWARE_PLAN.md) and [Final Planning State](planning/FINAL_PLANNING_STATE.md) |
| Targeted planning retrieval | Generated [decision index](planning/INDEX.md); `python tools/plan-get.py D-131` retrieves the required range without preloading frozen sources |
| Repository contract | [AGENTS.md](../AGENTS.md) |
| Source precedence, authorization, and Definition of Done | [Execution rules](implementation/EXECUTION_RULES.md) |
| Current milestone, active task, blockers, durable decisions | [Implementation state](implementation/IMPLEMENTATION_STATE.md) |
| Branch, combined PR, and CI conventions | [Repository workflow](implementation/REPOSITORY_WORKFLOW.md) |
| Independent trusted-main integration | [Git Integration Agent procedure](implementation/GIT_INTEGRATION_AGENT.md); execute `tools/pr-gates.py` only from current clean trusted main |
| Work orders and context packets | [Task index](tasks/README.md) and [packet usage](tasks/README.md#task-context-packets) |
| Local first-administrator operation | [Operator runbook](deployment/first-administrator.md) |

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

These indexes route to focused records as work is implemented and verified. The planning index and task packets are derived routing aids, never authority or gate evidence. Read repository rules directly; operator prompts need only supply task authorization and specific constraints rather than repeat governance, source code, or full frozen plans.

Current authorized product work is [IMP-014](implementation/tasks/IMP-014.md) /
[TASK-0036](tasks/TASK-0036.md), Client Company/Site master-data lifecycle.
TASK-0035 is Complete / POST_MERGE_VERIFIED after PR #60; fresh review closes IMP-013.
Current evidence and integration gates are routed through implementation state.
TASK-0037 is not allocated and IMP-015 is not started.
