CREATE TABLE identity_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    canonical_login VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    enabled BOOLEAN NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until_utc DATETIME(6) NULL,
    credential_updated_at_utc DATETIME(6) NOT NULL,
    security_updated_at_utc DATETIME(6) NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_identity_account_public_id UNIQUE (public_id),
    CONSTRAINT uq_identity_account_canonical_login UNIQUE (canonical_login),
    CONSTRAINT ck_identity_account_canonical_login CHECK (canonical_login = LOWER(canonical_login)),
    CONSTRAINT ck_identity_account_failed_attempts CHECK (failed_attempts >= 0)
);
