package io.github.vncntz.hris.identityaccess;

/** Bounded command failure; never retains infrastructure diagnostics. */
public final class AccountLifecycleException extends RuntimeException {
    public enum Reason {
        INVALID_TARGET, ACCOUNT_UNAVAILABLE, ALREADY_ENABLED, ALREADY_DISABLED,
        SELF_DISABLE_REJECTED, PERSISTENCE_FAILED, SESSION_REVOCATION_FAILED
    }

    private final Reason reason;

    AccountLifecycleException(Reason reason) {
        super("Account lifecycle failed: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
