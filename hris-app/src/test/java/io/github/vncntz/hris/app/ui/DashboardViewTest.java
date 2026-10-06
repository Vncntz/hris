package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardViewTest {

    public static class SyntheticModuleOne extends Component { }
    public static class SyntheticModuleTwo extends Component { }
    public static class SyntheticModuleNoIcon extends Component { }

    private static final MenuEntry DASHBOARD_ENTRY =
            new MenuEntry("", "Dashboard", 0.0, "vaadin:dashboard", DashboardView.class);
    private static final MenuEntry MODULE_ONE =
            new MenuEntry("synthetic-one", "Synthetic Module One", 1.0, "vaadin:cubes", SyntheticModuleOne.class);
    private static final MenuEntry MODULE_TWO =
            new MenuEntry("synthetic-two", "Synthetic Module Two", 2.0, "vaadin:calendar", SyntheticModuleTwo.class);
    private static final MenuEntry MODULE_NO_ICON =
            new MenuEntry("synthetic-plain", "Plain Module", 3.0, null, SyntheticModuleNoIcon.class);

    private AuthenticationContext authenticationContext;

    @BeforeEach
    void setUp() {
        authenticationContext = mock(AuthenticationContext.class);
        when(authenticationContext.getPrincipalName()).thenReturn(Optional.of("synthetic.operator"));
    }

    @Test
    void zeroNonDashboardEntriesRendersClearEmptyState() {
        DashboardView viewOnlyDashboard = new DashboardView(authenticationContext, () -> List.of(DASHBOARD_ENTRY));

        assertTrue(descendants(viewOnlyDashboard).anyMatch(c ->
                c.getId().equals(Optional.of("hris-dashboard-modules-empty"))
                        && "No business modules are enabled yet.".equals(c.getElement().getText())));
        assertFalse(descendants(viewOnlyDashboard).anyMatch(Nav.class::isInstance));
        assertFalse(descendants(viewOnlyDashboard).anyMatch(RouterLink.class::isInstance));

        DashboardView viewEmptyList = new DashboardView(authenticationContext, List::of);
        assertTrue(descendants(viewEmptyList).anyMatch(c ->
                c.getId().equals(Optional.of("hris-dashboard-modules-empty"))
                        && "No business modules are enabled yet.".equals(c.getElement().getText())));
        assertFalse(descendants(viewEmptyList).anyMatch(Nav.class::isInstance));
    }

    @Test
    void singleModuleEntryRenderedWithCorrectMetadataAndExcludesDashboardSelf() {
        DashboardView view = new DashboardView(authenticationContext, () -> List.of(DASHBOARD_ENTRY, MODULE_ONE));

        assertFalse(descendants(view).anyMatch(c -> c.getId().equals(Optional.of("hris-dashboard-modules-empty"))));

        Nav nav = find(view, Nav.class, "hris-dashboard-modules-nav");
        assertNotNull(nav);
        assertEquals("Available business modules", nav.getElement().getAttribute("aria-label"));

        List<RouterLink> links = descendants(nav).filter(RouterLink.class::isInstance).map(RouterLink.class::cast).toList();
        assertEquals(1, links.size(), "Dashboard/self must be excluded; exactly one module rendered");

        RouterLink link = links.getFirst();
        assertEquals("synthetic-one", link.getHref());

        Span title = descendants(link).filter(Span.class::isInstance).map(Span.class::cast)
                .filter(s -> "Synthetic Module One".equals(s.getText()))
                .findFirst().orElse(null);
        assertNotNull(title, "Link must display the menu title");

        Icon icon = descendants(link).filter(Icon.class::isInstance).map(Icon.class::cast).findFirst().orElse(null);
        assertNotNull(icon, "Link must display the optional menu icon");
        assertEquals("true", icon.getElement().getAttribute("aria-hidden"));
        assertEquals("vaadin:cubes", icon.getElement().getAttribute("icon"));
    }

    @Test
    void multipleModuleEntriesPreserveExactMenuOrderWithoutDuplication() {
        List<MenuEntry> suppliedEntries = List.of(DASHBOARD_ENTRY, MODULE_TWO, MODULE_ONE, MODULE_NO_ICON);
        DashboardView view = new DashboardView(authenticationContext, () -> suppliedEntries);

        Nav nav = find(view, Nav.class, "hris-dashboard-modules-nav");
        List<RouterLink> links = descendants(nav).filter(RouterLink.class::isInstance).map(RouterLink.class::cast).toList();
        assertEquals(3, links.size(), "All 3 non-dashboard entries rendered, dashboard excluded");

        assertEquals("synthetic-two", links.get(0).getHref());
        assertEquals("synthetic-one", links.get(1).getHref());
        assertEquals("synthetic-plain", links.get(2).getHref());

        List<String> titles = links.stream()
                .map(link -> descendants(link).filter(Span.class::isInstance).map(Span.class::cast)
                        .map(Span::getText).findFirst().orElse(""))
                .toList();
        assertEquals(List.of("Synthetic Module Two", "Synthetic Module One", "Plain Module"), titles,
                "Relative menu order must be preserved exactly");
    }

    @Test
    void optionalIconMetadataHandlesNullAndBlankGracefully() {
        DashboardView view = new DashboardView(authenticationContext, () -> List.of(MODULE_NO_ICON));

        Nav nav = find(view, Nav.class, "hris-dashboard-modules-nav");
        RouterLink link = descendants(nav).filter(RouterLink.class::isInstance).map(RouterLink.class::cast).findFirst().orElseThrow();

        assertEquals("synthetic-plain", link.getHref());
        assertTrue(descendants(link).noneMatch(Icon.class::isInstance),
                "No decorative icon rendered when icon metadata is missing");
        assertTrue(descendants(link).anyMatch(c -> c instanceof Span s && "Plain Module".equals(s.getText())));
    }

    @Test
    void moduleNavigationAccessibleStructureAndSemantics() {
        DashboardView view = new DashboardView(authenticationContext, () -> List.of(MODULE_ONE, MODULE_TWO));

        Nav nav = find(view, Nav.class, "hris-dashboard-modules-nav");
        assertEquals("Available business modules", nav.getElement().getAttribute("aria-label"));

        UnorderedList list = descendants(nav).filter(UnorderedList.class::isInstance).map(UnorderedList.class::cast).findFirst().orElseThrow();
        assertEquals(2, descendants(list).filter(ListItem.class::isInstance).count());

        List<RouterLink> links = descendants(list).filter(RouterLink.class::isInstance).map(RouterLink.class::cast).toList();
        for (RouterLink link : links) {
            assertTrue(link.getClassNames().contains("hris-module-link"));
            assertFalse(link.getElement().hasAttribute("tabindex")
                            && Integer.parseInt(link.getElement().getAttribute("tabindex")) > 0,
                    "Link must not introduce positive tabindex");
        }
    }

    @Test
    void welcomeAndSessionPreservedWithoutSecurityOrInternalLeakage() {
        DashboardView view = new DashboardView(authenticationContext, () -> List.of(MODULE_ONE));

        assertTrue(descendants(view).anyMatch(c -> c.getId().equals(Optional.of("hris-dashboard-welcome"))
                && "Welcome, synthetic.operator".equals(c.getElement().getText())));

        assertTrue(descendants(view).anyMatch(c -> c instanceof Span s && "synthetic.operator".equals(s.getText())));
        assertFalse(descendants(view).anyMatch(H1.class::isInstance), "Navbar owns the single h1");

        String completeText = descendants(view)
                .map(c -> c.getElement().getText())
                .reduce("", (a, b) -> a + " " + b);

        assertFalse(completeText.contains("ROLE_"));
        assertFalse(completeText.contains("client:admin"));
        assertFalse(completeText.contains("authorities"));
        assertFalse(completeText.contains("session-id"));
    }

    @Test
    void noFabricatedMetricsOrCountsIntroduced() {
        DashboardView view = new DashboardView(authenticationContext, () -> List.of(MODULE_ONE));

        assertEquals(3, view.getChildren().count(), "Dashboard must only contain hero, modules, and session cards");

        String text = descendants(view).map(c -> c.getElement().getText()).reduce("", (a, b) -> a + " " + b);
        assertFalse(text.contains("100%"));
        assertFalse(text.contains("Active employees"));
        assertFalse(text.contains("Total clients"));
        assertFalse(text.contains("Payroll run"));
    }

    @Test
    void handlesNullSupplierOrNullEntriesResiliently() {
        DashboardView viewNullSupplier = new DashboardView(authenticationContext, null);
        assertTrue(descendants(viewNullSupplier).anyMatch(c ->
                c.getId().equals(Optional.of("hris-dashboard-modules-empty"))));

        DashboardView viewNullResult = new DashboardView(authenticationContext, () -> null);
        assertTrue(descendants(viewNullResult).anyMatch(c ->
                c.getId().equals(Optional.of("hris-dashboard-modules-empty"))));
    }

    private static <T extends Component> T find(Component root, Class<T> type, String id) {
        return descendants(root).filter(type::isInstance).map(type::cast)
                .filter(c -> c.getId().equals(Optional.of(id))).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(DashboardViewTest::descendants));
    }
}
