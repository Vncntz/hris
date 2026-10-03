package io.github.vncntz.hris.identityaccess;

/** Request-only owned buffer. Never attach this to an authenticated token or session. */
public final class MfaInput implements AutoCloseable {
    private final char[] value;
    public MfaInput(char[] value) { this.value = value; }
    char[] value() { return value; }
    @Override public void close() { InitialCredentials.clear(value); }
    @Override public String toString() { return "MfaInput[redacted]"; }
}
