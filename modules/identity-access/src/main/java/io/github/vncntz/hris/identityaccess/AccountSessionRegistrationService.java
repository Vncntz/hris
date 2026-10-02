package io.github.vncntz.hris.identityaccess;

import jakarta.persistence.EntityManager;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Serializes the final servlet registration decision with security-state mutation. */
@Service
public class AccountSessionRegistrationService {
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final EntityManager entities;
    private final TransactionTemplate registration;

    AccountSessionRegistrationService(AccountRepository accounts, RoleRepository roles, EntityManager entities,
            PlatformTransactionManager transactions) {
        this.accounts = accounts;
        this.roles = roles;
        this.entities = entities;
        registration = new TransactionTemplate(transactions);
        registration.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        registration.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        registration.setTimeout(15);
    }

    /** Callback must perform the one standard registration synchronously under the lock. */
    public void register(Authentication authentication, Runnable registerSession) {
        try {
            if (!authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof AccountPrincipal principal)
                    || !(authentication.getDetails() instanceof AuthenticationGeneration generation)
                    || !(authentication instanceof AbstractAuthenticationToken)
                    || TransactionSynchronizationManager.isActualTransactionActive()) {
                throw rejected();
            }
            registration.executeWithoutResult(status -> {
                AccountEntity account = accounts.findByPublicId(principal.publicId()).orElseThrow(
                        AccountSessionRegistrationService::rejected);
                // A servlet-bound persistence context may still contain the authentication snapshot.
                entities.refresh(account);
                if (!account.enabled() || generation.value() != account.authenticationGeneration()) {
                    throw rejected();
                }
                var assignedIds = account.assignedRoles().stream().map(RoleEntity::publicId)
                        .collect(java.util.stream.Collectors.toSet());
                if (!assignedIds.equals(generation.roles().values().keySet())) {
                    throw rejected();
                }
                // One Account first, then every assigned Role in UUID order, including disabled Roles.
                for (var roleId : assignedIds.stream().sorted().toList()) {
                    RoleEntity role = roles.findByPublicId(roleId).orElseThrow(
                            AccountSessionRegistrationService::rejected);
                    entities.refresh(role);
                    if (generation.roles().values().get(roleId) != role.authorizationGeneration()) {
                        throw rejected();
                    }
                }
                registerSession.run();
            });
        } catch (RuntimeException failure) {
            // Includes database/commit/callback failure; retain no infrastructure cause or data.
            throw rejected();
        } finally {
            if (authentication instanceof AbstractAuthenticationToken token) {
                token.setDetails(null);
            }
        }
    }

    private static SessionAuthenticationException rejected() {
        return new SessionAuthenticationException("Authentication could not establish a session");
    }
}
