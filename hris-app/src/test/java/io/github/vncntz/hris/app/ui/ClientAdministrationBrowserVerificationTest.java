package io.github.vncntz.hris.app.ui;

import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.vncntz.hris.clientmanagement.ClientAdministrationQueries;
import io.github.vncntz.hris.clientmanagement.ClientCompanyPage;
import io.github.vncntz.hris.clientmanagement.ClientCompanyReference;
import io.github.vncntz.hris.clientmanagement.ClientManagementException;
import io.github.vncntz.hris.clientmanagement.ClientManagementService;
import io.github.vncntz.hris.clientmanagement.ClientReferences;
import io.github.vncntz.hris.clientmanagement.ClientSitePage;
import io.github.vncntz.hris.clientmanagement.ClientSiteReference;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationService;
import io.github.vncntz.hris.identityaccess.AccountCreationService;
import io.github.vncntz.hris.identityaccess.AccountLifecycleService;
import io.github.vncntz.hris.identityaccess.AccountPrincipal;
import io.github.vncntz.hris.identityaccess.AccountRoleAssignmentService;
import io.github.vncntz.hris.identityaccess.AccountSessionRegistrationService;
import io.github.vncntz.hris.identityaccess.AuthenticatedAccount;
import io.github.vncntz.hris.identityaccess.CredentialReauthenticationService;
import io.github.vncntz.hris.identityaccess.MfaAdministrationService;
import io.github.vncntz.hris.identityaccess.PasswordChangeService;
import io.github.vncntz.hris.identityaccess.PasswordResetService;
import io.github.vncntz.hris.identityaccess.RoleAdministrationService;
import io.github.vncntz.hris.identityaccess.RoleGenerations;
import io.github.vncntz.hris.platformoperations.AgencyConfigurationService;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "vaadin.productionMode=true",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
        }
)
class ClientAdministrationBrowserVerificationTest {
    private static final Path EDGE_EXE = Path.of("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
    private static final Path SCREENSHOT_DIR = Path.of(System.getProperty("java.io.tmpdir"), "hris-ui-verification");

    @MockitoBean private ClientManagementService clientManagement;
    @MockitoBean private ClientReferences clientReferences;
    @MockitoBean private ClientAdministrationQueries clientAdministrationQueries;
    @MockitoBean private MfaAdministrationService mfaAdministration;
    @MockitoBean private RoleAdministrationService roleAdministration;
    @MockitoBean private AuditRecorder auditRecorder;
    @MockitoBean private AccountAuthenticationService accountAuthenticationService;
    @MockitoBean private AccountCreationService accountCreationService;
    @MockitoBean private CredentialReauthenticationService credentialReauthenticationService;
    @MockitoBean private PasswordChangeService passwordChangeService;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private AccountLifecycleService accountLifecycleService;
    @MockitoBean private AccountRoleAssignmentService accountRoleAssignmentService;
    @MockitoBean private AccountSessionRegistrationService sessionRegistrationService;
    @MockitoBean private AgencyConfigurationService agencyConfigurationService;

    @Value("${local.server.port}")
    private int port;

    private final UUID company1Id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID company2Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID site1Id = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private final UUID site2Id = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private final UUID site3Id = UUID.fromString("00000000-0000-0000-0000-000000000021");

    private final SyntheticClientStore syntheticStore = new SyntheticClientStore();

    @BeforeAll
    static void checkPrerequisites() {
        Assumptions.assumeTrue(Files.isRegularFile(EDGE_EXE), "Edge browser executable must exist for headless verification");
        try {
            Files.createDirectories(SCREENSHOT_DIR);
        } catch (Exception ignored) {
        }
    }

    @BeforeEach
    void setUpDefaults() {
        doAnswer(inv -> {
            Runnable r = inv.getArgument(1, Runnable.class);
            if (r != null) r.run();
            return null;
        }).when(sessionRegistrationService).register(any(), any());

        syntheticStore.resetToDefaults(company1Id, company2Id, site1Id, site2Id, site3Id);

        when(clientAdministrationQueries.companies(anyInt(), anyInt()))
                .thenAnswer(inv -> syntheticStore.getCompanies(inv.getArgument(0), inv.getArgument(1)));
        when(clientAdministrationQueries.sites(any(), anyInt(), anyInt()))
                .thenAnswer(inv -> syntheticStore.getSites(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));

        when(clientManagement.createCompany(anyString()))
                .thenAnswer(inv -> syntheticStore.createCompany(inv.getArgument(0)));
        when(clientManagement.renameCompany(any(), anyString(), anyLong()))
                .thenAnswer(inv -> syntheticStore.renameCompany(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        when(clientManagement.activateCompany(any(), anyLong()))
                .thenAnswer(inv -> syntheticStore.activateCompany(inv.getArgument(0), inv.getArgument(1)));
        when(clientManagement.deactivateCompany(any(), anyLong()))
                .thenAnswer(inv -> syntheticStore.deactivateCompany(inv.getArgument(0), inv.getArgument(1)));

        when(clientManagement.createSite(any(), anyString()))
                .thenAnswer(inv -> syntheticStore.createSite(inv.getArgument(0), inv.getArgument(1)));
        when(clientManagement.renameSite(any(), anyString(), anyLong()))
                .thenAnswer(inv -> syntheticStore.renameSite(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
        when(clientManagement.activateSite(any(), anyLong()))
                .thenAnswer(inv -> syntheticStore.activateSite(inv.getArgument(0), inv.getArgument(1)));
        when(clientManagement.deactivateSite(any(), anyLong()))
                .thenAnswer(inv -> syntheticStore.deactivateSite(inv.getArgument(0), inv.getArgument(1)));
    }

    @Test
    void anonymousAccessToClientsRedirectsToLogin() throws Exception {
        String url = "http://127.0.0.1:" + port + "/clients";
        Path screenshot = SCREENSHOT_DIR.resolve("clients-anonymous-redirect.png");

        try (EdgeCdpSession session = EdgeCdpSession.start("1280,800")) {
            session.navigate(url);
            session.waitForSelector("vaadin-login-form", 8000);
            session.captureScreenshot(screenshot);

            String path = session.eval("window.location.pathname");
            assertTrue("/login".equals(path) || path.endsWith("/login"),
                    "Anonymous access to /clients must redirect to /login, but was: " + path);

            String hasLoginCard = session.eval("String(!!document.querySelector('.hris-login-card'))");
            assertEquals("true", hasLoginCard, "Login card must be rendered after redirect");
        }
    }

    @Test
    void authenticatedUserWithoutClientAdminIsDeniedAccessAndMenuIsHidden() throws Exception {
        AccountPrincipal principal = new AccountPrincipal(UUID.randomUUID(), "synthetic.user");
        AuthenticatedAccount account = new AuthenticatedAccount(
                principal,
                List.of(), // No authorities
                1L,
                new RoleGenerations(Map.of())
        );

        when(accountAuthenticationService.authenticate(eq("synthetic.user"), eq("ValidPass123!")))
                .thenReturn(Optional.of(account));
        when(accountAuthenticationService.authenticate(eq("synthetic.user"), eq("ValidPass123!"), any()))
                .thenReturn(Optional.of(account));

        try (EdgeCdpSession session = EdgeCdpSession.start("1280,800")) {
            login(session, "synthetic.user", "ValidPass123!");

            // Verify shell reached
            session.waitForSelector("#hris-current-user", 8000);
            String userDisplay = session.eval("document.querySelector('#hris-current-user')?.textContent");
            assertTrue(userDisplay != null && userDisplay.contains("synthetic.user"),
                    "Shell must display synthetic.user");

            // Verify Clients is NOT in navigation
            String hasClientsInNav = session.eval("""
                    (() => {
                        const items = Array.from(document.querySelectorAll('vaadin-side-nav-item'));
                        return String(items.some(item => (item.getAttribute('path') || '').includes('clients')
                                || (item.textContent || '').includes('Clients')));
                    })()
                    """);
            assertEquals("false", hasClientsInNav, "Clients item must be hidden from side navigation for non-admin");

            // Attempt direct navigation to /clients
            session.navigate("http://127.0.0.1:" + port + "/clients");
            Thread.sleep(1000);
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-denied-non-admin.png"));

            // Must NOT render ClientAdministrationView
            String hasClientsView = session.eval("String(!!document.querySelector('#hris-clients-view'))");
            assertEquals("false", hasClientsView, "Client administration view must not be rendered for non-admin");
        }
    }

    @Test
    void clientAdminDirectAccessDesktopResponsiveViewportsAndInteractions() throws Exception {
        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            loginAsAdmin(session);

            // Verify Clients item is present in navigation
            String hasClientsInNav = session.eval("""
                    (() => {
                        const items = Array.from(document.querySelectorAll('vaadin-side-nav-item'));
                        return String(items.some(item => (item.getAttribute('path') || '').includes('clients')
                                || (item.textContent || '').includes('Clients')));
                    })()
                    """);
            assertEquals("true", hasClientsInNav, "Clients item must be present in side navigation for client:admin");

            navigateToClients(session);
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-desktop-1920x1080.png"));

            // Verify single H1 page heading owned by MainLayout
            String h1Count = session.eval("String(document.querySelectorAll('h1').length)");
            assertEquals("1", h1Count, "Must have exactly one H1 heading on the page");

            // Verify H2 section headings
            String h2Count = session.eval("String(document.querySelectorAll('h2').length)");
            assertTrue(Integer.parseInt(h2Count) >= 2, "Must have H2 section headings for Companies and Sites");

            // Verify no horizontal overflow in 1920x1080
            String overflowDesktop = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertEquals("true", overflowDesktop, "Desktop viewport must not exhibit horizontal overflow");

            // Verify company section and site section exist
            String companySection = session.eval("String(!!document.querySelector('#hris-company-section'))");
            String siteSection = session.eval("String(!!document.querySelector('#hris-site-section'))");
            assertEquals("true", companySection, "Company administration section must be rendered");
            assertEquals("true", siteSection, "Site administration section must be rendered");

            // Verify dialog autofocus and Escape key closure
            clickElement(session, "#hris-create-company-btn");
            assertTrue(session.waitForSelector("#hris-create-company-dialog", 4000));
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-dialog-new-company.png"));

            String dialogTitle = session.eval("document.querySelector('#hris-create-company-dialog')?.getAttribute('header-title') || document.querySelector('#hris-create-company-dialog')?.headerTitle");
            assertTrue(dialogTitle != null && dialogTitle.contains("New Company"),
                    "Dialog header must be New Company: " + dialogTitle);

            String hasAutofocus = session.eval("""
                    (() => {
                        const f = document.querySelector('#hris-company-name-input');
                        if (!f) return 'false';
                        return String(Boolean(f.hasAttribute('autofocus') || f.autofocus || f.querySelector('input')?.autofocus || f.contains(document.activeElement)));
                    })()
                    """);
            assertEquals("true", hasAutofocus, "Dialog input must have autofocus");

            // Press Escape key to close dialog
            session.eval("""
                    (() => {
                        const evt = new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true, composed: true });
                        (document.activeElement || window).dispatchEvent(evt);
                    })()
                    """);
            assertTrue(session.waitForCondition("!document.querySelector('#hris-create-company-dialog')?.opened", 4000),
                    "Escape key must close dialog");

            // Test duplicate-submit prevention and validation feedback
            clickElement(session, "#hris-create-company-btn");
            assertTrue(session.waitForSelector("#hris-create-company-dialog", 4000));
            clickElement(session, "#hris-save-company-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-company-name-input').hasAttribute('invalid')", 4000),
                    "Blank submit must mark field invalid without closing dialog");

            // Close dialog via cancel
            clickElement(session, "#hris-cancel-company-btn");
            assertTrue(session.waitForCondition("!document.querySelector('#hris-create-company-dialog')?.opened", 4000));

            // Test narrow mobile viewport (375x667)
            session.setViewport(375, 667);
            Thread.sleep(300);
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-narrow-375x667.png"));

            String overflowNarrow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertEquals("true", overflowNarrow, "Narrow viewport (375x667) must not exhibit horizontal overflow");

            // Verify absence of sensitive leakages or stack traces in DOM
            String dom = session.eval("document.documentElement.outerHTML").toLowerCase(Locale.ROOT);
            assertFalse(dom.contains("sqlexception"), "No SQL exception leakage in DOM");
            assertFalse(dom.contains("stacktrace"), "No stack traces in DOM");

            // Verify 0 browser console errors
            List<String> consoleErrors = session.getConsoleErrors();
            assertTrue(consoleErrors.isEmpty(), "Browser console must report zero application errors: " + consoleErrors);
        }
    }

    @Test
    void clientAdminCompanyAndSiteBrowsingAndPagingWorkflows() throws Exception {
        syntheticStore.populatePagingRecords(company1Id);
        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            loginAsAdmin(session);
            navigateToClients(session);

            // Verify Company page 1
            String compPage1 = session.eval("document.querySelector('#hris-company-page-info')?.textContent");
            assertTrue(compPage1 != null && compPage1.contains("Page 1"), "Must start on Company Page 1");
            assertEquals("true", session.eval("String(document.querySelector('#hris-company-prev-btn').hasAttribute('disabled'))"));
            assertEquals("false", session.eval("String(document.querySelector('#hris-company-next-btn').hasAttribute('disabled'))"));

            // Navigate to Company Page 2
            clickElement(session, "#hris-company-next-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-company-page-info')?.textContent.includes('Page 2')", 5000));
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-paging-company.png"));
            assertEquals("false", session.eval("String(document.querySelector('#hris-company-prev-btn').hasAttribute('disabled'))"));
            assertEquals("true", session.eval("String(document.querySelector('#hris-company-next-btn').hasAttribute('disabled'))"));

            // Navigate back to Company Page 1
            clickElement(session, "#hris-company-prev-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-company-page-info')?.textContent.includes('Page 1')", 5000));

            // Select Alpha Corp row
            clickGridRow(session, "#hris-company-grid", "Alpha Corp");
            assertTrue(session.waitForCondition("document.querySelector('#hris-sites-context')?.textContent.includes('Sites for Alpha Corp')", 5000));
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-company-selected-sites.png"));

            // Verify company action buttons enabled
            assertEquals("false", session.eval("String(document.querySelector('#hris-rename-company-btn').hasAttribute('disabled'))"));
            assertEquals("false", session.eval("String(document.querySelector('#hris-toggle-company-btn').hasAttribute('disabled'))"));
            assertEquals("false", session.eval("String(document.querySelector('#hris-create-site-btn').hasAttribute('disabled'))"));

            // Verify Site page 1 loaded for Alpha Corp (16 sites total)
            String sitePage1 = session.eval("document.querySelector('#hris-site-page-info')?.textContent");
            assertTrue(sitePage1 != null && sitePage1.contains("Page 1"), "Must start on Site Page 1");
            assertEquals("true", session.eval("String(document.querySelector('#hris-site-prev-btn').hasAttribute('disabled'))"));
            assertEquals("false", session.eval("String(document.querySelector('#hris-site-next-btn').hasAttribute('disabled'))"));

            // Navigate to Site Page 2
            clickElement(session, "#hris-site-next-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-site-page-info')?.textContent.includes('Page 2')", 5000));
            assertEquals("false", session.eval("String(document.querySelector('#hris-site-prev-btn').hasAttribute('disabled'))"));
            assertEquals("true", session.eval("String(document.querySelector('#hris-site-next-btn').hasAttribute('disabled'))"));

            // Navigate back to Site Page 1
            clickElement(session, "#hris-site-prev-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-site-page-info')?.textContent.includes('Page 1')", 5000));

