# HRIS IMPLEMENTATION EXECUTION RULES

Version: 1.0
Last Updated: 2026-09-29

## 1. MISSION

Implement the finalized Philippine Manpower & Staffing HRIS incrementally from the approved implementation backlog.

The goal is not merely to make code compile.

Every implementation task must preserve:

- architectural decisions;
- payroll and compliance correctness;
- module boundaries;
- database integrity;
- security;
- testability;
- maintainability;
- Windows/Linux portability;
- solo-developer operability.

## 2. AUTHORITATIVE SOURCES

Use this precedence when implementation sources conflict:

1. Latest explicit user instruction.
2. Verified external technical or compliance facts.
3. Approved ADRs created after planning.
4. `docs/planning/MASTER_SOFTWARE_PLAN.md`
5. `docs/planning/FINAL_PLANNING_STATE.md`
6. `docs/implementation/IMPLEMENTATION_STATE.md`
7. Current implementation task specification.
8. AI recommendation.

Do not silently contradict a higher-authority source.

The planning documents are frozen.

Do not modify:

- `docs/planning/MASTER_SOFTWARE_PLAN.md`
- `docs/planning/FINAL_PLANNING_STATE.md`

during ordinary implementation.

If implementation reveals that a frozen architecture decision should change:

1. stop;
2. identify the conflict;
3. explain the impact;
4. propose an ADR;
5. wait for explicit approval before changing the architecture.

## 3. ONE-TASK RULE

Work on ONE `IMP-###` implementation item at a time.

Do not implement later backlog items merely because they are nearby or convenient.

Supporting changes are allowed only when they are strictly necessary for the current task.

If discovered work belongs to another backlog item:

- record it;
- report it;
- do not implement it unless explicitly authorized.

## 4. REPOSITORY-FIRST RULE

Never hallucinate repository state.

Before proposing or making implementation changes:

1. inspect the repository;
2. inspect the current branch;
3. inspect `git status`;
4. read the current task;
5. read the relevant architecture documentation;
6. inspect existing source/tests/configuration.

Reuse existing project conventions where they exist.

If code and documentation disagree, report the discrepancy instead of guessing.

## 5. STANDARD TASK WORKFLOW

For every implementation task:

1. Read authoritative documentation.
2. Inspect repository and Git state.
3. Review task requirements and acceptance criteria.
4. Identify affected files/modules.
5. Identify risks and conflicts.
6. Produce a focused implementation plan.
7. Wait for approval unless explicitly authorized to proceed automatically.
8. Implement only the approved scope.
9. Run required tests.
10. Run relevant regression tests.
11. Review the complete diff.
12. Check for scope leakage.
13. Update relevant mutable documentation.
14. Update `IMPLEMENTATION_STATE.md` only when warranted.
15. Report completion status.
16. Prepare suggested commit and PR information.

## 6. DEFINITION OF DONE

An implementation task is complete only when:

- all acceptance criteria are satisfied;
- the project compiles;
- required automated tests pass;
- relevant regression tests pass;
- database migrations are validated when applicable;
- authorization/security implications are addressed when applicable;
- audit implications are addressed when applicable;
- documentation affected by the change is updated;
- no unrelated backlog scope was introduced;
- implementation state is updated;
- no unresolved blocker prevents completion.

A task is not complete merely because the happy path works or the code compiles.

## 7. TESTING RULES

Prefer automated verification.

Never:

- delete a valid test merely to make a change pass;
- weaken assertions merely to obtain a green build;
- hide failing tests;
- silently skip required tests.

When fixing a defect, add a regression test where practical.

For MySQL-specific behavior, verify against MySQL when the relevant task requires it.

## 8. ARCHITECTURE RULES

The approved architecture is a modular monolith.

Do not introduce without an approved architectural change:

- microservices;
- Kafka;
- Kubernetes;
- distributed databases;
- Redis as required infrastructure;
- service meshes;
- generalized distributed messaging;
- customer-specific source-code forks.

Cross-module behavior must respect the ownership and dependency rules defined in the Master Software Plan.

`shared-kernel` must remain small and contain only genuinely neutral primitives.

## 9. DATABASE RULES

Do not:

- edit production data manually as an implementation shortcut;
- rewrite released migration history;
- introduce schema changes without the required migration mechanism once Flyway is established;
- rely on unsupported database behavior.

Database changes must preserve:

- deterministic migrations;
- data integrity;
- upgradeability;
- rollback/recovery strategy.

## 10. COMPLIANCE RULE

Never invent Philippine statutory:

- formulas;
- contribution rates;
- withholding tables;
- deadlines;
- forms;
- file formats;
- legal requirements.

Use authoritative verified sources when required.

If not verified, keep the item explicitly marked for verification.

## 11. PRIVACY RULE

Never place real customer production data into:

- source control;
- normal development fixtures;
- AI prompts/context;
- synthetic datasets;
- CI artifacts.

Use synthetic or explicitly sanitized test data.

Secrets must never be committed.

## 12. GIT RULE

Normal implementation workflow:

one implementation task
→ one focused branch
→ one focused PR
→ review
→ merge

Suggested branch formats:

- `feat/imp-###-short-description`
- `fix/imp-###-short-description`
- `chore/imp-###-short-description`

Do not mix unrelated implementation items into the same PR unless explicitly approved.

Do not commit directly to protected `main` once branch protection is established.

Do not push, merge, or delete branches unless authorized by the user or the active task workflow explicitly allows it. The dedicated Git Integration Agent has standing authority to merge an eligible PR through protected `main` only after independently verifying every gate in the [Git Integration Agent specification](GIT_INTEGRATION_AGENT.md).

## 13. AI AUTONOMY

Unless explicitly authorized otherwise, the coding agent may:

- inspect files;
- inspect Git state;
- plan changes;
- edit files after approval;
- run builds;
- run tests;
- review diffs.

The coding agent must not automatically:

- change frozen planning artifacts;
- change architecture;
- expand task scope;
- merge protected `main` except as the dedicated Git Integration Agent under its mandatory gates;
- use real customer data;
- expose secrets;
- bypass failing tests;
- claim verification it did not execute.

## 14. IMPLEMENTATION STATE

`docs/implementation/IMPLEMENTATION_STATE.md` is the authoritative mutable execution state.

Conversation history is not authoritative implementation state.

When a task is completed, state must record:

- completed task;
- current milestone;
- next task;
- blockers;
- active implementation decisions;
- approved ADRs;
- outstanding verification items.

Never mark a task COMPLETE unless its acceptance criteria and required verification have passed.

## 15. SOLO-DEVELOPER FILTER

Every implementation decision should satisfy:

> Can one developer realistically understand, test, deploy, update, diagnose, and support this across multiple customer installations?

Prefer the simplest design that satisfies correctness, compliance, security, data integrity, performance, and scale requirements.
