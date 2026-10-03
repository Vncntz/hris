package io.github.vncntz.hris.app;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import io.github.vncntz.hris.identityaccess.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;

class ServletRecentAuthenticationSessionTest {
    final SessionRegistryImpl registry = new SessionRegistryImpl();
    final ServletRecentAuthenticationSession adapter = new ServletRecentAuthenticationSession(registry);
    final UUID id = UUID.randomUUID();
    final RecentAuthenticationSession.Proof proof = new RecentAuthenticationSession.Proof(id, Instant.EPOCH);
    final MockHttpSession first = new MockHttpSession(), second = new MockHttpSession();

    void bind(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest(); request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    void register(MockHttpSession session) { registry.registerNewSession(session.getId(), new AccountPrincipal(id, "synthetic.self")); }
    @AfterEach void clear() { RequestContextHolder.resetRequestAttributes(); }
    @Test void sessionIndependenceMarkerReplacementFailedAttemptAndNewAuthentication() {
        register(first); register(second); bind(first); adapter.attempt(() -> proof);
        assertEquals(proof, adapter.proof().orElseThrow());
        bind(second); assertTrue(adapter.proof().isEmpty());
        bind(first);
        var later = new RecentAuthenticationSession.Proof(id, Instant.EPOCH.plusSeconds(1));
        adapter.attempt(() -> later); assertEquals(later, adapter.proof().orElseThrow());
        assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> {
            assertTrue(adapter.proof().isEmpty());
            throw new RecentAuthenticationException(RecentAuthenticationException.Reason.CREDENTIAL_REJECTED);
        }));
        assertTrue(adapter.proof().isEmpty()); adapter.attempt(() -> proof);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.setSession(first);
        adapter.clearOnAuthentication(request); assertTrue(adapter.proof().isEmpty());
        adapter.attempt(() -> proof);
        assertEquals(1, java.util.Collections.list(first.getAttributeNames()).size());
        assertEquals(2, RecentAuthenticationSession.Proof.class.getRecordComponents().length);
    }
    @Test void invalidatedMissingAndRevokedSessionCannotPublishOrUseProof() {
        assertTrue(adapter.proof().isEmpty());
        assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> proof));
        bind(first); assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> proof));
        register(first); adapter.attempt(() -> proof); registry.getSessionInformation(first.getId()).expireNow();
        assertTrue(adapter.proof().isEmpty());
        assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> proof));
        bind(second); register(second);
        assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> {
            registry.getSessionInformation(second.getId()).expireNow(); return proof;
        }));
        assertTrue(adapter.proof().isEmpty());
        second.invalidate(); assertTrue(adapter.proof().isEmpty());
    }
    @Test void failedPublicationAfterAttributeStorageLeavesNoProof() {
        MockHttpSession failing = org.mockito.Mockito.spy(new MockHttpSession());
        register(failing); bind(failing);
        org.mockito.Mockito.doAnswer(call -> {
            call.callRealMethod(); throw new IllegalStateException("Test publication failure");
        }).when(failing).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        var failure = assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> proof));
        assertNull(failure.getCause()); assertTrue(adapter.proof().isEmpty());
    }
    @Test void overlappingAttemptsAreSerializedAndLaterFailureRemovesEarlierProof() throws Exception {
        register(first);
        CountDownLatch verifying = new CountDownLatch(1), release = new CountDownLatch(1), secondStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { bind(first); try { adapter.attempt(() -> {
                verifying.countDown(); await(release); return proof;
            }); } finally { clear(); } });
            assertTrue(verifying.await(10, TimeUnit.SECONDS));
            var b = executor.submit(() -> { bind(first); try { secondStarted.countDown();
                assertThrows(RecentAuthenticationException.class, () -> adapter.attempt(() -> {
                    throw new RecentAuthenticationException(RecentAuthenticationException.Reason.CREDENTIAL_REJECTED);
                }));
            } finally { clear(); } });
            try { assertTrue(secondStarted.await(10, TimeUnit.SECONDS)); } finally { release.countDown(); }
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
        } finally { release.countDown(); }
        bind(first); assertTrue(adapter.proof().isEmpty());
    }
    static void await(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Test barrier timed out"); } }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException("Test interrupted"); }
    }
}
