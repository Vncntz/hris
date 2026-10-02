package io.github.vncntz.hris.identityaccess;

import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/** Local-process expiry marking, reusable by later Identity administrative commands. */
@Service
public class AuthenticatedSessionRevoker {
    private final SessionRegistry sessions;

    public AuthenticatedSessionRevoker(SessionRegistry sessions) {
        this.sessions = sessions;
    }

    public void revoke(UUID publicId) {
        Objects.requireNonNull(publicId);
        try {
            for (Object principal : sessions.getAllPrincipals()) {
                if (principal instanceof AccountPrincipal account && publicId.equals(account.publicId())) {
                    for (var session : sessions.getAllSessions(principal, false)) {
                        session.expireNow();
                    }
                }
            }
        } catch (RuntimeException failure) {
            throw new SessionRevocationException();
        }
    }
}
