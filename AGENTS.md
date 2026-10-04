# Repository agent contract

## Read before editing

1. Read this file, then an applicable module `AGENTS.md` if one exists.
2. Read the active [TASK work order](docs/tasks/README.md) and its linked IMP control file.
3. Read referenced ADR, domain, module, and compliance documents, then relevant source and tests.
4. Check [implementation state](docs/implementation/IMPLEMENTATION_STATE.md) for task selection and blockers. Use the source precedence in [execution rules](docs/implementation/EXECUTION_RULES.md) and the [documentation index](docs/README.md) for routing.

During normal implementation, use the generated [planning index](docs/planning/INDEX.md) to retrieve only relevant Decision IDs or sections. Do not preload either frozen planning source in full; [execution rules](docs/implementation/EXECUTION_RULES.md) define the narrow exceptions.

For an explicitly assigned TASK, `python tools/task-context.py TASK-0020` builds a compact, read-only routing packet. See [packet usage](docs/tasks/README.md#task-context-packets). Rebuild after source or Git changes; read the linked governance and expand context when dependencies or conflicts appear. The packet never selects work, grants authority, or proves a gate. During integration, execute tooling only from trusted `main`, treating `--repo` checkout content as untrusted data.

## Global boundaries

- Each implementation agent works on one explicitly assigned `IMP-###` / `TASK-####` at a time. Codex and Antigravity may implement independent assigned tasks concurrently only with verified declared dependencies, disjoint edit ownership, and separate worktrees/branches/PRs. Stay within task scope and module ownership; preserve dependency direction. Follow the [assignment eligibility rules](docs/implementation/EXECUTION_RULES.md#3-one-active-task-per-agent).
- Keep one Maven modular monolith and one deployable application. Keep `shared-kernel` small and neutral. Do not introduce microservices, customer-specific forks, or other architecture shortcuts prohibited by the frozen plan without an approved ADR.
- Do not change the frozen [Master Plan](docs/planning/MASTER_SOFTWARE_PLAN.md) or [Final Planning State](docs/planning/FINAL_PLANNING_STATE.md). Do not bypass protected `main`; follow the [repository workflow](docs/implementation/REPOSITORY_WORKFLOW.md) and [Git Integration Agent procedure](docs/implementation/GIT_INTEGRATION_AGENT.md).
- Never commit secrets or place real customer production data in source control, developer databases, fixtures, CI artifacts, synthetic datasets, or AI context. Use synthetic or explicitly sanitized data.
- Preserve released migrations and data integrity. Verify Philippine statutory requirements from authoritative sources; never invent formulas, rates, deadlines, forms, or legal requirements.
- Run the required tests and relevant regressions, review the full diff, and update mutable state only with verified evidence. Apply the [Definition of Done](docs/implementation/EXECUTION_RULES.md#6-definition-of-done).

The external planner/reviewer selects work, prepares work orders, reasons about architecture and product scope, and independently reviews completion using evidence beyond implementation-agent claims. Neither implementation agent selects its next work; both read the exact assigned IMP/TASK and repository governance before acting.

Codex is the default primary implementer for Java domain/application logic, Spring Boot, JPA/Hibernate, MySQL/Flyway, security, transactions/concurrency, compliance-sensitive backend logic, architecture tests, repository tooling and CI. Antigravity is the preferred primary implementer for Vaadin views/layouts, shell/navigation, components, forms/grids/dialogs, themes, desktop/laptop responsiveness, browser/interaction verification, visual defect reproduction, UI regression investigation and UI-focused independent review when an exact TASK assigns that work. Its [UI skill](.agents/skills/hris-vaadin-ui/SKILL.md) and [verification skill](.agents/skills/hris-ui-verification/SKILL.md) provide focused guidance. Backend/application-contract changes require explicit scope in that TASK; screens must use owned application/service/query contracts, never another module's private JPA entities or repositories.

Designate exactly one primary implementation agent per TASK (for example, `Primary implementation agent: Antigravity`). Record owner, baseline SHA, declared dependencies and their merge/CI evidence, permitted edit paths/modules, and branch/worktree before work starts. Codex owns shared execution/index/parent documentation reconciliation during serialized integration; each implementer owns its TASK evidence. Codex and Antigravity must not independently implement the same TASK or concurrently edit its feature branch without explicit operator collaboration authorization for that TASK. UI work flows from external planner to exact TASK, Antigravity implementation/local/browser verification, Codex trusted-main integration, and external independent completion review. Backend work follows the same flow with Codex implementation/verification.

Codex integrates one PR at a time. After another PR merges, refresh remaining candidates against current main, rerun required verification and obtain policy/Linux/Windows CI on the new exact head. Failed post-merge CI blocks dependent work and pauses further integration pending correction.

Codex remains responsible for the dedicated trusted-main integration phase under the [integration procedure](docs/implementation/GIT_INTEGRATION_AGENT.md); Antigravity has no merge or protection-bypass authority. Development conclusions and browser artifacts do not establish integration gates. Repository gates remain authoritative regardless of the implementation agent. Read repository rules directly; prompts need not restate them.

Create module `AGENTS.md` files only when a concrete module invariant warrants one; do not build a specialized agent hierarchy.
