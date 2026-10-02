ALTER TABLE identity_account
    ADD COLUMN authentication_generation BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_identity_account_authentication_generation CHECK (authentication_generation >= 0);
