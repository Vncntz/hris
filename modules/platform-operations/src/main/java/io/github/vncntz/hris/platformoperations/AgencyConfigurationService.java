package io.github.vncntz.hris.platformoperations;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import io.github.vncntz.hris.sharedkernel.AuditRecorder;
import io.github.vncntz.hris.sharedkernel.AuditRequest;
import io.github.vncntz.hris.sharedkernel.BusinessTimeZone;
import io.github.vncntz.hris.sharedkernel.PublicId;

/** Narrow installation-level Agency command and query boundary. */
@Service
public class AgencyConfigurationService {
    private static final byte SINGLETON_KEY = 1;
    private final AgencyConfigurationRepository repository;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    AgencyConfigurationService(AgencyConfigurationRepository repository,
                               AuditRecorder auditRecorder, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The caller supplies an actor's public ID; IMP-013 wires authenticated authorization. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgencyConfiguration initialize(String displayName, BusinessTimeZone businessTimeZone,
                                          PublicId actorId) {
        String name = validName(displayName);
        Objects.requireNonNull(businessTimeZone, "businessTimeZone");
        Objects.requireNonNull(actorId, "actorId");
        if (repository.existsById(SINGLETON_KEY)) {
            throw new IllegalStateException("Agency configuration is already initialized");
        }
        Instant now = now();
        AgencyConfigurationEntity entity = new AgencyConfigurationEntity(
                PublicId.of(UUID.randomUUID()), name, businessTimeZone, now);
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException duplicate) {
            throw new IllegalStateException("Agency configuration is already initialized", duplicate);
        }
        AgencyConfiguration result = entity.snapshot();
        auditRecorder.record(audit(actorId, "AGENCY_CONFIGURATION_INITIALIZED", result.publicId()));
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<AgencyConfiguration> current() {
        return repository.findById(SINGLETON_KEY).map(AgencyConfigurationEntity::snapshot);
    }

    /** Expected version prevents stale updates even across separate application requests. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgencyConfiguration update(String displayName, BusinessTimeZone businessTimeZone,
                                      long expectedVersion, PublicId actorId) {
        String name = validName(displayName);
        Objects.requireNonNull(businessTimeZone, "businessTimeZone");
        Objects.requireNonNull(actorId, "actorId");
        AgencyConfigurationEntity entity = repository.findById(SINGLETON_KEY)
                .orElseThrow(() -> new IllegalStateException("Agency configuration is not initialized"));
        AgencyConfiguration before = entity.snapshot();
        if (expectedVersion != before.version()) {
            throw new IllegalStateException("Agency configuration version is stale");
        }
        if (before.displayName().equals(name) && before.businessTimeZone().equals(businessTimeZone)) {
            return before;
        }
        entity.update(name, businessTimeZone, now());
        try {
            repository.flush();
        } catch (ObjectOptimisticLockingFailureException conflict) {
            throw new IllegalStateException("Agency configuration version is stale", conflict);
        }
        AgencyConfiguration result = entity.snapshot();
        auditRecorder.record(audit(actorId, "AGENCY_CONFIGURATION_UPDATED", result.publicId()));
        return result;
    }

    static String validName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("Agency display name is required");
        }
        String name = displayName.strip();
        if (name.isEmpty() || name.length() > 200
                || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Agency display name must be 1 to 200 non-control characters");
        }
        return name;
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static AuditRequest audit(PublicId actorId, String action, PublicId agencyId) {
        return new AuditRequest(actorId.toString(), action, "AGENCY_CONFIGURATION",
                agencyId.toString(), null, null);
    }
}
