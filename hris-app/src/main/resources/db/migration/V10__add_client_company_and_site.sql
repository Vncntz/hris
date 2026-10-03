CREATE TABLE client_company (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL,
    row_version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_client_company_public_id UNIQUE (public_id),
    CONSTRAINT ck_client_company_name CHECK (CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 200),
    CONSTRAINT ck_client_company_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_client_company_version CHECK (row_version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE client_site (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    company_id BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL,
    row_version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_client_site_public_id UNIQUE (public_id),
    INDEX ix_client_site_company (company_id),
    CONSTRAINT fk_client_site_company FOREIGN KEY (company_id) REFERENCES client_company (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_client_site_name CHECK (CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 200),
    CONSTRAINT ck_client_site_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_client_site_version CHECK (row_version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
