package io.github.vncntz.hris.app.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.lumo.Lumo;

/** Application-wide page shell: one consistent theme for the login page and the authenticated shell. */
@StyleSheet(Lumo.STYLESHEET)
public class HrisAppShell implements AppShellConfigurator {
    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addMetaTag("color-scheme", "light");
    }
}
