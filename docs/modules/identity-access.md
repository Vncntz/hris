# Identity and Access

[IMP-009](../implementation/tasks/IMP-009.md) / [TASK-0012](../tasks/TASK-0012.md) established local account authentication, security-principal state, and the `CurrentActor` authorization boundary. [IMP-013](../implementation/tasks/IMP-013.md) / [TASK-0022](../tasks/TASK-0022.md) adds role/permission persistence and authenticated authorities. The public account UUID is independent of any Worker identity. A newly migrated database has no usable login until the explicitly invoked [first-administrator command](../deployment/first-administrator.md) succeeds.

`identity_account` is an internal JPA aggregate in the global V3 Flyway migration. Login names are stripped, folded with `Locale.ROOT`, and restricted to ASCII letters, digits, `.`, `_`, and `-` before storage; a database unique constraint enforces the canonical name. The table stores only a Spring Security delegating encoding with the `{argon2@SpringSecurity_v5_8}` identifier and an Argon2id payload. Raw credentials and password hashes must not enter logs, audit context, diagnostics, fixtures, or support reports. Password hashes are not session principal data. The initial Spring Security work factor is an engineering baseline and **has not been calibrated on supported production hardware**; production security qualification must measure and set an appropriate factor before deployment.

Authentication uses a short transaction and a pessimistic row lock per known account. Five consecutive failed password attempts lock the account for 15 minutes by default; the count and UTC lock instant survive process restart. Disabled, locked, unknown, and wrong-password accounts receive the same ordinary external failure. Unknown and unsupported login names trigger a dummy hash verification. Successful authentication after lock expiry clears failure state. `HRIS_MAX_FAILED_ATTEMPTS` and `HRIS_LOCK_DURATION` configure the bounded defaults. The injectable `Clock` controls security instants; `DATETIME(6)` columns hold UTC wall time by explicit conversion.

Vaadin's `VaadinSecurityConfigurer` handles the anonymous login view, form login, navigation access, CSRF exceptions required for Vaadin internals, and logout. The root view requires an authenticated principal; an unannotated view is denied by Vaadin navigation access control. Standard Spring Security session fixation, CSRF, and logout behavior remain enabled. Servlet session idle timeout defaults to 30 minutes through `server.servlet.session.timeout`; `HRIS_SESSION_IDLE_TIMEOUT` can override it. Sessions stay in the local servlet container. HTTPS provisioning and distributed sessions are outside this task.

V5 adds the internal JPA Role and Permission model and account-role/role-permission join tables. A role has a stable binary public UUID, unique canonical ASCII name, enabled flag, and optimistic row version. Role names are stripped and folded with `Locale.ROOT`; canonical names start with an ASCII letter or digit and contain only lowercase ASCII letters, digits, `.`, `_`, and `-`, up to 128 characters. Permission authority keys are stable identifiers, starting with a lowercase ASCII letter and otherwise allowing lowercase ASCII letters, digits, `.`, `_`, `:`, and `-`, up to 128 characters. Application constructors canonicalize these values and database CHECK/unique constraints enforce their stored forms. Neither a complete permission catalog nor any authorization seed is introduced.

Membership exists only while its join-table row exists. Composite primary keys reject duplicate assignments. Foreign keys use RESTRICT for both deletion and key updates; future administration must revoke memberships deliberately before deleting referenced records. Primary-key order supports the account-to-role-to-permission lookup, and reverse indexes support referenced-key checks. Internal entities, mappings, and repositories remain inside Identity & Access.

Successful password authentication fetches assigned roles and their permissions in one joined JPA query within the existing short account transaction. Only enabled roles contribute authorities; missing roles, roles without permissions, and disabled roles grant none. Authority keys are deduplicated and sorted, then copied into Spring Security's authenticated token. The principal retains only the stable public account ID and canonical login; token credentials are absent. A lookup failure cannot yield an authenticated token. An account without an assigned permission can authenticate with zero authorities.

Authorities are a login/session snapshot. Assignment changes and role disablement take effect on a new authentication; an existing authenticated token keeps its snapshot. Later administrative work must add explicit session revocation/re-authentication behavior where required, including account disablement and privileged operations. There is no authorization cache or distributed session invalidation in TASK-0022.

TASK-0025 supplies a reusable local revocation primitive for later administrative commands;
it does not implement assignment or lifecycle administration. Password change uses it now
to expire all tracked sessions belonging to the authenticated account.

Application services may depend on `CurrentActor` to require the authenticated public user ID and test or require a named authority. The normal authentication provider now supplies authorities from persisted assignments; synthetic tests prove both successful enforcement and denial. Role/permission administration, account lifecycle beyond creation, their append-only audit events, TOTP enrollment/challenge/recovery, privileged re-authentication, and business-module call sites remain later focused work.

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

No production UI/HTTP adapter, migration, dependency or lifecycle/assignment operation
is added. The HTTP integration probe is test-only and exercises the production filter
chain with a narrow authenticated test-route rule. Recovery/reset, privileged MFA and
re-authentication, administration and password-cost calibration remain deferred.

## Credential-generation check at final session registration

TASK-0025's original PR #44 left a gap between successful credential verification and
servlet session registration. A password-change registry scan could miss a login paused
in that gap. The corrective implementation carries the existing credential-updated
DATETIME(6) value as non-secret, request-only authentication details. Each credential
replacement advances that generation strictly, using Clock at microsecond precision
or the preceding value plus one microsecond when the clock is equal/backwards. Ordinary
login/security-state updates do not change it; concurrent logins remain unlimited.

`AccountSessionRegistrationService` opens a short REQUIRES_NEW READ COMMITTED transaction,
locks the same public-UUID account row as password change, explicitly refreshes it, and
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
principal, audit or log state. No migration or dependency change is needed. Deterministic
real HTTP/MySQL tests pause after verification and after registration, exercise both
orderings, preserve unrelated sessions and prove normal concurrent logins. A fixed-clock
repeated-change test prevents generation reuse; a real registration commit-failure test
checks registry/context cleanup. See the [TASK evidence](../tasks/TASK-0025.md).
