package io.github.vncntz.hris.app.security;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.auth.AccessCheckDecision;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.VaadinRolePrefixHolder;
import io.github.vncntz.hris.app.ui.DashboardView;
import io.github.vncntz.hris.app.ui.LoginView;
import io.github.vncntz.hris.app.ui.MainLayout;
import io.github.vncntz.hris.identityaccess.AccountAuthenticationService;
import io.github.vncntz.hris.identityaccess.AccountPrincipal;
import io.github.vncntz.hris.identityaccess.CurrentActor;
import io.github.vncntz.hris.platformoperations.AgencyConfigurationService;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises the production security beans without a database or credentials. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "vaadin.productionMode=true",
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
class RouteAuthoritySecurityTest {
    @MockitoBean
    private io.github.vncntz.hris.clientmanagement.ClientManagementService clientManagement;
    @MockitoBean
    private io.github.vncntz.hris.clientmanagement.ClientReferences clientReferences;
    @MockitoBean
    private io.github.vncntz.hris.clientmanagement.ClientAdministrationQueries clientAdministrationQueries;
    @MockitoBean
    private io.github.vncntz.hris.identityaccess.MfaAdministrationService mfaAdministration;
    @MockitoBean
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

    @Autowired
    private FilterChainProxy filters;
    @Autowired
    private ServletRegistrationBean<SpringServlet> servletRegistration;
    @Autowired
    private NavigationAccessControl accessControl;
    @Autowired
    private AuthenticationContext authenticationContext;
    @Autowired
    private CurrentActor actor;

    private VaadinServletService service;
    private UI ui;
    private RouteConfiguration routes;

    @BeforeEach
    void registerSyntheticRoutes() {
        service = servletRegistration.getServlet().getService();
        assertNotNull(service);
        assertTrue(accessControl.isEnabled());
        VaadinService.setCurrent(service);
        routes = RouteConfiguration.forRegistry(service.getRouter().getRegistry());
        routes.setAnnotatedRoute(ProtectedRoute.class);
        routes.setAnnotatedRoute(UnannotatedRoute.class);
        routes.setAnnotatedRoute(DeniedRoute.class);
        ui = mock(UI.class, RETURNS_DEEP_STUBS);
        when(ui.getSession().getConfiguration().isProductionMode()).thenReturn(true);
        when(ui.getInternals().getRouter()).thenReturn(service.getRouter());
        when(ui.getInternals().getListeners(BeforeEnterHandler.class)).thenReturn(List.of(accessControl));
        UI.setCurrent(ui);
    }

