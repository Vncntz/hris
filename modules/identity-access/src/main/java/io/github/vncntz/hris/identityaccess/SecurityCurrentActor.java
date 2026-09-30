package io.github.vncntz.hris.identityaccess;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class SecurityCurrentActor implements CurrentActor {
    @Override
    public UUID requireUserId() {
        return authenticated().publicId();
    }

    @Override
    public boolean hasAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            throw new IllegalArgumentException("Authority is required");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AccountPrincipal)) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    @Override
    public void requireAuthority(String authority) {
        authenticated();
        if (!hasAuthority(authority)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private AccountPrincipal authenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal;
    }
}
