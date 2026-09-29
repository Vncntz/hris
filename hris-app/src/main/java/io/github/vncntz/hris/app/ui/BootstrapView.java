package io.github.vncntz.hris.app.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("")
public class BootstrapView extends Div {
    public BootstrapView() {
        setText("HRIS application skeleton is running.");
    }
}
