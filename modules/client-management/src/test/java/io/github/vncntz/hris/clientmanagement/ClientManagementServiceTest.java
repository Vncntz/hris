package io.github.vncntz.hris.clientmanagement;

import java.util.*;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import io.github.vncntz.hris.sharedkernel.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.dao.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static io.github.vncntz.hris.clientmanagement.ClientManagementException.Reason.*;

class ClientManagementServiceTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final ClientCompanyRepository companies = mock(ClientCompanyRepository.class);
    private final ClientSiteRepository sites = mock(ClientSiteRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final AuditRecorder audit = mock(AuditRecorder.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final UUID administrator = UUID.randomUUID();
    private final ClientCompanyEntity company = new ClientCompanyEntity(UUID.randomUUID(), "Synthetic Company");
    private final ClientSiteEntity site = new ClientSiteEntity(UUID.randomUUID(), "Synthetic Site", company);
    private final ClientManagementService service = new ClientManagementService(actor, companies, sites, entities, audit, transactions);

    @BeforeEach void ready() {
        when(actor.requireUserId()).thenReturn(administrator);
        when(companies.lock(company.publicId())).thenReturn(Optional.of(company));
        when(sites.reference(site.publicId())).thenReturn(Optional.of(site.snapshot()));
        when(sites.lock(site.publicId())).thenReturn(Optional.of(site));
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            TransactionDefinition definition = call.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
    }
    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }

    @Test void everyCommandRequiresAuthorityBeforeActorValidationOrPersistence() {
        doThrow(new AccessDeniedException("Access denied")).when(actor).requireAuthority("client:admin");
        List<org.junit.jupiter.api.function.Executable> commands = List.of(
            () -> service.createCompany(null), () -> service.createSite(null, null),
            () -> service.renameCompany(null, null, -1), () -> service.renameSite(null, null, -1),
            () -> service.activateCompany(null, -1), () -> service.deactivateCompany(null, -1),
            () -> service.activateSite(null, -1), () -> service.deactivateSite(null, -1));
        for (var command : commands) { assertThrows(AccessDeniedException.class, command); }
        verify(actor, times(8)).requireAuthority("client:admin");
        verify(actor, never()).requireUserId();
        verifyNoInteractions(transactions, entities, audit, companies, sites);
    }

    @Test void namesAreBoundedTrimmedNonblankAndNotIdentity() {
        for (String value : Arrays.asList(null, "", "   ", "\u2003", "x".repeat(201), "x\ny", "x\u0000", "\uD800")) {
            bounded(INVALID_NAME, () -> service.createCompany(value));
        }
        assertEquals("Synthetic", ClientManagementService.validName("  Synthetic  "));
        assertEquals(200, ClientManagementService.validName("x".repeat(200)).length());
        var first = service.createCompany("Same synthetic name");
        var second = service.createCompany("Same synthetic name");
        assertNotEquals(first.publicId(), second.publicId());
    }

    @Test void companyCreationDerivesActorAndAuditsBeforeFlushAndCommit() {
        var result = service.createCompany("  Synthetic Company  ");
        assertTrue(result.active()); assertEquals(0, result.version());
        var order = inOrder(actor, transactions, companies, audit, entities);
        order.verify(actor).requireAuthority("client:admin"); order.verify(actor).requireUserId();
        order.verify(transactions).getTransaction(any()); order.verify(companies).save(any());
        order.verify(audit).record(new AuditRequest(administrator.toString(), "CLIENT_COMPANY_CREATED", "CLIENT_COMPANY", result.publicId().toString(), null, null));
        order.verify(entities).flush(); order.verify(transactions).commit(any());
    }

    @Test void siteCreationLocksParentAndAuditsOnlyStableIds() {
        var result = service.createSite(company.publicId(), "  Synthetic Site  ");
        assertEquals(company.publicId(), result.companyPublicId()); assertTrue(result.effectiveActive());
        var order = inOrder(companies, entities, sites, audit);
        order.verify(companies).lock(company.publicId()); order.verify(entities).refresh(company);
        order.verify(sites).save(any());
        order.verify(audit).record(new AuditRequest(administrator.toString(), "CLIENT_SITE_CREATED", "CLIENT_SITE", result.publicId().toString(), null, "company=" + company.publicId()));
        order.verify(entities).flush();
    }

    @Test void renameAndLifecycleUseDistinctAuditsAndPreserveIdentities() {
        service.renameCompany(company.publicId(), "Renamed synthetic company", 0);
        service.renameSite(site.publicId(), "Renamed synthetic site", 0);
        service.deactivateSite(site.publicId(), 0);
        service.activateSite(site.publicId(), 0);
        service.deactivateCompany(company.publicId(), 0);
        service.activateCompany(company.publicId(), 0);
        assertEquals(company.publicId(), site.snapshot().companyPublicId());
        var requests = org.mockito.ArgumentCaptor.forClass(AuditRequest.class);
        verify(audit, times(6)).record(requests.capture());
        assertEquals(List.of("CLIENT_COMPANY_RENAMED", "CLIENT_SITE_RENAMED", "CLIENT_SITE_DEACTIVATED", "CLIENT_SITE_ACTIVATED", "CLIENT_COMPANY_DEACTIVATED", "CLIENT_COMPANY_ACTIVATED"), requests.getAllValues().stream().map(AuditRequest::action).toList());
        requests.getAllValues().forEach(r -> { assertEquals(administrator.toString(), r.actorReference()); assertNull(r.reason()); assertFalse(r.toString().contains("Renamed")); });
    }

    @Test void parentDeactivationDoesNotRewriteSitesAndReactivationDoesNotActivateThem() {
        service.deactivateCompany(company.publicId(), 0);
        assertTrue(site.active()); assertFalse(site.snapshot().effectiveActive());
        bounded(INACTIVE_COMPANY, () -> service.activateSite(site.publicId(), 0));
        bounded(INACTIVE_COMPANY, () -> service.createSite(company.publicId(), "Synthetic"));
        service.deactivateSite(site.publicId(), 0);
        service.activateCompany(company.publicId(), 0);
        assertFalse(site.active()); assertFalse(site.snapshot().effectiveActive());
        verify(sites, never()).save(any());
    }

    @Test void invalidMissingStaleAndNoOpProduceBoundedOutcomesWithoutAudit() {
        bounded(INVALID_TARGET, () -> service.activateCompany(null, 0));
        bounded(NOT_FOUND, () -> service.deactivateCompany(UUID.randomUUID(), 0));
        bounded(NOT_FOUND, () -> service.deactivateSite(UUID.randomUUID(), 0));
        bounded(STALE_VERSION, () -> service.deactivateCompany(company.publicId(), 1));
        bounded(STALE_VERSION, () -> service.renameSite(site.publicId(), "Changed", -1));
        bounded(NO_CHANGE, () -> service.renameCompany(company.publicId(), company.displayName(), 0));
        bounded(NO_CHANGE, () -> service.renameSite(site.publicId(), site.displayName(), 0));
        bounded(NO_CHANGE, () -> service.activateCompany(company.publicId(), 0));
        bounded(NO_CHANGE, () -> service.activateSite(site.publicId(), 0));
        verifyNoInteractions(audit);
    }

    @Test void lockedStateIsRefreshedAndParentIsLockedBeforeSite() {
        doAnswer(call -> { company.changeActive(false); return null; }).when(entities).refresh(company);
        bounded(INACTIVE_COMPANY, () -> service.activateSite(site.publicId(), 0));
        var order = inOrder(companies, sites, entities);
        order.verify(sites).reference(site.publicId()); order.verify(companies).lock(company.publicId());
        order.verify(entities).refresh(company); order.verify(sites).lock(site.publicId()); order.verify(entities).refresh(site);
        verifyNoInteractions(audit);
    }

    @Test void ambientTransactionIsRejectedWithoutIndependentCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        bounded(PERSISTENCE_FAILED, () -> service.createCompany("Synthetic"));
        verifyNoInteractions(transactions, companies, sites, audit, entities);
    }

    @Test void persistenceAuditFlushAndCommitFailuresNeverExposeInfrastructure() {
        for (int scenario=0; scenario<4; scenario++) {
            reset(companies, audit, entities, transactions); ready();
            var failure = new IllegalStateException("Synthetic private infrastructure details");
            switch (scenario) {
                case 0 -> when(companies.save(any())).thenThrow(failure);
                case 1 -> when(audit.record(any())).thenThrow(failure);
                case 2 -> doThrow(failure).when(entities).flush();
                case 3 -> doThrow(failure).when(transactions).commit(any());
            }
            bounded(PERSISTENCE_FAILED, () -> service.createCompany("Synthetic"));
            if (scenario < 3) { verify(transactions).rollback(any()); }
        }
    }

    @Test void lockAndOptimisticConflictsHaveFixedOutcomesWithoutRetry() {
        when(companies.lock(company.publicId())).thenThrow(new PessimisticLockingFailureException("Private"));
        bounded(CONFLICT, () -> service.deactivateCompany(company.publicId(), 0));
        doThrow(new OptimisticLockingFailureException("Private")).when(companies).lock(company.publicId());
        bounded(STALE_VERSION, () -> service.renameCompany(company.publicId(), "Changed", 0));
        verify(companies, times(2)).lock(company.publicId()); verifyNoInteractions(audit);
    }

    @Test void versionCannotWrap() {
        ReflectionTestUtils.setField(company, "rowVersion", Long.MAX_VALUE);
        bounded(STALE_VERSION, () -> service.deactivateCompany(company.publicId(), Long.MAX_VALUE));
        assertTrue(company.active()); verifyNoInteractions(audit);
    }

    static void bounded(ClientManagementException.Reason reason, org.junit.jupiter.api.function.Executable command) {
        var failure = assertThrows(ClientManagementException.class, command);
        assertEquals(reason, failure.reason()); assertEquals("Client management failed: " + reason, failure.getMessage());
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
    }
}
