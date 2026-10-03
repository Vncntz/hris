package io.github.vncntz.hris.identityaccess;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Dedicated external key; randomized authenticated encryption bound to Account identity. */
@Component
class MfaSecrets {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();
    MfaSecrets(@Value("${hris.security.mfa-key-file:}") String file) {
        byte[] loaded = null;
        try {
            if (!file.isBlank()) {
                Path path = Path.of(file);
                if (!path.isAbsolute() || !Files.isRegularFile(path) || Files.size(path) != 32) {
                    throw new MfaException();
                }
                loaded = Files.readAllBytes(path);
                if (loaded.length != 32) { throw new MfaException(); }
            }
            key = loaded;
        } catch (Exception failure) {
            if (loaded != null) { Arrays.fill(loaded, (byte) 0); }
            throw new MfaException();
        }
    }
    byte[] generateSeed() { byte[] seed = new byte[20]; random.nextBytes(seed); return seed; }
    byte[] encrypt(UUID id, byte[] seed) {
        requireKey();
        byte[] nonce = new byte[12]; random.nextBytes(nonce);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, id, nonce, seed);
        return ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
    }
    byte[] decrypt(UUID id, byte[] payload) {
        requireKey();
        if (payload == null || payload.length != 48) { throw new MfaException(); }
        return crypt(Cipher.DECRYPT_MODE, id, Arrays.copyOf(payload, 12), Arrays.copyOfRange(payload, 12, payload.length));
    }
    private byte[] crypt(int mode, UUID id, byte[] nonce, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array());
            return cipher.doFinal(input);
        } catch (Exception failure) { throw new MfaException(); }
    }
    void requireKey() { if (key == null) { throw new MfaException(); } }
    @jakarta.annotation.PreDestroy void clear() { if (key != null) { Arrays.fill(key, (byte) 0); } }
}
