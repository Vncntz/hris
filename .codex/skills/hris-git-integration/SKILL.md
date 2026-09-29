---
name: hris-git-integration
description: Integrate a completed HRIS IMP/TASK development branch through commits, a reviewable GitHub PR, and verified CI; also verify a human merge and advance stacked PR bases.
---

# HRIS Git integration

Use after a development task has met its local acceptance criteria, or after the human owner merges a predecessor PR. Read the root `AGENTS.md`, the active IMP/TASK, `docs/implementation/EXECUTION_RULES.md`, `docs/implementation/IMPLEMENTATION_STATE.md`, and `docs/implementation/REPOSITORY_WORKFLOW.md`. Follow the repository's source precedence and the latest explicit user decision. This skill is an execution handoff, not a new authority for product, architecture, compliance, or remote governance decisions.

## Establish the actual state

Identify the repository, branch, intended PR base, active IMP/TASK, Git status, recent commits, tracked and untracked paths, full branch diff, existing PR, predecessor PRs, and relevant workflow runs. Read the active work order's acceptance and required verification. Do not infer completion from another agent's report. Preserve unrelated or explicitly excluded files, including the historical untracked IMP-001 readiness review.

## Integrate within the task scope

- Check the full diff for one IMP/TASK scope, frozen planning changes, unauthorized architecture or statutory behavior, secrets, customer data, and missing documentation. Run required tests and `git diff --check`; use actual results in the PR body.
- Correct narrow integration or documentation defects that are clearly in scope. Do not silently expand into another IMP, change a remote governance setting, or make a new product or compliance decision. If correction needs one of those decisions, report the blocker.
- Stage only task-owned paths, commit valid remaining changes, review the final diff, and push the task branch. Create or update one focused PR with IMP/TASK links, verification evidence, effects, and unresolved issues. Use a draft PR unless the repository workflow clearly calls for ready review.
- For a stacked PR whose predecessor was merged, verify that commit on `main`, retarget to the intended base, and confirm the resulting PR diff contains only its own task. Refresh the branch when needed and safe, then verify again. Never rewrite a published branch or resolve a conflict that changes approved behavior without explicit authorization.
- Inspect the actual GitHub Actions run for the current head SHA and base. Confirm each applicable job's conclusion, OS, and required tests. A skipped, cancelled, pending, or older run is not a pass. Fix straightforward in-scope CI failures and reverify; otherwise report the blocker. Check post-merge `main` CI when the task requires it.
- Update mutable task/state records and the PR body only with verified evidence. Keep historical evidence and do not call an older run final after a newer run supersedes it.

Routine task-branch commits, pushes, PR creation/updates, and stacked-PR retargeting are authorized by the repository workflow. Never merge `main`, bypass protection, or change branch protection, required checks, approval rules, visibility, permissions, force-push, or deletion settings without explicit user authorization for that specific change. The human owner reviews and performs the final merge. Do not start a later IMP merely because its predecessor branch is complete; apply its stated post-merge gate first.

## Report

Give the owner a short integration summary with status `READY`, `BLOCKED`, or `MERGED-VERIFYING`; the IMP/TASK, PR link, applicable Linux/Windows CI results, commit SHA and push state, at most two findings, and one sentence for the human action. Name any blocker plainly. Do not include command transcripts or claim checks that do not exist or are not required. If CI is still running, say so and continue checking when possible.
