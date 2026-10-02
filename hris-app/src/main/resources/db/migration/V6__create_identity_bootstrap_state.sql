CREATE TABLE identity_bootstrap_state (
    id INT NOT NULL,
    completed BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_identity_bootstrap_singleton CHECK (id = 1),
    CONSTRAINT ck_identity_bootstrap_completed CHECK (completed IN (0, 1))
);

-- Technical coordination only: no account, authorization or credential seed.
INSERT INTO identity_bootstrap_state (id, completed) VALUES (1, 0);

DELIMITER $$
CREATE TRIGGER identity_bootstrap_no_reopen BEFORE UPDATE ON identity_bootstrap_state
FOR EACH ROW
BEGIN
    IF OLD.completed = 1 OR NEW.id <> OLD.id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bootstrap state cannot be reopened';
    END IF;
END$$
CREATE TRIGGER identity_bootstrap_no_delete BEFORE DELETE ON identity_bootstrap_state
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bootstrap state cannot be deleted';
END$$
DELIMITER ;
