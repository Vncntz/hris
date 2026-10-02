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
        AuthenticatedAccount account = service.authenticate(login, password)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        var token = UsernamePasswordAuthenticationToken.authenticated(account.principal(), null,
                account.authorityKeys().stream().map(SimpleGrantedAuthority::new).toList());
        token.setDetails(new CredentialGeneration(account.credentialGeneration()));
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
