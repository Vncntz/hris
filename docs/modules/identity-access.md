# Identity and Access

[IMP-009](../implementation/tasks/IMP-009.md) / [TASK-0012](../tasks/TASK-0012.md) established local account authentication, security-principal state, and the `CurrentActor` authorization boundary. [IMP-013](../implementation/tasks/IMP-013.md) / [TASK-0022](../tasks/TASK-0022.md) adds role/permission persistence and authenticated authorities. The public account UUID is independent of any Worker identity. A newly migrated database has no usable login until the explicitly invoked [first-administrator command](../deployment/first-administrator.md) succeeds.

`identity_account` is an internal JPA aggregate in the global V3 Flyway migration. Login names are stripped, folded with `Locale.ROOT`, and restricted to ASCII letters, digits, `.`, `_`, and `-` before storage; a database unique constraint enforces the canonical name. The table stores only a Spring Security delegating encoding with the `{argon2@SpringSecurity_v5_8}` identifier and an Argon2id payload. Raw credentials and password hashes must not enter logs, audit context, diagnostics, fixtures, or support reports. Password hashes are not session principal data. The initial Spring Security work factor is an engineering baseline and **has not been calibrated on supported production hardware**; production security qualification must measure and set an appropriate factor before deployment.

Authentication uses a short transaction and a pessimistic row lock per known account. Five consecutive failed password attempts lock the account for 15 minutes by default; the count and UTC lock instant survive process restart. Disabled, locked, unknown, and wrong-password accounts receive the same ordinary external failure. Unknown and unsupported login names trigger a dummy hash verification. Successful authentication after lock expiry clears failure state. `HRIS_MAX_FAILED_ATTEMPTS` and `HRIS_LOCK_DURATION` configure the bounded defaults. The injectable `Clock` controls security instants; `DATETIME(6)` columns hold UTC wall time by explicit conversion.

[TASK-0033](../tasks/TASK-0033.md) keeps the Account provider as the sole Spring-managed
authentication provider, discovered by the global authentication manager. The servlet
filter chain does not register it again in its child manager: otherwise a credential
rejection retries the same provider through the parent and doubles eligible failure
bookkeeping. One wrong HTTP form submission now contributes one persisted failure;
the configured threshold locks on exactly that request count. Account policy and
TASK-0032 recent-proof behavior remain unchanged.

Vaadin's `VaadinSecurityConfigurer` handles the anonymous login view, form login, navigation access, CSRF exceptions required for Vaadin internals, and logout. The root view requires an authenticated principal; an unannotated view is denied by Vaadin navigation access control. Standard Spring Security session fixation, CSRF, and logout behavior remain enabled. Servlet session idle timeout defaults to 30 minutes through `server.servlet.session.timeout`; `HRIS_SESSION_IDLE_TIMEOUT` can override it. Sessions stay in the local servlet container. HTTPS provisioning and distributed sessions are outside this task.

V5 adds the internal JPA Role and Permission model and account-role/role-permission join tables. A role has a stable binary public UUID, unique canonical ASCII name, enabled flag, and optimistic row version. Role names are stripped and folded with `Locale.ROOT`; canonical names start with an ASCII letter or digit and contain only lowercase ASCII letters, digits, `.`, `_`, and `-`, up to 128 characters. Permission authority keys are stable identifiers, starting with a lowercase ASCII letter and otherwise allowing lowercase ASCII letters, digits, `.`, `_`, `:`, and `-`, up to 128 characters. Application constructors canonicalize these values and database CHECK/unique constraints enforce their stored forms. Neither a complete permission catalog nor any authorization seed is introduced.

Membership exists only while its join-table row exists. Composite primary keys reject duplicate assignments. Foreign keys use RESTRICT for both deletion and key updates; future administration must revoke memberships deliberately before deleting referenced records. Primary-key order supports the account-to-role-to-permission lookup, and reverse indexes support referenced-key checks. Internal entities, mappings, and repositories remain inside Identity & Access.

