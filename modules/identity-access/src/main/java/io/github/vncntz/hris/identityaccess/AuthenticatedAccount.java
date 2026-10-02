package io.github.vncntz.hris.identityaccess;

import java.util.List;
import java.time.LocalDateTime;

/** Authentication result containing non-secret identity, authorities and transient credential generation. */
public record AuthenticatedAccount(AccountPrincipal principal, List<String> authorityKeys, LocalDateTime credentialGeneration) {
    public AuthenticatedAccount {
        java.util.Objects.requireNonNull(principal);
        java.util.Objects.requireNonNull(credentialGeneration);
        authorityKeys = List.copyOf(authorityKeys);
    }
}
