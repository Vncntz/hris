package io.github.vncntz.hris.app;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import io.github.vncntz.hris.app.ui.LoginView;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountAuthenticationProvider provider,
            SessionRegistry sessions)
            throws Exception {
        http.authenticationProvider(provider);
        http.with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class, "/login"));
        // -1 is unlimited: installs standard registration/expiry support without a session cap.
        http.sessionManagement(session -> session.maximumSessions(-1)
                .sessionRegistry(sessions).expiredUrl("/login"));
        return http.build();
    }
}
