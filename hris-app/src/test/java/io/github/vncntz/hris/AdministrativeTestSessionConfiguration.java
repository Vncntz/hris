package io.github.vncntz.hris;

import java.util.Optional;
import java.util.function.Supplier;
import io.github.vncntz.hris.identityaccess.RecentAuthenticationSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

/** Test-session adapter for historical direct-service regressions; HTTP uses the production adapter. */
@TestConfiguration(proxyBeanMethods = false)
class AdministrativeTestSessionConfiguration {
    @Bean @Primary RecentAuthenticationSession administrativeTestSession(
            @Qualifier("servletRecentAuthenticationSession") RecentAuthenticationSession servlet) {
        return new RecentAuthenticationSession() {
            private final ThreadLocal<Proof> local = new ThreadLocal<>();
            @Override public void attempt(Supplier<Proof> verification) {
                if (realServletRequest()) { servlet.attempt(verification); }
                else { local.remove(); local.set(verification.get()); }
            }
            private boolean realServletRequest() {
                return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                        && !(attributes.getRequest() instanceof MockHttpServletRequest);
            }
            @Override public Optional<Proof> proof() {
                return realServletRequest() ? servlet.proof() : Optional.ofNullable(local.get());
            }
        };
    }
}
