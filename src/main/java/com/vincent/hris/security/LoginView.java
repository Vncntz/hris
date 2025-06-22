package com.vincent.hris.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vincent.hris.modules.userandroles.model.User;
import com.vincent.hris.modules.userandroles.service.UserService;

@Route(value = "login", layout = LoginLayout.class)
@AnonymousAllowed
public class LoginView extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	private final UserService userService;
	private final SessionService sessionService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public LoginView(UserService userService, SessionService sessionService) {
		this.userService = userService;
		this.sessionService = sessionService;

		LoginOverlay loginOverlay = new LoginOverlay();
		loginOverlay.setOpened(true);
		loginOverlay.setTitle("HRIS");
		loginOverlay.setDescription("Login to continue");
		loginOverlay.addClassName("login-background");

		LoginI18n i18n = LoginI18n.createDefault();
		i18n.setAdditionalInformation(
				"Contact inocenciopaulvincent@gmail.com if you're experiencing issues logging into your account");
		loginOverlay.setI18n(i18n);

		loginOverlay.addLoginListener(event -> authenticate(event.getUsername(), event.getPassword(), loginOverlay));

		add(loginOverlay);
	}

	private void authenticate(String username, String password, LoginOverlay loginOverlay) {
		User user = userService.findByUsername(username);
		if (user != null && passwordEncoder.matches(password, user.getPassword())) {
			sessionService.setUser(user);
			loginOverlay.close();
			loginOverlay.getUI().ifPresent(ui -> ui.navigate("dashboard"));
		} else {
			loginOverlay.setError(true);
		}
	}
}
