package io.github.vncntz.hris.app;

import java.util.Optional;
import java.util.function.Supplier;
import io.github.vncntz.hris.identityaccess.RecentAuthenticationException;
import io.github.vncntz.hris.identityaccess.RecentAuthenticationSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Servlet-memory adapter for Identity's session port. No session is created here. */
@Component
class ServletRecentAuthenticationSession implements RecentAuthenticationSession {
    private static final String ATTRIBUTE = ServletRecentAuthenticationSession.class.getName() + ".proof";
    private final SessionRegistry registry;

    ServletRecentAuthenticationSession(SessionRegistry registry) { this.registry = registry; }

    @Override public void attempt(Supplier<Proof> verification) {
        HttpSession session = currentSession();
        if (session == null) { throw unavailable(); }
        try {
            synchronized (session) {
                session.removeAttribute(ATTRIBUTE);
                if (!usable(session)) { throw unavailable(); }
                boolean established = false;
                try {
                    Proof proof = verification.get();
                    // Revocation may happen between the locked DB recheck/commit and publication.
                    if (proof == null || !usable(session)) { throw unavailable(); }
                    session.setAttribute(ATTRIBUTE, proof);
                    established = true;
                } finally {
                    // Even a publication failure after attribute storage must leave no proof.
                    if (!established) { session.removeAttribute(ATTRIBUTE); }
                }
            }
        } catch (IllegalStateException failure) {
            throw unavailable();
        }
    }

    @Override public Optional<Proof> proof() {
        HttpSession session = currentSession();
        if (session == null) { return Optional.empty(); }
        try {
            synchronized (session) {
                if (!usable(session)) { return Optional.empty(); }
                Object proof = session.getAttribute(ATTRIBUTE);
                return proof instanceof Proof value ? Optional.of(value) : Optional.empty();
            }
        } catch (IllegalStateException failure) {
            return Optional.empty();
        }
    }

    /** Standard fixation can migrate attributes; new authentication explicitly discards proof. */
    void clearOnAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) { session.removeAttribute(ATTRIBUTE); }
        }
    }

    private boolean usable(HttpSession session) {
        var information = registry.getSessionInformation(session.getId());
        return information != null && !information.isExpired();
    }

    private static HttpSession currentSession() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getSession(false) : null;
    }

    private static RecentAuthenticationException unavailable() {
        return new RecentAuthenticationException(RecentAuthenticationException.Reason.SESSION_UNAVAILABLE);
    }
}
