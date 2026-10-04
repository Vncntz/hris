---
name: hris-vaadin-ui
description: Implements an explicitly assigned HRIS Vaadin UI TASK as Antigravity primary implementer, with browser acceptance verification and module ownership constraints.
---

# HRIS Vaadin UI implementation

Start only from an exact operator/planner-assigned IMP/TASK that designates Antigravity
as primary implementer. Skill discovery does not authorize backlog selection. Follow the
root [AGENTS.md](../../../AGENTS.md) and its one-primary-implementer rule. One active
TASK per agent; independent Codex and Antigravity assignments may run concurrently only
under [assignment eligibility](../../../docs/implementation/EXECUTION_RULES.md#3-one-active-task-per-agent).
Verify owner, baseline SHA, declared dependencies with exact-head/exact-merge CI,
permitted edit paths/modules and separate branch/worktree before editing. Never share
an active checkout or branch or overlap another lane's source/test ownership. Same-TASK
collaboration still requires explicit operator authorization.

Use only verified application contracts. An unfinished required backend contract blocks
this UI TASK even when its view files are disjoint. Record evidence in this TASK;
Codex reconciles shared execution/index/parent documentation during serialized integration.
After another PR merges, refresh the candidate with current main, rerun required checks
and obtain new exact-head policy/Linux/Windows CI before integration. Skill discovery
and an unrelated task's completion never authorize successor work.

Read the exact work order from the [task index](../../../docs/tasks/README.md), its parent
IMP, [execution rules](../../../docs/implementation/EXECUTION_RULES.md),
[workflow](../../../docs/implementation/REPOSITORY_WORKFLOW.md), and
[current state](../../../docs/implementation/IMPLEMENTATION_STATE.md).
Use the [documentation index](../../../docs/README.md) to read relevant module AGENTS,
module/domain docs, approved ADRs and compliance references. Retrieve only relevant
planning decisions via the [planning index](../../../docs/planning/INDEX.md) and
`python tools/plan-get.py D-120` (D-121 when applicable). Inspect existing UI, contracts,
source/tests and Git state before editing; remain within the exact TASK and owning module.

## Implementation boundaries

Preserve one Maven modular monolith and deployable application, a small neutral shared
kernel, and dependency direction. Reuse existing application/service/query contracts.
Do not consume another module's private JPA entity or repository, retain lazy entity
state across the UI boundary, or write another module's aggregates. Backend/application
contract changes require explicit authorization in the exact TASK; otherwise report the
missing contract for the planner without implementing it. Do not create customer forks,
change frozen plans or invent statutory/compliance behavior.

Preserve authentication and application-service authorization; UI permission visibility
supplements server enforcement. Use synthetic or explicitly sanitized data only, including
prompts, tests, fixtures, recordings and screenshots. Never introduce secrets or real
employee/applicant/client personal data into Git or verification artifacts.

## Vaadin performance constraints

Apply **D-120 - Vaadin Screens Must Be Lazy, Bounded, and Session-Light** from the
indexed frozen source. Normal lists/grids require server-side lazy filtering/sorting and
bounded fetch/page sizes. Never load complete employee, attendance, payroll or client
collections into Vaadin session memory. Store IDs, filters and small view models rather
than entity graphs or generated files. Avoid expensive exact counts when unnecessary;
use seek/keyset pagination for deep/high-volume browsing where applicable.

Do not perform expensive long-running work on the Vaadin UI/request thread. When the
TASK includes long operations, retrieve **D-121 - Long-Running Vaadin Operations Use
Persisted Job Progress and Polling by Default**. Start a durable job through its authorized
contract and return a progress view immediately; poll modestly only while relevant views
are open. Refreshing/closing the browser must not stop the job. Do not introduce global
push/WebSocket infrastructure without the required architectural authorization.

## Verification and handoff

Run the assigned automated checks and regressions under repository governance. For runnable
screens, use Antigravity's browser capability and the
[UI verification skill](../hris-ui-verification/SKILL.md) to check TASK acceptance and
capture useful synthetic evidence. Report unavailable startup/browser prerequisites as
verification gaps; never claim an unexecuted check passed.

Review the full diff and report commands/results, acceptance evidence, changed paths and
remaining defects. Browser screenshots/recordings are supplemental evidence, never substitutes
for automated tests, exact-head CI or integration gates. Hand the locally verified branch to
Codex's [trusted-main integration procedure](../../../docs/implementation/GIT_INTEGRATION_AGENT.md).
Antigravity has no merge/bypass authority and cannot select the next TASK. External
planner/reviewer independently reviews completion after integration.
