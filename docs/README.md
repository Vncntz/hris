# Documentation index

The frozen [Master Software Plan](planning/MASTER_SOFTWARE_PLAN.md) and [Final Planning State](planning/FINAL_PLANNING_STATE.md) govern architecture. Use the generated [planning decision index](planning/INDEX.md) for targeted retrieval; it is not authoritative. [Execution rules](implementation/EXECUTION_RULES.md), [implementation state](implementation/IMPLEMENTATION_STATE.md), [repository workflow](implementation/REPOSITORY_WORKFLOW.md), and [Git Integration Agent procedure](implementation/GIT_INTEGRATION_AGENT.md) govern execution. The completed [IMP-004 control file](implementation/tasks/IMP-004.md) records the MySQL baseline; the completed [IMP-005 control file](implementation/tasks/IMP-005.md) records the Flyway foundation; the completed [IMP-006 control file](implementation/tasks/IMP-006.md) records the shared-primitives work; the completed [IMP-007 control file](implementation/tasks/IMP-007.md) records the configuration work; the completed [IMP-008 control file](implementation/tasks/IMP-008.md) records the audit foundation; the completed [IMP-009 control file](implementation/tasks/IMP-009.md) records the authentication foundation; the completed [IMP-010 control file](implementation/tasks/IMP-010.md) records the synthetic-data generator foundation; the completed [IMP-011 control file](implementation/tasks/IMP-011.md) records the architecture-guardrail work and [TASK-0015 coverage follow-up](tasks/TASK-0015.md). Use [implementation state](implementation/IMPLEMENTATION_STATE.md) for the current work order. [IMP-012](implementation/tasks/IMP-012.md) is complete after fresh-cycle review; [IMP-088](implementation/tasks/IMP-088.md) TASK-0019/0020/0021 are post-merge verified. [IMP-013](implementation/tasks/IMP-013.md) remains ACTIVE with [TASK-0022](tasks/TASK-0022.md) POST_MERGE_VERIFIED and [TASK-0023](tasks/TASK-0023.md) POST_MERGE_VERIFIED / complete after PR #40 and its verified merge-SHA push CI. [TASK-0024](tasks/TASK-0024.md), authenticated account creation boundary, is POST_MERGE_VERIFIED / complete after [PR #42](https://github.com/Vncntz/hris/pull/42) and successful exact-merge-SHA push run [36955721444](https://github.com/Vncntz/hris/actions/runs/36955721444). [TASK-0025](tasks/TASK-0025.md) is Complete / POST_MERGE_VERIFIED after corrective [PR #45](https://github.com/Vncntz/hris/pull/45), final head `8668ad6da15b81260758c1821452381535860b9d`, exact-head run [36979448732](https://github.com/Vncntz/hris/actions/runs/36979448732), merge `d5e0cc99ffc88ee909f145b817de23c888b2f596`, and successful exact-merge main push run [36979936228](https://github.com/Vncntz/hris/actions/runs/36979936228). All three policy/Linux/Windows jobs passed in both runs. [TASK-0026](tasks/TASK-0026.md) is Complete / `POST_MERGE_VERIFIED` after PR #47 and its verified exact-merge push CI. [TASK-0027](tasks/TASK-0027.md) is Complete / `POST_MERGE_VERIFIED` after PR #49 and exact-merge push run 37007864655. [TASK-0028](tasks/TASK-0028.md) is PLANNED for Role/Permission administration with Role authority-generation invalidation. IMP-013 remains ACTIVE. Pass the TASK-0028 planning gate before implementation; no implementation is authorized or started in this run. Do not allocate TASK-0029 or advance to IMP-014. See the [operator runbook](deployment/first-administrator.md). [TASK work orders](tasks/README.md) map focused execution to IMP items.

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

These indexes are entry points. Add focused records as the relevant work is implemented and verified.

For an explicitly assigned TASK, the [task context packet](tasks/README.md#task-context-packets) provides hashed routing references and targeted frozen-decision excerpts. It is a read-only derived cache; source review and integration gates remain authoritative.

TASK-0025's original PR #44 finding was closed by corrective PR #45; its task record preserves the historical defect and independently verified completion evidence. TASK-0026 planning preserves the supplied work order without beginning implementation.

TASK-0026 completion is independently verified from PR #47, final head `b21da2b5f9684d639388f0ead70bb36f5ac900a7`, exact-head run [36988841675](https://github.com/Vncntz/hris/actions/runs/36988841675), merge `8a44a36e1a7683c36f130f5836b7fb532e3b16f2` and exact-merge push run [36989610995](https://github.com/Vncntz/hris/actions/runs/36989610995); all three policy/Linux/Windows jobs succeeded. TASK-0027 preserves the supplied scope; the later operator instruction authorizes implementation after its verified planning gate.

## TASK-0027 verified closeout and TASK-0028 planning - 2026-10-02

Implementation [PR #49](https://github.com/Vncntz/hris/pull/49) final head `b00dd5d7aa5f51c00c0015d33582decdeb26c761` passed `ci / policy`, `ci / build-linux`, and `ci / build-windows` in [exact-head run 37007056476](https://github.com/Vncntz/hris/actions/runs/37007056476). It merged through protected `main` as `c0807b0df23efa6f4799793c5a289d76d77ccbd5`; [exact-merge main push run 37007864655](https://github.com/Vncntz/hris/actions/runs/37007864655) passed the same three jobs on that exact SHA. GitHub metadata, job steps and actual policy/Linux/Windows logs were independently inspected. Policy ran 42 tooling tests; both build jobs reported BUILD SUCCESS, and each Linux run executed 74 real-MySQL integration tests with zero failures, errors or skips. TASK-0027 is Complete / `POST_MERGE_VERIFIED`.


TASK-0028 is the next planned Role/Permission administration work order; its acceptance criteria and planning checks are recorded in the work order and IMP-013. Pass its planning integration and exact-merge push CI gate before implementation. No application implementation is authorized or started in this run. Recovery/reset, privileged recent re-authentication, offline TOTP MFA and final closure review remain deferred. Do not allocate TASK-0029 or begin IMP-014.
