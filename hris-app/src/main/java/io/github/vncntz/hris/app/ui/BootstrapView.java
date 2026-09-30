package io.github.vncntz.hris.app.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@Route("")
@PermitAll
public class BootstrapView extends Div {
    public BootstrapView(AuthenticationContext authenticationContext) {
        setText("HRIS application skeleton is running.");
        add(new Button("Log out", event -> authenticationContext.logout()));
    }
}
