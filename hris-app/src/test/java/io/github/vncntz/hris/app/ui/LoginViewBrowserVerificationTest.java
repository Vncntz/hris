package io.github.vncntz.hris.app.ui;

import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
class LoginViewBrowserVerificationTest {
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

    @BeforeAll
    static void checkPrerequisites() {
        Assumptions.assumeTrue(Files.isRegularFile(EDGE_EXE), "Edge browser executable must exist for headless verification");
        try {
            Files.createDirectories(SCREENSHOT_DIR);
        } catch (Exception ignored) {
        }
    }

    @Test
    void desktopViewportRendersLoginCardAndFactorField() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";
        Path screenshot = SCREENSHOT_DIR.resolve("login-desktop-1920x1080.png");

        try (EdgeCdpSession session = EdgeCdpSession.start("1920,1080")) {
            session.navigate(url);
            session.captureScreenshot(screenshot);

            String dom = session.eval("document.documentElement.outerHTML");
            Files.writeString(SCREENSHOT_DIR.resolve("login-desktop.html"), dom);

            assertTrue(dom.contains("hris-login-page"), "Login page container must exist");
            assertTrue(dom.contains("hris-login-card"), "Login card must exist");
            assertTrue(dom.contains("Sign in to your account"), "Single primary heading must exist");
            assertTrue(dom.contains("hris-login-heading"), "Heading must have id hris-login-heading");
            assertTrue(dom.contains("hris-brand-mark"), "Brand mark must exist");
            assertTrue(dom.contains("aria-hidden=\"true\""), "Brand mark must be decorative aria-hidden");
            assertTrue(dom.contains("HRIS"), "Brand name HRIS must exist");
            assertTrue(dom.contains("Manpower &amp; Staffing") || dom.contains("Manpower & Staffing"), "Brand tagline must exist");

            // Native form & factor attributes
            assertTrue(dom.contains("name=\"factor\""), "Factor input name must be 'factor'");
            assertTrue(dom.contains("type=\"password\""), "Factor input type must be 'password'");
            assertTrue(dom.contains("maxlength=\"32\""), "Factor input maxLength must be 32");
            assertTrue(dom.contains("autocomplete=\"one-time-code\""), "Factor autocomplete must be 'one-time-code'");
            assertTrue(dom.contains("hris-factor-group"), "Factor group wrapper must exist");
            assertTrue(dom.contains("hris-factor-hint"), "Factor hint element must exist");

            // Natural DOM placement verification in actual browser DOM
            String inForm = session.eval("String(document.querySelector('vaadin-login-form form')?.contains(document.querySelector('.hris-factor-group')))");
            assertTrue("true".equals(inForm), "Factor group must reside inside the form for native submission");

            String pwdBeforeFactor = session.eval("""
                    (() => {
                        const pwd = document.querySelector('vaadin-login-form form #vaadinLoginPassword');
                        const factor = document.querySelector('vaadin-login-form form .hris-factor-group');
                        return String(pwd && factor && (pwd.compareDocumentPosition(factor) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0);
                    })()
                    """);
            assertTrue("true".equals(pwdBeforeFactor), "Password field must precede factor group in DOM");

            String factorBeforeSubmit = session.eval("""
                    (() => {
                        const factor = document.querySelector('vaadin-login-form form .hris-factor-group');
                        const submit = document.querySelector('vaadin-login-form [slot=submit]');
                        return String(factor && submit && (factor.compareDocumentPosition(submit) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0);
                    })()
                    """);
            assertTrue("true".equals(factorBeforeSubmit), "Factor group must precede submit button in DOM");

            // Responsive geometry: no horizontal overflow
            String overflow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertTrue("true".equals(overflow), "Desktop layout must not exhibit horizontal overflow");

            // No internal exposure or stack traces
            assertFalse(dom.toLowerCase(Locale.ROOT).contains("exception"), "No stack traces or exception details");
            assertFalse(dom.contains("java.lang"), "No internal Java class references");
        }
    }

    @Test
    void laptopViewportRendersResponsively() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";
        Path screenshot = SCREENSHOT_DIR.resolve("login-laptop-1366x768.png");

        try (EdgeCdpSession session = EdgeCdpSession.start("1366,768")) {
            session.navigate(url);
            session.captureScreenshot(screenshot);

            String overflow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertTrue("true".equals(overflow), "Laptop viewport must have no horizontal scroll");

            String cardVisible = session.eval("""
                    (() => {
                        const card = document.querySelector('.hris-login-card');
                        if (!card) return 'false';
                        const r = card.getBoundingClientRect();
                        return String(r.width > 0 && r.left >= 0 && r.right <= window.innerWidth);
                    })()
                    """);
            assertTrue("true".equals(cardVisible), "Card must fit within laptop viewport");
        }
    }

    @Test
    void narrowMobileViewportRendersResponsively() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";
        Path screenshot500 = SCREENSHOT_DIR.resolve("login-narrow-500x800.png");

        try (EdgeCdpSession session = EdgeCdpSession.start("500,800")) {
            session.navigate(url);
            session.captureScreenshot(screenshot500);

            String overflow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertTrue("true".equals(overflow), "Narrow viewport (500x800) must have no horizontal overflow");

            String badgeVisible = session.eval("""
                    (() => {
                        const b = document.querySelector('.hris-factor-badge');
                        return String(b && b.getBoundingClientRect().width > 0);
                    })()
                    """);
            assertTrue("true".equals(badgeVisible), "Optional badge must remain visible without clipping on narrow viewport");
        }

        Path screenshot375 = SCREENSHOT_DIR.resolve("login-narrow-375x667.png");
        try (EdgeCdpSession session = EdgeCdpSession.start("375,667")) {
            session.navigate(url);
            session.captureScreenshot(screenshot375);

            String overflow = session.eval("String(document.documentElement.scrollWidth <= document.documentElement.clientWidth)");
            assertTrue("true".equals(overflow), "Mobile viewport (375x667) must have no horizontal overflow");
        }
    }

    @Test
    void genericErrorStateRendersWithoutDetailExposure() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login?error";
        Path screenshot = SCREENSHOT_DIR.resolve("login-error-state.png");

        try (EdgeCdpSession session = EdgeCdpSession.start("1280,800")) {
            session.navigate(url);
            session.captureScreenshot(screenshot);

            String dom = session.eval("document.documentElement.outerHTML");
            assertTrue(dom.contains("hris-login-card"));
            assertTrue(dom.contains("error") || dom.contains("Sign in failed") || dom.contains("Check that you have entered"),
                    "Generic error notification must be rendered");
            assertFalse(dom.toLowerCase(Locale.ROOT).contains("badcredentialsexception"),
                    "Must not leak BadCredentialsException");
            assertFalse(dom.toLowerCase(Locale.ROOT).contains("lockedexception"),
                    "Must not leak LockedException");
            assertFalse(dom.toLowerCase(Locale.ROOT).contains("disabledexception"),
                    "Must not leak DisabledException");
        }
    }

    @Test
    void keyboardNavigationAndFocusOrder() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";

        try (EdgeCdpSession session = EdgeCdpSession.start("1280,800")) {
            session.navigate(url);

            // Focus sequence helper identifying deep active element
            String getFocusIdJs = """
                    (() => {
                        let el = document.activeElement;
                        while (el && el.shadowRoot && el.shadowRoot.activeElement) {
                            el = el.shadowRoot.activeElement;
                        }
                        if (!el) return 'none';
                        if (el.id === 'hris-factor-input' || el.name === 'factor') return 'factor';
                        const host = (el.getRootNode && el.getRootNode().host) || null;
                        if (el.name === 'username' || el.id === 'vaadinLoginUsername' || (el.closest && el.closest('#vaadinLoginUsername')) || (host && host.id === 'vaadinLoginUsername')) return 'username';
                        if (el.name === 'password' || el.id === 'vaadinLoginPassword' || (el.closest && el.closest('#vaadinLoginPassword')) || (host && host.id === 'vaadinLoginPassword')) {
                            if ((el.getAttribute && el.getAttribute('slot') === 'reveal') || el.classList?.contains('reveal-button')) return 'password-reveal';
                            return 'password';
                        }
                        if ((el.getAttribute && el.getAttribute('slot') === 'reveal') || el.classList?.contains('reveal-button')) return 'password-reveal';
                        if ((el.getAttribute && el.getAttribute('slot') === 'submit') || (el.closest && el.closest('[slot=submit]')) || (host && host.getAttribute && host.getAttribute('slot') === 'submit')) return 'submit';
                        return el.tagName.toLowerCase();
                    })()
                    """;

            // Focus on username input explicitly first
            session.eval("""
                    (() => {
                        const tf = document.querySelector('#vaadinLoginUsername');
                        if (tf) {
                            tf.focus();
                            const input = tf.querySelector('input') || tf.shadowRoot?.querySelector('input');
                            if (input) input.focus();
                        }
                    })()
                    """);
            assertEquals("username", session.eval(getFocusIdJs), "Focus must begin on username input");

            // Tab 1 -> reaches Password
            session.pressTab();
            String focusAfterTab1 = session.eval(getFocusIdJs);
            assertEquals("password", focusAfterTab1, "First Tab from username must reach password");

            // Tab 2 -> reaches Password reveal or Factor
            session.pressTab();
            String focusAfterTab2 = session.eval(getFocusIdJs);
            if ("password-reveal".equals(focusAfterTab2)) {
                // Reveal button is within password component; next Tab advances to factor
                session.pressTab();
                focusAfterTab2 = session.eval(getFocusIdJs);
            }
            assertEquals("factor", focusAfterTab2, "Sequential keyboard order must reach factor after password");

            // Check visible focus indicator on factor input
            String focusVisible = session.eval("""
                    (() => {
                        const input = document.querySelector('#hris-factor-input');
                        if (document.activeElement !== input) return 'false';
                        const cs = window.getComputedStyle(input);
                        return String(cs.outlineStyle !== 'none' || cs.boxShadow !== 'none');
                    })()
                    """);
            assertTrue("true".equals(focusVisible), "Active factor input must have perceivable focus indication");

            // Tab 3 -> reaches Sign in submit button
            session.pressTab();
            String focusAfterTab3 = session.eval(getFocusIdJs);
            assertEquals("submit", focusAfterTab3, "Sequential keyboard order must reach Sign in after factor");

            // Check no focus trap: tabbing further moves past submit button without trapping
            session.pressTab();
            String focusAfterTab4 = session.eval(getFocusIdJs);
            assertFalse("submit".equals(focusAfterTab4), "Focus must not trap on submit control");
        }
    }

    @Test
    void syntheticAuthenticationReachesShellAndLogoutReturnsToLogin() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";

        AccountPrincipal principal = new AccountPrincipal(UUID.randomUUID(), "synthetic.admin");
        AuthenticatedAccount account = new AuthenticatedAccount(
                principal,
                List.of("ROLE_USER"),
                1L,
                new RoleGenerations(Map.of())
        );

        when(accountAuthenticationService.authenticate(eq("synthetic.admin"), eq("ValidPass123!")))
                .thenReturn(Optional.of(account));
        when(accountAuthenticationService.authenticate(eq("synthetic.admin"), eq("ValidPass123!"), any()))
                .thenReturn(Optional.of(account));

        doAnswer(inv -> {
            Runnable r = inv.getArgument(1, Runnable.class);
            if (r != null) r.run();
            return null;
        }).when(sessionRegistrationService).register(any(), any());

        try (EdgeCdpSession session = EdgeCdpSession.start("1280,800")) {
            session.navigate(url);

            // Populate credentials into native elements and trigger login
            session.eval("""
                    (() => {
                        const u = document.querySelector('#vaadinLoginUsername');
                        if (u) {
                            u.value = 'synthetic.admin';
                            const input = u.querySelector('input');
                            if (input) {
                                input.value = 'synthetic.admin';
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

            // Wait for navigation to shell (pathname "/")
            boolean reachedShell = false;
            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                String path = session.eval("window.location.pathname");
                if ("/".equals(path) || path.isEmpty()) {
                    String userEl = session.eval("String(!!document.querySelector('#hris-current-user'))");
                    if ("true".equals(userEl)) {
                        reachedShell = true;
                        break;
                    }
                }
                Thread.sleep(150);
            }
            assertTrue(reachedShell, "Successful synthetic authentication must reach the authenticated shell");
            session.captureScreenshot(SCREENSHOT_DIR.resolve("shell-authenticated.png"));

            // Verify shell displays signed-in user name
            String userDisplay = session.eval("document.querySelector('#hris-current-user')?.textContent");
            assertTrue(userDisplay != null && userDisplay.contains("synthetic.admin"),
                    "Shell must display synthetic user's principal login name: " + userDisplay);

            // Execute TASK-0040 logout control
            session.eval("""
                    (() => {
                        const logoutBtn = document.querySelector('#hris-logout');
                        if (logoutBtn) logoutBtn.click();
                    })()
                    """);

            // Wait for return to /login
            boolean returnedToLogin = false;
            deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                String path = session.eval("window.location.pathname");
                if ("/login".equals(path)) {
                    String h1 = session.eval("String(!!document.querySelector('#hris-login-heading'))");
                    if ("true".equals(h1)) {
                        returnedToLogin = true;
                        break;
                    }
                }
                Thread.sleep(150);
            }
            assertTrue(returnedToLogin, "Logout must return the user to /login");
            session.captureScreenshot(SCREENSHOT_DIR.resolve("login-after-logout.png"));
        }
    }

    @Test
    void browserConsoleInspectionReportsNoErrors() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";

        try (EdgeCdpSession session = EdgeCdpSession.start("1280,800")) {
            session.navigate(url);

            // Exercise interactions
            session.eval("""
                    (() => {
                        const u = document.querySelector('vaadin-text-field input');
                        if (u) {
                            u.value = 'test.user';
                            u.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                        const f = document.querySelector('#hris-factor-input');
                        if (f) {
                            f.value = '999999';
                            f.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                    })()
                    """);

            List<String> errors = session.getConsoleErrors();
            assertTrue(errors.isEmpty(), "Browser console must report zero application errors during login experience: " + errors);
        }
    }

    // Zero-dependency Edge DevTools Protocol (CDP) session
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
            Path userDataDir = Files.createTempDirectory("edge-cdp-profile");
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
