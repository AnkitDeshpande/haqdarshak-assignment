-- Haqdarshak Scheme Knowledge Base Schema Definition
-- Flyway Migration V1: Initial Schema

CREATE TABLE IF NOT EXISTS schemes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_ref VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    state VARCHAR(100),
    current_published_version_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scheme_ref (scheme_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scheme_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    review_notes TEXT,
    flagged_for_re_review BOOLEAN NOT NULL DEFAULT FALSE,
    re_review_reason TEXT,
    created_by VARCHAR(100) NOT NULL,
    last_modified_by VARCHAR(100),
    approved_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    CONSTRAINT fk_version_scheme FOREIGN KEY (scheme_id) REFERENCES schemes (id) ON DELETE CASCADE,
    INDEX idx_version_scheme_id (scheme_id),
    INDEX idx_version_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scheme_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_version_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_value TEXT,
    evidence_text TEXT,
    evidence_location VARCHAR(255),
    evidence_source_url VARCHAR(1000),
    evidence_captured_date VARCHAR(50),
    confidence VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    status VARCHAR(20) NOT NULL DEFAULT 'FOUND',
    note TEXT,
    CONSTRAINT fk_field_version FOREIGN KEY (scheme_version_id) REFERENCES scheme_versions (id) ON DELETE CASCADE,
    INDEX idx_field_version_id (scheme_version_id),
    INDEX idx_field_name (field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shared_eligibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(100) NOT NULL UNIQUE,
    rule_name VARCHAR(255) NOT NULL,
    description TEXT,
    criteria_json TEXT,
    rule_version INT NOT NULL DEFAULT 1,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scheme_version_rules (
    scheme_version_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    PRIMARY KEY (scheme_version_id, rule_id),
    CONSTRAINT fk_svr_version FOREIGN KEY (scheme_version_id) REFERENCES scheme_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_svr_rule FOREIGN KEY (rule_id) REFERENCES shared_eligibility_rules (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_version_id BIGINT NOT NULL,
    scheme_ref VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_reference VARCHAR(1000),
    INDEX idx_audit_version_id (scheme_version_id),
    INDEX idx_audit_scheme_ref (scheme_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
