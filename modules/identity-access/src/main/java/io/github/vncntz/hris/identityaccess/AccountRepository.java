package io.github.vncntz.hris.identityaccess;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountEntity> findByCanonicalLogin(String canonicalLogin);

    @Query("select distinct role from AccountEntity account join account.roles role "
            + "left join fetch role.permissions where account.id = :accountId")
    List<RoleEntity> findAssignedRoles(@Param("accountId") Long accountId);
}
