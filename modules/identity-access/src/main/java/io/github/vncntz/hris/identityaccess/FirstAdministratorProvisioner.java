package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
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
    public static final int MIN_PASSWORD_LENGTH = InitialCredentials.MIN_LENGTH;
    public static final int MAX_PASSWORD_LENGTH = InitialCredentials.MAX_LENGTH;

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
            InitialCredentials.clear(password);
            InitialCredentials.clear(confirmation);
        }
    }

    public static void validatePasswords(char[] password, char[] confirmation) {
        InitialCredentials.validate(password, confirmation);
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
}
