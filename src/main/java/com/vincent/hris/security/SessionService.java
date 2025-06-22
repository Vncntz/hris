package com.vincent.hris.security;

import com.vaadin.flow.server.VaadinSession;
import com.vincent.hris.modules.userandroles.model.User;
import org.springframework.stereotype.Component;

@Component
public class SessionService {

	private static final String USER_SESSION_KEY = "loggedInUser";

	public void setUser(User user) {
		VaadinSession.getCurrent().setAttribute(USER_SESSION_KEY, user);
	}

	public User getUser() {
		return (User) VaadinSession.getCurrent().getAttribute(USER_SESSION_KEY);
	}

	public void logout() {
		VaadinSession.getCurrent().getSession().invalidate(); // destroys HttpSession
		VaadinSession.getCurrent().close();
	}
}
