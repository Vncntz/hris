---
name: hris-ui-verification
description: Performs browser-driven HRIS UI acceptance review, defect reproduction and UI regression investigation for an exact assigned TASK; defaults to read-only verification.
---

# HRIS browser UI verification

Default to read-only review/verification: do not edit source or implement fixes unless the
exact assigned TASK explicitly authorizes them. Skill discovery grants no backlog authority.
Read root [AGENTS.md](../../../AGENTS.md), the exact IMP/TASK from the
[task index](../../../docs/tasks/README.md),
[execution rules](../../../docs/implementation/EXECUTION_RULES.md) and
[current state](../../../docs/implementation/IMPLEMENTATION_STATE.md).
Follow the [documentation index](../../../docs/README.md) to relevant module AGENTS,
module/domain docs, ADRs and acceptance decisions. This is UI verification, not generic
backend code review. Respect the designated primary implementer and do not concurrently
edit its feature branch without explicit operator authorization. If fixes are authorized,
verify the [assignment eligibility rules](../../../docs/implementation/EXECUTION_RULES.md#3-one-active-task-per-agent):
one active TASK per agent, verified declared dependencies, disjoint edit ownership and
a separate worktree/branch. Read-only review may inspect another lane without gaining
edit authority. Record findings in the assigned TASK; Codex coordinates shared
execution/index/parent documentation during serialized integration.

## Prepare a reproducible browser session

Inspect repository startup/test conventions and the TASK before starting the application.
Use an isolated local synthetic/sanitized test environment and task-approved startup
configuration; never production/customer data or committed credentials. Read-only describes
source/review authority: workflow mutations, where acceptance needs them, are confined to
that disposable test environment. Do not change permissions or data in a live installation.

Record reviewed commit, startup command, browser/version, viewport, synthetic role/permission
preconditions and relevant test-data state. Use Antigravity's browser capability for runnable
screens. If startup, authentication or browser access is unavailable, report the specific
blocker and unverified acceptance criteria rather than infer a pass.

## Exercise the assigned UI

Check the TASK's relevant acceptance criteria:

- Application startup, authentication, session behavior, navigation and route access.
- Permission visibility for authorized/unauthorized roles, including direct URL attempts;
  hidden controls never substitute for application-service authorization.
- Primary workflow and expected resulting state; validation/error and empty states.
- Stale/concurrent conflict behavior and activation/deactivation states when relevant.
- Keyboard navigation, focus, labels and basic accessibility where practical.
- Browser console errors, including reproducible timing/context.
- Representative desktop/laptop viewport sizes; record actual dimensions and inspect
  overflow, clipped controls, dialogs, grids and navigation.
- Accidental exposure of internal IDs, credentials/secrets, stack traces or infrastructure
  details. Distinguish approved public reference IDs from private relational identifiers.

For grid/session or long-operation acceptance retrieve D-120/D-121 through the
[planning index](../../../docs/planning/INDEX.md) and `tools/plan-get.py`; verify observable
bounded loading and progress behavior without treating screenshots as memory/performance proof.

## Report evidence and defects

Report each failure with commit/environment, synthetic role and preconditions, exact ordered
steps, expected versus actual behavior, reproducibility, viewport and useful console evidence.
Identify the affected screen/acceptance criterion so the assigned implementer can reproduce it.
Separate passed, failed, blocked and untested criteria; do not turn unavailable checks into passes.

Capture screenshots/recordings only when useful; use synthetic/sanitized content and keep
machine-local artifacts outside Git unless the exact TASK authorizes a safe tracked artifact.
Never put real employee/applicant/client personal data, secrets or prohibited runtime details
into prompts, screenshots, logs, fixtures or CI artifacts. Report an exposure by category and
location without reproducing sensitive values.

Route defects to the assigned primary implementer; implement only explicitly authorized fixes
and then rerun affected browser and automated checks. UI-focused independent review does not
establish broader architecture/completion authority. Browser artifacts supplement automated
tests and never replace exact-head CI or the
[trusted-main integration gates](../../../docs/implementation/GIT_INTEGRATION_AGENT.md).
Antigravity has no merge/protection-bypass authority and must not start successor work.
