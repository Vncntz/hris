package io.github.vncntz.hris.clientmanagement;

import java.util.Optional;
import java.util.UUID;

/** Module-owned read contract. Consumers must re-resolve activity before new business references. */
public interface ClientReferences {
    Optional<ClientCompanyReference> company(UUID publicId);
    Optional<ClientSiteReference> site(UUID publicId);
}
