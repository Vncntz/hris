package io.github.vncntz.hris;

import java.util.*;
import java.util.concurrent.*;
import io.github.vncntz.hris.clientmanagement.*;
import io.github.vncntz.hris.identityaccess.AccountPrincipal;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static io.github.vncntz.hris.clientmanagement.ClientManagementException.Reason.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"vaadin.productionMode=true", "logging.level.root=OFF"})
class ClientManagementIT {
    @Container @ServiceConnection static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.11")
        .withCommand("--log-bin-trust-function-creators=1");
    @Autowired ClientManagementService commands;
    @Autowired ClientReferences queries;
    @Autowired ClientAdministrationQueries administration;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean AuditRecorder audit;
    private final UUID administrator = UUID.randomUUID();
    @BeforeEach void authenticate() { login(administrator, true); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); reset(audit); }
    static void login(UUID actor, boolean allowed) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            new AccountPrincipal(actor, "synthetic.client.admin"), null,
            allowed ? List.of(new SimpleGrantedAuthority("client:admin")) : List.of()));
    }
    ClientCompanyReference company() { return commands.createCompany("Synthetic Company " + UUID.randomUUID()); }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    int audits() { return jdbc.queryForObject("SELECT COUNT(*) FROM audit_event WHERE target_type IN ('CLIENT_COMPANY','CLIENT_SITE')", Integer.class); }
    void bounded(ClientManagementException.Reason reason, org.junit.jupiter.api.function.Executable command) {
        var failure = assertThrows(ClientManagementException.class, command);
        assertEquals(reason, failure.reason()); assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
        assertEquals("Client management failed: " + reason, failure.getMessage());
    }

    @Test void creationRoundTripsUuidNamesVersionsAndExactlyOnePrivateAuditPerMutation() {
        int before = audits();
        var company = commands.createCompany("  Synthetic duplicated name  ");
        var duplicate = commands.createCompany(company.displayName());
        assertNotEquals(company.publicId(), duplicate.publicId());
        var site = commands.createSite(company.publicId(), "  Synthetic Site  ");
        var otherSite = commands.createSite(company.publicId(), site.displayName());
        assertNotEquals(site.publicId(), otherSite.publicId());
        assertEquals(company, queries.company(company.publicId()).orElseThrow());
        assertEquals(site, queries.site(site.publicId()).orElseThrow());
        assertTrue(site.effectiveActive()); assertEquals(0, site.version());
        assertEquals(before + 4, audits());
        var rows = jdbc.queryForList("SELECT actor_reference,action,target_type,target_reference,reason,context FROM audit_event WHERE target_reference IN (?,?) ORDER BY id", company.publicId().toString(), site.publicId().toString());
        assertEquals(2, rows.size());
        assertEquals(Map.of("actor_reference",administrator.toString(),"action","CLIENT_COMPANY_CREATED","target_type","CLIENT_COMPANY","target_reference",company.publicId().toString()), withoutNulls(rows.get(0)));
        assertEquals(Map.of("actor_reference",administrator.toString(),"action","CLIENT_SITE_CREATED","target_type","CLIENT_SITE","target_reference",site.publicId().toString(),"context","company="+company.publicId()), withoutNulls(rows.get(1)));
        assertEquals(16, jdbc.queryForObject("SELECT LENGTH(public_id) FROM client_site WHERE public_id=UUID_TO_BIN(?)", Integer.class, site.publicId().toString()));
    }
    Map<String,Object> withoutNulls(Map<String,Object> row) { var copy = new HashMap<>(row); copy.values().removeIf(Objects::isNull); return copy; }

    @Test void lifecyclePreservesSiteStateIdentitiesAndExplicitRenames() {
        var c = company(); var s = commands.createSite(c.publicId(), "Synthetic Site"); int before = audits();
        var renamedCompany = commands.renameCompany(c.publicId(), "Corrected Synthetic Company", c.version());
        var renamedSite = commands.renameSite(s.publicId(), "Corrected Synthetic Site", s.version());
        assertEquals(1, renamedCompany.version()); assertEquals(1, renamedSite.version());
        var inactive = commands.deactivateCompany(c.publicId(), renamedCompany.version());
        var effective = queries.site(s.publicId()).orElseThrow();
        assertTrue(effective.active()); assertFalse(effective.effectiveActive()); assertEquals(renamedSite.version(), effective.version());
        bounded(INACTIVE_COMPANY, () -> commands.createSite(c.publicId(), "Synthetic Rejected"));
        bounded(INACTIVE_COMPANY, () -> commands.activateSite(s.publicId(), effective.version()));
        var inactiveSite = commands.deactivateSite(s.publicId(), effective.version());
        commands.activateCompany(c.publicId(), inactive.version());
        assertFalse(queries.site(s.publicId()).orElseThrow().effectiveActive());
        var activated = commands.activateSite(s.publicId(), inactiveSite.version());
        assertTrue(activated.effectiveActive()); assertEquals(c.publicId(), activated.companyPublicId());
        assertEquals(before+6, audits());
        assertEquals(List.of("CLIENT_COMPANY_RENAMED", "CLIENT_SITE_RENAMED", "CLIENT_COMPANY_DEACTIVATED", "CLIENT_SITE_DEACTIVATED", "CLIENT_COMPANY_ACTIVATED", "CLIENT_SITE_ACTIVATED"), jdbc.queryForList("SELECT action FROM audit_event WHERE target_reference IN (?,?) AND action NOT LIKE '%CREATED' ORDER BY id", String.class,c.publicId().toString(),s.publicId().toString()));
    }

    @Test void boundedValidationMissingStaleNoOpsLeaveStateAndAuditUnchanged() {
        var c = company(); var s = commands.createSite(c.publicId(), "Synthetic Site"); int before=audits();
        bounded(INVALID_NAME, () -> commands.renameCompany(c.publicId(), "x".repeat(201), 0));
        bounded(INVALID_TARGET, () -> commands.activateSite(null, 0));
        bounded(NOT_FOUND, () -> commands.renameSite(UUID.randomUUID(), "Synthetic", 0));
        bounded(NOT_FOUND, () -> commands.createSite(UUID.randomUUID(), "Synthetic"));
        bounded(STALE_VERSION, () -> commands.deactivateCompany(c.publicId(), 3));
        bounded(STALE_VERSION, () -> commands.renameSite(s.publicId(), "Synthetic Changed", 3));
        bounded(NO_CHANGE, () -> commands.activateCompany(c.publicId(), 0));
        bounded(NO_CHANGE, () -> commands.activateSite(s.publicId(), 0));
        bounded(NO_CHANGE, () -> commands.renameCompany(c.publicId(), c.displayName(), 0));
        bounded(NO_CHANGE, () -> commands.renameSite(s.publicId(), s.displayName(), 0));
        assertEquals(c,queries.company(c.publicId()).orElseThrow()); assertEquals(s,queries.site(s.publicId()).orElseThrow());
        assertEquals(before,audits()); assertTrue(queries.company(UUID.randomUUID()).isEmpty()); assertTrue(queries.site(UUID.randomUUID()).isEmpty());
    }

    @Test void realActorRejectsAnonymousMissingAuthorityAndUnauthenticatedPrincipalsBeforeWrites() {
        int before=count("client_company");
        SecurityContextHolder.clearContext(); assertThrows(AuthenticationCredentialsNotFoundException.class, () -> commands.createCompany("Synthetic"));
        login(administrator,false); assertThrows(AccessDeniedException.class, () -> commands.createCompany("Synthetic"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated(new AccountPrincipal(administrator,"synthetic"),null));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> commands.createCompany("Synthetic"));
        assertEquals(before,count("client_company"));
    }

    @Test void databaseForeignKeysAndConstraintsRejectOrphansDuplicatesAndDestructiveParentDelete() {
        var c=company(); var s=commands.createSite(c.publicId(),"Synthetic Site");
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("DELETE FROM client_company WHERE public_id=UUID_TO_BIN(?)",c.publicId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("UPDATE client_site SET company_id=-1 WHERE public_id=UUID_TO_BIN(?)",s.publicId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("UPDATE client_company SET active=2 WHERE public_id=UUID_TO_BIN(?)",c.publicId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("UPDATE client_site SET row_version=-1 WHERE public_id=UUID_TO_BIN(?)",s.publicId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("UPDATE client_site SET display_name=' ' WHERE public_id=UUID_TO_BIN(?)",s.publicId().toString()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("INSERT INTO client_company(public_id,display_name,active,row_version) VALUES(UUID_TO_BIN(?),'Synthetic',1,0)",c.publicId().toString()));
    }

    @Test void auditFailureAfterAppendRollsBackCreateRenameAndLifecycle() {
        var c=company(); var s=commands.createSite(c.publicId(),"Synthetic Site"); int before=audits(), companies=count("client_company"),sites=count("client_site");
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("Synthetic internal details"); }).when(audit).record(any());
        bounded(PERSISTENCE_FAILED, () -> commands.createCompany("Synthetic Rollback"));
        bounded(PERSISTENCE_FAILED, () -> commands.createSite(c.publicId(),"Synthetic Rollback"));
        bounded(PERSISTENCE_FAILED, () -> commands.renameCompany(c.publicId(),"Synthetic Rollback",0));
        bounded(PERSISTENCE_FAILED, () -> commands.renameSite(s.publicId(),"Synthetic Rollback",0));
        bounded(PERSISTENCE_FAILED, () -> commands.deactivateCompany(c.publicId(),0));
        bounded(PERSISTENCE_FAILED, () -> commands.deactivateSite(s.publicId(),0));
        assertEquals(before,audits()); assertEquals(companies,count("client_company")); assertEquals(sites,count("client_site"));
        assertEquals(c,queries.company(c.publicId()).orElseThrow()); assertEquals(s,queries.site(s.publicId()).orElseThrow());
    }

    @Test void databaseAuditFailureRollsBackBusinessInsert() {
        int before=count("client_company"), auditBefore=audits();
        jdbc.execute("CREATE TRIGGER synthetic_client_audit_reject BEFORE INSERT ON audit_event FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic audit rejection'");
        try { bounded(PERSISTENCE_FAILED, () -> commands.createCompany("Synthetic Rejected")); }
        finally { jdbc.execute("DROP TRIGGER synthetic_client_audit_reject"); }
        assertEquals(before,count("client_company")); assertEquals(auditBefore,audits());
    }

    @Test void businessFlushFailureRollsBackAlreadyAppendedAudit() {
        var c=company(); int before=audits();
        jdbc.execute("CREATE TRIGGER synthetic_client_mutation_reject BEFORE UPDATE ON client_company FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic mutation rejection'");
        try { bounded(PERSISTENCE_FAILED, () -> commands.renameCompany(c.publicId(),"Synthetic Rejected",0)); }
        finally { jdbc.execute("DROP TRIGGER synthetic_client_mutation_reject"); }
        assertEquals(c,queries.company(c.publicId()).orElseThrow()); assertEquals(before,audits());
    }

    @Test void referenceReadsObserveCommittedDeactivationOutsideAnOlderAmbientSnapshot() throws Exception {
        var c = company(); var s = commands.createSite(c.publicId(), "Synthetic Snapshot Site");
        int before = audits();
        var ambient = new TransactionTemplate(transactions);
        ambient.setIsolationLevel(TransactionTemplate.ISOLATION_REPEATABLE_READ);
        ambient.setTimeout(30);
        try (var executor = Executors.newSingleThreadExecutor()) {
            ambient.executeWithoutResult(status -> {
                assertEquals(1, jdbc.queryForObject("SELECT active FROM client_company WHERE public_id=UUID_TO_BIN(?)", Integer.class, c.publicId().toString()));
                assertEquals(1, jdbc.queryForObject("SELECT c.active AND s.active FROM client_site s JOIN client_company c ON c.id=s.company_id WHERE s.public_id=UUID_TO_BIN(?)", Integer.class, s.publicId().toString()));
                var deactivation = executor.submit(() -> {
                    login(administrator, true);
                    try { return commands.deactivateCompany(c.publicId(), c.version()); }
                    finally { SecurityContextHolder.clearContext(); }
                });
                try { assertFalse(deactivation.get(10, TimeUnit.SECONDS).active()); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
                catch (ExecutionException | TimeoutException failure) { throw new AssertionError(failure); }
                // Ordinary reads still see the original snapshot; the reference boundary must not.
                assertEquals(1, jdbc.queryForObject("SELECT active FROM client_company WHERE public_id=UUID_TO_BIN(?)", Integer.class, c.publicId().toString()));
                var currentCompany = queries.company(c.publicId()).orElseThrow();
                assertFalse(currentCompany.active()); assertEquals(c.version() + 1, currentCompany.version());
                var currentSite = queries.site(s.publicId()).orElseThrow();
                assertEquals(s.publicId(), currentSite.publicId()); assertEquals(c.publicId(), currentSite.companyPublicId());
                assertTrue(currentSite.active()); assertFalse(currentSite.companyActive()); assertFalse(currentSite.effectiveActive());
                assertEquals(s.version(), currentSite.version());
                assertEquals(1, jdbc.queryForObject("SELECT c.active AND s.active FROM client_site s JOIN client_company c ON c.id=s.company_id WHERE s.public_id=UUID_TO_BIN(?)", Integer.class, s.publicId().toString()));
            });
        }
        assertEquals(before + 1, audits());
        assertFalse(queries.company(c.publicId()).orElseThrow().active());
    }

    @Test void ambientTransactionCannotReportAnIndependentCommit() {
        int before=count("client_company");
        new TransactionTemplate(transactions).executeWithoutResult(status -> bounded(PERSISTENCE_FAILED, () -> commands.createCompany("Synthetic Rejected")));
        assertEquals(before,count("client_company"));
    }

    @RepeatedTest(3) void concurrentSiteRenamesWithSameVersionHaveOneWinnerAndOneStaleOutcome() throws Exception {
        var c=company(); var s=commands.createSite(c.publicId(),"Synthetic Site"); int before=audits();
        var start=new CountDownLatch(1);
        try (var executor=Executors.newFixedThreadPool(2)) {
            var results=new ArrayList<Future<Object>>();
            for (int i=0;i<2;i++) { final int attempt=i; results.add(executor.submit(() -> {
                login(administrator,true); start.await();
                try { return commands.renameSite(s.publicId(),"Synthetic Winner "+attempt,s.version()); }
                catch (ClientManagementException failure) { return failure.reason(); }
                finally { SecurityContextHolder.clearContext(); }
            })); }
            start.countDown(); var values=List.of(results.get(0).get(30,TimeUnit.SECONDS),results.get(1).get(30,TimeUnit.SECONDS));
            assertEquals(1,values.stream().filter(ClientSiteReference.class::isInstance).count());
            assertEquals(1,values.stream().filter(STALE_VERSION::equals).count());
        }
        assertEquals(before+1,audits()); assertEquals(1,queries.site(s.publicId()).orElseThrow().version());
    }

    @Test void siteActivationAndCompanyDeactivationSerializeOnParentWithoutLostSiteState() throws Exception {
        var c=company(); var s=commands.createSite(c.publicId(),"Synthetic Site"); var inactive=commands.deactivateSite(s.publicId(),0);
        var siteLocked=new CountDownLatch(1); var releaseSite=new CountDownLatch(1); var companyStarted=new CountDownLatch(1);
        doAnswer(call -> {
            io.github.vncntz.hris.sharedkernel.AuditRequest request=call.getArgument(0);
            if (request.action().equals("CLIENT_SITE_ACTIVATED")) { siteLocked.countDown(); assertTrue(releaseSite.await(10,TimeUnit.SECONDS)); }
            return call.callRealMethod();
        }).when(audit).record(any());
        try (var executor=Executors.newFixedThreadPool(2)) {
            var activate=executor.submit(() -> { login(administrator,true); try { return commands.activateSite(s.publicId(),inactive.version()); } finally { SecurityContextHolder.clearContext(); } });
            assertTrue(siteLocked.await(10,TimeUnit.SECONDS));
            var deactivate=executor.submit(() -> { login(administrator,true); companyStarted.countDown(); try { return commands.deactivateCompany(c.publicId(),c.version()); } finally { SecurityContextHolder.clearContext(); } });
            try { assertTrue(companyStarted.await(10,TimeUnit.SECONDS)); assertThrows(TimeoutException.class, () -> deactivate.get(200,TimeUnit.MILLISECONDS)); }
            finally { releaseSite.countDown(); }
            assertTrue(activate.get(30,TimeUnit.SECONDS).active()); assertFalse(deactivate.get(30,TimeUnit.SECONDS).active());
        } finally { releaseSite.countDown(); }
        var result=queries.site(s.publicId()).orElseThrow(); assertTrue(result.active()); assertFalse(result.effectiveActive());
    }

    @Test void administrationCompanyPagesHaveTotalOrderDistinctDuplicateNamesAndNoMissingRows() {
        var first = commands.createCompany("000 Synthetic browse duplicate");
        var second = commands.createCompany(first.displayName());
        var inactive = commands.deactivateCompany(first.publicId(), first.version());
        int before = audits();
        List<UUID> expected = jdbc.queryForList("SELECT BIN_TO_UUID(public_id) FROM client_company ORDER BY display_name, public_id", String.class)
            .stream().map(UUID::fromString).toList();
        var observed = new ArrayList<ClientCompanyReference>();
        int offset = 0;
        do {
            var page = administration.companies(offset, 2);
            assertTrue(page.rows().size() <= 2);
            assertEquals(offset + page.rows().size() < expected.size(), page.hasMore());
            observed.addAll(page.rows()); offset += page.rows().size();
            if (!page.hasMore()) { break; }
        } while (offset <= expected.size());
        assertEquals(expected, observed.stream().map(ClientCompanyReference::publicId).toList());
        assertEquals(expected.size(), new HashSet<>(observed.stream().map(ClientCompanyReference::publicId).toList()).size());
        assertTrue(observed.contains(inactive)); assertTrue(observed.contains(second));
        assertEquals(observed.subList(0, 2), administration.companies(0, 2).rows());
        assertEquals(new ClientCompanyPage(List.of(), false), administration.companies(expected.size(), 2));
        assertEquals(new ClientCompanyPage(List.of(), false), administration.companies(Integer.MAX_VALUE, 1));
        assertEquals(before, audits());
    }

    @Test void administrationSitePagesRestrictParentAndKeepDuplicateNamesAndLifecycleStates() {
        var parent = company(); var other = company();
        var first = commands.createSite(parent.publicId(), "Synthetic duplicate");
        var second = commands.createSite(parent.publicId(), first.displayName());
        var third = commands.createSite(parent.publicId(), "Synthetic later");
        commands.createSite(other.publicId(), first.displayName());
        commands.deactivateSite(second.publicId(), second.version());
        commands.deactivateCompany(parent.publicId(), parent.version());
        int before = audits();
        List<UUID> expected = jdbc.queryForList("SELECT BIN_TO_UUID(s.public_id) FROM client_site s JOIN client_company c ON c.id=s.company_id WHERE c.public_id=UUID_TO_BIN(?) ORDER BY s.display_name, s.public_id", String.class, parent.publicId().toString())
            .stream().map(UUID::fromString).toList();
        var observed = new ArrayList<ClientSiteReference>();
        for (int offset = 0; offset < expected.size(); offset++) {
            var page = administration.sites(parent.publicId(), offset, 1);
            assertEquals(1, page.rows().size()); assertEquals(offset < expected.size() - 1, page.hasMore());
            observed.addAll(page.rows());
        }
        assertEquals(expected, observed.stream().map(ClientSiteReference::publicId).toList());
        assertEquals(Set.of(first.publicId(), second.publicId(), third.publicId()), new HashSet<>(expected));
        assertTrue(observed.stream().allMatch(s -> s.companyPublicId().equals(parent.publicId()) && !s.companyActive() && !s.effectiveActive()));
        var inactive = observed.stream().filter(s -> s.publicId().equals(second.publicId())).findFirst().orElseThrow();
        assertFalse(inactive.active()); assertEquals(1, inactive.version());
        assertTrue(observed.stream().filter(s -> !s.publicId().equals(second.publicId())).allMatch(ClientSiteReference::active));
        assertEquals(observed, administration.sites(parent.publicId(), 0, 3).rows());
        assertEquals(new ClientSitePage(List.of(), false), administration.sites(parent.publicId(), 3, 2));
        assertEquals(new ClientSitePage(List.of(), false), administration.sites(company().publicId(), 0, 1));
        // Only the explicit empty Company creation above is a mutation.
        assertEquals(before + 1, audits());
        bounded(NOT_FOUND, () -> administration.sites(UUID.randomUUID(), 0, 1));
    }

    @Test void administrationReadsUseAuthenticatedAuthorityBeforeInvalidArguments() {
        int before = audits();
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> administration.companies(-1, 0));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> administration.sites(null, -1, 0));
        login(administrator, false);
        assertThrows(AccessDeniedException.class, () -> administration.companies(0, 1));
        assertThrows(AccessDeniedException.class, () -> administration.sites(UUID.randomUUID(), 0, 1));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated(new AccountPrincipal(administrator,"synthetic"),null));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> administration.companies(0, 1));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> administration.sites(UUID.randomUUID(), 0, 1));
        login(administrator, true);
        bounded(INVALID_PAGE, () -> administration.companies(-1, 1));
        bounded(INVALID_PAGE, () -> administration.sites(UUID.randomUUID(), 0, 201));
        bounded(INVALID_TARGET, () -> administration.sites(null, 0, 1));
        assertEquals(before, audits());
    }

    @Test void administrationReadsObserveCommittedCompanyAndSiteVersionsDespiteAmbientSnapshot() throws Exception {
        var parent = commands.createCompany("000 Synthetic stale browse");
        var site = commands.createSite(parent.publicId(), "Synthetic stale browse");
        int before = audits();
        var ambient = new TransactionTemplate(transactions);
        ambient.setIsolationLevel(TransactionTemplate.ISOLATION_REPEATABLE_READ); ambient.setTimeout(30);
        try (var executor = Executors.newSingleThreadExecutor()) {
            ambient.executeWithoutResult(status -> {
                assertEquals(1, jdbc.queryForObject("SELECT active FROM client_company WHERE public_id=UUID_TO_BIN(?)", Integer.class, parent.publicId().toString()));
                assertEquals(1, jdbc.queryForObject("SELECT active FROM client_site WHERE public_id=UUID_TO_BIN(?)", Integer.class, site.publicId().toString()));
                var change = executor.submit(() -> {
                    login(administrator, true);
                    try {
                        commands.deactivateSite(site.publicId(), site.version());
                        return commands.deactivateCompany(parent.publicId(), parent.version());
                    } finally { SecurityContextHolder.clearContext(); }
                });
                try { assertFalse(change.get(10, TimeUnit.SECONDS).active()); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
                catch (ExecutionException | TimeoutException failure) { throw new AssertionError(failure); }
                assertEquals(1, jdbc.queryForObject("SELECT active FROM client_company WHERE public_id=UUID_TO_BIN(?)", Integer.class, parent.publicId().toString()));
                var current = administration.companies(0, ClientAdministrationQueries.MAX_LIMIT).rows().stream()
                    .filter(c -> c.publicId().equals(parent.publicId())).findFirst().orElseThrow();
                assertFalse(current.active()); assertEquals(parent.version() + 1, current.version());
                var currentSite = administration.sites(parent.publicId(), 0, 1).rows().getFirst();
                assertEquals(site.publicId(), currentSite.publicId()); assertFalse(currentSite.active());
                assertFalse(currentSite.companyActive()); assertFalse(currentSite.effectiveActive()); assertEquals(site.version() + 1, currentSite.version());
                assertEquals(1, jdbc.queryForObject("SELECT active FROM client_site WHERE public_id=UUID_TO_BIN(?)", Integer.class, site.publicId().toString()));
            });
        }
        assertEquals(before + 2, audits());
    }
}
