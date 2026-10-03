package io.github.vncntz.hris.clientmanagement;

import java.util.UUID;

/** Stored Site activity is independent of its parent's activity. */
public record ClientSiteReference(UUID publicId, UUID companyPublicId, String displayName,
                                  boolean active, boolean companyActive, long version) {
    public boolean effectiveActive() { return active && companyActive; }
}
