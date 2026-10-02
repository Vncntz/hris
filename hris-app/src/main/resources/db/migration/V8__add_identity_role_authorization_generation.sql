ALTER TABLE identity_role
    ADD COLUMN authorization_generation BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_identity_role_authorization_generation CHECK (authorization_generation >= 0);
