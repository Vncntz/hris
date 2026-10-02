package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Registered only by explicit local composition, never by normal component scanning. */
public final class FirstAdministratorProvisioner {
    public static final String ROLE = "administrator";
    public static final String AUTHORITY = "identity:admin";
    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private final EntityManager entities;
    private final PasswordEncoder encoder;
    private final AuditRecorder audit;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public FirstAdministratorProvisioner(EntityManager entities, PasswordEncoder encoder,
            AuditRecorder audit, Clock clock, PlatformTransactionManager transactions) {
        this.entities = entities;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
        transaction = new TransactionTemplate(transactions);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(15);
    }

    /** Takes ownership of both mutable secret arrays and clears them even on validation failure. */
    public UUID provision(String login, char[] password, char[] confirmation) {
        try {
            String canonical = LoginNames.canonicalize(login);
            validatePasswords(password, confirmation);
            // CharBuffer avoids an application-created immutable plaintext String. The encoder's
            // unavoidable internal copies remain short-lived. Hashing does not hold a database lock.
            String hash = encoder.encode(CharBuffer.wrap(password));
            return transaction.execute(status -> persist(canonical, hash));
        } finally {
            clear(password);
            clear(confirmation);
        }
    }

    public static void validatePasswords(char[] password, char[] confirmation) {
        if (password == null || confirmation == null) {
            throw new IllegalArgumentException("Provisioning cancelled");
        }
        int length = Character.codePointCount(password, 0, password.length);
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain 12 to 128 Unicode characters");
        }
        for (int i = 0; i < password.length; i++) {
            if (Character.isHighSurrogate(password[i])) {
                if (++i >= password.length || !Character.isLowSurrogate(password[i])) {
                    throw new IllegalArgumentException("Password contains invalid Unicode");
                }
            } else if (Character.isLowSurrogate(password[i])) {
                throw new IllegalArgumentException("Password contains invalid Unicode");
            }
        }
        if (!Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
    }

    private UUID persist(String login, String hash) {
        BootstrapStateEntity state = entities.find(BootstrapStateEntity.class, 1,
                LockModeType.PESSIMISTIC_WRITE);
        if (state == null || state.completed()) {
            throw new IllegalStateException("First-administrator provisioning is closed");
        }
        for (String entity : new String[]{"AccountEntity", "RoleEntity", "PermissionEntity"}) {
            if (entities.createQuery("select count(e) from " + entity + " e", Long.class)
                    .getSingleResult() != 0) {
                throw new IllegalStateException("Existing identity state requires manual reconciliation");
            }
        }
        UUID accountId = UUID.randomUUID();
        PermissionEntity permission = new PermissionEntity(AUTHORITY);
        RoleEntity role = new RoleEntity(UUID.randomUUID(), ROLE, true);
        AccountEntity account = new AccountEntity(accountId, login, hash,
                clock.instant().truncatedTo(ChronoUnit.MICROS));
        role.assignBootstrapPermission(permission);
        account.assignBootstrapRole(role);
        entities.persist(permission);
        entities.persist(role);
        entities.persist(account);
        state.complete();
        audit.record(new AuditRequest(accountId.toString(), "FIRST_ADMINISTRATOR_PROVISIONED",
                "IDENTITY_ACCOUNT", accountId.toString(), null, "local-operator-bootstrap"));
        entities.flush();
        return accountId;
    }

    private static void clear(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }
}
