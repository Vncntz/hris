package io.github.vncntz.hris.platformoperations;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import io.github.vncntz.hris.sharedkernel.PublicId;
import io.github.vncntz.hris.sharedkernel.RecordedAuditEvent;
import io.github.vncntz.hris.sharedkernel.UtcInstant;
import jakarta.persistence.EntityManager;

@Service
public class JpaAuditRecorder implements AuditRecorder {
    private final EntityManager entityManager;
    private final Clock clock;

    public JpaAuditRecorder(EntityManager entityManager, Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public RecordedAuditEvent record(AuditRequest request) {
        Objects.requireNonNull(request, "request");
        PublicId eventId = PublicId.of(UUID.randomUUID());
        Instant occurredAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        entityManager.persist(new AuditEventEntity(eventId.value(), occurredAt,
                request.actorReference(), request.action(), request.targetType(),
                request.targetReference(), request.reason(), request.context()));
        return new RecordedAuditEvent(eventId, UtcInstant.of(occurredAt));
    }
}
