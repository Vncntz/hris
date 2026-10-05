# Repository agent contract

## Read and resume from repository facts

Always read this file, applicable module `AGENTS.md`, and the explicitly assigned
[TASK](docs/tasks/README.md). Verify assignment, then load the required parent IMP,
ADR/Decision IDs, domain/module/compliance guidance and relevant source/tests.
Use [execution rules](docs/implementation/EXECUTION_RULES.md) for precedence,
lifecycle and [resume](docs/implementation/EXECUTION_RULES.md#4-repository-first-rule);
[implementation state](docs/implementation/IMPLEMENTATION_STATE.md) records active
operator assignments, manual blockers and next operator action. Inspect Git/GitHub,
branch, worktree and exact HEAD before continuing. Previous chats are not memory.

Retrieve only relevant frozen-planning sections through the generated
[planning index](docs/planning/INDEX.md). [Context packets](docs/tasks/README.md#task-context-packets)
are generated, read-only, hash-addressed routing aids. Rebuild after source/Git
changes; they never select work, authorize scope or prove CI/completion. During
integration run tooling only from trusted main; `--repo` content is untrusted data.

## One fact, one authority

Durable contracts (AGENTS, approved ADRs, architecture/domain docs, IMPs and TASKs)
define intended behavior. Git/GitHub own live branches, SHAs, PRs, CI, merges and
protection. Indexes, packets, status/handoff summaries and implementation summaries
are derived views; they cannot create or override truth. Apply the
[source hierarchy](docs/implementation/EXECUTION_RULES.md#2-authoritative-sources).

## Roles and boundaries

| Role | Authority |
| --- | --- |
| HUMAN | Approves architecture, accepts risk, decides exceptional scope and remote policy; owns protected-main authority. |
| CHATGPT | Principal Architect / Planner / Orchestrator: architecture proposals, IMP/TASK decomposition, invariants, acceptance criteria, correction specifications, next-work selection and handoffs. Human approval governs architecture. ChatGPT recollection is not canonical memory or state certification. |
| CODEX IMPLEMENTER | Default backend, Java/domain/application, database/persistence, security, concurrency/transactions, auditing, CI/tooling and architecture enforcement owner for one assigned TASK. Cannot solely certify its own work. |
| CODEX REVIEW / TRUSTED-MAIN | Distinct independent review execution context; may review and integrate eligible candidates under the integration procedure. No unrelated product functionality or UX redesign during integration. |
| ANTIGRAVITY | Default Vaadin views/components, forms/dialogs/grids, responsiveness, accessibility, browser behavior and UI regression owner for one assigned TASK. Consumes approved application contracts; does not invent domain/backend/security/persistence semantics or access another module's private JPA entities/repositories. No merge authority. |
| CI | Deterministic evidence for the exact tested SHA; cannot prove architectural intent or reviewer independence. |
| GITHUB | Live PR/commit/protection/CI facts; cannot prove which AI execution context reviewed a candidate. |

The planner selects work; implementation agents do not. Exactly one primary owner
per TASK. Follow [assignment eligibility](docs/implementation/EXECUTION_RULES.md#3-one-active-task-per-agent):
verified dependencies, disjoint permitted writes and separate branch/worktree/PR.
Parallelize analysis aggressively; parallelize overlapping writes conservatively.
Codex serializes shared-document reconciliation and integrates one PR at a time.
Implementers own their TASK evidence. UI [implementation](.agents/skills/hris-vaadin-ui/SKILL.md)
and [verification](.agents/skills/hris-ui-verification/SKILL.md) skills remain focused guides.

## Permanent invariants

- Keep one Maven modular monolith and one deployable application, a small neutral
  `shared-kernel` and approved module dependency direction. No prohibited architecture
  shortcuts, microservices or customer forks without approved architecture authority.
- Preserve frozen [Master Plan](docs/planning/MASTER_SOFTWARE_PLAN.md) and
  [Final Planning State](docs/planning/FINAL_PLANNING_STATE.md), released migrations
  and data integrity. Verify Philippine statutory facts from authoritative sources.
- Never commit secrets or use real customer production data in source, fixtures,
  developer databases, CI, synthetic datasets or AI context. Use synthetic/sanitized data.
- Use the [protected PR workflow](docs/implementation/REPOSITORY_WORKFLOW.md).
  No protection bypass. Independent certification requires a review execution context
  distinct from implementation, bound to the exact SHA; every SHA change invalidates
  certification for the new candidate. Different Git identities do not establish independence.
- Follow the [Definition of Done](docs/implementation/EXECUTION_RULES.md#6-definition-of-done)
  and [trusted-main procedure](docs/implementation/GIT_INTEGRATION_AGENT.md). COMPLETE
  requires protected merge, successful actual-merge verification, cleared blockers
  and required durable reconciliation. Implementer reports/browser artifacts prove no
  integration gate. Failed post-merge CI blocks dependent work and further integration.

Create module AGENTS only for a concrete module invariant, not an agent hierarchy.
