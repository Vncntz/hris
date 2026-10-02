package io.github.vncntz.hris.identityaccess;

import java.nio.CharBuffer;
import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirstAdministratorProvisionerTest {
    @Test
    void passwordBoundsIncludeBothEndpointsAndPreserveWhitespaceAndUnicode() {
        for (int size : new int[]{12, 128}) {
            char[] value = ephemeral(size);
            assertDoesNotThrow(() -> FirstAdministratorProvisioner.validatePasswords(value, value.clone()));
        }
        for (int size : new int[]{0, 11, 129}) {
            char[] value = ephemeral(size);
            assertThrows(IllegalArgumentException.class,
                    () -> FirstAdministratorProvisioner.validatePasswords(value, value.clone()));
        }
        char[] value = ("  " + UUID.randomUUID() + "  ").toCharArray();
        PasswordEncoder encoder = new AuthenticationSecurityConfiguration().passwordEncoder();
        String hash = encoder.encode(CharBuffer.wrap(value));
        assertTrue(encoder.matches(CharBuffer.wrap(value), hash));
        assertFalse(encoder.matches(CharBuffer.wrap(value, 2, value.length - 4), hash));
        char[] maximum = ephemeral(128);
        String maximumHash = encoder.encode(CharBuffer.wrap(maximum));
        assertTrue(encoder.matches(CharBuffer.wrap(maximum), maximumHash));
        maximum[127] ^= 1;
        assertFalse(encoder.matches(CharBuffer.wrap(maximum), maximumHash));
        char[] unicode = new String(Character.toChars(0x1F680)).repeat(128).toCharArray();
        assertDoesNotThrow(() -> FirstAdministratorProvisioner.validatePasswords(unicode, unicode.clone()));
        char[] over = new String(Character.toChars(0x1F680)).repeat(129).toCharArray();
        assertThrows(IllegalArgumentException.class,
                () -> FirstAdministratorProvisioner.validatePasswords(over, over.clone()));
        char[] invalidUnicode = ephemeral(24);
        invalidUnicode[0] = '\uD800';
        assertThrows(IllegalArgumentException.class,
                () -> FirstAdministratorProvisioner.validatePasswords(invalidUnicode, invalidUnicode.clone()));
    }

    @Test
    void everyValidationAndEncodingFailureClearsBothBuffersWithoutTouchingDatabase() {
        EntityManager entities = mock(EntityManager.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        FirstAdministratorProvisioner service = new FirstAdministratorProvisioner(entities, encoder,
                mock(AuditRecorder.class), Clock.systemUTC(), transactions);
        for (int scenario = 0; scenario < 5; scenario++) {
            char[] password = ephemeral(scenario == 0 ? 11 : 24);
            char[] confirmation = password.clone();
            String login = scenario == 1 ? "bad login" : "synthetic.login";
            if (scenario == 2) { confirmation[0] ^= 1; }
            if (scenario == 3) { confirmation = null; }
            if (scenario == 4) { when(encoder.encode(any())).thenThrow(new IllegalStateException("Encoding failed")); }
            char[] second = confirmation;
            assertThrows(RuntimeException.class, () -> service.provision(login, password, second));
            assertTrue(allCleared(password));
            if (second != null) { assertTrue(allCleared(second)); }
        }
        verifyNoInteractions(entities, transactions);
    }

    @Test
    void loginValidationAndCanonicalBootstrapNamesAreBounded() {
        assertEquals("synthetic.login", LoginNames.canonicalize(" SYNTHETIC.LOGIN "));
        for (String invalid : new String[]{null, "", "ab", "bad login", "x".repeat(129), "éclair"}) {
            assertThrows(IllegalArgumentException.class, () -> LoginNames.canonicalize(invalid));
        }
        assertEquals("administrator", FirstAdministratorProvisioner.ROLE);
        assertEquals("identity:admin", FirstAdministratorProvisioner.AUTHORITY);
        assertFalse(new RoleEntity(UUID.randomUUID(), FirstAdministratorProvisioner.ROLE, true)
                .authorityKeys().contains(FirstAdministratorProvisioner.AUTHORITY));
    }

    private static char[] ephemeral(int length) {
        char[] result = new char[length];
        char[] random = UUID.randomUUID().toString().toCharArray();
        for (int i = 0; i < length; i++) { result[i] = random[i % random.length]; }
        Arrays.fill(random, '\0');
        return result;
    }

    private static boolean allCleared(char[] value) {
        for (char item : value) { if (item != '\0') { return false; } }
        return true;
    }
}
