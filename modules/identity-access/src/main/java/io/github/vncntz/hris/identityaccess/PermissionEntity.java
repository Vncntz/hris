package io.github.vncntz.hris.identityaccess;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Stable authority key owned by Identity & Access. */
@Entity
@Table(name = "identity_permission")
class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "authority_key", nullable = false, unique = true, length = 128)
    private String authorityKey;

    protected PermissionEntity() {
    }

    PermissionEntity(String authorityKey) {
        if (authorityKey == null) {
            throw new IllegalArgumentException("Authority key is required");
        }
        String canonical = authorityKey.strip().toLowerCase(Locale.ROOT);
        if (!canonical.matches("[a-z][a-z0-9._:-]{0,127}")) {
            throw new IllegalArgumentException("Invalid authority key");
        }
        this.authorityKey = canonical;
    }

    String authorityKey() {
        return authorityKey;
    }
}
