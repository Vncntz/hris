package com.vincent.hris.views;

import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vincent.hris.modules.userandroles.model.Access;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.SubMenu;
import com.vincent.hris.modules.userandroles.service.AccessService;
import com.vincent.hris.modules.userandroles.service.SubMenuService;
import com.vincent.hris.security.SessionService;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements BeforeEnterObserver {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final H1 viewTitle = new H1();
	private final SessionService sessionService;
	private final AccessService accessService;
	private final SubMenuService subMenuService;

	public MainLayout(SessionService sessionService, AccessService accessService, SubMenuService subMenuService) {
		this.sessionService = sessionService;
		this.accessService = accessService;
		this.subMenuService = subMenuService;

		setPrimarySection(Section.DRAWER);
		addDrawerContent();
		addHeaderContent();
	}

	private void addHeaderContent() {
		DrawerToggle toggle = new DrawerToggle();
		toggle.setAriaLabel("Menu toggle");

		viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

		Button buttonDarkModeToggle = new Button("", VaadinIcon.MOON.create());
		buttonDarkModeToggle.addClickListener(e -> toggleDarkMode());

		Button buttonLogout = new Button("Logout", e -> logout());
		buttonLogout.addThemeVariants(ButtonVariant.LUMO_ERROR);

		Button buttonDashBoard = new Button("", VaadinIcon.DASHBOARD.create());
		buttonDashBoard.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("dashboard")));

		HorizontalLayout headerLayout = new HorizontalLayout(toggle, viewTitle, buttonDashBoard, buttonDarkModeToggle,
				buttonLogout);
		headerLayout.setWidthFull();
		headerLayout.setAlignItems(Alignment.CENTER);
		headerLayout.expand(viewTitle);

		addToNavbar(true, headerLayout);
	}

	private void addDrawerContent() {
		Span appName = new Span("HRIS");
		appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
		Header header = new Header(appName);

		Scroller scroller = new Scroller(createNavigation());

		addToDrawer(header, scroller, createFooter());
	}

	private void logout() {
		sessionService.setUser(null);
		getUI().ifPresent(ui -> ui.navigate("login"));
	}

	private SideNav createNavigation() {
		SideNav nav = new SideNav();

		if (sessionService.getUser() != null) {
			List<Access> accesses = accessService.findByRole(sessionService.getUser().getRole());

			accesses.stream().map(Access::getModule).distinct().sorted(Comparator.comparingInt(Modules::getSequence))
					.forEach(module -> {
						Icon moduleIcon = createIcon(module.getIcon());
						SideNavItem parentNav = new SideNavItem(module.getName(), (String) null, moduleIcon);

						List<SubMenu> subMenus = subMenuService.findByModule(module);
						subMenus.stream().filter(SubMenu::isActive).sorted(Comparator.comparing(SubMenu::getName))
								.forEach(submenu -> {
									Icon submenuIcon = createIcon(submenu.getIcon());
									parentNav.addItem(
											new SideNavItem(submenu.getName(), submenu.getRoute(), submenuIcon));
								});

						if (!parentNav.getItems().isEmpty()) {
							nav.addItem(parentNav);
						}
					});
		}

		return nav;
	}

	private void toggleDarkMode() {
		var ui = getUI().orElse(null);
		if (ui != null) {
			var body = ui.getElement();
			if (body.hasAttribute("theme") && "dark".equals(body.getAttribute("theme"))) {
				body.removeAttribute("theme");
			} else {
				body.setAttribute("theme", "dark");
			}
		}
	}

	private Icon createIcon(String iconName) {
		try {
			// Example: if iconName = "DASHBOARD" -> VaadinIcon.DASHBOARD
			return iconName != null && !iconName.isEmpty() ? VaadinIcon.valueOf(iconName.toUpperCase()).create() : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private Footer createFooter() {
		return new Footer();
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();

		viewTitle.setText(getCurrentPageTitle());
	}

	private String getCurrentPageTitle() {
		return getContent().getClass().getSimpleName();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (sessionService.getUser() == null) {
			event.forwardTo("login");
		}
	}
}
