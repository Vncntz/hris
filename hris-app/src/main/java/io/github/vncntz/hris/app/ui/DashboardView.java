package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

/** Authenticated landing view. Holds no business data; modules add their own views later. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@Menu(order = 0, icon = "vaadin:dashboard", title = "Dashboard")
@PermitAll
public class DashboardView extends Div {

    @Autowired
    public DashboardView(AuthenticationContext authenticationContext) {
        this(authenticationContext, () -> {
            try {
                return MenuConfiguration.getMenuEntries();
            } catch (Exception ignored) {
                return List.of();
            }
        });
    }

    DashboardView(AuthenticationContext authenticationContext, Supplier<List<MenuEntry>> menuEntriesSupplier) {
        addClassName("hris-dashboard");
        String loginName = authenticationContext.getPrincipalName().orElse("there");

        H2 welcome = new H2("Welcome, " + loginName);
        welcome.setId("hris-dashboard-welcome");
        Paragraph intro = new Paragraph("You are signed in to the HRIS workspace. "
                + "Use the navigation menu to open the areas available to your account.");
        intro.addClassName("hris-lead");
        Div hero = new Div(welcome, intro);
        hero.addClassName("hris-hero");

        List<MenuEntry> allEntries = menuEntriesSupplier != null && menuEntriesSupplier.get() != null
                ? menuEntriesSupplier.get()
                : List.of();
        List<MenuEntry> moduleEntries = allEntries.stream()
                .filter(entry -> entry != null && entry.menuClass() != DashboardView.class)
                .toList();

        add(hero, modules(moduleEntries), session(loginName));
    }

    private static Section modules(List<MenuEntry> entries) {
        Icon icon = new Icon(VaadinIcon.CLUSTER);
        icon.addClassName("hris-card-icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        H3 heading = new H3("Business modules");

        if (entries.isEmpty()) {
            Paragraph state = new Paragraph("No business modules are enabled yet.");
            state.setId("hris-dashboard-modules-empty");
            state.addClassName("hris-card-emphasis");
            Paragraph detail = new Paragraph("Areas appear in the navigation menu as they become "
                    + "available and are granted to your account.");
            Section card = new Section(icon, heading, state, detail);
            card.addClassNames("hris-card", "hris-card-empty");
            return card;
        }

        Paragraph intro = new Paragraph("Select an area to open:");
        intro.addClassName("hris-card-intro");

        Nav nav = new Nav();
        nav.setId("hris-dashboard-modules-nav");
        nav.addClassName("hris-dashboard-modules-nav");
        nav.setAriaLabel("Available business modules");

        UnorderedList list = new UnorderedList();
        list.addClassName("hris-module-list");

        for (MenuEntry entry : entries) {
            ListItem item = new ListItem();
            item.addClassName("hris-module-item");

            RouterLink link = new RouterLink();
            link.addClassName("hris-module-link");
            String path = entry.path() != null ? entry.path() : "";
            link.getElement().setAttribute("href", path);

            if (entry.menuClass() != null && VaadinService.getCurrent() != null) {
                try {
                    link.setRoute(entry.menuClass());
                } catch (Exception ignored) {
                    link.getElement().setAttribute("href", path);
                }
            }
            if (link.getHref() == null || link.getHref().isBlank()) {
                link.getElement().setAttribute("href", path);
            }

            MainLayout.icon(entry.icon()).ifPresent(moduleIcon -> {
                moduleIcon.addClassName("hris-module-link-icon");
                link.add(moduleIcon);
            });

            Span title = new Span(entry.title());
            title.addClassName("hris-module-link-title");
            link.add(title);

            item.add(link);
            list.add(item);
        }

        nav.add(list);
        Section card = new Section(icon, heading, intro, nav);
        card.addClassName("hris-card");
        return card;
    }

    private static Section session(String loginName) {
        Icon icon = new Icon(VaadinIcon.SHIELD);
        icon.addClassName("hris-card-icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        H3 heading = new H3("Your session");
        Span label = new Span("Signed in as ");
        Span name = new Span(loginName);
        name.addClassName("hris-card-emphasis");
        Paragraph signedIn = new Paragraph(label, name);
        Paragraph advice = new Paragraph("Log out when you finish, especially on a shared computer.");
        Section card = new Section(icon, heading, signedIn, advice);
        card.addClassName("hris-card");
        return card;
    }
}
