package io.github.vncntz.hris.platformoperations;

import java.util.Objects;

import io.github.vncntz.hris.sharedkernel.BusinessTimeZone;
import io.github.vncntz.hris.sharedkernel.PublicId;
import io.github.vncntz.hris.sharedkernel.UtcInstant;

/** Read-only application projection; persistence remains module-internal. */
public record AgencyConfiguration(PublicId publicId, String displayName,
                                  BusinessTimeZone businessTimeZone, UtcInstant createdAt,
                                  UtcInstant updatedAt, long version) {
    public AgencyConfiguration {
        Objects.requireNonNull(publicId, "publicId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(businessTimeZone, "businessTimeZone");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
