package io.github.vncntz.hris;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalConfigurationTest {
    @TempDir
    Path installationConfig;

    @Test
    void externalApplicationPropertiesOverridePackagedSafeDefault() throws Exception {
        Files.writeString(installationConfig.resolve("application.properties"),
                "spring.application.name=synthetic-installation\n", StandardCharsets.UTF_8);

        try (ConfigurableApplicationContext context = run(
                "--spring.config.additional-location=" + installationConfig.toUri())) {
            assertEquals("synthetic-installation", context.getEnvironment().getProperty("spring.application.name"));
        }
    }

    @Test
    void separatelySuppliedSyntheticSecretFileLoadsAfterInstallationConfiguration() throws Exception {
        Path config = installationConfig.resolve("application.properties");
        Path secrets = installationConfig.resolve("secrets.properties");
        Files.writeString(config, "synthetic.config.probe=installation\n", StandardCharsets.UTF_8);
        Files.writeString(secrets, "synthetic.config.probe=protected-source\n", StandardCharsets.UTF_8);

        try (ConfigurableApplicationContext context = run(
                "--spring.config.additional-location=" + config.toUri() + "," + secrets.toUri())) {
            assertEquals("protected-source", context.getEnvironment().getProperty("synthetic.config.probe"));
        }
    }

    @Test
    void developmentProfileIsLoadedOnlyWhenExplicitlyActivated() {
        try (ConfigurableApplicationContext context = run()) {
            assertEquals("hris", context.getEnvironment().getProperty("spring.application.name"));
            assertFalse(context.getEnvironment().acceptsProfiles("dev"));
            assertNull(context.getEnvironment().getProperty("server.address"));
        }

        try (ConfigurableApplicationContext context = run("--spring.profiles.active=dev")) {
            assertTrue(context.getEnvironment().acceptsProfiles("dev"));
            assertEquals("127.0.0.1", context.getEnvironment().getProperty("server.address"));
        }
    }

    @Test
    void missingRequiredExternalLocationStopsStartup() {
        Path missing = installationConfig.resolve("missing.properties");

        ConfigDataResourceNotFoundException failure = assertThrows(ConfigDataResourceNotFoundException.class,
                () -> run("--spring.config.additional-location=" + missing.toUri()));
        assertTrue(failure.getMessage().contains("missing"), failure.getMessage());
    }

    private static ConfigurableApplicationContext run(String... additionalArguments) {
        SpringApplication application = new SpringApplication(EmptyConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setEnvironment(new MockEnvironment());
        application.setLogStartupInfo(false);

        String[] arguments = new String[additionalArguments.length + 1];
        arguments[0] = "--spring.config.location=classpath:/";
        System.arraycopy(additionalArguments, 0, arguments, 1, additionalArguments.length);
        return application.run(arguments);
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfiguration {
    }
}