    @AfterEach
    void clearSyntheticState() {
        routes.removeRoute(ProtectedRoute.class);
        routes.removeRoute(UnannotatedRoute.class);
        routes.removeRoute(DeniedRoute.class);
        UI.setCurrent(null);
        service.setCurrentInstances(null, null);
        VaadinService.setCurrent(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void exactPersistedAuthorityAllowsDirectRequestAndNavigationWithoutChangingAuthorities() throws Exception {
        Authentication principal = principal("client:admin");
        request("/task0042-protected", principal, 200, wrapped -> {
            assertEquals("", context.getBean(GrantedAuthorityDefaults.class).getRolePrefix());
            assertEquals("", context.getBean(VaadinRolePrefixHolder.class).getRolePrefix());
            assertTrue(wrapped.isUserInRole("client:admin"));
            assertTrue(authenticationContext.hasRole("client:admin"));
            assertEquals(List.of("client:admin"), authenticationContext.getGrantedAuthorities().stream()
                    .map(a -> a.getAuthority()).toList());
            assertEquals(principal.getAuthorities(), SecurityContextHolder.getContext()
                    .getAuthentication().getAuthorities());
            assertAllowed(ProtectedRoute.class, "task0042-protected");
            assertTrue(actor.hasAuthority("client:admin"));
            actor.requireAuthority("client:admin");
            assertEquals(principal.getPrincipal(), authenticationContext
                    .getAuthenticatedUser(AccountPrincipal.class).orElseThrow());
        });
    }

    @Test
    void missingOrPrefixedOrCaseChangedAuthorityCannotDirectlyAccessProtectedRoute() throws Exception {
        for (String authority : List.of("identity:admin", "ROLE_client:admin", "Client:admin", "client:admin-extra")) {
            request("/task0042-protected", principal(authority), 200, ignored ->
                    assertDenied(ProtectedRoute.class, "task0042-protected"));
            // Vaadin bootstrap is authenticated; PAGE_LOAD and internal navigation enforce route permissions.
            request("/", principal(authority), 200, ignored -> {
                assertDenied(ProtectedRoute.class, "task0042-protected");
                assertFalse(actor.hasAuthority("client:admin"));
                assertThrows(org.springframework.security.access.AccessDeniedException.class,
                        () -> actor.requireAuthority("client:admin"));
            });
        }
        request("/task0042-protected", principal(), 200, ignored ->
                assertDenied(ProtectedRoute.class, "task0042-protected"));
    }

    @Test
    void anonymousDirectRequestRedirectsToLoginAndInternalNavigationForwardsToExistingLogin() throws Exception {
        MockHttpServletResponse response = request("/task0042-protected", null, 302,
                ignored -> fail("Anonymous protected request reached route"));
        assertTrue(response.getRedirectedUrl().endsWith("/login"));
        request("/login", null, 200, ignored -> {
            BeforeEnterEvent event = navigation(ProtectedRoute.class, "task0042-protected");
            accessControl.beforeEnter(event);
            assertTrue(event.hasForwardTarget());
            assertEquals(LoginView.class, event.getForwardTargetType());
            assertFalse(event.hasRerouteTarget());
            assertFalse(actor.hasAuthority("client:admin"));
        });
    }

    @Test
    void existingLoginIsAnonymousAndShellDashboardRemainAuthenticatedWithNoAuthorities() throws Exception {
        request("/login", null, 200, ignored -> {
            assertAllowed(LoginView.class, "login");
            assertDenied(MainLayout.class, "");
            BeforeEnterEvent event = navigation(DashboardView.class, "");
            accessControl.beforeEnter(event);
            assertEquals(LoginView.class, event.getForwardTargetType());
        });
        request("/", principal(), 200, ignored -> {
            assertAllowed(MainLayout.class, "");
            assertAllowed(DashboardView.class, "");
            assertAllowed(LoginView.class, "login");
        });
        request("/", null, 302, ignored -> fail("Anonymous dashboard reached"));
    }

    @Test
    void actualMenuFilteringAgreesWithServerRouteAuthorization() throws Exception {
        for (Authentication authentication : List.of(principal("client:admin"), principal(), principal("ROLE_client:admin"))) {
            request("/", authentication, 200, ignored -> assertMenuMatchesNavigation());
        }
        request("/login", null, 200, ignored -> assertMenuMatchesNavigation());
    }

    @Test
    void springFallbackWithoutActiveRequestUsesTheSameExactAuthorityNamespace() throws Exception {
        for (Authentication authentication : List.of(principal("client:admin"), principal("ROLE_client:admin"))) {
            request("/", authentication, 200, ignored -> {
                service.setCurrentInstances(null, null);
                var decision = accessControl.checkAccess(accessControl.createNavigationContext(
                        ProtectedRoute.class, "task0042-protected", service, null), true);
                assertEquals(actor.hasAuthority("client:admin"), decision.decision() == AccessCheckDecision.ALLOW);
            });
        }
    }

    @Test
    void denyAllAndUnannotatedRoutesStillFailClosedEvenWithExactAuthority() throws Exception {
        request("/", principal("client:admin"), 200, ignored -> {
            assertDenied(DeniedRoute.class, "task0042-denied");
            assertDenied(UnannotatedRoute.class, "task0042-unannotated");
        });
        request("/task0042-denied", principal("client:admin"), 200, ignored ->
                assertDenied(DeniedRoute.class, "task0042-denied"));
        request("/task0042-unannotated", principal("client:admin"), 200, ignored ->
                assertDenied(UnannotatedRoute.class, "task0042-unannotated"));
    }

    private void assertMenuMatchesNavigation() {
        // This is the same MenuConfiguration API the production MainLayout uses.
        boolean visible = MenuConfiguration.getMenuEntries().stream()
                .anyMatch(entry -> entry.menuClass() == ProtectedRoute.class);
        var decision = accessControl.checkAccess(accessControl.createNavigationContext(
                ProtectedRoute.class, "task0042-protected", service, VaadinRequest.getCurrent()), true);
        assertEquals(decision.decision() == AccessCheckDecision.ALLOW, visible);
        assertEquals(actor.hasAuthority("client:admin"), visible);
        boolean dashboardVisible = MenuConfiguration.getMenuEntries().stream()
                .anyMatch(entry -> entry.menuClass() == DashboardView.class);
        assertEquals(authenticationContext.isAuthenticated(), dashboardVisible);
    }

    private void assertAllowed(Class<? extends Component> target, String path) {
        for (NavigationTrigger trigger : List.of(NavigationTrigger.PAGE_LOAD, NavigationTrigger.ROUTER_LINK)) {
            BeforeEnterEvent event = navigation(target, path, trigger);
            accessControl.beforeEnter(event);
            assertFalse(event.hasForwardTarget());
            assertFalse(event.hasRerouteTarget());
        }
    }

    private void assertDenied(Class<? extends Component> target, String path) {
        for (NavigationTrigger trigger : List.of(NavigationTrigger.PAGE_LOAD, NavigationTrigger.ROUTER_LINK)) {
            BeforeEnterEvent event = navigation(target, path, trigger);
            accessControl.beforeEnter(event);
            if (authenticationContext.isAuthenticated()) {
                assertTrue(event.hasRerouteTarget());
                assertInstanceOf(AccessDeniedException.class, event.getErrorParameter().getCaughtException());
                assertEquals("", event.getErrorParameter().getCustomMessage());
                assertFalse(event.hasForwardTarget());
            } else {
                assertTrue(event.hasForwardTarget());
                assertEquals(LoginView.class, event.getForwardTargetType());
            }
        }
    }

    private BeforeEnterEvent navigation(Class<? extends Component> target, String path) {
        return navigation(target, path, NavigationTrigger.PAGE_LOAD);
    }

    private BeforeEnterEvent navigation(Class<? extends Component> target, String path, NavigationTrigger trigger) {
        return new BeforeEnterEvent(service.getRouter(), trigger,
                new Location(path), target, ui, List.of());
    }

    private MockHttpServletResponse request(String path, Authentication authentication, int expectedStatus,
            Consumer<HttpServletRequest> accepted) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (authentication != null) {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(authentication));
            request.setSession(session);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reached = new AtomicBoolean();
        filters.doFilter(request, response, (wrapped, ignored) -> {
            reached.set(true);
            HttpServletRequest servletRequest = (HttpServletRequest) wrapped;
            service.setCurrentInstances(new VaadinServletRequest(servletRequest, service), null);
            accepted.accept(servletRequest);
        });
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedStatus == 200, reached.get());
        if (expectedStatus != 200) {
            assertFalse(response.getContentAsString().contains("client:admin"));
            assertFalse(response.getContentAsString().contains(ProtectedRoute.class.getName()));
        }
        service.setCurrentInstances(null, null);
        return response;
    }

    private Authentication principal(String... authorities) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(UUID.fromString("00000000-0000-0000-0000-000000000042"), "synthetic.route"),
                null, java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    @Route(value = "task0042-protected", layout = MainLayout.class, registerAtStartup = false)
    @Menu(title = "Synthetic protected route")
    @RolesAllowed("client:admin")
    public static class ProtectedRoute extends Div { }

    @Route(value = "task0042-unannotated", registerAtStartup = false)
    public static class UnannotatedRoute extends Div { }

    @Route(value = "task0042-denied", registerAtStartup = false)
    @DenyAll
    public static class DeniedRoute extends Div { }
}
