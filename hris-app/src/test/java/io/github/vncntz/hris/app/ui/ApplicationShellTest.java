package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationShellTest {
    private static final MenuEntry DASHBOARD =
            new MenuEntry("", "Dashboard", 0.0, "vaadin:dashboard", DashboardView.class);

    @Test
    void shellAndDashboardRequireAuthenticationAndDashboardOwnsRoot() {
        assertNotNull(MainLayout.class.getAnnotation(PermitAll.class));
        assertNull(MainLayout.class.getAnnotation(AnonymousAllowed.class));
        assertNotNull(DashboardView.class.getAnnotation(PermitAll.class));
        assertNull(DashboardView.class.getAnnotation(AnonymousAllowed.class));
        Route route = DashboardView.class.getAnnotation(Route.class);
        assertEquals("", route.value());
        assertEquals(MainLayout.class, route.layout());
        Menu menu = DashboardView.class.getAnnotation(Menu.class);
        assertEquals("Dashboard", menu.title());
        assertEquals("Dashboard", DashboardView.class.getAnnotation(PageTitle.class).value());
    }

    @Test
    void navigationIsLabelledAndRendersOnlySuppliedAccessFilteredEntries() {
        SideNav nav = MainLayout.createNavigation(List.of(DASHBOARD));

        assertEquals("Main navigation", nav.getElement().getAttribute("aria-label"));
        assertEquals(1, nav.getItems().size());
        SideNavItem item = nav.getItems().getFirst();
        assertEquals("Dashboard", item.getLabel());
        assertEquals("", item.getPath());
        assertTrue(item.getPrefixComponent() instanceof Icon);
        assertTrue(MainLayout.createNavigation(List.of()).getItems().isEmpty());
    }

    @Test
    void menuIconsAreDecorativeAndOptional() {
        assertTrue(MainLayout.icon(null).isEmpty());
        assertTrue(MainLayout.icon(" ").isEmpty());
        Icon icon = MainLayout.icon("vaadin:dashboard").orElseThrow();
        assertEquals("vaadin:dashboard", icon.getElement().getAttribute("icon"));
        assertEquals("true", icon.getElement().getAttribute("aria-hidden"));
    }

    @Test
    void shellShowsSignedInLoginAndDelegatesLogoutToExistingAuthenticationContext() {
        AuthenticationContext authentication = mock(AuthenticationContext.class);
        when(authentication.getPrincipalName()).thenReturn(Optional.of("synthetic.admin"));
        MainLayout layout = new MainLayout(authentication, () -> List.of(DASHBOARD));

        Span user = find(layout, Span.class, "hris-current-user");
        assertEquals("synthetic.admin", user.getText());
        assertEquals(1, descendants(layout).filter(H1.class::isInstance).count());
        assertTrue(descendants(layout).anyMatch(SideNav.class::isInstance));

        Button logout = find(layout, Button.class, "hris-logout");
        assertEquals("Log out", logout.getText());
        verify(authentication, never()).logout();
        logout.click();
        verify(authentication).logout();
    }

    @Test
    void headingFollowsViewPageTitleWithApplicationFallback() {
        AuthenticationContext authentication = mock(AuthenticationContext.class);
        when(authentication.getPrincipalName()).thenReturn(Optional.of("synthetic.user"));

        assertEquals("Dashboard", MainLayout.titleOf(new DashboardView(authentication)));
        assertEquals("HRIS", MainLayout.titleOf(new Span()));
        assertEquals("HRIS", MainLayout.titleOf(null));
    }

    @Test
    void dashboardWelcomesUserWithHonestEmptyStateAndNoFabricatedData() {
        AuthenticationContext authentication = mock(AuthenticationContext.class);
        when(authentication.getPrincipalName()).thenReturn(Optional.of("synthetic.user"));
        DashboardView view = new DashboardView(authentication);

        assertTrue(descendants(view).anyMatch(c -> c.getId().equals(Optional.of("hris-dashboard-welcome"))
                && c.getElement().getText().equals("Welcome, synthetic.user")));
        assertTrue(descendants(view).anyMatch(c -> c.getId().equals(Optional.of("hris-dashboard-modules-empty"))
                && c.getElement().getText().equals("No business modules are enabled yet.")));
        assertFalse(descendants(view).anyMatch(H1.class::isInstance), "shell navbar owns the single h1");
    }

    private static <T extends Component> T find(Component root, Class<T> type, String id) {
        return descendants(root).filter(type::isInstance).map(type::cast)
                .filter(c -> c.getId().equals(Optional.of(id))).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(ApplicationShellTest::descendants));
    }
}
