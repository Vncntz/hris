package io.github.vncntz.hris.identityaccess;

/** Bounded failure without request data or persistence/encoder exception causes. */
public final class AccountCreationException extends RuntimeException {
    public enum Reason { DUPLICATE_LOGIN, ENCODING_FAILED, PERSISTENCE_FAILED }

    private final Reason reason;

    AccountCreationException(Reason reason) {
        super("Account creation failed: " + reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
