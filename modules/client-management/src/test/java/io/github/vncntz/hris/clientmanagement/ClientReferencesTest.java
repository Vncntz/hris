package io.github.vncntz.hris.clientmanagement;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientReferencesTest {
    @Test void immutableReferencesIncludeStableParentAndEffectiveState() {
        var companies = mock(ClientCompanyRepository.class); var sites = mock(ClientSiteRepository.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            org.springframework.transaction.TransactionDefinition definition=call.getArgument(0);
            assertTrue(definition.isReadOnly());
            assertEquals(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
        var queries = new JpaClientReferences(companies, sites, transactions);
        UUID id = UUID.randomUUID(), parent = UUID.randomUUID();
        var reference = new ClientSiteReference(id, parent, "Synthetic", true, false, 2);
        when(sites.reference(id)).thenReturn(Optional.of(reference));
        assertEquals(reference, queries.site(id).orElseThrow()); assertFalse(reference.effectiveActive());
        when(companies.reference(parent)).thenReturn(Optional.empty()); assertTrue(queries.company(parent).isEmpty());
        when(sites.reference(id)).thenReturn(Optional.empty()); assertTrue(queries.site(id).isEmpty());
        assertTrue(ClientCompanyReference.class.isRecord()); assertTrue(ClientSiteReference.class.isRecord());
    }
    @Test void invalidTargetAndQueryFailuresAreBounded() {
        var companies = mock(ClientCompanyRepository.class); var sites = mock(ClientSiteRepository.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            org.springframework.transaction.TransactionDefinition definition=call.getArgument(0);
            assertTrue(definition.isReadOnly());
            assertEquals(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
        var queries = new JpaClientReferences(companies, sites, transactions);
        ClientManagementServiceTest.bounded(ClientManagementException.Reason.INVALID_TARGET, () -> queries.site(null));
        when(companies.reference(any())).thenThrow(new IllegalStateException("Private SQL"));
        ClientManagementServiceTest.bounded(ClientManagementException.Reason.PERSISTENCE_FAILED, () -> queries.company(UUID.randomUUID()));
    }
}
