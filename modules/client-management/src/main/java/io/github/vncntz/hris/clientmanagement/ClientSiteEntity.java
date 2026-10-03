package io.github.vncntz.hris.clientmanagement;

import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistence remains exclusively owned by Client Management. */
@Entity
@Table(name = "client_site")
class ClientSiteEntity {
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private ClientCompanyEntity company;

    protected ClientSiteEntity() {}
    ClientSiteEntity(UUID publicId, String displayName, ClientCompanyEntity company) {
        this.publicId = publicId;
        this.displayName = displayName;
        this.active = true;
        this.company = company;
    }
    UUID publicId() { return publicId; }
    String displayName() { return displayName; }
    boolean active() { return active; }
    long version() { return rowVersion == null ? 0 : rowVersion; }
    void rename(String name) { displayName = name; }
    void changeActive(boolean value) { active = value; }
    ClientSiteReference snapshot() {
        return new ClientSiteReference(publicId, company.publicId(), displayName, active, company.active(), version());
    }
}
