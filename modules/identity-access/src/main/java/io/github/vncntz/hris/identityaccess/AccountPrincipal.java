package io.github.vncntz.hris.identityaccess;

import java.io.Serializable;
import java.util.UUID;

/** Only stable, non-secret identity data is retained in the HTTP session. */
public record AccountPrincipal(UUID publicId, String canonicalLogin) implements Serializable {
}
