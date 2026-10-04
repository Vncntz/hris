package io.github.vncntz.hris.clientmanagement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A single projection query observes Site and parent state without exposing managed objects. */
@Service
class JpaClientReferences implements ClientReferences {
    private final ClientCompanyRepository companies;
    private final ClientSiteRepository sites;
    private final TransactionTemplate read;
    JpaClientReferences(ClientCompanyRepository companies, ClientSiteRepository sites,
                        PlatformTransactionManager transactions) {
        this.companies = companies;
        this.sites = sites;
        read = new TransactionTemplate(transactions);
        read.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        read.setReadOnly(true);
        read.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
        read.setTimeout(15);
    }
    public Optional<ClientCompanyReference> company(UUID publicId) {
        ClientManagementService.validTarget(publicId);
        try { return read.execute(status -> companies.reference(publicId)); }
        catch (RuntimeException failure) { throw new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED); }
    }
    public Optional<ClientSiteReference> site(UUID publicId) {
        ClientManagementService.validTarget(publicId);
        try { return read.execute(status -> sites.reference(publicId)); }
        catch (RuntimeException failure) { throw new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED); }
    }
}
