package io.github.vncntz.hris;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationService;
import io.github.vncntz.hris.identityaccess.FirstAdministratorProvisioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import io.github.vncntz.hris.platformoperations.AgencyConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "vaadin.productionMode=true",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
        }
)
class HrisApplicationSmokeTest {
    @MockitoBean
    io.github.vncntz.hris.identityaccess.MfaAdministrationService mfaAdministration;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private io.github.vncntz.hris.identityaccess.RoleAdministrationService roleAdministration;
    @Autowired
    private ApplicationContext context;
    @MockitoBean
    private AuditRecorder auditRecorder;

    @MockitoBean
    private AccountAuthenticationService accountAuthenticationService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.AccountCreationService accountCreationService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.CredentialReauthenticationService credentialReauthenticationService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.PasswordChangeService passwordChangeService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.PasswordResetService passwordResetService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.AccountLifecycleService accountLifecycleService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.AccountRoleAssignmentService accountRoleAssignmentService;

    @MockitoBean
    private io.github.vncntz.hris.identityaccess.AccountSessionRegistrationService sessionRegistrationService;

    @MockitoBean
    private AgencyConfigurationService agencyConfigurationService;

    @Value("${local.server.port}")
    private int port;

    @Test
    void anonymousLoginServesVaadinBootstrapAndRootRequiresAuthentication() throws Exception {
        assertEquals(0, context.getBeanNamesForType(FirstAdministratorProvisioner.class).length);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/login"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String contentType = response.headers().firstValue("content-type").orElse("");

            assertEquals(200, response.statusCode());
            assertTrue(contentType.toLowerCase(Locale.ROOT).startsWith("text/html"), contentType);
            assertTrue(response.body().toLowerCase(Locale.ROOT).contains("<html"));
            assertTrue(response.body().contains("window.Vaadin"));
            HttpResponse<Void> protectedRoot = client.send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port + "/")).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertEquals(302, protectedRoot.statusCode());
            assertTrue(protectedRoot.headers().firstValue("location").orElse("").contains("login"));
            for (String path : new String[]{"/setup", "/bootstrap", "/provision-first-administrator"}) {
                HttpResponse<Void> setup = client.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + path)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.discarding());
                assertEquals(302, setup.statusCode());
                assertTrue(setup.headers().firstValue("location").orElse("").contains("login"));
            }
        }
    }
}
