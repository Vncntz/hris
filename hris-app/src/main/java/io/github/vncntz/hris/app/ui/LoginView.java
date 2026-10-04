package io.github.vncntz.hris.app.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("HRIS login")
@AnonymousAllowed
@StyleSheet("themes/hris/login.css")
public class LoginView extends Div implements BeforeEnterObserver {
    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("hris-login-page");

        login.addClassName("hris-login-form");
        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("");
        i18n.getForm().setUsername("Username");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Sign in");
        i18n.getErrorMessage().setTitle("Sign in failed");
        i18n.getErrorMessage().setMessage(
                "Check that you have entered the correct username, password, and authenticator code (if enrolled) and try again.");
        login.setI18n(i18n);

        // LoginForm posts native inputs in its form. Keep factor solely in the browser request,
        // rather than synchronizing secret text into Vaadin's server-side component/session tree.
        login.getElement().executeJs("""
                const install = () => {
                    const form = this.querySelector('form');
                    if (!form || form.querySelector('[name=factor]')) return !!form;

                    const group = document.createElement('div');
                    group.className = 'hris-factor-group';

                    const label = document.createElement('label');
                    label.htmlFor = 'hris-factor-input';
                    label.className = 'hris-factor-label';
                    label.textContent = 'Authenticator or recovery code ';

                    const badge = document.createElement('span');
                    badge.className = 'hris-factor-badge';
                    badge.textContent = 'Optional';
                    label.appendChild(badge);

                    const inputWrapper = document.createElement('div');
                    inputWrapper.className = 'hris-factor-input-wrapper';

                    const input = document.createElement('input');
                    input.id = 'hris-factor-input';
                    input.name = 'factor';
                    input.type = 'password';
                    input.maxLength = 32;
                    input.autocomplete = 'one-time-code';
                    input.className = 'hris-factor-input';
                    input.placeholder = '6-digit code or recovery key';
                    input.setAttribute('aria-describedby', 'hris-factor-hint');

                    inputWrapper.appendChild(input);

                    const hint = document.createElement('div');
                    hint.id = 'hris-factor-hint';
                    hint.className = 'hris-factor-hint';
                    hint.textContent = 'Enter only if two-factor authentication is enrolled for your account.';

                    group.appendChild(label);
                    group.appendChild(inputWrapper);
                    group.appendChild(hint);

                    form.appendChild(group);

                    form.addEventListener('formdata', () => {
                        input.value = '';
                    });
                    return true;
                };
                if (!install()) {
                    const observer = new MutationObserver(() => {
                        if (install()) observer.disconnect();
                    });
                    observer.observe(this, { childList: true, subtree: true });
                }
                """);

        Icon mark = new Icon(VaadinIcon.USERS);
        mark.addClassName("hris-brand-mark");
        mark.getElement().setAttribute("aria-hidden", "true");

        Span brandName = new Span("HRIS");
        brandName.addClassName("hris-brand-name");
        Span tagline = new Span("Manpower & Staffing");
        tagline.addClassName("hris-brand-tagline");
        Div brandText = new Div(brandName, tagline);
        brandText.addClassName("hris-brand-text");
        Div brand = new Div(mark, brandText);
        brand.addClassName("hris-login-brand");

        H1 heading = new H1("Sign in to your account");
        heading.setId("hris-login-heading");
        heading.addClassName("hris-login-title");

        Paragraph subtitle = new Paragraph("Enter your workforce credentials to access the workspace.");
        subtitle.addClassName("hris-login-subtitle");

        Header header = new Header(brand, heading, subtitle);
        header.addClassName("hris-login-header");

        Footer footer = new Footer(new Span("Authorized workforce personnel only"));
        footer.addClassName("hris-login-footer");

        Main card = new Main(header, login, footer);
        card.addClassName("hris-login-card");
        card.getElement().setAttribute("role", "main");
        card.getElement().setAttribute("aria-labelledby", "hris-login-heading");

        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
