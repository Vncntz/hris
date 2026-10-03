package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static io.github.vncntz.hris.identityaccess.RecentAuthenticationException.Reason.*;

/** CurrentActor-only proof of the current credential; no Account or audit mutation. */
@Service
public class CredentialReauthenticationService {
    private final CurrentActor actor;
    private final RecentAuthenticationSession session;
    private final AccountRepository accounts;
    private final EntityManager entities;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final TransactionTemplate verification;

    CredentialReauthenticationService(CurrentActor actor, RecentAuthenticationSession session,
            AccountRepository accounts, EntityManager entities, PasswordEncoder encoder,
            Clock clock, PlatformTransactionManager transactions) {
        this.actor = actor;
        this.session = session;
        this.accounts = accounts;
        this.entities = entities;
        this.encoder = encoder;
        this.clock = clock;
        verification = new TransactionTemplate(transactions);
        verification.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verification.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        verification.setTimeout(15);
    }

    /** Takes ownership, clearing the supplied buffer even on session/authentication denial. */
    public void reauthenticate(char[] password) {
        try {
            // The session boundary clears old proof before any validation and serializes attempts.
            session.attempt(() -> verify(password));
        } catch (RecentAuthenticationException failure) {
            throw failure;
        } catch (org.springframework.security.core.AuthenticationException failure) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Authentication required");
        } catch (RuntimeException failure) {
            throw new RecentAuthenticationException(PERSISTENCE_FAILED);
        } finally {
            InitialCredentials.clear(password);
        }
    }

    private RecentAuthenticationSession.Proof verify(char[] password) {
        UUID id = actor.requireUserId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RecentAuthenticationException(PERSISTENCE_FAILED);
        }
        if (password == null) { throw new RecentAuthenticationException(CREDENTIAL_REJECTED); }
        String verified = accounts.findEnabledCredential(id).orElseThrow(() ->
                new RecentAuthenticationException(ACCOUNT_UNAVAILABLE));
        if (!encoder.matches(CharBuffer.wrap(password), verified)) {
            throw new RecentAuthenticationException(CREDENTIAL_REJECTED);
        }
        // Execute returns only after commit; a commit failure cannot publish proof.
        return verification.execute(status -> {
            AccountEntity account = accounts.findByPublicId(id).orElseThrow(() ->
                    new RecentAuthenticationException(ACCOUNT_UNAVAILABLE));
            entities.refresh(account);
            if (!account.enabled()) { throw new RecentAuthenticationException(ACCOUNT_UNAVAILABLE); }
            if (!verified.equals(account.passwordHash())) {
                throw new RecentAuthenticationException(STALE_CREDENTIAL);
            }
            return new RecentAuthenticationSession.Proof(id, clock.instant());
        });
    }
}
