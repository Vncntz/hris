package io.github.vncntz.hris.identityaccess;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Internal credential state. Do not expose this entity through application contracts. */
@Entity
@Table(name = "identity_account")
class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @Column(name = "canonical_login", nullable = false, unique = true, length = 128)
    private String canonicalLogin;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until_utc", columnDefinition = "DATETIME(6)")
    private LocalDateTime lockedUntilUtc;

    @Column(name = "credential_updated_at_utc", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime credentialUpdatedAtUtc;

    @Column(name = "security_updated_at_utc", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime securityUpdatedAtUtc;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected AccountEntity() {
    }

    AccountEntity(UUID publicId, String canonicalLogin, String passwordHash, Instant createdAt) {
        this.publicId = publicId;
        this.canonicalLogin = canonicalLogin;
        this.passwordHash = passwordHash;
        this.enabled = true;
        this.credentialUpdatedAtUtc = utc(createdAt);
        this.securityUpdatedAtUtc = utc(createdAt);
    }

    UUID publicId() {
        return publicId;
    }

    String canonicalLogin() {
        return canonicalLogin;
    }

    String passwordHash() {
        return passwordHash;
    }

    boolean enabled() {
        return enabled;
    }

    boolean isLockedAt(Instant now) {
        return lockedUntilUtc != null && now.isBefore(lockedUntilUtc.toInstant(ZoneOffset.UTC));
    }

    void clearExpiredLock(Instant now) {
        if (lockedUntilUtc != null && !isLockedAt(now)) {
            lockedUntilUtc = null;
            failedAttempts = 0;
            securityUpdatedAtUtc = utc(now);
        }
    }

    void recordFailure(Instant now, int limit, java.time.Duration duration) {
        failedAttempts = Math.min(limit, failedAttempts + 1);
        if (failedAttempts >= limit) {
            lockedUntilUtc = utc(now.plus(duration));
        }
        securityUpdatedAtUtc = utc(now);
    }

    void recordSuccess(Instant now) {
        failedAttempts = 0;
        lockedUntilUtc = null;
        securityUpdatedAtUtc = utc(now);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
