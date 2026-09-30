package io.github.vncntz.hris.sharedkernel;

import java.util.Objects;
import java.util.UUID;

/** A public identifier with UUID value semantics and canonical UUID text. */
public record PublicId(UUID value) {
    public PublicId {
        Objects.requireNonNull(value, "value");
    }

    public static PublicId of(UUID value) {
        return new PublicId(value);
    }

    public static PublicId parse(String text) {
        Objects.requireNonNull(text, "text");
        UUID parsed = UUID.fromString(text);
        if (!parsed.toString().equalsIgnoreCase(text)) {
            throw new IllegalArgumentException("Not canonical UUID text: " + text);
        }
        return new PublicId(parsed);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
