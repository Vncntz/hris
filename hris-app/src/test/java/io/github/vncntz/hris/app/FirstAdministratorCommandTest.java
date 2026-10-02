package io.github.vncntz.hris.app;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;
import io.github.vncntz.hris.identityaccess.FirstAdministratorProvisioner;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirstAdministratorCommandTest {
    @Test
    void explicitOptInRejectsExtraArgumentsAndNonTerminalBeforeOpeningApplication() {
        assertFalse(FirstAdministratorCommand.requested(new String[]{}));
        assertFalse(FirstAdministratorCommand.requested(new String[]{"--server.port=0"}));
        assertTrue(FirstAdministratorCommand.requested(new String[]{FirstAdministratorCommand.OPTION}));
        assertTrue(FirstAdministratorCommand.requested(new String[]{FirstAdministratorCommand.OPTION + "=invalid"}));
        Supplier<ConfigurableApplicationContext> application = mock(Supplier.class);
        assertEquals(2, FirstAdministratorCommand.execute(new String[]{FirstAdministratorCommand.OPTION}, null, application));
        Input input = new Input();
        input.terminal = false;
        assertEquals(2, FirstAdministratorCommand.execute(new String[]{FirstAdministratorCommand.OPTION}, input, application));
        input.terminal = true;
        for (String[] args : new String[][]{{}, {FirstAdministratorCommand.OPTION, "extra"},
                {FirstAdministratorCommand.OPTION + "=invalid"}}) {
            assertEquals(2, FirstAdministratorCommand.execute(args, input, application));
        }
        verifyNoInteractions(application);
    }

    @Test
    void successClearsSecretsClosesContextAndPrintsOnlyFixedResult() {
        Input input = new Input();
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        FirstAdministratorProvisioner provisioner = mock(FirstAdministratorProvisioner.class);
        when(context.getBean(FirstAdministratorProvisioner.class)).thenReturn(provisioner);
        assertEquals(0, FirstAdministratorCommand.execute(new String[]{FirstAdministratorCommand.OPTION}, input, () -> context));
        verify(provisioner).provision("synthetic.login", input.password, input.confirmation);
        verify(context).close();
        assertCleared(input);
        assertTrue(input.message.startsWith("First administrator provisioned."));
    }

    @Test
    void cancellationMismatchInvalidLoginAndApplicationFailureHaveNoSecretDiagnostics() {
        for (int scenario = 0; scenario < 5; scenario++) {
            Input input = new Input();
            if (scenario == 0) { input.login = null; }
            if (scenario == 1) { input.confirmation[0] ^= 1; }
            if (scenario == 2) { input.password = null; }
            if (scenario == 3) { input.confirmation = null; }
            boolean[] opened = {false};
            assertEquals(1, FirstAdministratorCommand.execute(new String[]{FirstAdministratorCommand.OPTION}, input, () -> {
                opened[0] = true;
                throw new IllegalStateException(UUID.randomUUID().toString());
            }));
            assertEquals(scenario == 4, opened[0]);
            // Invalid login exits before reading any password; the console still owns unread buffers.
            if (scenario != 0) { assertCleared(input); }
            assertTrue(input.message.startsWith("Provisioning refused or failed;"));
        }
    }

    private static void assertCleared(Input input) {
        for (char[] value : new char[][]{input.password, input.confirmation}) {
            if (value != null) { assertTrue(Arrays.equals(value, new char[value.length])); }
        }
    }

    private static class Input implements FirstAdministratorCommand.SecureInput {
        boolean terminal = true;
        String login = " SYNTHETIC.LOGIN ";
        char[] password = UUID.randomUUID().toString().toCharArray();
        char[] confirmation = password.clone();
        String message;
        public boolean isTerminal() { return terminal; }
        public String login() { return login; }
        public char[] password() { return password; }
        public char[] confirmation() { return confirmation; }
        public void message(String value) { message = value; }
    }
}
