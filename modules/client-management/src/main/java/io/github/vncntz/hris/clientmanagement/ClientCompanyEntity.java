package io.github.vncntz.hris.clientmanagement;

import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistence remains exclusively owned by Client Management. */
@Entity
@Table(name = "client_company")
class ClientCompanyEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID publicId;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Column(nullable = false)
    private boolean active;
    @Version @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    protected ClientCompanyEntity() {}
    ClientCompanyEntity(UUID publicId, String displayName) {
        this.publicId = publicId;
        this.displayName = displayName;
        this.active = true;

    }
    UUID publicId() { return publicId; }
    String displayName() { return displayName; }
    boolean active() { return active; }
    long version() { return rowVersion == null ? 0 : rowVersion; }
    void rename(String name) { displayName = name; }
    void changeActive(boolean value) { active = value; }
    ClientCompanyReference snapshot() {
        return new ClientCompanyReference(publicId, displayName, active, version());
    }
}
