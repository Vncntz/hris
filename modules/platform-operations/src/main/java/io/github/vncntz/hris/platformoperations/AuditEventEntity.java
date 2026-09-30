package io.github.vncntz.hris.platformoperations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Module-internal JPA mapping; the audit API never exposes this entity. */
@Entity
@Table(name = "audit_event")
@Immutable
class AuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "occurred_at_utc", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime occurredAtUtc;

    @Column(name = "actor_reference", nullable = false, length = 128)
    private String actorReference;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_reference", nullable = false, length = 128)
    private String targetReference;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "context", length = 1024)
    private String context;

    protected AuditEventEntity() {
        // Required by JPA.
    }

    AuditEventEntity(UUID eventId, Instant occurredAt, String actorReference, String action,
                     String targetType, String targetReference, String reason, String context) {
        this.eventId = eventId;
        this.occurredAtUtc = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC);
        this.actorReference = actorReference;
        this.action = action;
        this.targetType = targetType;
        this.targetReference = targetReference;
        this.reason = reason;
        this.context = context;
    }
}
