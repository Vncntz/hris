package io.github.vncntz.hris.identityaccess;

/** Fixed non-secret outcomes without credential-bearing infrastructure causes. */
public final class RecentAuthenticationException extends RuntimeException {
    public enum Reason { PROOF_REQUIRED, CREDENTIAL_REJECTED, ACCOUNT_UNAVAILABLE,
        STALE_CREDENTIAL, PERSISTENCE_FAILED, SESSION_UNAVAILABLE }
    private final Reason reason;

    public RecentAuthenticationException(Reason reason) {
        super("Recent authentication failed: " + reason.name());
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
