package com.vincent.hris.util;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.html.Span;

public class NotificationUtil {

    private static final int DEFAULT_DURATION = 3000; 

    public static void success(String message) {
        show(message, NotificationVariant.LUMO_SUCCESS);
    }

    public static void error(String message) {
        show(message, NotificationVariant.LUMO_ERROR);
    }

    public static void warning(String message) {
        show(message, NotificationVariant.LUMO_CONTRAST);
    }

    public static void info(String message) {
        show(message, NotificationVariant.LUMO_PRIMARY);
    }

    private static void show(String message, NotificationVariant variant) {
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.setDuration(DEFAULT_DURATION);
        notification.addThemeVariants(variant);
        notification.add(new Span(message));
        notification.open();
    }
}
