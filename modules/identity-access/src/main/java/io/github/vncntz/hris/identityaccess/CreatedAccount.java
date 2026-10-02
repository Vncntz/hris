package io.github.vncntz.hris.identityaccess;

import java.util.Objects;
import java.util.UUID;

/** Stable, non-secret acknowledgement of ordinary account creation. */
public record CreatedAccount(UUID publicId) {
    public CreatedAccount { Objects.requireNonNull(publicId); }
}
