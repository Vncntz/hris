package io.github.vncntz.hris.identityaccess;

/** Bounded failure without secret-bearing infrastructure causes. */
public final class MfaException extends RuntimeException {
    public MfaException() { super("MFA operation could not be completed"); }
}
