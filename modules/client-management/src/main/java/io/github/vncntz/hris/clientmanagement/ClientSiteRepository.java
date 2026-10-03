package io.github.vncntz.hris.clientmanagement;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface ClientSiteRepository extends JpaRepository<ClientSiteEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ClientSiteEntity s where s.publicId = :id")
    Optional<ClientSiteEntity> lock(@Param("id") UUID id);

    @Query("select new io.github.vncntz.hris.clientmanagement.ClientSiteReference(s.publicId, c.publicId, s.displayName, s.active, c.active, s.rowVersion) from ClientSiteEntity s join s.company c where s.publicId = :id")
    Optional<ClientSiteReference> reference(@Param("id") UUID id);
}
