package io.github.vncntz.hris.app;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import io.github.vncntz.hris.app.ui.LoginView;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountAuthenticationProvider provider)
            throws Exception {
        http.authenticationProvider(provider);
        http.with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.class, "/login"));
        return http.build();
    }
}
