package io.github.vncntz.hris.app;

import java.time.Clock;
import io.github.vncntz.hris.identityaccess.LocalProvisioningIdentityConfiguration;
import io.github.vncntz.hris.platformoperations.JpaAuditRecorder;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Explicit source, with no component scan of web, Vaadin or ordinary application services. */
@EnableAutoConfiguration(excludeName = {"com.vaadin.flow.spring.SpringBootAutoConfiguration",
        "com.vaadin.flow.spring.SpringSecurityAutoConfiguration"})
@EntityScan(basePackages = {"io.github.vncntz.hris.identityaccess", "io.github.vncntz.hris.platformoperations"})
@EnableJpaRepositories(basePackages = "io.github.vncntz.hris.identityaccess")
@Import({LocalProvisioningIdentityConfiguration.class, JpaAuditRecorder.class})
public class LocalProvisioningApplication {
    @Bean
    Clock bootstrapClock() {
        return Clock.systemUTC();
    }

    public static ConfigurableApplicationContext open() {
        SpringApplication application = new SpringApplication(LocalProvisioningApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        // Fixed factory also prevents external spring.main configuration from creating a web context.
        application.setApplicationContextFactory(
                ApplicationContextFactory.ofContextClass(AnnotationConfigApplicationContext.class));
        application.setLogStartupInfo(false);
        return application.run("--spring.main.web-application-type=none", "--logging.level.root=OFF",
                "--spring.main.banner-mode=off", "--debug=false", "--trace=false",
                "--spring.jpa.show-sql=false", "--logging.level.org.hibernate.orm.jdbc.bind=OFF");
    }
}