Successful password authentication fetches assigned roles and their permissions in one joined JPA query within the existing short account transaction. Only enabled roles contribute authorities; missing roles, roles without permissions, and disabled roles grant none. Authority keys are deduplicated and sorted, then copied into Spring Security's authenticated token. The principal retains only the stable public account ID and canonical login; token credentials are absent. A lookup failure cannot yield an authenticated token. An account without an assigned permission can authenticate with zero authorities.

Authorities are a login/session snapshot. TASK-0027 Account-to-Role mutations invalidate that snapshot through generation advancement and affected-session expiry, requiring fresh authentication. TASK-0026 does the same for account lifecycle. TASK-0028 Role enablement and Role-to-Permission administration invalidate affected Accounts through Role generation and targeted session expiry as described below. There is no authorization cache or distributed session invalidation.

TASK-0025 supplies a reusable local revocation primitive for later administrative commands;
TASK-0027 reuses it for assignment administration and TASK-0026 for lifecycle commands. Password change uses it now
to expire all tracked sessions belonging to the authenticated account.

Application services may depend on `CurrentActor` to require the authenticated public user ID and test or require a named authority. The normal authentication provider now supplies authorities from persisted assignments; synthetic tests prove both successful enforcement and denial. TASK-0027 supplies Account-to-Role administration and its assignment audit events; TASK-0028 supplies Role/Permission administration. TASK-0029 adds recent credential re-authentication below. TOTP enrollment/challenge/recovery and business-module call sites remain later focused work.

