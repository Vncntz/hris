package io.github.vncntz.hris.clientmanagement;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface ClientCompanyRepository extends JpaRepository<ClientCompanyEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClientCompanyEntity c where c.publicId = :id")
    Optional<ClientCompanyEntity> lock(@Param("id") UUID id);

    @Query("select new io.github.vncntz.hris.clientmanagement.ClientCompanyReference(c.publicId, c.displayName, c.active, c.rowVersion) from ClientCompanyEntity c where c.publicId = :id")
    Optional<ClientCompanyReference> reference(@Param("id") UUID id);
}
