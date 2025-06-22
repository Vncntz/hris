package com.vincent.hris.security;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.router.RouterLayout;

public class LoginLayout extends AppLayout implements RouterLayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LoginLayout() {
		// Optional: styling for login-specific layout
		getElement().getStyle().set("display", "flex");
		getElement().getStyle().set("align-items", "center");
		getElement().getStyle().set("justify-content", "center");
		getElement().getStyle().set("height", "100vh");
	}
}
