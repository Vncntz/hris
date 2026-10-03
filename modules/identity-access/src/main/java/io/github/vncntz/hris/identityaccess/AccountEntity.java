package io.github.vncntz.hris.identityaccess;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

    @Column(name = "authentication_generation", nullable = false)
    private long authenticationGeneration;

    @Column(name = "mfa_secret", columnDefinition = "VARBINARY(48)")
    private byte[] mfaSecret;
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;
    @Column(name = "mfa_pending_until_utc", columnDefinition = "DATETIME(6)")
    private LocalDateTime mfaPendingUntilUtc;
    @Column(name = "mfa_last_step", nullable = false)
    private long mfaLastStep = -1;
    @Column(name = "mfa_recovery_hashes", nullable = false, length = 650)
    private String mfaRecoveryHashes = "";

    @Column(name = "security_updated_at_utc", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime securityUpdatedAtUtc;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @ManyToMany
    @JoinTable(name = "identity_account_role",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

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

    Long id() {
        return id;
    }

    String canonicalLogin() {
        return canonicalLogin;
    }

    String passwordHash() {
        return passwordHash;
    }

    long authenticationGeneration() {
        return authenticationGeneration;
    }

    boolean enabled() {
        return enabled;
    }

    boolean mfaEnabled() { return mfaEnabled; }
    byte[] mfaSecret() { return mfaSecret; }
    long mfaLastStep() { return mfaLastStep; }
    boolean pendingMfaAt(Instant now) {
        return !mfaEnabled && mfaPendingUntilUtc != null
                && now.isBefore(mfaPendingUntilUtc.toInstant(ZoneOffset.UTC));
    }
    void beginMfa(byte[] encrypted, Instant until) {
        if (mfaEnabled) { throw new MfaException(); }
        mfaSecret = encrypted;
        mfaPendingUntilUtc = utc(until);
    }
    void activateMfa(long step, String recovery) {
        advanceAuthenticationGeneration();
        mfaEnabled = true;
        mfaPendingUntilUtc = null;
        mfaLastStep = step;
        mfaRecoveryHashes = recovery;
    }
    void consumeStep(long step) { mfaLastStep = step; }
    boolean consumeRecovery(String digest) {
        if (digest.isEmpty()) { return false; }
        var hashes = new java.util.ArrayList<>(java.util.List.of(mfaRecoveryHashes.split(",")));
        boolean found = false;
        for (var hash : hashes) {
            if (java.security.MessageDigest.isEqual(hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    digest.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) { found = true; }
        }
        if (found) { hashes.remove(digest); mfaRecoveryHashes = String.join(",", hashes); }
        return found;
    }
    void replaceRecovery(String hashes) {
        advanceAuthenticationGeneration();
        mfaRecoveryHashes = hashes;
    }
    void removeMfa() {
        advanceAuthenticationGeneration();
        mfaEnabled = false;
        mfaSecret = null;
        mfaPendingUntilUtc = null;
        mfaLastStep = -1;
        mfaRecoveryHashes = "";
    }

    void assignBootstrapRole(RoleEntity role) {
        roles.add(role);
    }

    Set<RoleEntity> assignedRoles() {
        return Set.copyOf(roles);
    }

    void changeRoleAssignment(RoleEntity role, boolean assigned) {
        // Validate before advancing; overflow must leave membership untouched.
        if (roles.contains(role) == assigned) {
            throw new IllegalStateException("Membership did not change");
        }
        advanceAuthenticationGeneration();
        if (assigned) {
            roles.add(role);
        } else {
            roles.remove(role);
        }
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

    void changePassword(String encoding, Instant now) {
        advanceAuthenticationGeneration();
        passwordHash = encoding;
        credentialUpdatedAtUtc = utc(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        recordSuccess(now);
    }

    void changeEnabled(boolean enabled, Instant now) {
        advanceAuthenticationGeneration();
        this.enabled = enabled;
        securityUpdatedAtUtc = utc(now);
        if (enabled) {
            recordSuccess(now);
        }
    }

    private void advanceAuthenticationGeneration() {
        // Never wrap and reuse an old authentication generation.
        authenticationGeneration = Math.incrementExact(authenticationGeneration);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
