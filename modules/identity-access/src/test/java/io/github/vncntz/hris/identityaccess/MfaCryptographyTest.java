package io.github.vncntz.hris.identityaccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MfaCryptographyTest {
    @TempDir Path temporary;
    MfaSecrets secrets() throws Exception {
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key);
        Path file = temporary.resolve(UUID.randomUUID().toString()); Files.write(file, key);
        java.util.Arrays.fill(key, (byte) 0);
        return new MfaSecrets(file.toString());
    }
    @Test void randomizedEncryptionRejectsWrongIdentityTamperKeyAndMissingKey() throws Exception {
        var secrets = secrets(); byte[] seed = secrets.generateSeed(); UUID id = UUID.randomUUID();
        byte[] encrypted = secrets.encrypt(id, seed);
        assertEquals(48, encrypted.length);
        assertTrue(java.util.Arrays.equals(seed, secrets.decrypt(id, encrypted)), "Decryption must recover the generated seed");
        assertFalse(java.util.Arrays.equals(encrypted, secrets.encrypt(id, seed)));
        assertThrows(MfaException.class, () -> secrets.decrypt(UUID.randomUUID(), encrypted));
        assertThrows(MfaException.class, () -> secrets().decrypt(id, encrypted));
        encrypted[15] ^= 1;
        var failure = assertThrows(MfaException.class, () -> secrets.decrypt(id, encrypted));
        assertNull(failure.getCause());
        assertThrows(MfaException.class, () -> new MfaSecrets("").encrypt(id, seed));
        assertThrows(MfaException.class, () -> new MfaSecrets("relative.key"));
        Files.write(temporary.resolve("invalid"), new byte[31]);
        assertThrows(MfaException.class, () -> new MfaSecrets(temporary.resolve("invalid").toString()));
    }
    @Test void rfcPublishedVectorIsComputedWithoutCommittingASeedFixture() {
        // Public RFC 6238 test vector, constructed algorithmically; never an installation secret.
        byte[] seed = new byte[20];
        for (int i = 0; i < seed.length; i++) { seed[i] = (byte) ('0' + (i + 1) % 10); }
        assertArrayEquals("287082".toCharArray(), Totp.code(seed, 1));
        assertArrayEquals("081804".toCharArray(), Totp.code(seed, 1111111109L / 30));
        assertArrayEquals("353130".toCharArray(), Totp.code(seed, 20000000000L / 30));
        java.util.Arrays.fill(seed, (byte) 0);
    }
    @Test void boundedSkewAndDurableCounterRejectReplayMalformedAndOldCodes() throws Exception {
        byte[] seed = secrets().generateSeed(); Instant now = Instant.ofEpochSecond(3000);
        assertEquals(99, Totp.match(seed, Totp.code(seed, 99), now, -1));
        assertEquals(100, Totp.match(seed, Totp.code(seed, 100), now, 99));
        assertEquals(101, Totp.match(seed, Totp.code(seed, 101), now, 100));
        assertEquals(-1, Totp.match(seed, Totp.code(seed, 100), now, 100));
        assertEquals(-1, Totp.match(seed, "12345x".toCharArray(), now, -1));
        assertEquals(-1, Totp.match(seed, new char[7], now, -1));
        assertEquals(-1, Totp.match(seed, Totp.code(seed, 100), now.plusSeconds(120), -1));
    }
    @Test void operationalPolicyRejectsUnboundedValuesAndAllowsTighterWindows() throws Exception {
        var window = java.time.Duration.ofMinutes(10);
        assertThrows(MfaException.class, () -> new MfaPolicy(2, window, 10));
        assertThrows(MfaException.class, () -> new MfaPolicy(-1, window, 10));
        assertThrows(MfaException.class, () -> new MfaPolicy(1, window.plusSeconds(1), 10));
        assertThrows(MfaException.class, () -> new MfaPolicy(1, java.time.Duration.ZERO, 10));
        assertThrows(MfaException.class, () -> new MfaPolicy(1, window, 0));
        assertThrows(MfaException.class, () -> new MfaPolicy(1, window, 11));
        assertThrows(MfaException.class, () -> new MfaPolicy(1, window, 1));
        assertEquals(2, new MfaPolicy(0, java.time.Duration.ofMinutes(2), 2).recoveryCount());
        byte[] seed = secrets().generateSeed();
        assertEquals(-1, Totp.match(seed, Totp.code(seed, 99), Instant.ofEpochSecond(3000), -1, 0));
    }
    @Test void factorConsumptionNeverAllowsPasswordOnlyAndClearsOwnedMaterial() throws Exception {
        var secrets = secrets(); var verifier = new MfaVerifier(secrets, new MfaPolicy(1, java.time.Duration.ofMinutes(10), 10));
        var account = new AccountEntity(UUID.randomUUID(), "synthetic", UUID.randomUUID().toString(), Instant.EPOCH);
        byte[] seed = secrets.generateSeed(); Instant now = Instant.ofEpochSecond(3000);
        char[] recovery = UUID.randomUUID().toString().replace("-", "").toCharArray();
        account.beginMfa(secrets.encrypt(account.publicId(), seed), now.plusSeconds(600));
        account.activateMfa(99, Totp.recoveryDigest(recovery));
        var unavailable = new MfaVerifier(new MfaSecrets(""), new MfaPolicy(1, java.time.Duration.ofMinutes(10), 10));
        assertThrows(MfaException.class, () -> unavailable.verify(account, recovery, now));
        account.mfaSecret()[1] ^= 1;
        assertThrows(MfaException.class, () -> verifier.verify(account, recovery, now));
        account.mfaSecret()[1] ^= 1;
        assertFalse(verifier.verify(account, null, now));
        assertTrue(verifier.verify(account, Totp.code(seed, 100), now));
        assertFalse(verifier.verify(account, Totp.code(seed, 100), now));
        assertTrue(verifier.verify(account, recovery, now));
        assertFalse(verifier.verify(account, recovery, now));
        var material = new MfaMaterial(new char[][] { recovery }); material.close();
        for (char c : recovery) { assertEquals(0, c); }
        char[] factor = Totp.code(seed, 101); new MfaInput(factor).close();
        for (char c : factor) { assertEquals(0, c); }
        assertFalse(material.toString().contains("secret"));
    }
}
