package io.github.vncntz.hris;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.vncntz.hris.app.FirstAdministratorCommand;

@SpringBootApplication
public class HrisApplication {
    public static void main(String[] args) {
        if (FirstAdministratorCommand.requested(args)) {
            System.exit(FirstAdministratorCommand.run(args));
            return;
        }
        SpringApplication.run(HrisApplication.class, args);
    }
}
