package io.github.vncntz.hris.identityaccess;

import java.time.LocalDateTime;

/** Non-secret, request-only proof of the credential state used by authentication. */
record CredentialGeneration(LocalDateTime updatedAt) {
}
