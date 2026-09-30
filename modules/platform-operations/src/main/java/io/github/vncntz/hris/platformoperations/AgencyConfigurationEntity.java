package io.github.vncntz.hris.platformoperations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.vncntz.hris.sharedkernel.BusinessTimeZone;
import io.github.vncntz.hris.sharedkernel.PublicId;
import io.github.vncntz.hris.sharedkernel.UtcInstant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Module-internal mapping of the installation's single Agency root. */
@Entity
@Table(name = "agency_configuration")
class AgencyConfigurationEntity {
    @Id
    @Column(name = "singleton_key", nullable = false)
    private Byte singletonKey;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "business_time_zone", nullable = false, length = 64)
    private String businessTimeZone;

    @Column(name = "created_at_utc", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAtUtc;

    @Column(name = "updated_at_utc", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAtUtc;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected AgencyConfigurationEntity() {
        // Required by JPA.
    }

    AgencyConfigurationEntity(PublicId publicId, String displayName,
                              BusinessTimeZone businessTimeZone, Instant now) {
        this.singletonKey = (byte) 1;
        this.publicId = publicId.value();
        this.displayName = displayName;
        this.businessTimeZone = businessTimeZone.toString();
        this.createdAtUtc = utc(now);
        this.updatedAtUtc = utc(now);
    }

    AgencyConfiguration snapshot() {
        return new AgencyConfiguration(PublicId.of(publicId), displayName,
                BusinessTimeZone.of(businessTimeZone),
                UtcInstant.of(createdAtUtc.toInstant(ZoneOffset.UTC)),
                UtcInstant.of(updatedAtUtc.toInstant(ZoneOffset.UTC)), rowVersion);
    }

    void update(String displayName, BusinessTimeZone businessTimeZone, Instant now) {
        this.displayName = displayName;
        this.businessTimeZone = businessTimeZone.toString();
        this.updatedAtUtc = utc(now);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
