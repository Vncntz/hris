package io.github.vncntz.hris.sharedkernel;

import java.util.Objects;

/** The stable public identity and UTC time assigned to an appended event. */
public record RecordedAuditEvent(PublicId eventId, UtcInstant occurredAt) {
    public RecordedAuditEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
