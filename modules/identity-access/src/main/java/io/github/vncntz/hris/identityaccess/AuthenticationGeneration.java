package io.github.vncntz.hris.identityaccess;

/** Non-secret, request-only proof of the security state used by authentication. */
record AuthenticationGeneration(long value, RoleGenerations roles) {
    AuthenticationGeneration(long value) {
        this(value, new RoleGenerations(java.util.Map.of()));
    }
}
