package io.github.vncntz.hris.identityaccess;

/** One-time provisioning/recovery display. Caller owns and must close this result. */
public final class MfaMaterial implements AutoCloseable {
    private final char[][] values;
    MfaMaterial(char[][] values) { this.values = values; }
    public char[][] values() { return values; }
    @Override public void close() {
        for (char[] value : values) { InitialCredentials.clear(value); }
    }
    @Override public String toString() { return "MfaMaterial[redacted]"; }
}
