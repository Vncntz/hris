package io.github.vncntz.hris.identityaccess;

import java.time.Clock;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

/** Lite configuration imported by the local command only; intentionally not a component. */
@Import(AuthenticationSecurityConfiguration.class)
public class LocalProvisioningIdentityConfiguration {
    @Bean
    FirstAdministratorProvisioner firstAdministratorProvisioner(EntityManager entities,
            PasswordEncoder encoder, AuditRecorder audit, Clock clock,
            PlatformTransactionManager transactions) {
        return new FirstAdministratorProvisioner(entities, encoder, audit, clock, transactions);
    }
}
