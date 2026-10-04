package io.github.vncntz.hris.app.ui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

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
import io.github.vncntz.hris.identityaccess.AccountRoleAssignmentService;
import io.github.vncntz.hris.identityaccess.AccountSessionRegistrationService;
import io.github.vncntz.hris.identityaccess.CredentialReauthenticationService;
import io.github.vncntz.hris.identityaccess.MfaAdministrationService;
import io.github.vncntz.hris.identityaccess.PasswordChangeService;
import io.github.vncntz.hris.identityaccess.PasswordResetService;
import io.github.vncntz.hris.identityaccess.RoleAdministrationService;
import io.github.vncntz.hris.platformoperations.AgencyConfigurationService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        String dom = executeHeadlessBrowser(url, "1920,1080", screenshot);
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

        // Submit button
        assertTrue(dom.contains("Sign in"), "Submit button label must be 'Sign in'");

        // No internal exposure or stack traces
        assertFalse(dom.toLowerCase(Locale.ROOT).contains("exception"), "No stack traces or exception details");
        assertFalse(dom.contains("java.lang"), "No internal Java class references");
    }

    @Test
    void laptopViewportRendersResponsively() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";
        Path screenshot = SCREENSHOT_DIR.resolve("login-laptop-1366x768.png");

        String dom = executeHeadlessBrowser(url, "1366,768", screenshot);
        assertTrue(dom.contains("hris-login-card"));
        assertTrue(dom.contains("Sign in to your account"));
        assertTrue(dom.contains("name=\"factor\""));
    }

    @Test
    void narrowMobileViewportRendersResponsively() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login";
        Path screenshot = SCREENSHOT_DIR.resolve("login-narrow-500x800.png");

        String dom = executeHeadlessBrowser(url, "500,800", screenshot);
        Files.writeString(SCREENSHOT_DIR.resolve("login-narrow.html"), dom);
        assertTrue(dom.contains("hris-login-card"));
        assertTrue(dom.contains("Sign in to your account"));
        assertTrue(dom.contains("name=\"factor\""));
    }

    @Test
    void genericErrorStateRendersWithoutDetailExposure() throws Exception {
        String url = "http://127.0.0.1:" + port + "/login?error";
        Path screenshot = SCREENSHOT_DIR.resolve("login-error-state.png");

        String dom = executeHeadlessBrowser(url, "1280,800", screenshot);
        assertTrue(dom.contains("hris-login-card"));
        assertTrue(dom.contains("error") || dom.contains("Sign in failed") || dom.contains("Incorrect"),
                "Generic error indication must be rendered");
        assertFalse(dom.toLowerCase(Locale.ROOT).contains("badcredentialsexception"),
                "Must not leak BadCredentialsException");
        assertFalse(dom.toLowerCase(Locale.ROOT).contains("lockedexception"),
                "Must not leak LockedException");
        assertFalse(dom.toLowerCase(Locale.ROOT).contains("disabledexception"),
                "Must not leak DisabledException");
    }

    private static String executeHeadlessBrowser(String url, String windowSize, Path screenshot) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                EDGE_EXE.toString(),
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--force-device-scale-factor=1",
                "--window-size=" + windowSize,
                "--screenshot=" + screenshot.toAbsolutePath(),
                "--virtual-time-budget=5000",
                "--dump-dom",
                url
        );
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        String dom = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        return dom;
    }
}
