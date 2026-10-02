package io.github.vncntz.hris.identityaccess;

/** Non-secret failure shared by Identity commands using local session revocation. */
public final class SessionRevocationException extends RuntimeException {
    SessionRevocationException() {
        super("Authenticated session revocation failed");
    }
}
