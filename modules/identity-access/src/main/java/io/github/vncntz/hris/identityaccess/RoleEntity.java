package io.github.vncntz.hris.identityaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Internal authorization role; administration belongs to a later IMP-013 task. */
@Entity
@Table(name = "identity_role")
class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @Column(name = "canonical_name", nullable = false, unique = true, length = 128)
    private String canonicalName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @ManyToMany
    @JoinTable(name = "identity_role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionEntity> permissions = new HashSet<>();

    protected RoleEntity() {
    }

    RoleEntity(UUID publicId, String name, boolean enabled) {
        this.publicId = java.util.Objects.requireNonNull(publicId);
        this.canonicalName = canonicalize(name);
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }

    void assignBootstrapPermission(PermissionEntity permission) {
        permissions.add(permission);
    }

    List<String> authorityKeys() {
        return permissions.stream().map(PermissionEntity::authorityKey).toList();
    }

    private static String canonicalize(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Role name is required");
        }
        String canonical = name.strip().toLowerCase(Locale.ROOT);
        if (!canonical.matches("[a-z0-9][a-z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("Invalid role name");
        }
        return canonical;
    }
}
