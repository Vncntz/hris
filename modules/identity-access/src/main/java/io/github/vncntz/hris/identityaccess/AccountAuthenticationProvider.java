package io.github.vncntz.hris.identityaccess;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Deliberately gives unknown, disabled, locked, and wrong-password accounts one response. */
@Component
public class AccountAuthenticationProvider implements AuthenticationProvider {
    private final AccountAuthenticationService service;

    AccountAuthenticationProvider(AccountAuthenticationService service) {
        this.service = service;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String login = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());
        AccountPrincipal principal = service.authenticate(login, password)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
