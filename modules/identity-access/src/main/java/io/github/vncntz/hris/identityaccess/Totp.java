package io.github.vncntz.hris.identityaccess;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238 SHA-1 / six digits / thirty seconds; no network dependency. */
final class Totp {
    private Totp() { }
    static char[] code(byte[] seed, long counter) {
        byte[] digest = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(seed, "HmacSHA1"));
            digest = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = digest[digest.length - 1] & 15;
            int binary = ((digest[offset] & 127) << 24) | ((digest[offset + 1] & 255) << 16)
                    | ((digest[offset + 2] & 255) << 8) | (digest[offset + 3] & 255);
            char[] result = new char[6];
            int number = binary % 1_000_000;
            for (int i = 5; i >= 0; i--) { result[i] = (char) ('0' + number % 10); number /= 10; }
            return result;
        } catch (Exception failure) { throw new MfaException(); }
        finally { if (digest != null) { java.util.Arrays.fill(digest, (byte) 0); } }
    }
    static long match(byte[] seed, char[] supplied, Instant now, long last) {
        return match(seed, supplied, now, last, 1);
    }
    static long match(byte[] seed, char[] supplied, Instant now, long last, int skew) {
        if (supplied == null || supplied.length != 6) { return -1; }
        for (char c : supplied) { if (c < '0' || c > '9') { return -1; } }
        long current = Math.floorDiv(now.getEpochSecond(), 30);
        long matched = -1;
        // Inspect the entire bounded window; choose latest if codes collide.
        for (long step = current - skew; step <= current + skew; step++) {
            char[] expected = code(seed, step);
            int difference = 0;
            for (int i = 0; i < 6; i++) { difference |= expected[i] ^ supplied[i]; }
            InitialCredentials.clear(expected);
            if (difference == 0 && step >= 0 && step > last) { matched = step; }
        }
        return matched;
    }
    static char[] base32(byte[] seed) {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        char[] result = new char[(seed.length * 8 + 4) / 5];
        int bits = 0, accumulator = 0, at = 0;
        for (byte b : seed) {
            accumulator = (accumulator << 8) | (b & 255); bits += 8;
            while (bits >= 5) { bits -= 5; result[at++] = alphabet[(accumulator >>> bits) & 31]; }
        }
        if (bits > 0) { result[at] = alphabet[(accumulator << (5 - bits)) & 31]; }
        return result;
    }
    static String recoveryDigest(char[] code) {
        if (code == null || code.length != 32) { return ""; }
        byte[] bytes = new byte[32];
        try {
            for (int i = 0; i < code.length; i++) {
                char c = code[i];
                if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) { return ""; }
                bytes[i] = (byte) c;
            }
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception failure) { throw new MfaException(); }
        finally { java.util.Arrays.fill(bytes, (byte) 0); }
    }
}
