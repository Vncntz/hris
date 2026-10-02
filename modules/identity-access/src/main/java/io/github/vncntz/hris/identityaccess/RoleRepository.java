package io.github.vncntz.hris.identityaccess;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

/** Shared Role serialization boundary; callers acquire Account before ordered Roles. */
interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoleEntity> findByPublicId(UUID publicId);

    boolean existsByCanonicalName(String canonicalName);
}
