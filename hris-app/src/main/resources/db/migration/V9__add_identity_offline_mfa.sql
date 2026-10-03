-- Optional factor state only; no secret or identity seed.
ALTER TABLE identity_account
    ADD COLUMN mfa_secret VARBINARY(48) NULL,
    ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mfa_pending_until_utc DATETIME(6) NULL,
    ADD COLUMN mfa_last_step BIGINT NOT NULL DEFAULT -1,
    ADD COLUMN mfa_recovery_hashes VARCHAR(650) NOT NULL DEFAULT '',
    ADD CONSTRAINT ck_identity_mfa_state CHECK (
        (mfa_secret IS NULL AND mfa_enabled = FALSE AND mfa_pending_until_utc IS NULL
            AND mfa_last_step = -1 AND mfa_recovery_hashes = '')
        OR (mfa_secret IS NOT NULL AND OCTET_LENGTH(mfa_secret) = 48 AND
            ((mfa_enabled = TRUE AND mfa_pending_until_utc IS NULL AND mfa_last_step >= 0)
            OR (mfa_enabled = FALSE AND mfa_pending_until_utc IS NOT NULL
                AND mfa_last_step = -1 AND mfa_recovery_hashes = ''))));
