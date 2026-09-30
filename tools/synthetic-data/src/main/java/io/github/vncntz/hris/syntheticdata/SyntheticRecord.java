package io.github.vncntz.hris.syntheticdata;

import io.github.vncntz.hris.sharedkernel.PublicId;
import java.util.Objects;

/** A generator identity only; it is not a Worker or other business entity. */
public record SyntheticRecord(int ordinal, PublicId publicId, String label) {
    public SyntheticRecord {
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be positive");
        }
        Objects.requireNonNull(publicId, "publicId");
        Objects.requireNonNull(label, "label");
    }
}
