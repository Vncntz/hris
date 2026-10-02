package io.github.vncntz.hris.identityaccess;

/** Bounded command outcome with no retained infrastructure cause. */
public final class RoleAdministrationException extends RuntimeException {
    public enum Reason {
        INVALID_INPUT, DUPLICATE_ROLE, DUPLICATE_PERMISSION, ROLE_UNAVAILABLE,
        PERMISSION_UNAVAILABLE, ALREADY_ENABLED, ALREADY_DISABLED, ALREADY_ASSIGNED,
        NOT_ASSIGNED, SELF_ADMIN_REMOVAL_REJECTED, PERSISTENCE_FAILED, SESSION_REVOCATION_FAILED
    }

    private final Reason reason;

    RoleAdministrationException(Reason reason) {
        super("Role administration failed: " + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
