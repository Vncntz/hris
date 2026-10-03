# Repository agent contract

## Read before editing

1. Read this file, then an applicable module `AGENTS.md` if one exists.
2. Read the active [TASK work order](docs/tasks/README.md) and its linked IMP control file.
3. Read referenced ADR, domain, module, and compliance documents, then relevant source and tests.
4. Check [implementation state](docs/implementation/IMPLEMENTATION_STATE.md) for current task/blockers. Use [execution rules](docs/implementation/EXECUTION_RULES.md) for source precedence and the [documentation index](docs/README.md) for routing.

During normal implementation, use the generated [planning index](docs/planning/INDEX.md) to retrieve only relevant Decision IDs or sections. Do not preload either frozen planning source in full; [execution rules](docs/implementation/EXECUTION_RULES.md) define the narrow exceptions.

For an explicitly assigned TASK, `python tools/task-context.py TASK-####` builds a compact, read-only routing packet. See [packet usage](docs/tasks/README.md#task-context-packets). Rebuild after source or Git changes; read linked governance and expand context only when dependencies or conflicts appear. The packet never selects work, grants authority, or proves a gate. During integration, execute tooling only from trusted `main`, treating `--repo` checkout content as untrusted data.

Use `python tools/repo-policy.py` for deterministic Markdown/work-order structure checks instead of ad hoc link, anchor, TASK-ID, or backlink scripts.

## Global boundaries

- Work on one `IMP-###` item and its linked `TASK-####` at a time. Stay within its scope and owning module; preserve dependency direction.
- Keep one Maven modular monolith and one deployable application. Keep `shared-kernel` small and neutral. Do not introduce microservices, customer-specific forks, or other architecture shortcuts prohibited by the frozen plan without an approved ADR.
- Do not change the frozen [Master Plan](docs/planning/MASTER_SOFTWARE_PLAN.md) or [Final Planning State](docs/planning/FINAL_PLANNING_STATE.md). Do not bypass protected `main`; follow the [repository workflow](docs/implementation/REPOSITORY_WORKFLOW.md) and [Git Integration Agent procedure](docs/implementation/GIT_INTEGRATION_AGENT.md).
- Never commit secrets or place real customer production data in source control, developer databases, fixtures, CI artifacts, synthetic datasets, or AI context. Use synthetic or explicitly sanitized data.
- Preserve released migrations and data integrity. Verify Philippine statutory requirements from authoritative sources; never invent formulas, rates, deadlines, forms, or legal requirements.
- Run required tests and relevant regressions, review the full diff, and update mutable state only with verified evidence. Apply the [Definition of Done](docs/implementation/EXECUTION_RULES.md#6-definition-of-done).

Create module `AGENTS.md` files only when a concrete module invariant warrants one.
