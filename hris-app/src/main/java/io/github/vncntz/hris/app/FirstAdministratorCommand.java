package io.github.vncntz.hris.app;

import java.io.Console;
import java.util.Arrays;
import java.util.function.Supplier;
import io.github.vncntz.hris.identityaccess.FirstAdministratorProvisioner;
import io.github.vncntz.hris.identityaccess.LoginNames;
import org.springframework.context.ConfigurableApplicationContext;

/** Local adapter. No credentials in arguments, properties, request objects or diagnostic output. */
public final class FirstAdministratorCommand {
    public static final String OPTION = "--provision-first-administrator";

    private FirstAdministratorCommand() {
    }

    public static boolean requested(String[] args) {
        return Arrays.stream(args).anyMatch(arg -> arg.startsWith(OPTION));
    }

    public static int run(String[] args) {
        Console console = System.console();
        SecureInput input = console == null ? null : new SecureInput() {
            public boolean isTerminal() { return console.isTerminal(); }
            public String login() { return console.readLine("Login: "); }
            public char[] password() { return console.readPassword("Password: "); }
            public char[] confirmation() { return console.readPassword("Confirm password: "); }
            public void message(String value) { console.printf("%s%n", value); }
        };
        int result = execute(args, input, LocalProvisioningApplication::open);
        if (input == null || !input.isTerminal()) {
            System.err.println("Provisioning refused: an interactive secure terminal is required");
        }
        return result;
    }

    static int execute(String[] args, SecureInput input,
            Supplier<ConfigurableApplicationContext> application) {
        if (input == null || !input.isTerminal()) {
            return 2;
        }
        if (args.length != 1 || !OPTION.equals(args[0])) {
            input.message("Provisioning refused: use only the explicit local command option");
            return 2;
        }
        char[] password = null;
        char[] confirmation = null;
        char[][] secrets = new char[2][];
        Thread cleanup = new Thread(() -> clear(secrets), "bootstrap-secret-cleanup");
        Runtime.getRuntime().addShutdownHook(cleanup);
        try {
            String login = LoginNames.canonicalize(input.login());
            password = secrets[0] = input.password();
            confirmation = secrets[1] = input.confirmation();
            FirstAdministratorProvisioner.validatePasswords(password, confirmation);
            try (ConfigurableApplicationContext context = application.get()) {
                context.getBean(FirstAdministratorProvisioner.class).provision(login, password, confirmation);
            }
            input.message("First administrator provisioned. Use normal login after starting the application.");
            return 0;
        } catch (RuntimeException failure) {
            // Do not print exception details: persistence/encoder exceptions may contain secrets.
            input.message("Provisioning refused or failed; no successful completion was reported. "
                    + "Check installation state through authorized local administration.");
            return 1;
        } finally {
            clear(secrets);
            try { Runtime.getRuntime().removeShutdownHook(cleanup); }
            catch (IllegalStateException shutdownInProgress) { /* Hook clears any remaining buffers. */ }
        }
    }

    private static void clear(char[][] secrets) {
        for (char[] value : secrets) { if (value != null) { Arrays.fill(value, '\0'); } }
    }

    interface SecureInput {
        boolean isTerminal();
        String login();
        char[] password();
        char[] confirmation();
        void message(String value);
    }
}
