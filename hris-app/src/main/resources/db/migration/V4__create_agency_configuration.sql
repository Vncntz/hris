-- One installation has one Agency configuration. No production row is seeded.
CREATE TABLE agency_configuration (
    singleton_key TINYINT NOT NULL,
    public_id BINARY(16) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    business_time_zone VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at_utc DATETIME(6) NOT NULL,
    updated_at_utc DATETIME(6) NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (singleton_key),
    CONSTRAINT ck_agency_singleton_key CHECK (singleton_key = 1),
    CONSTRAINT uq_agency_public_id UNIQUE (public_id),
    CONSTRAINT ck_agency_display_name CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT ck_agency_row_version CHECK (row_version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
