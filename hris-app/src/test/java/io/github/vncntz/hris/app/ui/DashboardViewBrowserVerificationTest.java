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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.spring.SpringServlet;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.github.vncntz.hris.clientmanagement.ClientAdministrationQueries;
import io.github.vncntz.hris.clientmanagement.ClientManagementService;
import io.github.vncntz.hris.clientmanagement.ClientReferences;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
class DashboardViewBrowserVerificationTest {
    private static final Path SCREENSHOT_DIR = Path.of(System.getProperty("java.io.tmpdir"), "hris-ui-verification");

    private static final List<Path> BROWSER_CANDIDATES = List.of(
            Path.of("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe"),
            Path.of("C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"),
            Path.of("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"),
            Path.of("/usr/bin/google-chrome"),
            Path.of("/usr/bin/chromium"),
            Path.of("/usr/bin/chromium-browser")
    );

    private static Path browserExe;

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

    @Autowired private ServletRegistrationBean<SpringServlet> servletRegistration;

    @Value("${local.server.port}")
    private int port;

    private RouteConfiguration routeConfiguration;

    @BeforeAll
    static void checkPrerequisites() {
        for (Path p : BROWSER_CANDIDATES) {
            if (Files.isRegularFile(p)) {
                browserExe = p;
                break;
            }
        }
        Assumptions.assumeTrue(browserExe != null, "A browser executable must exist for headless verification");
        try {
            Files.createDirectories(SCREENSHOT_DIR);
        } catch (Exception ignored) {
        }
    }

