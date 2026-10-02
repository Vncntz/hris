package io.github.vncntz.hris.identityaccess;

/** Bounded command failure; never retains infrastructure diagnostics. */
public final class AccountRoleAssignmentException extends RuntimeException {
    public enum Reason {
        INVALID_TARGET, ACCOUNT_UNAVAILABLE, ROLE_UNAVAILABLE, ROLE_DISABLED,
        ALREADY_ASSIGNED, NOT_ASSIGNED, SELF_ADMIN_REMOVAL_REJECTED,
        PERSISTENCE_FAILED, SESSION_REVOCATION_FAILED
    }

    private final Reason reason;

    AccountRoleAssignmentException(Reason reason) {
        super("Account role assignment failed: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
