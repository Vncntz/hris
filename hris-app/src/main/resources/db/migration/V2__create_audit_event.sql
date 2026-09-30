-- Platform/Operations audit events; the application account can append and read.
CREATE TABLE audit_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BINARY(16) NOT NULL,
    occurred_at_utc DATETIME(6) NOT NULL,
    actor_reference VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_reference VARCHAR(128) NOT NULL,
    reason VARCHAR(512) NULL,
    context VARCHAR(1024) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_audit_event_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TRIGGER audit_event_no_update BEFORE UPDATE ON audit_event
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_event is append-only';

CREATE TRIGGER audit_event_no_delete BEFORE DELETE ON audit_event
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_event is append-only';
