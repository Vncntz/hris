# First-administrator provisioning

[TASK-0023](../tasks/TASK-0023.md) provides a once-only local command in the
existing application artifact. Possession of local OS/process access and the
installation's database access is privileged. Restrict both to authorized
installation operators. This is an installation bootstrap command, not an
account administration or recovery channel.

## Invocation

Use Java 25 and the protected external datasource configuration described in
[configuration](configuration.md). Stop the ordinary application while performing
first-use installation provisioning. From an interactive Windows PowerShell or
Ubuntu terminal, invoke the built application:

```text
java -jar hris-app/target/hris-app-0.1.0-SNAPSHOT.jar --provision-first-administrator
```

This is the only accepted command argument. External technical datasource settings
remain available through the existing configuration convention. The command reads
the chosen login and password from the terminal, and asks for the password again
with echo disabled. Never place the provisioning password in arguments, environment,
configuration, URLs, forms, redirected stdin, scripts, support tickets or diagnostics.
No example credential is supplied. IDE consoles, unattended services, redirected
input/output and some terminal wrappers cannot supply a secure console; refusal is
the intended behavior. Java's console must report an interactive terminal.

Login uses the normal canonical ASCII login rules. Passwords must contain 12–128
Unicode code points. Valid Unicode content, including whitespace and case, is
preserved; invalid Unicode and over-limit input are rejected without truncation.
Passwords and confirmation buffers are cleared on normal/exceptional exits and by
a shutdown hook where JVM shutdown permits. The encoder may make short-lived
internal copies; process termination cannot guarantee erasure of immutable copies
or operating-system memory. No plaintext or hash is retained in a session or result.

Provisioning opens a non-web application context and exits after the command.
It starts no HTTP listener and exposes no Vaadin setup route. Normal startup
does not register or invoke the provisioner and continues to require normal login.
A successful command does not authenticate the operator or create a session.
After success, start the ordinary application and use its existing login page.

## Once-only state and failure handling

V6 creates a technical singleton coordination row, with no identity or credential
seed. Provisioning hashes before entering a short READ COMMITTED transaction,
locks that row, and rejects completed or pre-existing identity/authorization state.
Exactly one enabled account, enabled `administrator` role, `identity:admin`
permission and the two memberships commit with completion state and
`FIRST_ADMINISTRATOR_PROVISIONED` audit evidence. The account public UUID is the
audit actor and target; context is the fixed `local-operator-bootstrap` value.
Login and credential material are omitted. Audit or persistence failure rolls back
the transaction. Concurrent commands serialize and only one can succeed.

Completion survives application restart, account disablement/deletion, role
disablement and membership removal. Database constraints/triggers reject ordinary
deletion or reopening of the coordination row. Privileged DDL, restore or database
replacement is outside those protections and remains a DBA-controlled operation.
Released V1–V5 are unchanged. V6 shares V2's trigger-creation privilege prerequisite
documented in the [migration record](../migrations/README.md).

Exit codes: `0` reports success; `1` reports a bounded generic refusal/failure;
`2` refuses missing secure terminal or invalid command arguments. Diagnostics never
include the supplied values or underlying exception. Cancellation, invalid input
and confirmation mismatch do not open the datasource. An uncertain process or
connection failure must be reconciled through authorized local inspection; do not
assume that an absent success message proves no transaction committed.

Existing populated identity/authorization tables without a completed marker are
refused for manual reconciliation. The command never adopts, elevates, overwrites
or resets an existing account. There is no reset switch, recovery endpoint or
supported marker deletion/reopening procedure. Reconciliation and normal account,
role and assignment administration require separately authorized later work.

Argon2id reuses the existing engineering work factor; production hardware cost
calibration, installer/service qualification, HTTPS, recovery, privileged MFA and
re-authentication remain later qualification work. This command does not supply
business-module permissions or a wildcard/superuser bypass.

## Verification record

Implementation verification is recorded in [TASK-0023](../tasks/TASK-0023.md).
Do not infer a platform or production qualification from an unexecuted check.
