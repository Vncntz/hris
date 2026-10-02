package io.github.vncntz.hris.identityaccess;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Immutable authority definitions, internal to Identity. */
interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByAuthorityKey(String authorityKey);
}
