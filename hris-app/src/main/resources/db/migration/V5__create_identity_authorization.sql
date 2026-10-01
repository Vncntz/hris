CREATE TABLE identity_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    canonical_name VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    enabled BOOLEAN NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_identity_role_public_id UNIQUE (public_id),
    CONSTRAINT uq_identity_role_canonical_name UNIQUE (canonical_name),
    CONSTRAINT ck_identity_role_canonical_name CHECK (
        REGEXP_LIKE(canonical_name, '^[a-z0-9]', 'c')
        AND NOT REGEXP_LIKE(canonical_name, '[^a-z0-9._-]', 'c')
    ),
    CONSTRAINT ck_identity_role_enabled CHECK (enabled IN (0, 1))
);

CREATE TABLE identity_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    authority_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_identity_permission_authority_key UNIQUE (authority_key),
    CONSTRAINT ck_identity_permission_authority_key CHECK (
        REGEXP_LIKE(authority_key, '^[a-z]', 'c')
        AND NOT REGEXP_LIKE(authority_key, '[^a-z0-9._:-]', 'c')
    )
);

CREATE TABLE identity_account_role (
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, role_id),
    INDEX ix_identity_account_role_role (role_id),
    CONSTRAINT fk_identity_account_role_account FOREIGN KEY (account_id)
        REFERENCES identity_account (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_identity_account_role_role FOREIGN KEY (role_id)
        REFERENCES identity_role (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE identity_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    INDEX ix_identity_role_permission_permission (permission_id),
    CONSTRAINT fk_identity_role_permission_role FOREIGN KEY (role_id)
        REFERENCES identity_role (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_identity_role_permission_permission FOREIGN KEY (permission_id)
        REFERENCES identity_permission (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
