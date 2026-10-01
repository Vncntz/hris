package io.github.vncntz.hris.identityaccess;

import java.util.List;

/** Authentication result containing only session-safe identity and authorities. */
public record AuthenticatedAccount(AccountPrincipal principal, List<String> authorityKeys) {
    public AuthenticatedAccount {
        java.util.Objects.requireNonNull(principal);
        authorityKeys = List.copyOf(authorityKeys);
    }
}
