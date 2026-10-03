package io.github.vncntz.hris.identityaccess;

/** Bounded reset failure; never retains credential or infrastructure causes. */
public final class PasswordResetException extends RuntimeException {
    public enum Reason {
        INVALID_TARGET, SELF_RESET_REJECTED, INVALID_CREDENTIAL, ACCOUNT_UNAVAILABLE,
        STALE_ACCOUNT, ENCODING_FAILED, PERSISTENCE_FAILED, SESSION_REVOCATION_FAILED
    }

    private final Reason reason;

    PasswordResetException(Reason reason) {
        super("Password reset failed: " + reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