API and dependency choices were checked against [Spring Security 7 password storage](https://docs.spring.io/spring-security/reference/7.0/features/authentication/password-storage.html), [Vaadin Spring Boot security](https://vaadin.com/docs/latest/flow/security/enabling-security), and the [Bouncy Castle 1.86 provider release](https://www.bouncycastle.org/download/bouncy-castle-java/). Bouncy Castle is centrally versioned in the repository because the Spring Boot/Vaadin BOMs do not manage its provider artifact.

## First-administrator provisioning

[TASK-0023](../tasks/TASK-0023.md) adds an explicitly imported local provisioner, absent from ordinary web startup. Identity & Access owns all identity writes; application composition supplies the secure terminal adapter and existing shared-kernel AuditRecorder contract. No dependency on Platform/Operations implementation is introduced in this module. Password input is twice-entered with echo disabled, bounded at 12–128 Unicode code points, preserved without trimming/case folding, and adaptively encoded before acquiring database locks. Mutable secret buffers are cleared; no credential-bearing request/result is exposed.

V6 adds a technical singleton with irreversible completion protected by constraints/triggers. A short READ COMMITTED row-locked transaction rejects prior identity/authorization state and commits the enabled account, enabled administrator role, sole identity:admin authority, both memberships, completion marker and FIRST_ADMINISTRATOR_PROVISIONED audit event together. Actor/target are the new public account UUID; fixed context is local-operator-bootstrap. Audit failure rolls back everything. Closure is independent of account, role or membership lifetime. No recovery/reset path, administration API, HTTP listener or anonymous setup route is added. Normal authentication produces the explicit authority and CurrentActor enforcement; provisioning itself creates no session. See the [operator runbook](../deployment/first-administrator.md) and TASK verification record. Production password-cost calibration and remaining privileged-account hardening remain later work.

## Authenticated ordinary account creation

[TASK-0024](../tasks/TASK-0024.md) provides AccountCreationService in ordinary application composition. Every invocation requires CurrentActor identity:admin and derives the audit actor from requireUserId. Only a login and two mutable credential buffers are accepted. The immutable CreatedAccount result contains only the new stable public UUID. The account starts enabled with existing initial credential/security timestamps and version state, zero roles and zero authorities. Creation never copies creator privileges or changes bootstrap completion.

Canonical login validation and the V3 unique constraint remain authoritative. Shared Identity-owned InitialCredentials preserves bootstrap's 12-128 Unicode-code-point validation, malformed-surrogate rejection, exact confirmation and unchanged password code units. Both owned buffers clear on all exits. Existing Argon2id encoding runs before opening a 15-second READ COMMITTED transaction. The service is a standalone command and refuses an active caller transaction before hashing.

Account and exactly one IDENTITY_ACCOUNT_CREATED audit event commit atomically. Actor is the authenticated creator UUID; target type is IDENTITY_ACCOUNT and target is the new UUID. Context is fixed authenticated-account-creation, with no login, request serialization or credential. Duplicate canonical logins return a bounded DUPLICATE_LOGIN reason after rollback; encoder and other persistence failures return fixed reasons without internal causes. Audit failure rolls back creation. No migration, seed, role/permission/membership mutation, UI or HTTP administration route is added. Remaining IMP-013 hardening and administration are deferred as recorded in the TASK.

## Authenticated password change and local session revocation

[TASK-0025](../tasks/TASK-0025.md) supplies `PasswordChangeService.change` in ordinary
application composition. Its only inputs are three owned mutable character buffers:
current password, new password and confirmation. `CurrentActor.requireUserId()` rejects
anonymous/unsupported authentication and provides both actor and target; no authority,
login or arbitrary-account parameter is accepted. New credentials reuse InitialCredentials
validation. All three buffers clear on every exit. No credential-bearing request or result
is serialized, and bounded exceptions retain no underlying infrastructure cause.

The standalone command refuses an ambient transaction before reading/verifying the
credential. A scalar enabled-account encoding snapshot is read without a write lock;
current-password verification and replacement Argon2id encoding run outside the mutation
transaction. A 15-second REQUIRES_NEW, READ COMMITTED transaction locks the public UUID,
refreshes the managed entity under that lock, rechecks enabled state and the verified
encoding, and rejects stale changes without retry. This also protects requests with a
servlet-bound persistence context retained across an earlier transaction.
The mutation updates only the encoding, credential/security UTC timestamps from Clock,
failure count and temporary lock. UUID, canonical login and assignments are preserved.

Exactly one IDENTITY_PASSWORD_CHANGED audit event commits with the credential update.
Actor/target are the account public UUID, target type is IDENTITY_ACCOUNT and context is
fixed self-service-password-change. No login, credential, hash or session identifier is
recorded. A shared persistence-context flush covers both writes before expiry marking.
Audit or flush failure rolls back without revocation. Revocation failure rolls back the
database; sessions already marked expired may remain revoked. Likewise, a later commit
failure can leave sessions expired while the old password remains valid. The user must
authenticate again with the old credential in that safe failure case; there is no
distributed transaction between MySQL and servlet memory.

`AuthenticatedSessionRevoker.revoke(UUID)` matches every supported AccountPrincipal
snapshot by public UUID, ignoring unrelated accounts and unsupported principals. The
in-memory SessionRegistryImpl, servlet HttpSessionEventPublisher and standard Spring
Security registration/expiry filter track local authenticated sessions. The resolved
repository-managed Spring Security version is 7.1.1; its supported maximumSessions(-1)
configuration retains unlimited concurrent-session semantics. Marked sessions are
invalidated and require login on their next request, including the caller. Servlet
destruction/logout events remove registry entries. Idle timeout remains 30 minutes by
default, with existing fixation protection, CSRF and logout. Restart destroys local
sessions as before; no distributed-session guarantee is added. APIs were checked against
the resolved JARs and [Spring Security session documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).

TASK-0025 added no production UI/HTTP adapter or assignment operation. TASK-0026 adds
only V7 and the application-service lifecycle boundary. The HTTP integration probe is test-only and exercises the production filter
chain with a narrow authenticated test-route rule. Recovery/reset, privileged MFA, re-authentication and password-cost calibration remain deferred.
TASK-0027 supplies the separately authorized assignment boundary below.

## Authentication-generation check at final session registration

TASK-0025's original PR #44 left a gap between successful credential verification and
servlet session registration. A password-change registry scan could miss a login paused
in that gap. The correction originally carried the credential-update timestamp as request-only generation.
TASK-0026 replaces that mechanism with V7's dedicated non-secret BIGINT authentication_generation.
Password replacement and enable/disable increment it; ordinary login/security bookkeeping does
not. Math.incrementExact fails closed before overflow. credential_updated_at_utc is the actual
credential-change timestamp and is never advanced for lifecycle invalidation. The counter is
captured in request-only AuthenticationGeneration details and stripped before context persistence.

`AccountSessionRegistrationService` opens a short REQUIRES_NEW READ COMMITTED transaction,
locks the same public-UUID account row as password change and lifecycle commands, explicitly refreshes it, and
checks enabled state plus generation. The one standard registry registration runs while
that row lock is held. Replacement first means old authentication is rejected before
registration; registration first means password change sees and expires that entry.
Failure exposes a fixed SessionAuthenticationException without infrastructure causes.
The app removes a partial registry entry if registration/commit fails. This preserves
the existing safe revocation/rollback asymmetry.

An ObjectPostProcessor replaces only RegisterSessionAuthenticationStrategy inside
Spring Security's standard composite. The resolved 7.1.1 JAR bytecode verifies the
concurrency/fixation/registration ordering, the call to session authentication before
successfulAuthentication, and subsequent SecurityContextRepository.saveContext. The
framework's CSRF strategy, unlimited concurrency, fixation, expiry filter, 1800-second
default idle timeout and logout lifecycle remain configured. See the
[Spring Security 7.1.1 session lifecycle](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).

The generation is removed in a finally block on both success and failure before the
security context can be saved. AccountPrincipal remains its stable UUID/canonical-login
record; credentials are null and no password/hash/session identifier is added to token,
principal, audit or log state. V7 is the only schema addition; no dependency change is needed. Deterministic
real HTTP/MySQL tests pause after verification and after registration, exercise both
orderings, preserve unrelated sessions and prove normal concurrent logins. A fixed-clock
repeated-change test prevents generation reuse; a real registration commit-failure test
checks registry/context cleanup. See the [TASK evidence](../tasks/TASK-0025.md).

## Administrative account lifecycle

[TASK-0026](../tasks/TASK-0026.md) adds AccountLifecycleService.enable/disable with only
a target public UUID. Both require CurrentActor identity:admin and derive the audit actor
from requireUserId. Self-disable, no-op and missing targets are rejected. The short standalone
REQUIRES_NEW READ COMMITTED transaction locks and refreshes the target, changes enabled/security
state and generation, and clears failed-attempt/temporary-lock state on enable. Credentials,
credential timestamp, identity and assignments remain unchanged.

Exactly one fixed-context IDENTITY_ACCOUNT_ENABLED/DISABLED audit event flushes with the
mutation before every target session is expired locally. Revocation failure rolls back the
database; a later commit failure may retain expired sessions with the preceding database state.
Both directions and both registration race orderings are tested against MySQL/HTTP. A generation
captured before disable cannot register after disable then enable. Unrelated accounts/sessions remain
unchanged. The boundary adds no production UI/REST adapter. TASK-0027 now supplies assignment
administration below; recovery, MFA and privileged re-authentication remain future IMP-013 scope.


## Administrative Account-to-Role membership

[TASK-0027](../tasks/TASK-0027.md) adds AccountRoleAssignmentService.assign/remove with
only Account/Role stable public UUID inputs. CurrentActor requires identity:admin before
any lookup and exclusively supplies the actor. Missing targets, duplicate/absent membership,
disabled-Role assignment and self-removal eliminating effective identity:admin are explicit
bounded rejections without successful audit or expiry. Disabled memberships can be removed.
Self-removal checks the remaining enabled persisted Roles/Permissions, not Role names or
the session authority snapshot. Harmless self changes expire the actor's own sessions.

A 15-second standalone REQUIRES_NEW READ COMMITTED command locks and refreshes the Account,
locks and refreshes selected/remaining Roles in UUID order before validation, mutates only V5 membership and advances
V7 authentication_generation exactly once. All other Account security and identity fields,
unrelated memberships and Role/Permission state remain unchanged. No migration is introduced.
The Account/generation and exact fixed-context IDENTITY_ACCOUNT_ROLE_ASSIGNED/REMOVED event
flush together before target-session expiry. Audit targets are canonical account/role public
UUID pairs, with CurrentActor's UUID and null reason; names, permission dumps, credentials,
internal IDs and session data are absent. Infrastructure causes never escape the boundary.

Revocation failure rolls back membership/generation/audit. A later commit failure can leave
sessions expired while the preceding database membership remains: fresh login uses that state.
The existing final-registration Account lock and generation check prevent old in-flight
login from becoming usable. Deterministic HTTP/MySQL coverage proves both commands and both
registration orderings, rollback, late-commit asymmetry, self-admin protection, unrelated-session
preservation and fresh authority acquisition/removal. The HTTP probe is test-only.

TASK-0027 adds no production adapter. TASK-0028 adds Role/Permission administration as described below.
Recovery/reset, privileged re-authentication, offline TOTP and password-cost qualification
remain future IMP-013/production-security scope.


## Administrative Roles and Permissions (TASK-0028)

RoleAdministrationService requires CurrentActor identity:admin first and derives the audit actor
only from requireUserId. createRole returns a public UUID with canonical unique name, disabled
state, no Permissions and zero authorization_generation. createPermission returns only its
canonical authority key. enable/disable and assignPermission/removePermission target public UUID
and canonical key, preserve Role identity, and advance Role generation exactly once. Assignment
does not enable a Role; no-ops and rejected commands do not advance generations or audit success.
No rename/delete operation, production transport, future permission catalog or seed is added.

Each short standalone REQUIRES_NEW READ COMMITTED transaction locks one Account first, then
all needed Roles in UUID order. Membership commands lock their target Account; Role commands
lock the actor Account and actor's assigned Roles plus the target, to evaluate persisted effective
self-admin state. Final registration locks its Account and every assigned Role, including disabled
Roles. No path acquires a subsequent Account lock while holding a Role lock. Affected Account
UUID discovery is read-only under the target Role lock; membership changes share that lock.

Authentication carries Account generation and an immutable public Role UUID/generation map in
request-only details. Registration requires an exact assigned-Role set and matching generations,
refreshes managed entities under locks, and strips all details before context persistence.
Authorities still come only from enabled Roles. Registration first is seen by mutation expiry;
mutation first rejects stale registration. Role mutations never write affected Account generations.

Role/generation and exactly one privacy-minimized audit event flush before expiry. Role membership
audit uses the Role UUID as target and the complete canonical key only in its bounded context.
One SessionRegistry scan expires all affected UUIDs and leaves unrelated sessions untouched.
Expiry failure rolls back Role/generation/audit. A late commit failure may leave sessions expired
with the preceding database state retained; fresh authentication reads that state. This intentional
safe non-atomic asymmetry does not require distributed transactions.

## Recent credential re-authentication

[TASK-0029](../tasks/TASK-0029.md) adds `CredentialReauthenticationService.reauthenticate(char[])`.
The command accepts only an owned mutable password buffer; `CurrentActor.requireUserId()`
provides the Account identity. The buffer clears on every exit. An explicit attempt discards
previous session proof before validation; failure cannot refresh it. No login name or target
UUID is accepted. Authentication and proof failures carry fixed outcomes without internal causes.

The existing adaptive encoder verifies an enabled-account scalar snapshot without a caller
transaction or Account row lock. [TASK-0032](../tasks/TASK-0032.md) adds the shared Account
lockout policy: an observed active lock rejects proof before adaptive comparison. A 15-second
REQUIRES_NEW READ COMMITTED transaction then locks and refreshes the Account and rechecks
enabled state, the exact compared encoding and current lock state using the injected Clock.
An unchanged wrong credential records one failed attempt and the configured threshold creates
the same temporary lock used by ordinary login. The transaction commits before credential
rejection; commit failure exposes only a bounded persistence failure. Concurrent attempts in
distinct sessions serialize on the Account row. Active locks reject correct credentials too,
without extending the lock or incrementing failures. After expiry, common authentication
bookkeeping clears expired state; successful proof resets failures. Only failure count, lock
expiry, security timestamp and normal row version may change. Credential, credential timestamp,
enabled state, generations, identity, memberships and business audit data remain unchanged.
Ordinary-login locks block proof in existing sessions, and proof-created locks block ordinary
login. Lockout alone does not revoke sessions.
The session adapter publishes proof only after successful commit. Concurrent replacement
before the locked recheck rejects stale proof; replacement after it expires registered sessions.
The adapter checks registry expiry before publication and each read, closing the commit-to-publication gap.

`RecentAuthenticationGuard` derives CurrentActor before checking proof identity and age.
All Account creation/lifecycle/membership and Role/Permission administration commands require
`identity:admin` first, then recent proof before validation or mutation. Credential proof grants
no authority. Bootstrap and self-service password change keep their existing credential boundaries.
Administrative audit events and transaction/revocation behavior remain unchanged; proof alone
adds no business audit event.

The Identity-owned `RecentAuthenticationSession` port is composed with a servlet-memory
adapter in hris-app. Its only marker fields are Account public UUID and proof timestamp.
It never creates a session, persists state, or copies credential/login/authority/request/session
identifier data. Attempts in one session are serialized; concurrent sessions remain independent.
New authentication explicitly clears migrated proof during standard final registration.
Logout, invalidation, idle expiry and restart discard local sessions; targeted expiry makes proof
unusable even before the next request invalidates the servlet session.

`hris.security.reauthentication-window` / `HRIS_REAUTHENTICATION_WINDOW` defaults to `PT5M`.
Startup rejects non-positive durations and durations above `PT30M`. Exact expiry and a future
proof timestamp fail closed. Ordinary session idle timeout remains independent. No migration,
new dependency, production HTTP endpoint or administration view is introduced.

## Authenticated administrative credential reset

[TASK-0034](../tasks/TASK-0034.md) adds `PasswordResetService.reset(UUID, char[], char[])`.
The standalone command requires `identity:admin`, recent credential proof, then the
administrator UUID before validating the target. Self reset is rejected; self changes
continue through `PasswordChangeService`. Both owned buffers clear on every exit, with
the existing 12-128 Unicode-code-point policy and exact confirmation semantics.

An ambient caller transaction is refused before adaptive encoding. A non-secret scalar
target generation snapshot includes disabled Accounts. Encoding runs outside database
transactions and target locks. The 15-second REQUIRES_NEW READ COMMITTED mutation locks
and refreshes the target Account, rejects a changed generation without retry, and uses
`changePassword` to advance generation, update credential/security timestamps and clear
failed attempts/temporary lock. Disabled state, UUID, login and assignments are preserved.
Concurrent resets or self changes cannot overwrite a credential using stale preparation.

Exactly one `IDENTITY_PASSWORD_RESET` event commits with administrator actor UUID,
target Account UUID, target type `IDENTITY_ACCOUNT`, null reason and fixed context
`administrative-password-reset`. No credential, encoding, login, session identifier or
authority dump is recorded. Credential and audit flush precede target-session expiry;
expiry failure rolls back database writes. A later commit failure may leave sessions
expired while the preceding database credential remains, as with self password change.
The existing Account lock/generation final-registration check closes in-flight-login
races in both orderings. Reset adds no schema, dependency or production transport.
Anonymous recovery, total administrator access loss, offline TOTP and supported-hardware
password-cost qualification remain deferred.

## Privileged offline TOTP and recovery

[TASK-0035](../tasks/TASK-0035.md) adds optional MFA enrollment for identity:admin
Accounts through MfaAdministrationService, with recent proof and possession confirmation.
V9 stores only Account-bound AES-256-GCM encrypted seeds, pending expiry, enabled state,
last consumed TOTP step and one-use SHA-256 recovery digests. MFA authentication and recent
proof consume factors under the refreshed Account lock with existing bounded failure policy.
Password-only overloads deny enrolled Accounts. No partially authenticated token/session
is issued. The form submits a browser-only factor; owned request buffers clear and authenticated
token details retain only existing request-only generation before registration strips them.

Confirmation, self disable, recovery replacement and authenticated other-Account admin reset
advance Account generation, flush non-secret UUID/fixed-context audit and expire sessions.
Password reset preserves MFA, and MFA reset preserves password, lifecycle and assignments.
Existing Account/Role registration locking closes mutation-to-registration races. Pending
confirmation failures commit common lockout bookkeeping; audit/expiry failure rolls back
activation, while late commit failure retains the established safe expiry asymmetry.
The [operator runbook](../deployment/offline-mfa.md) specifies key permissions/backup,
clock handling, one-time material disposal, recovery and safe deployment/rollback boundaries.
Broad administrative adapters, total-access-loss recovery and key rotation tooling remain deferred.
