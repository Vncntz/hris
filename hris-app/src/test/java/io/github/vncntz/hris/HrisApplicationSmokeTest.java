package io.github.vncntz.hris;

import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class HrisApplicationSmokeTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithVaadinServlet() {
        assertNotNull(context.getBean(HrisApplication.class));
        ServletRegistrationBean<?> registration = context.getBean(
                "servletRegistrationBean", ServletRegistrationBean.class);
        assertInstanceOf(SpringServlet.class, registration.getServlet());
        assertEquals("/*", context.getBean(VaadinConfigurationProperties.class).getUrlMapping());
        // Root requests are forwarded by Vaadin's Spring MVC integration to this servlet.
        assertTrue(registration.getUrlMappings().contains("/vaadinServlet/*"));
    }
}