    @BeforeEach
    void setupAuthentication() {
        AccountPrincipal principal = new AccountPrincipal(
                UUID.fromString("00000000-0000-0000-0000-000000000048"),
                "synthetic.dashboard"
        );
        AuthenticatedAccount account = new AuthenticatedAccount(
                principal,
                List.of("ROLE_USER"),
                1L,
                new RoleGenerations(Map.of())
        );
        when(accountAuthenticationService.authenticate(eq("synthetic.dashboard"), eq("ValidPass123!")))
                .thenReturn(Optional.of(account));
        when(accountAuthenticationService.authenticate(eq("synthetic.dashboard"), eq("ValidPass123!"), any()))
                .thenReturn(Optional.of(account));

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1, Runnable.class);
            if (r != null) r.run();
            return null;
        }).when(sessionRegistrationService).register(any(), any());

        VaadinServletService service = servletRegistration.getServlet().getService();
        assertNotNull(service);
        routeConfiguration = RouteConfiguration.forRegistry(service.getRouter().getRegistry());
    }

    @AfterEach
    void cleanupRoutes() {
        if (routeConfiguration != null) {
            try { routeConfiguration.removeRoute(SyntheticModuleView.class); } catch (Exception ignored) {}
            try { routeConfiguration.removeRoute(RestrictedModuleView.class); } catch (Exception ignored) {}
        }
    }

    @Test
    void dashboardEmptyStateWhenNoAdditionalModulesAreEnabled() throws Exception {
        String loginUrl = "http://127.0.0.1:" + port + "/login";

        try (CdpBrowserSession session = CdpBrowserSession.start("1280,800")) {
            loginToDashboard(session, loginUrl);

            session.captureScreenshot(SCREENSHOT_DIR.resolve("dashboard-empty-state.png"));

            String dom = session.eval("document.documentElement.outerHTML");
            assertTrue(dom.contains("hris-dashboard"), "Dashboard container must exist");
            assertTrue(dom.contains("hris-dashboard-welcome"), "Welcome header must exist");

            String welcomeText = session.eval("document.querySelector('#hris-dashboard-welcome')?.textContent");
            assertTrue(welcomeText != null && welcomeText.contains("synthetic.dashboard"),
                    "Welcome must contain principal login name: " + welcomeText);

            assertTrue(dom.contains("hris-dashboard-modules-empty"), "Empty state element must exist when 0 modules enabled");
            assertTrue(dom.contains("No business modules are enabled yet."), "Empty state message must match specification");
            assertFalse(dom.contains("hris-dashboard-modules-nav"), "Navigation container must not exist when empty");

            List<String> errors = session.getConsoleErrors();
            assertTrue(errors.isEmpty(), "Zero browser console errors expected: " + errors);
        }
    }

    @Test
    void authorizedModuleRenderedWhileAccessFilteredModuleIsAbsent() throws Exception {
        routeConfiguration.setAnnotatedRoute(SyntheticModuleView.class);
        routeConfiguration.setAnnotatedRoute(RestrictedModuleView.class);

        String loginUrl = "http://127.0.0.1:" + port + "/login";

        try (CdpBrowserSession session = CdpBrowserSession.start("1280,800")) {
            loginToDashboard(session, loginUrl);

            session.captureScreenshot(SCREENSHOT_DIR.resolve("dashboard-with-authorized-module.png"));

            String dom = session.eval("document.documentElement.outerHTML");
            assertFalse(dom.contains("hris-dashboard-modules-empty"), "Empty state must not appear when module is present");
            assertTrue(dom.contains("hris-dashboard-modules-nav"), "Module navigation container must exist");
            assertTrue(dom.contains("Synthetic Module"), "Authorized synthetic module must appear in dashboard");
            assertTrue(dom.contains("dashboard-synthetic-test"), "Authorized module route must appear in link href");

            assertFalse(dom.contains("Restricted Module"), "DenyAll module must be filtered out by menu access filter");
            assertFalse(dom.contains("dashboard-restricted-test"), "Denied route must not appear in dashboard links");

            // Verify icon has aria-hidden=true
            String iconAria = session.eval("""
                    (() => {
                        const link = document.querySelector("a[href*='dashboard-synthetic-test']");
                        const icon = link ? link.querySelector('vaadin-icon') : null;
                        return icon ? icon.getAttribute('aria-hidden') : 'none';
                    })()
                    """);
            assertEquals("true", iconAria, "Module decorative icon must have aria-hidden='true'");

            List<String> errors = session.getConsoleErrors();
            assertTrue(errors.isEmpty(), "Zero console errors expected: " + errors);
        }
    }

    @Test
    void navigationToAuthorizedModuleOperatesInBrowser() throws Exception {
        routeConfiguration.setAnnotatedRoute(SyntheticModuleView.class);

        String loginUrl = "http://127.0.0.1:" + port + "/login";

        try (CdpBrowserSession session = CdpBrowserSession.start("1280,800")) {
            loginToDashboard(session, loginUrl);

            // Click the module link
            session.eval("""
                    (() => {
                        const link = document.querySelector("a[href*='dashboard-synthetic-test']");
                        if (link) link.click();
                    })()
                    """);

            boolean reached = waitForCondition(session, () -> {
                String path = session.eval("window.location.pathname");
                return path != null && path.contains("dashboard-synthetic-test");
            }, 8000);

            assertTrue(reached, "Clicking module link must navigate to target route: " + session.eval("window.location.pathname"));

            String content = session.eval("document.body.innerText");
            assertTrue(content.contains("Synthetic Module Landing Page"), "Target route content must be rendered");

            session.captureScreenshot(SCREENSHOT_DIR.resolve("dashboard-navigated-target.png"));
            List<String> errors = session.getConsoleErrors();
            assertTrue(errors.isEmpty(), "Zero console errors during navigation: " + errors);
        }
    }

    @Test
    void responsiveViewportsAndNoHorizontalOverflow() throws Exception {
        routeConfiguration.setAnnotatedRoute(SyntheticModuleView.class);

        String loginUrl = "http://127.0.0.1:" + port + "/login";

        // Desktop viewport
        try (CdpBrowserSession session = CdpBrowserSession.start("1920,1080")) {
            loginToDashboard(session, loginUrl);
            session.captureScreenshot(SCREENSHOT_DIR.resolve("dashboard-desktop-1920x1080.png"));

            String desktopOverflow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertEquals("true", desktopOverflow, "Desktop viewport must have zero horizontal overflow");
        }

        // Narrow/mobile viewport
        try (CdpBrowserSession session = CdpBrowserSession.start("375,667")) {
            loginToDashboard(session, loginUrl);
            session.captureScreenshot(SCREENSHOT_DIR.resolve("dashboard-mobile-375x667.png"));

            String mobileOverflow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertEquals("true", mobileOverflow, "Mobile viewport must have zero horizontal overflow");

            String linkVisible = session.eval("""
                    (() => {
                        const link = document.querySelector("a[href*='dashboard-synthetic-test']");
                        if (!link) return 'false';
                        const rect = link.getBoundingClientRect();
                        return String(rect.width > 0 && rect.height > 0);
                    })()
                    """);
            assertEquals("true", linkVisible, "Module link must remain visible and sized on mobile");
        }
    }

    @Test
    void keyboardFocusAndAbsenceOfSecurityInformationLeakage() throws Exception {
        routeConfiguration.setAnnotatedRoute(SyntheticModuleView.class);

        String loginUrl = "http://127.0.0.1:" + port + "/login";

        try (CdpBrowserSession session = CdpBrowserSession.start("1280,800")) {
            loginToDashboard(session, loginUrl);

            // Tab into dashboard interactive elements
            for (int i = 0; i < 6; i++) {
                session.pressTab();
            }

            session.captureScreenshot(SCREENSHOT_DIR.resolve("dashboard-keyboard-focus.png"));

            // Verify focusable element is focused
            String activeTag = session.eval("document.activeElement?.tagName");
            assertNotNull(activeTag, "An element must receive keyboard focus");

            // Inspect page content for security leakage
            String innerText = session.eval("document.body.innerText");
            assertFalse(innerText.contains("ROLE_"), "Granted roles must not be leaked");
            assertFalse(innerText.contains("client:admin"), "Internal authority names must not be leaked");
            assertFalse(innerText.contains("ValidPass123!"), "Password must not be leaked in page");
            assertFalse(innerText.contains("JSESSIONID"), "Session identifiers must not be exposed");
        }
    }

    private void loginToDashboard(CdpBrowserSession session, String loginUrl) throws Exception {
        session.navigate(loginUrl);

        session.eval("""
                (() => {
                    const u = document.querySelector('#vaadinLoginUsername');
                    if (u) {
                        u.value = 'synthetic.dashboard';
                        const input = u.querySelector('input');
                        if (input) {
                            input.value = 'synthetic.dashboard';
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    }

                    const p = document.querySelector('#vaadinLoginPassword');
                    if (p) {
                        p.value = 'ValidPass123!';
                        const input = p.querySelector('input');
                        if (input) {
                            input.value = 'ValidPass123!';
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    }

                    const f = document.querySelector('#hris-factor-input');
                    if (f) {
                        f.value = '123456';
                        f.dispatchEvent(new Event('input', { bubbles: true }));
                        f.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    const form = document.querySelector('vaadin-login-form');
                    if (form && typeof form.submit === 'function') {
                        form.submit();
                    } else {
                        const submit = document.querySelector('vaadin-button[slot=submit]');
                        if (submit) submit.click();
                    }
                })()
                """);

        boolean reachedDashboard = waitForCondition(session, () -> {
            String path = session.eval("window.location.pathname");
            if ("/".equals(path) || path.isEmpty()) {
                String welcome = session.eval("String(!!document.querySelector('#hris-dashboard-welcome'))");
                return "true".equals(welcome);
            }
            return false;
        }, 10000);

        assertTrue(reachedDashboard, "Authentication must navigate to authenticated dashboard root ('/')");
    }

    private static boolean waitForCondition(CdpBrowserSession session, Condition condition, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (condition.check()) return true;
            } catch (Exception ignored) {
            }
            Thread.sleep(150);
        }
        return false;
    }

    @FunctionalInterface
    interface Condition {
        boolean check() throws Exception;
    }

    @Route(value = "dashboard-synthetic-test", layout = MainLayout.class, registerAtStartup = false)
    @PageTitle("Synthetic Module")
    @Menu(order = 10, icon = "vaadin:cubes", title = "Synthetic Module")
    @PermitAll
    public static class SyntheticModuleView extends Div {
        public SyntheticModuleView() {
            add(new H2("Synthetic Module Landing Page"));
        }
    }

    @Route(value = "dashboard-restricted-test", layout = MainLayout.class, registerAtStartup = false)
    @PageTitle("Restricted Module")
    @Menu(order = 20, icon = "vaadin:lock", title = "Restricted Module")
    @DenyAll
    public static class RestrictedModuleView extends Div {
        public RestrictedModuleView() {
            add(new H2("Restricted Module"));
        }
    }

    static class CdpBrowserSession implements AutoCloseable {
        private final Process process;
        private final WebSocket webSocket;
        private final Path userDataDir;
        private final Map<Long, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
        private final List<String> consoleErrors = new CopyOnWriteArrayList<>();
        private final AtomicLong nextId = new AtomicLong(1);

        private CdpBrowserSession(Process process, WebSocket webSocket, Path userDataDir) {
            this.process = process;
            this.webSocket = webSocket;
            this.userDataDir = userDataDir;
        }

        public static CdpBrowserSession start(String windowSize) throws Exception {
            int cdpPort = findFreePort();
            Path userDataDir = Files.createTempDirectory("browser-cdp-profile");
            Process process = new ProcessBuilder(
                    browserExe.toString(),
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
                        if (wsUrl != null) break;
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(100);
            }

            if (wsUrl == null) {
                process.destroyForcibly();
                throw new IllegalStateException("Failed to connect to browser CDP on port " + cdpPort);
            }

            CompletableFuture<CdpBrowserSession> sessionHolder = new CompletableFuture<>();
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
            CdpBrowserSession session = new CdpBrowserSession(process, ws, userDataDir);
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
            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                try {
                    String ready = eval("document.readyState");
                    if ("complete".equals(ready)) {
                        String form = eval("String(!!document.querySelector('vaadin-login-form form'))");
                        if ("true".equals(form)) {
                            Thread.sleep(200);
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(100);
            }
        }

        public String eval(String expression) throws Exception {
            String params = String.format("{\"expression\":\"%s\",\"returnByValue\":true,\"awaitPromise\":true}", escapeJson(expression));
            String resp = sendRaw("Runtime.evaluate", params);
            return extractValue(resp);
        }

        public void pressTab() throws Exception {
            sendRaw("Input.dispatchKeyEvent", "{\"type\":\"rawKeyDown\",\"key\":\"Tab\",\"code\":\"Tab\",\"windowsVirtualKeyCode\":9,\"nativeVirtualKeyCode\":9}");
            sendRaw("Input.dispatchKeyEvent", "{\"type\":\"keyUp\",\"key\":\"Tab\",\"code\":\"Tab\",\"windowsVirtualKeyCode\":9,\"nativeVirtualKeyCode\":9}");
            Thread.sleep(150);
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
