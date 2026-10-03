# Privileged offline MFA

[TASK-0035](../tasks/TASK-0035.md) implements optional Identity-owned TOTP for privileged
Accounts. It needs no internet service. The algorithm is RFC 6238 HMAC-SHA1, six digits,
30-second steps, accepting the adjacent steps by default for clock skew. Keep the installation clock
and authenticator clock synchronized. A consumed step cannot be reused, including for
recent proof; wait for a fresh step. Clock rollback fails closed until a newer step is available.

Operational policy is bounded and configurable under D-151. `HRIS_MFA_SKEW_STEPS`
(`hris.security.mfa-skew-steps`) accepts 0 or 1, default 1. `HRIS_MFA_ENROLLMENT_WINDOW`
(`hris.security.mfa-enrollment-window`) is positive and at most PT10M, default PT10M.
`HRIS_MFA_RECOVERY_COUNT` (`hris.security.mfa-recovery-count`) accepts 2-10, default 10.
Invalid settings fail startup. Test any tighter policy against operator recovery/usability
needs; recovery using separate codes for login and recent proof needs at least two codes.
The common configurable authentication lockout and recent-proof policies remain in force.

Before enrollment, generate a cryptographically random **32-byte binary** key outside the
repository and application artifact. Set `HRIS_MFA_KEY_FILE` (or `hris.security.mfa-key-file`)
to its absolute path. No default key exists. Restrict the file and its parent directory to the
service identity and authorized administrators/root: restrictive Windows ACLs or Ubuntu
owner-only permissions. Ordinary installation users must not be able to read or replace it.
The installer/ACL automation remains separately owned work; operators must verify effective
permissions before using MFA. Do not supply key bytes through arguments or ordinary configuration.

Keep a separately protected, authenticated-encrypted recovery copy of this key with the
installation backup procedure under D-152. A database backup alone cannot recover factors.
Loss or replacement of the key blocks enrolled authentication; restore the matching protected
key. An empty key setting allows unenrolled authentication but refuses enrollment and enrolled
authentication. A configured unreadable, relative or non-32-byte file fails startup with a
bounded diagnostic. Key rotation/re-encryption tooling and total-administrator-loss recovery
are deferred; do not replace a live key as a rotation shortcut.

The Identity `MfaAdministrationService` provides the following authenticated commands.
The existing administrative application-service model is retained; a broad administration
screen/transport is separate work. Trusted composition must use CurrentActor and the normal
session adapter; callers cannot choose an actor UUID or manufacture recent proof.

1. With `identity:admin` and recent password proof, call `beginEnrollment()`. Show the
   returned Base32 seed once in a private local context, enter it manually into the offline
   authenticator with the algorithm above, then close the owned `MfaMaterial`. Do not send it
   to an external QR generator, log it or retain it in session state.
2. Within the configured enrollment window (ten minutes by default), call `confirmEnrollment(char[])` with the current authenticator code.
   Confirmation clears the supplied buffer. A wrong code uses the common lockout policy.
   Active enrollment cannot be overwritten. Successful confirmation returns the configured count (ten by default) of independent
   128-bit recovery codes once and expires existing sessions. Save these privately, separately
   from the authenticator and password, then close the material.
3. Log in with password and a fresh authenticator code or unused recovery code in the login
   form. Recovery codes are exact lowercase 32-character hexadecimal strings. Each works once
   with the password; it leaves MFA enabled. Do not reuse the enrollment code for login.
4. For privileged changes, `CredentialReauthenticationService.reauthenticate(password, factor)`
   requires both factors when enrolled. Failed proof clears prior proof. The password-only
   overload fails for enrolled Accounts. Full-factor proof grants no additional authority.
5. With recent full-factor proof, `replaceRecoveryCodes()` returns new codes once, invalidates
   all old codes and expires sessions. `disable()` removes factor material and expires sessions.
   On authenticator loss, use one recovery code with the password for login and a second unused
   code for recent proof, then disable/re-enroll or replace codes as appropriate.
6. An authenticated `identity:admin` with recent proof can `reset(otherAccountUuid)` after
   independently establishing the requester's identity through the Agency's local procedure.
   It clears MFA, records administrator/target UUID audit evidence and expires target sessions.
   It cannot reset itself, enable an Account, replace its password or change assignments.
   Administrative password reset alone preserves MFA. No anonymous reset endpoint exists.

Audit stores only stable UUIDs, fixed action names and a fixed non-sensitive context.
Encrypted seeds use AES-256-GCM with fresh nonces and Account UUID binding; recovery storage
contains only SHA-256 digests of high-entropy codes. Factor consumption and lockout state are
durable and serialized by the Account row lock. Activation/removal/recovery replacement
advance authentication generation; final registration rejects authentication prepared before
the change. Audit/flush/expiry failures roll back database writes; a late commit failure can
safely leave sessions expired while preceding database state remains. Reauthenticate using
the committed credential/factor state in that case.

V9 is additive; V1-V8 remain unchanged. Back up database and protected key together before
upgrade. After any Account is enrolled, rolling back to pre-MFA application code would remove
factor enforcement and is unsafe. Use forward correction, or deliberately remove all enrollment
through authenticated controls before considering that rollback. Do not drop the V9 columns,
rewrite Flyway history or manually clear production MFA as a recovery shortcut.

Offline TOTP does not make HTTP transport secure. Use the installation's protected transport
and workstation practices; HTTPS provisioning remains outside this TASK.
