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
records through independent 15-second REQUIRES_NEW read-only READ COMMITTED transactions
([TASK-0037](../tasks/TASK-0037.md)). Ambient caller transactions are suspended, so older
snapshots cannot hide committed Company/Site lifecycle changes. Each ambient caller retains
its connection while the read uses another; callers must keep transactions short and account
for that connection in pool capacity. Only public UUIDs, display names, activity and non-secret
edit versions cross the boundary. Site includes its parent UUID and parent activity; `effectiveActive()` is the
conjunction of stored Site and Company activity. Joined projection queries observe both
in one database statement, without entities, repositories, lazy state or internal IDs.
Consumers must resolve current effective activity before new active business references;
no future deployment/requisition workflow is implemented here. Reads are an internal module
contract; a future user-facing adapter must enforce its own authorized read policy.

## Bounded administrator browsing

[TASK-0039](../tasks/TASK-0039.md) adds `ClientAdministrationQueries.companies(offset, limit)`
and `sites(companyPublicId, offset, limit)`. Both require authenticated `CurrentActor`
`client:admin` authority before validation or database work. Offsets are zero-based nonnegative
Java integers; limits are 1 through `MAX_LIMIT` (200). Invalid pages return `INVALID_PAGE`;
null parent UUIDs return `INVALID_TARGET`; unknown parents return `NOT_FOUND`. Known Companies
with no Sites and offsets beyond the end return empty pages with `hasMore=false`.

`ClientCompanyPage` and `ClientSitePage` defensively copy immutable reference rows and expose
only `rows` and `hasMore`. Each query fetches at most limit plus one projection rows, discards
the look-ahead row, and uses it to determine `hasMore`; no full-table materialization or total
count query is exposed. Display names sort ascending under the installed database collation,
then public UUIDs ascending give equal names a stable tie-breaker. Sites are restricted to
one parent UUID, with stored and parent activity projected in the same joined statement.

Reads use independent 15-second REQUIRES_NEW read-only READ COMMITTED transactions, including
when a caller already has an older snapshot. Results include current committed edit versions.
Each call is a new committed-state read: concurrent inserts/renames may shift later offset
pages; callers should refresh after changes. No cross-call snapshot or locking is promised.
As with reference reads, an ambient transaction retains its connection while browsing uses
another. Query/commit failures return fixed `PERSISTENCE_FAILED` without causes or SQL details.
Browsing creates no mutation audit events. The existing known-UUID contract and mutation
semantics are preserved; no schema or frontend change is introduced.

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
