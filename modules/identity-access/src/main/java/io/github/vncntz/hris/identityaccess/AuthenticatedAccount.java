package io.github.vncntz.hris.identityaccess;

import java.util.List;

/** Authentication result containing non-secret identity, authorities and transient authentication generation. */
public record AuthenticatedAccount(AccountPrincipal principal, List<String> authorityKeys, long authenticationGeneration) {
    public AuthenticatedAccount {
        java.util.Objects.requireNonNull(principal);
        authorityKeys = List.copyOf(authorityKeys);
    }
}
