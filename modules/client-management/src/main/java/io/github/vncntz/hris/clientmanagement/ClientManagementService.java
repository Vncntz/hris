package io.github.vncntz.hris.clientmanagement;

import java.util.UUID;
import java.util.function.Supplier;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import jakarta.persistence.EntityManager;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import static io.github.vncntz.hris.clientmanagement.ClientManagementException.Reason.*;

/** Authenticated, bounded, atomic master-data administration. No hard-delete command exists. */
@Service
public class ClientManagementService {
    public static final String ADMIN_AUTHORITY = "client:admin";
    private final CurrentActor actor;
    private final ClientCompanyRepository companies;
    private final ClientSiteRepository sites;
    private final EntityManager entities;
    private final AuditRecorder audit;
    private final TransactionTemplate mutation;

    ClientManagementService(CurrentActor actor, ClientCompanyRepository companies,
            ClientSiteRepository sites, EntityManager entities, AuditRecorder audit,
            PlatformTransactionManager transactions) {
        this.actor = actor;
        this.companies = companies;
        this.sites = sites;
        this.entities = entities;
        this.audit = audit;
        mutation = new TransactionTemplate(transactions);
        mutation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        mutation.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        mutation.setTimeout(15);
    }

    public ClientCompanyReference createCompany(String displayName) {
        UUID administrator = administrator();
        String name = validName(displayName);
        return execute(() -> {
            var company = new ClientCompanyEntity(UUID.randomUUID(), name);
            companies.save(company);
            record(administrator, "CLIENT_COMPANY_CREATED", "CLIENT_COMPANY", company.publicId(), null);
            entities.flush();
            return company.snapshot();
        });
    }

    public ClientSiteReference createSite(UUID companyPublicId, String displayName) {
        UUID administrator = administrator();
        validTarget(companyPublicId);
        String name = validName(displayName);
        return execute(() -> {
            var company = company(companyPublicId);
            if (!company.active()) { throw new ClientManagementException(INACTIVE_COMPANY); }
            var site = new ClientSiteEntity(UUID.randomUUID(), name, company);
            sites.save(site);
            record(administrator, "CLIENT_SITE_CREATED", "CLIENT_SITE", site.publicId(), companyPublicId);
            entities.flush();
            return site.snapshot();
        });
    }

    public ClientCompanyReference renameCompany(UUID target, String displayName, long expectedVersion) {
        UUID administrator = administrator();
        validTarget(target);
        String name = validName(displayName);
        return execute(() -> {
            var company = company(target);
            version(expectedVersion, company.version());
            if (company.displayName().equals(name)) { throw new ClientManagementException(NO_CHANGE); }
            company.rename(name);
            record(administrator, "CLIENT_COMPANY_RENAMED", "CLIENT_COMPANY", target, null);
            entities.flush();
            return company.snapshot();
        });
    }

    public ClientSiteReference renameSite(UUID target, String displayName, long expectedVersion) {
        UUID administrator = administrator();
        validTarget(target);
        String name = validName(displayName);
        return execute(() -> {
            var site = site(target);
            version(expectedVersion, site.version());
            if (site.displayName().equals(name)) { throw new ClientManagementException(NO_CHANGE); }
            site.rename(name);
            record(administrator, "CLIENT_SITE_RENAMED", "CLIENT_SITE", target, site.snapshot().companyPublicId());
            entities.flush();
            return site.snapshot();
        });
    }

    public ClientCompanyReference activateCompany(UUID target, long expectedVersion) { return changeCompany(target, expectedVersion, true); }
    public ClientCompanyReference deactivateCompany(UUID target, long expectedVersion) { return changeCompany(target, expectedVersion, false); }
    public ClientSiteReference activateSite(UUID target, long expectedVersion) { return changeSite(target, expectedVersion, true); }
    public ClientSiteReference deactivateSite(UUID target, long expectedVersion) { return changeSite(target, expectedVersion, false); }

    private ClientCompanyReference changeCompany(UUID target, long expectedVersion, boolean active) {
        UUID administrator = administrator();
        validTarget(target);
        return execute(() -> {
            var company = company(target);
            version(expectedVersion, company.version());
            if (company.active() == active) { throw new ClientManagementException(NO_CHANGE); }
            company.changeActive(active);
            record(administrator, active ? "CLIENT_COMPANY_ACTIVATED" : "CLIENT_COMPANY_DEACTIVATED", "CLIENT_COMPANY", target, null);
            entities.flush();
            return company.snapshot();
        });
    }

    private ClientSiteReference changeSite(UUID target, long expectedVersion, boolean active) {
        UUID administrator = administrator();
        validTarget(target);
        return execute(() -> {
            var site = site(target);
            version(expectedVersion, site.version());
            if (active && !site.snapshot().companyActive()) { throw new ClientManagementException(INACTIVE_COMPANY); }
            if (site.active() == active) { throw new ClientManagementException(NO_CHANGE); }
            site.changeActive(active);
            record(administrator, active ? "CLIENT_SITE_ACTIVATED" : "CLIENT_SITE_DEACTIVATED", "CLIENT_SITE", target, site.snapshot().companyPublicId());
            entities.flush();
            return site.snapshot();
        });
    }

    private UUID administrator() {
        actor.requireAuthority(ADMIN_AUTHORITY);
        return actor.requireUserId();
    }
    private ClientCompanyEntity company(UUID target) {
        var company = companies.lock(target).orElseThrow(() -> new ClientManagementException(NOT_FOUND));
        // A persistence context retained by a caller must not supply stale lifecycle/version state.
        entities.refresh(company);
        return company;
    }
    private ClientSiteEntity site(UUID target) {
        var reference = sites.reference(target).orElseThrow(() -> new ClientManagementException(NOT_FOUND));
        // All Site commands lock parent first. Company commands never lock Sites.
        company(reference.companyPublicId());
        var site = sites.lock(target).orElseThrow(() -> new ClientManagementException(NOT_FOUND));
        entities.refresh(site);
        return site;
    }
    private <T> T execute(Supplier<T> command) {
        // Own commit so commit-time errors cannot escape with SQL/causes, or report premature success.
        if (TransactionSynchronizationManager.isActualTransactionActive()) { throw new ClientManagementException(PERSISTENCE_FAILED); }
        try { return mutation.execute(status -> command.get()); }
        catch (ClientManagementException failure) { throw failure; }
        catch (OptimisticLockingFailureException failure) { throw new ClientManagementException(STALE_VERSION); }
        catch (PessimisticLockingFailureException failure) { throw new ClientManagementException(CONFLICT); }
        catch (RuntimeException failure) { throw new ClientManagementException(PERSISTENCE_FAILED); }
    }
    static void validTarget(UUID target) {
        if (target == null) { throw new ClientManagementException(INVALID_TARGET); }
    }
    static String validName(String displayName) {
        if (displayName == null) { throw new ClientManagementException(INVALID_NAME); }
        String name = displayName.strip();
        if (name.isBlank() || name.length() > 200 || name.codePoints().anyMatch(c -> Character.isISOControl(c) || (c >= 0xD800 && c <= 0xDFFF))) {
            throw new ClientManagementException(INVALID_NAME);
        }
        return name;
    }
    private static void version(long expected, long actual) {
        if (expected < 0 || expected != actual || actual == Long.MAX_VALUE) { throw new ClientManagementException(STALE_VERSION); }
    }
    private void record(UUID administrator, String action, String type, UUID target, UUID parent) {
        audit.record(new AuditRequest(administrator.toString(), action, type, target.toString(), null,
                parent == null ? null : "company=" + parent));
    }
}
