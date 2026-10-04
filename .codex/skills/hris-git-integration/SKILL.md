---
name: hris-git-integration
description: Integrate a verified HRIS IMP/TASK branch through a reviewable PR and protected main.
---

# HRIS Git integration

Use after an active development task passes local verification, or after a predecessor PR merges.
Read the root [agent contract](../../../AGENTS.md).
Follow the canonical [Git Integration Agent procedure](../../../docs/implementation/GIT_INTEGRATION_AGENT.md); it contains the complete integration gates, workflow, and escalation rules. Integrate one PR at a time, independently
verify declared dependencies and active-lane edit ownership, and preserve explicit task
prerequisites. Codex coordinates shared documentation reconciliation. After another PR
merges, require candidate refresh, repeated required verification and fresh exact-head
policy/Linux/Windows CI; wait for exact-merge push CI before the next ordinary merge.
