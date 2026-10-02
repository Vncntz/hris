package io.github.vncntz.hris.identityaccess;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Internal public-ID lookup; no administrative Role mutation contract. */
interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByPublicId(UUID publicId);
}
