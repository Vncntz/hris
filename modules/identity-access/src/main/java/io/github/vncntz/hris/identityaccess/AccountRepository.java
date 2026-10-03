package io.github.vncntz.hris.identityaccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    interface AuthenticationState {
        String getPasswordHash();
        java.time.LocalDateTime getLockedUntilUtc();
    }

    // Both scalars come from one statement; no managed Account or row lock during hashing.
    @Query("select account.passwordHash as passwordHash, account.lockedUntilUtc as lockedUntilUtc "
            + "from AccountEntity account where account.publicId = :publicId and account.enabled = true")
    Optional<AuthenticationState> findEnabledAuthenticationState(@Param("publicId") UUID publicId);

    // Scalar snapshot avoids retaining a stale managed entity before the mutation lock.
    @Query("select account.passwordHash from AccountEntity account "
            + "where account.publicId = :publicId and account.enabled = true")
    Optional<String> findEnabledCredential(@Param("publicId") UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountEntity> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountEntity> findByCanonicalLogin(String canonicalLogin);

    @Query("select distinct role from AccountEntity account join account.roles role "
            + "left join fetch role.permissions where account.id = :accountId")
    List<RoleEntity> findAssignedRoles(@Param("accountId") Long accountId);

    // Read-only discovery while holding the Role lock; never acquire affected Account locks here.
    @Query("select account.publicId from AccountEntity account join account.roles role "
            + "where role.publicId = :roleId")
    java.util.Set<UUID> findPublicIdsAssignedToRole(@Param("roleId") UUID roleId);
}
