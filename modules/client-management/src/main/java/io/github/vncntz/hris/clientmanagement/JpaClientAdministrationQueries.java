package io.github.vncntz.hris.clientmanagement;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static io.github.vncntz.hris.clientmanagement.ClientManagementException.Reason.*;

/** Projection-only queries with a bounded look-ahead; no count or entity materialization. */
@Service
class JpaClientAdministrationQueries implements ClientAdministrationQueries {
    private final CurrentActor actor;
    private final EntityManager entities;
    private final ClientCompanyRepository companies;
    private final TransactionTemplate read;

    JpaClientAdministrationQueries(CurrentActor actor, EntityManager entities,
            ClientCompanyRepository companies, PlatformTransactionManager transactions) {
        this.actor = actor;
        this.entities = entities;
        this.companies = companies;
        read = new TransactionTemplate(transactions);
        read.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        read.setReadOnly(true);
        read.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
        read.setTimeout(15);
    }

    public ClientCompanyPage companies(int offset, int limit) {
        authorize();
        validPage(offset, limit);
        return execute(() -> {
            var query = entities.createQuery("select new io.github.vncntz.hris.clientmanagement.ClientCompanyReference(c.publicId, c.displayName, c.active, c.rowVersion) from ClientCompanyEntity c order by c.displayName asc, c.publicId asc", ClientCompanyReference.class);
            var rows = fetch(query, offset, limit);
            return new ClientCompanyPage(rows.subList(0, Math.min(limit, rows.size())), rows.size() > limit);
        });
    }

    public ClientSitePage sites(UUID companyPublicId, int offset, int limit) {
        authorize();
        ClientManagementService.validTarget(companyPublicId);
        validPage(offset, limit);
        return execute(() -> {
            if (companies.reference(companyPublicId).isEmpty()) { throw new ClientManagementException(NOT_FOUND); }
            var query = entities.createQuery("select new io.github.vncntz.hris.clientmanagement.ClientSiteReference(s.publicId, c.publicId, s.displayName, s.active, c.active, s.rowVersion) from ClientSiteEntity s join s.company c where c.publicId = :parent order by s.displayName asc, s.publicId asc", ClientSiteReference.class);
            query.setParameter("parent", companyPublicId);
            var rows = fetch(query, offset, limit);
            return new ClientSitePage(rows.subList(0, Math.min(limit, rows.size())), rows.size() > limit);
        });
    }

    private void authorize() {
        actor.requireAuthority(ClientManagementService.ADMIN_AUTHORITY);
        actor.requireUserId();
    }
    private static void validPage(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > MAX_LIMIT) { throw new ClientManagementException(INVALID_PAGE); }
    }
    private static <T> List<T> fetch(TypedQuery<T> query, int offset, int limit) {
        return query.setFirstResult(offset).setMaxResults(limit + 1).getResultList();
    }
    private <T> T execute(Supplier<T> query) {
        try { return read.execute(status -> query.get()); }
        catch (ClientManagementException failure) { throw failure; }
        catch (RuntimeException failure) { throw new ClientManagementException(PERSISTENCE_FAILED); }
    }
}
