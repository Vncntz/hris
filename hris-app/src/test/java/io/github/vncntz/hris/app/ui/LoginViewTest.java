package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.PermitAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginViewTest {

    @Test
    void routeIsAnonymousAndLoadsLoginStylesheet() {
        Route route = LoginView.class.getAnnotation(Route.class);
        assertNotNull(route);
        assertEquals("login", route.value());
        assertNotNull(LoginView.class.getAnnotation(AnonymousAllowed.class));
        assertNull(LoginView.class.getAnnotation(PermitAll.class));
        PageTitle title = LoginView.class.getAnnotation(PageTitle.class);
        assertNotNull(title);
        assertEquals("HRIS login", title.value());

        StyleSheet styleSheet = LoginView.class.getAnnotation(StyleSheet.class);
        assertNotNull(styleSheet, "LoginView should declare its stylesheet");
        assertEquals("themes/hris/login.css", styleSheet.value());
    }

    @Test
    void preservesLoginFormActionAndHidesForgotPassword() {
        LoginView view = new LoginView();
        LoginForm form = find(view, LoginForm.class);
        assertEquals("login", form.getAction());
        assertFalse(form.isForgotPasswordButtonVisible());
        assertFalse(form.isError());
    }

    @Test
    void genericErrorShownWhenQueryParamPresent() {
        LoginView view = new LoginView();
        LoginForm form = find(view, LoginForm.class);
        assertFalse(form.isError());

        BeforeEnterEvent errorEvent = mock(BeforeEnterEvent.class);
        Location location = new Location("login?error",
                new QueryParameters(Map.of("error", List.of(""))));
        when(errorEvent.getLocation()).thenReturn(location);

        view.beforeEnter(errorEvent);
        assertTrue(form.isError());
    }

    @Test
    void noErrorShownWhenQueryParamAbsent() {
        LoginView view = new LoginView();
        LoginForm form = find(view, LoginForm.class);

        BeforeEnterEvent cleanEvent = mock(BeforeEnterEvent.class);
        Location location = new Location("login", QueryParameters.empty());
        when(cleanEvent.getLocation()).thenReturn(location);

        view.beforeEnter(cleanEvent);
        assertFalse(form.isError());
    }

    @Test
    void visualHierarchyHasSingleH1AndDecorativeBrandMark() {
        LoginView view = new LoginView();
        assertEquals(1, descendants(view).filter(H1.class::isInstance).count(),
                "Page must have exactly one H1 heading");
        H1 h1 = find(view, H1.class);
        assertEquals("Sign in to your account", h1.getText());
        assertEquals(Optional.of("hris-login-heading"), h1.getId());

        Icon mark = find(view, Icon.class);
        assertEquals("true", mark.getElement().getAttribute("aria-hidden"));

        assertTrue(descendants(view).filter(Span.class::isInstance)
                .map(Span.class::cast)
                .anyMatch(s -> "HRIS".equals(s.getText())));
        assertTrue(descendants(view).filter(Span.class::isInstance)
                .map(Span.class::cast)
                .anyMatch(s -> "Manpower & Staffing".equals(s.getText())));
    }

    private static <T extends Component> T find(Component root, Class<T> type) {
        return descendants(root).filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(LoginViewTest::descendants));
    }
}
