package io.github.vncntz.hris.clientmanagement;

import java.util.*;
import java.lang.reflect.Modifier;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static io.github.vncntz.hris.clientmanagement.ClientManagementException.Reason.*;

class ClientAdministrationQueriesTest {
    private final CurrentActor actor = mock(CurrentActor.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final ClientCompanyRepository companies = mock(ClientCompanyRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final JpaClientAdministrationQueries queries = new JpaClientAdministrationQueries(actor, entities, companies, transactions);

    ClientAdministrationQueriesTest() {
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            TransactionDefinition definition = call.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getIsolationLevel());
            assertTrue(definition.isReadOnly()); assertEquals(15, definition.getTimeout());
            return new SimpleTransactionStatus();
        });
    }
    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> query(Class<T> type, List<T> rows) {
        TypedQuery<T> query = mock(TypedQuery.class);
        when(entities.createQuery(anyString(), eq(type))).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(rows);
        return query;
    }
    @Test void authorizationPrecedesValidationAndDatabaseAccess() {
        doThrow(new AccessDeniedException("Denied")).when(actor).requireAuthority("client:admin");
        assertThrows(AccessDeniedException.class, () -> queries.companies(-1, 0));
        assertThrows(AccessDeniedException.class, () -> queries.sites(null, -1, 0));
        verifyNoInteractions(entities, companies, transactions);
        verify(actor, never()).requireUserId();
        reset(actor);
        doThrow(new AuthenticationCredentialsNotFoundException("Anonymous")).when(actor).requireUserId();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> queries.companies(0, 1));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> queries.sites(UUID.randomUUID(), 0, 1));
        verifyNoInteractions(entities, companies, transactions);
    }
    @Test void invalidPagingAndNullParentFailBeforeAnyTransaction() {
        for (int[] page : List.of(new int[]{-1, 1}, new int[]{0, 0}, new int[]{0, -1},
                new int[]{0, ClientAdministrationQueries.MAX_LIMIT + 1}, new int[]{0, Integer.MAX_VALUE})) {
            ClientManagementServiceTest.bounded(INVALID_PAGE, () -> queries.companies(page[0], page[1]));
            ClientManagementServiceTest.bounded(INVALID_PAGE, () -> queries.sites(UUID.randomUUID(), page[0], page[1]));
        }
        ClientManagementServiceTest.bounded(INVALID_TARGET, () -> queries.sites(null, 0, 1));
        verifyNoInteractions(entities, companies, transactions);
    }
    @Test void companyPagingUsesProjectionTotalOrderAndBoundedLookAhead() {
        var first = new ClientCompanyReference(new UUID(0, 1), "Synthetic duplicate", false, 7);
        var second = new ClientCompanyReference(new UUID(0, 2), first.displayName(), true, 0);
        var query = query(ClientCompanyReference.class, List.of(first, second));
        var page = queries.companies(5, 1);
        assertEquals(List.of(first), page.rows()); assertTrue(page.hasMore());
        verify(query).setFirstResult(5); verify(query).setMaxResults(2);
        verify(entities).createQuery(argThat(sql -> sql.contains("new io.github.vncntz.hris.clientmanagement.ClientCompanyReference")
                && sql.endsWith("order by c.displayName asc, c.publicId asc")), eq(ClientCompanyReference.class));
        verify(actor).requireAuthority("client:admin"); verify(actor).requireUserId(); verify(transactions).commit(any());
        when(query.getResultList()).thenReturn(List.of(first));
        assertFalse(queries.companies(0, 1).hasMore());
        when(query.getResultList()).thenReturn(List.of());
        assertEquals(new ClientCompanyPage(List.of(), false), queries.companies(Integer.MAX_VALUE, ClientAdministrationQueries.MAX_LIMIT));
        verify(query).setMaxResults(ClientAdministrationQueries.MAX_LIMIT + 1);
    }
    @Test void sitePagingRestrictsParentAndPreservesStoredAndEffectiveState() {
        UUID parent = new UUID(0, 3);
        when(companies.reference(parent)).thenReturn(Optional.of(new ClientCompanyReference(parent, "Synthetic", false, 2)));
        var row = new ClientSiteReference(new UUID(0, 1), parent, "Synthetic", true, false, 9);
        var query = query(ClientSiteReference.class, List.of(row, row));
        var page = queries.sites(parent, 3, 1);
        assertEquals(List.of(row), page.rows()); assertTrue(page.hasMore()); assertFalse(page.rows().getFirst().effectiveActive());
        verify(query).setParameter("parent", parent); verify(query).setFirstResult(3); verify(query).setMaxResults(2);
        verify(entities).createQuery(argThat(sql -> sql.contains("where c.publicId = :parent")
                && sql.endsWith("order by s.displayName asc, s.publicId asc")), eq(ClientSiteReference.class));
        when(query.getResultList()).thenReturn(List.of());
        assertEquals(new ClientSitePage(List.of(), false), queries.sites(parent, 0, 1));
    }
    @Test void unknownParentIsDeterministicEvenBeyondEndOfPage() {
        when(companies.reference(any())).thenReturn(Optional.empty());
        ClientManagementServiceTest.bounded(NOT_FOUND, () -> queries.sites(new UUID(0, 4), Integer.MAX_VALUE, 1));
        verifyNoInteractions(entities);
    }
    @Test void queryAndCommitFailuresDoNotExposeInfrastructure() {
        when(entities.createQuery(anyString(), eq(ClientCompanyReference.class))).thenThrow(new IllegalStateException("Private SQL"));
        ClientManagementServiceTest.bounded(PERSISTENCE_FAILED, () -> queries.companies(0, 1));
        when(companies.reference(any())).thenThrow(new IllegalStateException("Private SQL"));
        ClientManagementServiceTest.bounded(PERSISTENCE_FAILED, () -> queries.sites(UUID.randomUUID(), 0, 1));
        query(ClientCompanyReference.class, List.of());
        doThrow(new IllegalStateException("Private commit")).when(transactions).commit(any());
        ClientManagementServiceTest.bounded(PERSISTENCE_FAILED, () -> queries.companies(0, 1));
    }
    @Test void publicBoundaryContainsOnlyImmutableModuleOwnedRowsAndPagingMetadata() {
        var rows = new ArrayList<ClientCompanyReference>();
        var page = new ClientCompanyPage(rows, false); rows.add(new ClientCompanyReference(new UUID(0, 1), "Synthetic", true, 0));
        assertTrue(page.rows().isEmpty()); assertThrows(UnsupportedOperationException.class, () -> page.rows().clear());
        var sitePage = new ClientSitePage(new ArrayList<>(), false);
        assertThrows(UnsupportedOperationException.class, () -> sitePage.rows().clear());
        assertEquals(List.of("publicId", "displayName", "active", "version"), Arrays.stream(ClientCompanyReference.class.getRecordComponents()).map(c -> c.getName()).toList());
        assertEquals(List.of("publicId", "companyPublicId", "displayName", "active", "companyActive", "version"), Arrays.stream(ClientSiteReference.class.getRecordComponents()).map(c -> c.getName()).toList());
        for (Class<?> type : List.of(ClientCompanyPage.class, ClientSitePage.class)) {
            assertTrue(type.isRecord()); assertEquals(List.of("rows", "hasMore"), Arrays.stream(type.getRecordComponents()).map(c -> c.getName()).toList());
        }
        assertFalse(Modifier.isPublic(JpaClientAdministrationQueries.class.getModifiers()));
        for (var method : ClientAdministrationQueries.class.getDeclaredMethods()) {
            assertTrue(List.of(ClientCompanyPage.class, ClientSitePage.class).contains(method.getReturnType()));
            assertTrue(Arrays.stream(method.getParameterTypes()).allMatch(t -> t == int.class || t == UUID.class));
        }
    }
}
