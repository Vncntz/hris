package io.github.vncntz.hris.identityaccess;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        AuthenticatedAccount account;
        MfaInput factor = authentication.getDetails() instanceof MfaInput input ? input : null;
        try {
            account = (factor == null ? service.authenticate(login, password)
                    : service.authenticate(login, password, factor.value()))
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        } catch (RuntimeException failure) {
            throw new BadCredentialsException("Invalid credentials");
        } finally {
            if (factor != null) { factor.close(); }
            if (authentication instanceof org.springframework.security.authentication.AbstractAuthenticationToken input) {
                input.setDetails(null);
            }
        }
        var token = UsernamePasswordAuthenticationToken.authenticated(account.principal(), null,
                account.authorityKeys().stream().map(SimpleGrantedAuthority::new).toList());
        token.setDetails(new AuthenticationGeneration(account.authenticationGeneration(), account.roleGenerations()));
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
