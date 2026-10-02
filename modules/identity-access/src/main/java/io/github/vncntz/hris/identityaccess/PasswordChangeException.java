package io.github.vncntz.hris.identityaccess;

/** Bounded application failure; never retains credential or infrastructure causes. */
public final class PasswordChangeException extends RuntimeException {
    public enum Reason {
        CURRENT_CREDENTIAL_REJECTED, STALE_CREDENTIAL, ACCOUNT_UNAVAILABLE,
        ENCODING_FAILED, PERSISTENCE_FAILED, SESSION_REVOCATION_FAILED
    }

    private final Reason reason;

    PasswordChangeException(Reason reason) {
        super("Password change failed: " + reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
