# Documentation index

Use this routing index to find contracts. Git/GitHub owns live PR/commit/CI/merge/protection facts. [Operator state](implementation/IMPLEMENTATION_STATE.md) records active assignments, manual blockers and next action; this index is a derived view and cannot create truth.

| Authority / execution | Entry point |
| --- | --- |
| Architecture baseline | Frozen [Master Software Plan](planning/MASTER_SOFTWARE_PLAN.md) and [Final Planning State](planning/FINAL_PLANNING_STATE.md) |
| Targeted planning retrieval | Generated [decision index](planning/INDEX.md); `python tools/plan-get.py D-131` retrieves the required range without preloading frozen sources |
| Repository contract | [AGENTS.md](../AGENTS.md) |
| Source precedence, authorization, and Definition of Done | [Execution rules](implementation/EXECUTION_RULES.md) |
| Active operator assignments, manual blockers, next operator action | [Implementation state](implementation/IMPLEMENTATION_STATE.md) |
| Branch, combined PR, and CI conventions | [Repository workflow](implementation/REPOSITORY_WORKFLOW.md) |
| Independent trusted-main integration | [Git Integration Agent procedure](implementation/GIT_INTEGRATION_AGENT.md); execute `tools/pr-gates.py` only from current clean trusted main |
| TASK/IMP schema and context packets | [Task index](tasks/README.md) and [packet usage](tasks/README.md#task-context-packets) |
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

The core constitution and source categories are in [AGENTS](../AGENTS.md);
[execution rules](implementation/EXECUTION_RULES.md) own hierarchy, lifecycle,
exact-SHA certification, risk, assignment and deterministic resume. The
[task guide](tasks/README.md#task-schema-version-2-prospective-specification) owns
prospective schema v2/legacy compatibility. The [Phase-3 work order](tasks/TASK-0045.md)
owns migration/active-candidate transition details. Live execution facts come from
Git/GitHub; required durable closure belongs to TASK/IMP records, not operator state.
