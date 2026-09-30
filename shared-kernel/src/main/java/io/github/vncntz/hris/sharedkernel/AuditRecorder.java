package io.github.vncntz.hris.sharedkernel;

/** Append an audit event at a caller-owned command boundary. */
public interface AuditRecorder {
    RecordedAuditEvent record(AuditRequest request);
}
