package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Authenticated application frame. Navigation comes from Vaadin's access-filtered menu
 * configuration, so later {@code @Menu} views appear only for users allowed to open them;
 * route access itself remains enforced server-side.
 */
@PermitAll
@StyleSheet("hris/shell.css")
public class MainLayout extends AppLayout implements AfterNavigationObserver {
    static final String APPLICATION_NAME = "HRIS";

    private final AuthenticationContext authenticationContext;
    private final H1 viewTitle = new H1();

    @Autowired
    public MainLayout(AuthenticationContext authenticationContext) {
        this(authenticationContext, MenuConfiguration::getMenuEntries);
    }

    MainLayout(AuthenticationContext authenticationContext, Supplier<List<MenuEntry>> menuEntries) {
        this.authenticationContext = authenticationContext;
        addClassName("hris-shell");
        setPrimarySection(Section.DRAWER);
        addToDrawer(createDrawer(menuEntries.get()));
        addToNavbar(true, createNavbar());
    }

    private Component createNavbar() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Toggle navigation menu");

        viewTitle.addClassName("hris-view-title");

        String loginName = authenticationContext.getPrincipalName().orElse("Signed-in user");
        Avatar avatar = new Avatar(loginName);
        avatar.addClassName("hris-user-avatar");
        avatar.getElement().setAttribute("aria-hidden", "true");
        Span user = new Span(loginName);
        user.addClassName("hris-user-name");
        user.setId("hris-current-user");

        Button logout = new Button("Log out", new Icon(VaadinIcon.SIGN_OUT),
                event -> authenticationContext.logout());
        logout.setId("hris-logout");
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.getElement().setAttribute("aria-label", "Log out of " + APPLICATION_NAME);

        Div account = new Div(avatar, user, logout);
        account.addClassName("hris-account");
        account.getElement().setAttribute("role", "group");
        account.getElement().setAttribute("aria-label", "Signed-in account");

        Header navbar = new Header(toggle, viewTitle, account);
        navbar.addClassName("hris-navbar");
        return navbar;
    }

    private Component createDrawer(List<MenuEntry> entries) {
        Icon mark = new Icon(VaadinIcon.USERS);
        mark.addClassName("hris-brand-mark");
        mark.getElement().setAttribute("aria-hidden", "true");
        Span name = new Span(APPLICATION_NAME);
        name.addClassName("hris-brand-name");
        Span tagline = new Span("Manpower & Staffing");
        tagline.addClassName("hris-brand-tagline");
        Div brandText = new Div(name, tagline);
        brandText.addClassName("hris-brand-text");
        Div brand = new Div(mark, brandText);
        brand.addClassName("hris-brand");

        Scroller navigation = new Scroller(createNavigation(entries));
        navigation.addClassName("hris-drawer-scroller");

        Footer footer = new Footer(new Span(APPLICATION_NAME + " workspace"));
        footer.addClassName("hris-drawer-footer");

        Div drawer = new Div(brand, navigation, footer);
        drawer.addClassName("hris-drawer");
        return drawer;
    }

    static SideNav createNavigation(List<MenuEntry> entries) {
        SideNav nav = new SideNav();
        nav.setId("hris-main-navigation");
        nav.getElement().setAttribute("aria-label", "Main navigation");
        nav.addClassName("hris-side-nav");
        for (MenuEntry entry : entries) {
            SideNavItem item = new SideNavItem(entry.title(), entry.path());
            icon(entry.icon()).ifPresent(item::setPrefixComponent);
            nav.addItem(item);
        }
        return nav;
    }

    static Optional<Icon> icon(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        int separator = name.indexOf(':');
        Icon icon = separator > 0
                ? new Icon(name.substring(0, separator), name.substring(separator + 1))
                : new Icon("vaadin", name);
        icon.getElement().setAttribute("aria-hidden", "true");
        return Optional.of(icon);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(titleOf(getContent()));
    }

    static String titleOf(Component content) {
        if (content == null) {
            return APPLICATION_NAME;
        }
        PageTitle title = content.getClass().getAnnotation(PageTitle.class);
        return title == null || title.value().isBlank() ? APPLICATION_NAME : title.value();
    }

    String currentTitle() {
        return viewTitle.getText();
    }
}
