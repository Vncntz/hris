package io.github.vncntz.hris.identityaccess;

import java.util.Locale;
import java.util.regex.Pattern;

/** Stable ASCII login identifiers; case folding never depends on the host locale. */
public final class LoginNames {
    private static final Pattern SUPPORTED = Pattern.compile("[a-z0-9][a-z0-9._-]{2,127}");

    private LoginNames() {
    }

    public static String canonicalize(String supplied) {
        if (supplied == null) {
            throw new IllegalArgumentException("Unsupported login identifier");
        }
        String value = supplied.strip().toLowerCase(Locale.ROOT);
        if (!SUPPORTED.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsupported login identifier");
        }
        return value;
    }
}
