package io.github.vncntz.hris.clientmanagement;

/** Fixed application outcome without infrastructure causes or request values. */
public final class ClientManagementException extends RuntimeException {
    public enum Reason { INVALID_TARGET, INVALID_NAME, NOT_FOUND, NO_CHANGE, INACTIVE_COMPANY,
                         STALE_VERSION, CONFLICT, PERSISTENCE_FAILED }
    private final Reason reason;
    public ClientManagementException(Reason reason) {
        super("Client management failed: " + reason, null, false, false);
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
