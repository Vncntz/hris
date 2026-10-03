package io.github.vncntz.hris.app.ui;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("HRIS login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final LoginForm login = new LoginForm();

    public LoginView() {
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        // LoginForm posts native inputs in its form. Keep factor solely in the browser request,
        // rather than synchronizing secret text into Vaadin's server-side component/session tree.
        login.getElement().executeJs("""
                const install = () => {
                    const form = this.querySelector('form');
                    if (!form || form.querySelector('[name=factor]')) return !!form;
                    const label = document.createElement('label');
                    label.textContent = 'Authenticator or recovery code (if enrolled)';
                    const input = document.createElement('input');
                    input.name = 'factor'; input.type = 'password'; input.maxLength = 32;
                    input.autocomplete = 'one-time-code';
                    label.appendChild(input); form.insertBefore(label, form.firstChild);
                    form.addEventListener('formdata', () => { input.value = ''; });
                    return true;
                };
                if (!install()) {
                    const observer = new MutationObserver(() => { if (install()) observer.disconnect(); });
                    observer.observe(this, { childList: true, subtree: true });
                }
                """);
        add(login);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
