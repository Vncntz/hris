package io.github.vncntz.hris.identityaccess;

import java.util.Arrays;

/** Shared initial-credential policy owned by Identity & Access. */
final class InitialCredentials {
    static final int MIN_LENGTH = 12;
    static final int MAX_LENGTH = 128;

    private InitialCredentials() { }

    static void validate(char[] password, char[] confirmation) {
        if (password == null || confirmation == null) {
            throw new IllegalArgumentException("Provisioning cancelled");
        }
        int length = Character.codePointCount(password, 0, password.length);
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException("Password must contain 12 to 128 Unicode characters");
        }
        for (int i = 0; i < password.length; i++) {
            if (Character.isHighSurrogate(password[i])) {
                if (++i >= password.length || !Character.isLowSurrogate(password[i])) {
                    throw new IllegalArgumentException("Password contains invalid Unicode");
                }
            } else if (Character.isLowSurrogate(password[i])) {
                throw new IllegalArgumentException("Password contains invalid Unicode");
            }
        }
        if (!Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
    }

    static void clear(char[] value) {
        if (value != null) { Arrays.fill(value, '\0'); }
    }
}
