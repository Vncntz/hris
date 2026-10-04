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
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.vncntz.hris.clientmanagement.ClientAdministrationQueries;
import io.github.vncntz.hris.clientmanagement.ClientCompanyPage;
import io.github.vncntz.hris.clientmanagement.ClientCompanyReference;
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

        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Alpha Corp", true, 1L);
        ClientCompanyReference c2 = new ClientCompanyReference(company2Id, "Beta Ltd", false, 1L);

        ClientSiteReference s1 = new ClientSiteReference(site1Id, company1Id, "Alpha HQ", true, true, 1L);
        ClientSiteReference s2 = new ClientSiteReference(site2Id, company1Id, "Alpha Warehouse", false, true, 1L);
        ClientSiteReference s3 = new ClientSiteReference(site3Id, company2Id, "Beta Branch", true, false, 1L);

        when(clientAdministrationQueries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1, c2), false));
        when(clientAdministrationQueries.sites(eq(company1Id), anyInt(), anyInt()))
                .thenReturn(new ClientSitePage(List.of(s1, s2), false));
        when(clientAdministrationQueries.sites(eq(company2Id), anyInt(), anyInt()))
                .thenReturn(new ClientSitePage(List.of(s3), false));
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

        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            login(session, "synthetic.admin", "ValidPass123!");

            session.waitForSelector("#hris-current-user", 8000);

            // Verify Clients item is present in navigation
            String hasClientsInNav = session.eval("""
                    (() => {
                        const items = Array.from(document.querySelectorAll('vaadin-side-nav-item'));
                        return String(items.some(item => (item.getAttribute('path') || '').includes('clients')
                                || (item.textContent || '').includes('Clients')));
                    })()
                    """);
            assertEquals("true", hasClientsInNav, "Clients item must be present in side navigation for client:admin");

            // Navigate to /clients
            session.navigate("http://127.0.0.1:" + port + "/clients");
            session.waitForSelector("#hris-clients-view", 8000);
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

            // Verify bounded paging info
            String pageInfo = session.eval("document.querySelector('#hris-company-page-info')?.textContent");
            assertTrue(pageInfo != null && pageInfo.contains("Page 1"), "Must display bounded page info Page 1");

            // Test opening "New company" dialog
            session.eval("""
                    (() => {
                        const btn = document.querySelector('#hris-create-company-btn');
                        if (btn) btn.click();
                    })()
                    """);
            session.waitForSelector("#hris-create-company-dialog", 4000);
            session.captureScreenshot(SCREENSHOT_DIR.resolve("clients-dialog-new-company.png"));

            String dialogTitle = session.eval("document.querySelector('#hris-create-company-dialog')?.getAttribute('header-title') || document.querySelector('#hris-create-company-dialog')?.headerTitle");
            assertTrue(dialogTitle != null && dialogTitle.contains("New Company"),
                    "Dialog header must be New Company: " + dialogTitle);

            // Close dialog via cancel
            session.eval("""
                    (() => {
                        const cancel = document.querySelector('#hris-cancel-company-btn');
                        if (cancel) cancel.click();
                    })()
                    """);
            Thread.sleep(300);

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
                    String found = eval("String(!!document.querySelector('" + escapeJson(selector) + "'))");
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