            // Select Alpha HQ site row
            clickGridRow(session, "#hris-site-grid", "Alpha HQ");
            assertTrue(session.waitForCondition("!document.querySelector('#hris-rename-site-btn').hasAttribute('disabled')", 5000));
            assertEquals("false", session.eval("String(document.querySelector('#hris-toggle-site-btn').hasAttribute('disabled'))"));

            assertTrue(session.getConsoleErrors().isEmpty(), "No console errors during browsing and paging: " + session.getConsoleErrors());
        }
    }

    @Test
    void clientAdminCompanyAndSiteCreateAndRenameWorkflows() throws Exception {
        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            loginAsAdmin(session);
            navigateToClients(session);

            // Create Company
            clickElement(session, "#hris-create-company-btn");
            assertTrue(session.waitForSelector("#hris-create-company-dialog", 5000));
            setInputValue(session, "#hris-company-name-input", "Zeta Enterprises");
            clickElement(session, "#hris-save-company-btn");

            assertTrue(session.waitForCondition("document.querySelector('#hris-company-feedback')?.textContent.includes('Zeta Enterprises') && !document.querySelector('#hris-company-feedback').classList.contains('hris-hidden')", 5000),
                    "Feedback must confirm company creation");

            // Select created company and Rename
            clickGridRow(session, "#hris-company-grid", "Zeta Enterprises");
            assertTrue(session.waitForCondition("!document.querySelector('#hris-rename-company-btn').hasAttribute('disabled')", 5000));

            clickElement(session, "#hris-rename-company-btn");
            assertTrue(session.waitForSelector("#hris-rename-company-dialog", 5000));
            assertEquals("Zeta Enterprises", session.eval("document.querySelector('#hris-rename-company-input')?.value"));

            setInputValue(session, "#hris-rename-company-input", "Zeta Global");
            clickElement(session, "#hris-save-rename-company-btn");

            assertTrue(session.waitForCondition("document.querySelector('#hris-company-feedback')?.textContent.includes('Zeta Global')", 5000),
                    "Feedback must confirm company rename");

            // Create Site under active company Alpha Corp
            clickGridRow(session, "#hris-company-grid", "Alpha Corp");
            assertTrue(session.waitForCondition("!document.querySelector('#hris-create-site-btn').hasAttribute('disabled')", 5000));

            clickElement(session, "#hris-create-site-btn");
            assertTrue(session.waitForSelector("#hris-create-site-dialog", 5000));
            String parentContext = session.eval("document.querySelector('#hris-create-site-dialog')?.textContent");
            assertTrue(parentContext.contains("Alpha Corp"), "Dialog must display parent company context");

            setInputValue(session, "#hris-site-name-input", "Alpha Innovation Lab");
            clickElement(session, "#hris-save-site-btn");

            assertTrue(session.waitForCondition("document.querySelector('#hris-site-feedback')?.textContent.includes('Alpha Innovation Lab')", 5000),
                    "Feedback must confirm site creation");

            // Rename Site
            clickGridRow(session, "#hris-site-grid", "Alpha Innovation Lab");
            assertTrue(session.waitForCondition("!document.querySelector('#hris-rename-site-btn').hasAttribute('disabled')", 5000));

            clickElement(session, "#hris-rename-site-btn");
            assertTrue(session.waitForSelector("#hris-rename-site-dialog", 5000));

            setInputValue(session, "#hris-rename-site-input", "Alpha Research Hub");
            clickElement(session, "#hris-save-rename-site-btn");

            assertTrue(session.waitForCondition("document.querySelector('#hris-site-feedback')?.textContent.includes('Alpha Research Hub')", 5000),
                    "Feedback must confirm site rename");

            assertTrue(session.getConsoleErrors().isEmpty(), "No console errors during create/rename: " + session.getConsoleErrors());
        }
    }

    @Test
    void clientAdminLifecycleAndStoredVsEffectiveSiteStateUnderInactiveCompany() throws Exception {
        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            loginAsAdmin(session);
            navigateToClients(session);

            // Select Alpha Corp
            clickGridRow(session, "#hris-company-grid", "Alpha Corp");
            assertTrue(session.waitForCondition("document.querySelector('#hris-sites-context')?.textContent.includes('Sites for Alpha Corp')", 5000));

            // Deactivate Alpha Corp
            clickElement(session, "#hris-toggle-company-btn");
            assertTrue(session.waitForSelector("#hris-lifecycle-dialog", 5000));

            String warning = session.eval("document.querySelector('#hris-lifecycle-message')?.textContent");
            assertTrue(warning.contains("effectively inactive"), "Warning must mention effectively inactive");
            assertTrue(warning.contains("preserve each site's stored active status"), "Warning must mention preserving stored status");

            clickElement(session, "#hris-confirm-lifecycle-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-company-feedback')?.textContent.includes('deactivated')", 5000));

            // Verify company button now says Activate
            String toggleText = session.eval("document.querySelector('#hris-toggle-company-btn')?.textContent?.trim()");
            assertEquals("Activate", toggleText);

            // Verify site creation disabled with explanatory hint
            assertEquals("true", session.eval("String(document.querySelector('#hris-create-site-btn').hasAttribute('disabled'))"));
            assertEquals("false", session.eval("String(document.querySelector('#hris-create-site-hint').classList.contains('hris-hidden'))"));

            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-effective-inactive-sites.png"));

            // Verify Alpha HQ site stored status is Active, effective status is Inactive (Company inactive)
            String hasEffectivelyInactiveBadge = session.eval("String(!!document.querySelector('.hris-status-effectively-inactive'))");
            assertEquals("true", hasEffectivelyInactiveBadge, "Must render effectively inactive badge");

            // Select Alpha HQ (stored active): Deactivate site is enabled
            clickGridRow(session, "#hris-site-grid", "Alpha HQ");
            assertTrue(session.waitForCondition("document.querySelector('#hris-toggle-site-btn')?.textContent?.includes('Deactivate')", 5000));
            assertEquals("false", session.eval("String(document.querySelector('#hris-toggle-site-btn').hasAttribute('disabled'))"),
                    "Deactivate site must remain enabled under inactive company");

            // Select Alpha Warehouse (stored inactive): Activate site is disabled with hint
            clickGridRow(session, "#hris-site-grid", "Alpha Warehouse");
            assertTrue(session.waitForCondition("document.querySelector('#hris-toggle-site-btn')?.textContent?.includes('Activate')", 5000));
            assertEquals("true", session.eval("String(document.querySelector('#hris-toggle-site-btn').hasAttribute('disabled'))"),
                    "Activate site must be disabled under inactive company");
            String hint = session.eval("document.querySelector('#hris-site-action-hint')?.textContent");
            assertTrue(hint != null && hint.contains("Cannot activate site because its company is inactive"),
                    "Site action hint must explain inactive company");

            // Reactivate Alpha Corp
            clickElement(session, "#hris-toggle-company-btn");
            assertTrue(session.waitForSelector("#hris-lifecycle-dialog", 5000));
            clickElement(session, "#hris-confirm-lifecycle-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-company-feedback')?.textContent.includes('activated')", 5000));

            // Effective status returns to Active
            assertEquals("false", session.eval("String(document.querySelector('#hris-create-site-btn').hasAttribute('disabled'))"));
            String activeBadge = session.eval("String(document.querySelectorAll('.hris-status-active').length > 0)");
            assertEquals("true", activeBadge);

            // Exercise site lifecycle deactivation and reactivation
            clickGridRow(session, "#hris-site-grid", "Alpha HQ");
            clickElement(session, "#hris-toggle-site-btn");
            assertTrue(session.waitForSelector("#hris-lifecycle-dialog", 5000));
            clickElement(session, "#hris-confirm-lifecycle-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-site-feedback')?.textContent.includes('deactivated')", 5000));

            clickElement(session, "#hris-toggle-site-btn");
            assertTrue(session.waitForSelector("#hris-lifecycle-dialog", 5000));
            clickElement(session, "#hris-confirm-lifecycle-btn");
            assertTrue(session.waitForCondition("document.querySelector('#hris-site-feedback')?.textContent.includes('activated')", 5000));

            assertTrue(session.getConsoleErrors().isEmpty(), "No console errors during lifecycle checks: " + session.getConsoleErrors());
        }
    }

    @Test
    void clientAdminStaleVersionAndRepresentativeSafeErrorHandling() throws Exception {
        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            loginAsAdmin(session);
            navigateToClients(session);

            clickGridRow(session, "#hris-company-grid", "Alpha Corp");
            assertTrue(session.waitForCondition("!document.querySelector('#hris-rename-company-btn').hasAttribute('disabled')", 5000));

            // Exercise STALE_VERSION error
            clickElement(session, "#hris-rename-company-btn");
            assertTrue(session.waitForSelector("#hris-rename-company-dialog", 5000));
            setInputValue(session, "#hris-rename-company-input", "Alpha Conflicted");
            syntheticStore.simulateStaleVersion.set(true);
            clickElement(session, "#hris-save-rename-company-btn");

            assertTrue(session.waitForCondition("document.querySelector('#hris-company-feedback')?.textContent.includes('modified by another operation') && document.querySelector('#hris-company-feedback').classList.contains('hris-feedback-error')", 5000),
                    "Stale version feedback must be displayed");
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-stale-version-feedback.png"));

            // Exercise representative safe PERSISTENCE_FAILED error without infrastructure leakage
            assertTrue(session.waitForCondition("!document.querySelector('#hris-rename-company-dialog')?.opened", 4000));
            if ("true".equals(session.eval("String(document.querySelector('#hris-rename-company-btn').hasAttribute('disabled'))"))) {
                clickGridRow(session, "#hris-company-grid", "Alpha Corp");
                assertTrue(session.waitForCondition("!document.querySelector('#hris-rename-company-btn').hasAttribute('disabled')", 5000));
            }
            clickElement(session, "#hris-rename-company-btn");
            assertTrue(session.waitForSelector("#hris-rename-company-dialog", 5000));
            setInputValue(session, "#hris-rename-company-input", "Alpha Outage Name");
            syntheticStore.simulatePersistenceFailure.set(true);
            clickElement(session, "#hris-save-rename-company-btn");

            assertTrue(session.waitForCondition("document.querySelector('#hris-company-feedback')?.textContent.includes('The operation could not be completed') && document.querySelector('#hris-company-feedback').classList.contains('hris-feedback-error')", 5000),
                    "Persistence failure must produce safe user feedback");

            String dom = session.eval("document.documentElement.outerHTML").toLowerCase(Locale.ROOT);
            assertFalse(dom.contains("sqlexception"), "No SQL leakage in DOM");
            assertFalse(dom.contains("stacktrace"), "No stack traces in DOM");
            assertFalse(dom.contains("exception"), "No raw exception names in DOM text");

            assertTrue(session.getConsoleErrors().isEmpty(), "No console errors during error handling: " + session.getConsoleErrors());
        }
    }

    private void loginAsAdmin(EdgeCdpSession session) throws Exception {
        AccountPrincipal principal = new AccountPrincipal(UUID.randomUUID(), "synthetic.admin");
        AuthenticatedAccount account = new AuthenticatedAccount(
                principal,
                List.of("client:admin"),
                1L,
                new RoleGenerations(Map.of())
        );

        when(accountAuthenticationService.authenticate(eq("synthetic.admin"), eq("ValidPass123!")))
                .thenReturn(Optional.of(account));
        when(accountAuthenticationService.authenticate(eq("synthetic.admin"), eq("ValidPass123!"), any()))
                .thenReturn(Optional.of(account));

        login(session, "synthetic.admin", "ValidPass123!");
        session.waitForSelector("#hris-current-user", 8000);
    }

    private void navigateToClients(EdgeCdpSession session) throws Exception {
        session.navigate("http://127.0.0.1:" + port + "/clients");
        session.waitForSelector("#hris-clients-view", 8000);
    }

    private void clickElement(EdgeCdpSession session, String selector) throws Exception {
        session.waitForCondition(String.format("Boolean(document.querySelector('%s'))", selector), 5000);
        session.eval(String.format("""
                (() => {
                    const el = document.querySelector('%s');
                    if (el) {
                        el.click();
                    }
                })()
                """, selector));
    }

    private void setInputValue(EdgeCdpSession session, String selector, String value) throws Exception {
        session.waitForCondition(String.format("Boolean(document.querySelector('%s'))", selector), 5000);
        session.eval(String.format("""
                (() => {
                    const field = document.querySelector('%s');
                    if (field) {
                        field.value = '%s';
                        const input = field.querySelector('input');
                        if (input) {
                            input.value = '%s';
                            input.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
                        }
                        field.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
                        field.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
                    }
                })()
                """, selector, escapeJs(value), escapeJs(value)));
    }

    private void clickGridRow(EdgeCdpSession session, String gridSelector, String textMatch) throws Exception {
        session.waitForCondition(String.format("""
                (() => {
                    const grid = document.querySelector('%s');
                    if (!grid) return false;
                    const cells = Array.from(grid.querySelectorAll('vaadin-grid-cell-content'));
                    return cells.some(c => (c.textContent || '').includes('%s'));
                })()
                """, gridSelector, escapeJs(textMatch)), 5000);

        session.eval(String.format("""
                (() => {
                    const grid = document.querySelector('%s');
                    if (!grid) return;
                    const cells = Array.from(grid.querySelectorAll('vaadin-grid-cell-content'));
                    const target = cells.find(c => (c.textContent || '').includes('%s'));
                    if (target) {
                        target.click();
                        target.dispatchEvent(new MouseEvent('click', { bubbles: true, composed: true }));
                        const slot = target.assignedSlot;
                        if (slot) {
                            slot.dispatchEvent(new MouseEvent('click', { bubbles: true, composed: true }));
                            const tr = slot.closest('tr');
                            if (tr) {
                                tr.dispatchEvent(new MouseEvent('click', { bubbles: true, composed: true }));
                                if (tr._item) {
                                    grid.activeItem = tr._item;
                                }
                            }
                        }
                    }
                })()
                """, gridSelector, escapeJs(textMatch)));
        Thread.sleep(200);
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void login(EdgeCdpSession session, String username, String password) throws Exception {
        session.navigate("http://127.0.0.1:" + port + "/login");
        session.waitForSelector("vaadin-login-form", 8000);

        session.eval(String.format("""
                (() => {
                    const u = document.querySelector('#vaadinLoginUsername');
                    if (u) {
                        u.value = '%s';
                        const input = u.querySelector('input');
                        if (input) {
                            input.value = '%s';
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    }

                    const p = document.querySelector('#vaadinLoginPassword');
                    if (p) {
                        p.value = '%s';
                        const input = p.querySelector('input');
                        if (input) {
                            input.value = '%s';
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    }

                    const form = document.querySelector('vaadin-login-form');
                    if (form && typeof form.submit === 'function') {
                        form.submit();
                    } else {
                        const submit = document.querySelector('vaadin-button[slot=submit]');
                        if (submit) submit.click();
                    }
                })()
                """, username, username, password, password));
    }

    static class SyntheticClientStore {
        final List<SyntheticCompany> companies = new CopyOnWriteArrayList<>();
        final List<SyntheticSite> sites = new CopyOnWriteArrayList<>();
        final AtomicBoolean simulateStaleVersion = new AtomicBoolean(false);
        final AtomicBoolean simulatePersistenceFailure = new AtomicBoolean(false);

        static class SyntheticCompany {
            final UUID id;
            String name;
            boolean active;
            long version;

            SyntheticCompany(UUID id, String name, boolean active, long version) {
                this.id = id;
                this.name = name;
                this.active = active;
                this.version = version;
            }

            ClientCompanyReference toReference() {
                return new ClientCompanyReference(id, name, active, version);
            }
        }

        static class SyntheticSite {
            final UUID id;
            final UUID companyId;
            String name;
            boolean active;
            long version;

            SyntheticSite(UUID id, UUID companyId, String name, boolean active, long version) {
                this.id = id;
                this.companyId = companyId;
                this.name = name;
                this.active = active;
                this.version = version;
            }

            ClientSiteReference toReference(boolean companyActive) {
                return new ClientSiteReference(id, companyId, name, active, companyActive, version);
            }
        }

        void resetToDefaults(UUID c1Id, UUID c2Id, UUID s1Id, UUID s2Id, UUID s3Id) {
            companies.clear();
            sites.clear();
            simulateStaleVersion.set(false);
            simulatePersistenceFailure.set(false);

            // Add Alpha Corp (active) and Beta Ltd (inactive)
            companies.add(new SyntheticCompany(c1Id, "Alpha Corp", true, 1L));
            companies.add(new SyntheticCompany(c2Id, "Beta Ltd", false, 1L));
            // Add sites for Alpha Corp (c1Id): s1 (active), s2 (inactive)
            sites.add(new SyntheticSite(s1Id, c1Id, "Alpha HQ", true, 1L));
            sites.add(new SyntheticSite(s2Id, c1Id, "Alpha Warehouse", false, 1L));

            // Add site for Beta Ltd (c2Id)
            sites.add(new SyntheticSite(s3Id, c2Id, "Beta Branch", true, 1L));
        }

        void populatePagingRecords(UUID c1Id) {
            for (int i = 3; i <= 16; i++) {
                companies.add(new SyntheticCompany(
                        UUID.fromString(String.format("00000000-0000-0000-0000-%012d", i)),
                        "Company " + i,
                        true,
                        1L
                ));
            }
            for (int i = 3; i <= 16; i++) {
                sites.add(new SyntheticSite(
                        UUID.fromString(String.format("00000000-0000-0000-0001-%012d", i)),
                        c1Id,
                        "Alpha Branch " + i,
                        true,
                        1L
                ));
            }
        }

        ClientCompanyPage getCompanies(int offset, int limit) {
            if (offset >= companies.size()) {
                return new ClientCompanyPage(List.of(), false);
            }
            int toIndex = Math.min(offset + limit, companies.size());
            List<ClientCompanyReference> rows = companies.subList(offset, toIndex).stream()
                    .map(SyntheticCompany::toReference)
                    .toList();
            boolean hasMore = toIndex < companies.size();
            return new ClientCompanyPage(rows, hasMore);
        }

        ClientSitePage getSites(UUID companyId, int offset, int limit) {
            SyntheticCompany company = companies.stream()
                    .filter(c -> c.id.equals(companyId))
                    .findFirst()
                    .orElse(null);
            boolean companyActive = company != null && company.active;

            List<SyntheticSite> companySites = sites.stream()
                    .filter(s -> s.companyId.equals(companyId))
                    .toList();

            if (offset >= companySites.size()) {
                return new ClientSitePage(List.of(), false);
            }
            int toIndex = Math.min(offset + limit, companySites.size());
            List<ClientSiteReference> rows = companySites.subList(offset, toIndex).stream()
                    .map(s -> s.toReference(companyActive))
                    .toList();
            boolean hasMore = toIndex < companySites.size();
            return new ClientSitePage(rows, hasMore);
        }

        ClientCompanyReference createCompany(String name) {
            checkSimulatedFailure();
            UUID id = UUID.randomUUID();
            SyntheticCompany comp = new SyntheticCompany(id, name, true, 1L);
            companies.add(comp);
            return comp.toReference();
        }

        ClientCompanyReference renameCompany(UUID id, String newName, long version) {
            checkSimulatedFailure();
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(id)).findFirst().orElseThrow();
            if (comp.version != version) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            comp.name = newName;
            comp.version++;
            return comp.toReference();
        }

        ClientCompanyReference activateCompany(UUID id, long version) {
            checkSimulatedFailure();
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(id)).findFirst().orElseThrow();
            if (comp.version != version) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            comp.active = true;
            comp.version++;
            return comp.toReference();
        }

        ClientCompanyReference deactivateCompany(UUID id, long version) {
            checkSimulatedFailure();
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(id)).findFirst().orElseThrow();
            if (comp.version != version) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            comp.active = false;
            comp.version++;
            return comp.toReference();
        }

        ClientSiteReference createSite(UUID companyId, String name) {
            checkSimulatedFailure();
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(companyId)).findFirst().orElseThrow();
            if (!comp.active) {
                throw new ClientManagementException(ClientManagementException.Reason.INACTIVE_COMPANY);
            }
            UUID id = UUID.randomUUID();
            SyntheticSite site = new SyntheticSite(id, companyId, name, true, 1L);
            sites.add(site);
            return site.toReference(comp.active);
        }

        ClientSiteReference renameSite(UUID id, String newName, long version) {
            checkSimulatedFailure();
            SyntheticSite site = sites.stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
            if (site.version != version) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(site.companyId)).findFirst().orElseThrow();
            site.name = newName;
            site.version++;
            return site.toReference(comp.active);
        }

        ClientSiteReference activateSite(UUID id, long version) {
            checkSimulatedFailure();
            SyntheticSite site = sites.stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(site.companyId)).findFirst().orElseThrow();
            if (!comp.active) {
                throw new ClientManagementException(ClientManagementException.Reason.INACTIVE_COMPANY);
            }
            if (site.version != version) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            site.active = true;
            site.version++;
            return site.toReference(comp.active);
        }

        ClientSiteReference deactivateSite(UUID id, long version) {
            checkSimulatedFailure();
            SyntheticSite site = sites.stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
            SyntheticCompany comp = companies.stream().filter(c -> c.id.equals(site.companyId)).findFirst().orElseThrow();
            if (site.version != version) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            site.active = false;
            site.version++;
            return site.toReference(comp.active);
        }

        private void checkSimulatedFailure() {
            if (simulateStaleVersion.getAndSet(false)) {
                throw new ClientManagementException(ClientManagementException.Reason.STALE_VERSION);
            }
            if (simulatePersistenceFailure.getAndSet(false)) {
                throw new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED);
            }
        }
    }

    static class EdgeCdpSession implements AutoCloseable {
        private final Process process;
        private final WebSocket webSocket;
        private final Path userDataDir;
        private final Map<Long, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
        private final List<String> consoleErrors = new CopyOnWriteArrayList<>();
        private final AtomicLong nextId = new AtomicLong(1);

        private EdgeCdpSession(Process process, WebSocket webSocket, Path userDataDir) {
            this.process = process;
            this.webSocket = webSocket;
            this.userDataDir = userDataDir;
        }

        public static EdgeCdpSession start(String windowSize) throws Exception {
            int cdpPort = findFreePort();
            Path userDataDir = Files.createTempDirectory("edge-cdp-profile-clients");
            Process process = new ProcessBuilder(
                    EDGE_EXE.toString(),
                    "--headless=new",
                    "--remote-debugging-port=" + cdpPort,
                    "--user-data-dir=" + userDataDir.toAbsolutePath(),
                    "--disable-gpu",
                    "--no-sandbox",
                    "--run-all-compositor-stages-before-draw",
                    "--force-device-scale-factor=1",
                    "--window-size=" + windowSize,
                    "about:blank"
            ).redirectOutput(ProcessBuilder.Redirect.DISCARD)
             .redirectError(ProcessBuilder.Redirect.DISCARD).start();

            String wsUrl = null;
            HttpClient client = HttpClient.newHttpClient();
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + cdpPort + "/json")).build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200) {
                        String body = resp.body();
                        Matcher objMatcher = Pattern.compile("\\{[^{}]+?\\}").matcher(body);
                        while (objMatcher.find()) {
                            String obj = objMatcher.group();
                            if (obj.contains("\"type\": \"page\"") || obj.contains("\"type\":\"page\"")) {
                                Matcher m = Pattern.compile("\"webSocketDebuggerUrl\"\\s*:\\s*\"([^\"]+)\"").matcher(obj);
                                if (m.find()) {
                                    wsUrl = m.group(1);
                                    break;
                                }
                            }
                        }
                        if (wsUrl != null) {
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(100);
            }

            if (wsUrl == null) {
                process.destroyForcibly();
                throw new IllegalStateException("Failed to connect to Edge CDP on port " + cdpPort);
            }

            CompletableFuture<EdgeCdpSession> sessionHolder = new CompletableFuture<>();
            CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        private final StringBuilder buffer = new StringBuilder();

                        @Override
                        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                            buffer.append(data);
                            if (last) {
                                String text = buffer.toString();
                                buffer.setLength(0);
                                try {
                                    Matcher idMatcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(text);
                                    if (idMatcher.find()) {
                                        long id = Long.parseLong(idMatcher.group(1));
                                        CompletableFuture<String> f = sessionHolder.join().pending.remove(id);
                                        if (f != null) {
                                            f.complete(text);
                                        }
                                    } else if (text.contains("\"method\"")) {
                                        if (text.contains("\"type\":\"error\"") || text.contains("Runtime.exceptionThrown")) {
                                            sessionHolder.join().consoleErrors.add(text);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            return WebSocket.Listener.super.onText(ws, data, last);
                        }
                    });

            WebSocket ws = wsFuture.get(5, TimeUnit.SECONDS);
            EdgeCdpSession session = new EdgeCdpSession(process, ws, userDataDir);
            sessionHolder.complete(session);

            session.sendRaw("Runtime.enable", "{}");
            session.sendRaw("Page.enable", "{}");
            return session;
        }

        public String sendRaw(String method, String paramsJson) throws Exception {
            long id = nextId.getAndIncrement();
            String req = String.format("{\"id\":%d,\"method\":\"%s\",\"params\":%s}", id, method, paramsJson);
            CompletableFuture<String> future = new CompletableFuture<>();
            pending.put(id, future);
            webSocket.sendText(req, true);
            return future.get(10, TimeUnit.SECONDS);
        }

        public void navigate(String url) throws Exception {
            sendRaw("Page.navigate", "{\"url\":\"" + escapeJson(url) + "\"}");
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                try {
                    String ready = eval("document.readyState");
                    if ("complete".equals(ready)) {
                        Thread.sleep(150);
                        return;
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(100);
            }
        }

        public boolean waitForSelector(String selector, long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    String found = eval("String(!!document.querySelector('" + escapeJs(selector) + "'))");
                    if ("true".equals(found)) {
                        Thread.sleep(150);
                        return true;
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(100);
            }
            return false;
        }

        public boolean waitForCondition(String jsCondition, long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    String result = eval("String(Boolean(" + jsCondition + "))");
                    if ("true".equals(result)) {
                        Thread.sleep(150);
                        return true;
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(100);
            }
            return false;
        }

        public void setViewport(int width, int height) throws Exception {
            sendRaw("Emulation.setDeviceMetricsOverride", String.format(
                    "{\"width\":%d,\"height\":%d,\"deviceScaleFactor\":1,\"mobile\":false}", width, height));
            Thread.sleep(200);
        }

        public String eval(String expression) throws Exception {
            String params = String.format("{\"expression\":\"%s\",\"returnByValue\":true,\"awaitPromise\":true}", escapeJson(expression));
            String resp = sendRaw("Runtime.evaluate", params);
            return extractValue(resp);
        }

        public void captureScreenshot(Path path) throws Exception {
            String resp = sendRaw("Page.captureScreenshot", "{\"format\":\"png\",\"captureBeyondViewport\":false}");
            Matcher m = Pattern.compile("\"data\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"").matcher(resp);
            if (m.find()) {
                byte[] bytes = Base64.getDecoder().decode(m.group(1));
                Files.write(path, bytes);
            }
        }

        public List<String> getConsoleErrors() {
            return consoleErrors;
        }

        @Override
        public void close() {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "close");
            } catch (Exception ignored) {
            }
            process.destroyForcibly();
            try {
                Files.walk(userDataDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {
            }
        }

        private static int findFreePort() throws Exception {
            try (ServerSocket s = new ServerSocket(0)) {
                return s.getLocalPort();
            }
        }

        private static String escapeJson(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        private static String extractValue(String json) {
            int idx = json.indexOf("\"value\":");
            if (idx < 0) return "";
            int start = idx + 8;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            if (start >= json.length()) return "";
            char first = json.charAt(start);
            if (first == '"') {
                int end = start + 1;
                while (end < json.length()) {
                    if (json.charAt(end) == '\\') { end += 2; continue; }
                    if (json.charAt(end) == '"') break;
                    end++;
                }
                return json.substring(start + 1, end)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");
            } else {
                int end = start;
                while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
                return json.substring(start, end).trim();
            }
        }
    }
}
