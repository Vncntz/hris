# Client Management master data

[IMP-014](../implementation/tasks/IMP-014.md) / [TASK-0036](../tasks/TASK-0036.md)
implements the Client Company and Client Site foundation under D-073/D-074/D-110/D-111/D-112.
Client Management exclusively owns both internal JPA entities and repositories. Public UUIDs
are separate from compact relational keys. Sites have one immutable parent Company with
RESTRICT foreign keys. Names are trimmed, nonblank, at most 200 Java UTF-16 units, and reject
control characters and unpaired surrogates. Duplicate names are allowed; UUIDs define identity.

## Application boundaries

`ClientManagementService` provides createCompany/createSite, renameCompany/renameSite and
explicit activate/deactivate Company/Site commands. Edits require the projection's expected
version; stale edits and no-ops reject without audit. There is no delete, parent reassignment,
import, deduplication, external API or broad administration UI.
Every command first requires `CurrentActor.requireAuthority("client:admin")`, then derives
its actor using `requireUserId()`. Identity administration creates/assigns the permission
through existing services with recent proof; migrations never seed it or any assignment.

`ClientReferences` returns Optional immutable `ClientCompanyReference`/`ClientSiteReference`
records through short read-only READ COMMITTED transactions. Only public UUIDs, display names, activity and non-secret edit versions cross the
boundary. Site includes its parent UUID and parent activity; `effectiveActive()` is the
conjunction of stored Site and Company activity. Joined projection queries observe both
in one database statement, without entities, repositories, lazy state or internal IDs.
Consumers must resolve current effective activity before new active business references;
no future deployment/requisition workflow is implemented here. Reads are an internal module
contract; a future user-facing adapter must enforce its own authorized read policy.

## Lifecycle and transaction invariants

Creation is active. Site creation and activation reject an inactive parent. Company deactivation
preserves all Sites, identities and their stored states; Company reactivation does not activate
inactive Sites. Corrections and Site deactivation remain allowed under an inactive Company.

Commands own a 15-second REQUIRES_NEW READ COMMITTED transaction and reject an ambient caller
transaction before mutation, so success means commit and commit errors are bounded. Parent
Company rows are locked/refreshed before Site rows; Company commands never lock Sites.
Immutable parentage lets a stable projection locate the parent before locks. Expected versions
and JPA versions prevent silent lost edits; database lock/optimistic conflicts are fixed outcomes
without retries. Version overflow rejects. Locked entities refresh retained persistence state.

Each successful mutation records exactly one append-only audit event in the same transaction.
Actions distinguish Company/Site CREATED, RENAMED, ACTIVATED and DEACTIVATED. Actor/target are
Account and Client public UUIDs; reason is null; Company context is null; Site context contains
only `company=<UUID>`. Display names, requests, internal IDs and infrastructure causes are
absent. Final flush covers business and audit writes; either failure rolls both back.
`ClientManagementException` exposes only a fixed Reason without underlying causes or payloads.

## Migration and operation

V10 adds only `client_company` and `client_site`, UUID uniqueness, bounded nonblank names,
Boolean/version checks, parent index and RESTRICT parent FK. No Company/Site, authority, Role,
membership or credential is seeded. Released V1-V9 remain immutable. Populated V9 upgrade
preserves every prior table and migration checksum and matches clean V10 schema on MySQL 8.4.11.
Rolling back to an older binary leaves additive tables unused; database corrections are forward-only.
Contact/address data, manpower requests, positions/deployment, compensation, billing and portal
behavior require separate focused authorization.
