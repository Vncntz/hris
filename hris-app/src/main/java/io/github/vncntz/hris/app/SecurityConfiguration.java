package io.github.vncntz.hris.app;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import io.github.vncntz.hris.app.ui.LoginView;
import io.github.vncntz.hris.identityaccess.AccountSessionRegistrationService;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            SessionRegistry sessions, AccountSessionRegistrationService registration,
            ServletRecentAuthenticationSession recent)
            throws Exception {
        // Spring discovers the sole provider bean in the global parent manager.
        // Registering it here too retries failed credentials through the same provider twice.
        http.with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class, "/login"));
        http.formLogin(form -> form.authenticationDetailsSource(request -> {
            String factor = request.getParameter("factor");
            // Bounded request-only secret; returned authenticated token replaces all details.
            return new io.github.vncntz.hris.identityaccess.MfaInput(
                    factor != null && factor.length() <= 32 ? factor.toCharArray() : new char[0]);
        }));
        // -1 is unlimited: installs standard registration/expiry support without a session cap.
        http.sessionManagement(session -> {
            session.maximumSessions(-1).sessionRegistry(sessions).expiredUrl("/login");
            // Replace only final registration; retain framework concurrency, fixation and CSRF strategies.
            session.addObjectPostProcessor(new ObjectPostProcessor<RegisterSessionAuthenticationStrategy>() {
                @Override
                @SuppressWarnings("unchecked")
                public <O extends RegisterSessionAuthenticationStrategy> O postProcess(O strategy) {
                    return (O) new RegisterSessionAuthenticationStrategy(sessions) {
                        @Override
                        public void onAuthentication(Authentication authentication, HttpServletRequest request,
                                HttpServletResponse response) {
                            try {
                                recent.clearOnAuthentication(request);
                                registration.register(authentication,
                                        () -> super.onAuthentication(authentication, request, response));
                            } catch (RuntimeException failure) {
                                // Registration can precede a failed database commit. Remove any partial entry.
                                var servletSession = request.getSession(false);
                                if (servletSession != null) {
                                    sessions.removeSessionInformation(servletSession.getId());
                                }
                                throw failure;
                            }
                        }
                    };
                }
            });
        });
        return http.build();
    }
}
