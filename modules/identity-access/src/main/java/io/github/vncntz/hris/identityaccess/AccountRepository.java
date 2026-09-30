package io.github.vncntz.hris.identityaccess;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountEntity> findByCanonicalLogin(String canonicalLogin);
}
