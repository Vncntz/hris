package io.github.vncntz.hris.identityaccess;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Optional privileged enrollment and authenticated factor recovery controls. */
@Service
public class MfaAdministrationService {
    private final CurrentActor actor;
    private final RecentAuthenticationGuard recent;
    private final AccountRepository accounts;
    private final EntityManager entities;
    private final MfaSecrets secrets;
    private final AuditRecorder audit;
    private final AuthenticatedSessionRevoker sessions;
    private final Clock clock;
    private final TransactionTemplate mutation;
    private final MfaPolicy mfaPolicy;
    private final SecureRandom random = new SecureRandom();
    private final SecurityPolicy policy;

    MfaAdministrationService(CurrentActor actor, RecentAuthenticationGuard recent, AccountRepository accounts,
            EntityManager entities, MfaSecrets secrets, AuditRecorder audit, AuthenticatedSessionRevoker sessions,
            Clock clock, PlatformTransactionManager transactions, MfaPolicy mfaPolicy, SecurityPolicy policy) {
        this.actor = actor; this.recent = recent; this.accounts = accounts; this.entities = entities;
        this.secrets = secrets; this.audit = audit; this.sessions = sessions; this.clock = clock;
        this.mfaPolicy = mfaPolicy;
        this.policy = policy;
        mutation = new TransactionTemplate(transactions);
        mutation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        mutation.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        mutation.setTimeout(15);
    }

    public MfaMaterial beginEnrollment() {
        actor.requireAuthority("identity:admin");
        UUID id = authorize();
        byte[] seed = secrets.generateSeed();
        MfaMaterial result = new MfaMaterial(new char[][] { Totp.base32(seed) });
        try {
            byte[] encrypted = secrets.encrypt(id, seed);
            mutation.executeWithoutResult(status -> {
                AccountEntity account = lock(id);
                if (!account.enabled() || account.mfaEnabled()) { throw new MfaException(); }
                account.beginMfa(encrypted, clock.instant().plus(mfaPolicy.enrollmentWindow()));
                record(id, id, "IDENTITY_MFA_ENROLLMENT_STARTED", false);
            });
            return result;
        } catch (RuntimeException failure) { result.close(); throw new MfaException(); }
        finally { Arrays.fill(seed, (byte) 0); }
    }

    public MfaMaterial confirmEnrollment(char[] code) {
        MfaMaterial recovery = null;
        try {
            actor.requireAuthority("identity:admin");
            UUID id = authorize();
            recovery = recovery();
            String hashes = hashes(recovery);
            Boolean confirmed = mutation.execute(status -> {
                AccountEntity account = lock(id);
                var now = clock.instant();
                if (!account.enabled() || !account.pendingMfaAt(now) || account.isLockedAt(now)) { throw new MfaException(); }
                account.clearExpiredLock(now);
                byte[] seed = secrets.decrypt(id, account.mfaSecret());
                try {
                    long step = Totp.match(seed, code, now, -1, mfaPolicy.skewSteps());
                    if (step < 0) {
                        account.recordFailure(now, policy.maxFailedAttempts(), policy.lockDuration());
                        return false;
                    }
                    account.activateMfa(step, hashes);
                    account.recordSuccess(now);
                } finally { Arrays.fill(seed, (byte) 0); }
                record(id, id, "IDENTITY_MFA_ENABLED", true);
                return true;
            });
            if (!Boolean.TRUE.equals(confirmed)) { throw new MfaException(); }
            return recovery;
        } catch (org.springframework.security.access.AccessDeniedException | org.springframework.security.core.AuthenticationException
                | RecentAuthenticationException failure) {
            if (recovery != null) { recovery.close(); } throw failure;
        } catch (RuntimeException failure) {
            if (recovery != null) { recovery.close(); } throw new MfaException();
        } finally { InitialCredentials.clear(code); }
    }

    public MfaMaterial replaceRecoveryCodes() {
        UUID id = authorize();
        MfaMaterial result = recovery();
        try {
            String hashes = hashes(result);
            mutation.executeWithoutResult(status -> {
                AccountEntity account = lock(id);
                if (!account.enabled() || !account.mfaEnabled()) { throw new MfaException(); }
                secrets.requireKey();
                account.replaceRecovery(hashes);
                record(id, id, "IDENTITY_MFA_RECOVERY_REPLACED", true);
            });
            return result;
        } catch (RuntimeException failure) { result.close(); throw new MfaException(); }
    }

    public void disable() {
        UUID id = authorize();
        remove(id, id, "IDENTITY_MFA_DISABLED");
    }

    public void reset(UUID target) {
        actor.requireAuthority("identity:admin");
        UUID id = authorize();
        if (target == null || target.equals(id)) { throw new MfaException(); }
        remove(id, target, "IDENTITY_MFA_RESET");
    }

    private void remove(UUID actorId, UUID target, String action) {
        try {
            mutation.executeWithoutResult(status -> {
                AccountEntity account = lock(target);
                if (account.mfaSecret() == null) { throw new MfaException(); }
                account.removeMfa();
                record(actorId, target, action, true);
            });
        } catch (RuntimeException failure) { throw new MfaException(); }
    }

    private UUID authorize() {
        recent.requireRecentAuthentication();
        UUID id = actor.requireUserId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) { throw new MfaException(); }
        return id;
    }
    private AccountEntity lock(UUID id) {
        AccountEntity account = accounts.findByPublicId(id).orElseThrow(MfaException::new);
        entities.refresh(account);
        return account;
    }
    private void record(UUID actorId, UUID target, String action, boolean revoke) {
        audit.record(new AuditRequest(actorId.toString(), action, "IDENTITY_ACCOUNT", target.toString(), null,
                "authenticated-mfa-control"));
        accounts.flush();
        if (revoke) { sessions.revoke(target); }
    }
    private MfaMaterial recovery() {
        char[][] values = new char[mfaPolicy.recoveryCount()][];
        byte[] bytes = new byte[16];
        try {
            for (int i = 0; i < values.length; i++) {
                random.nextBytes(bytes);
                values[i] = new char[32];
                char[] alphabet = "0123456789abcdef".toCharArray();
                for (int at = 0; at < bytes.length; at++) {
                    values[i][at * 2] = alphabet[(bytes[at] >>> 4) & 15];
                    values[i][at * 2 + 1] = alphabet[bytes[at] & 15];
                }
            }
            return new MfaMaterial(values);
        } finally { Arrays.fill(bytes, (byte) 0); }
    }
    private String hashes(MfaMaterial material) {
        return Arrays.stream(material.values()).map(Totp::recoveryDigest).collect(java.util.stream.Collectors.joining(","));
    }
}
