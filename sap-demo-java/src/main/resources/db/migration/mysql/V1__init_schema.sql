-- Customer 360 local schema — MySQL 8 dialect.
--
-- Functional twin of db/migration/postgres/V1__init_schema.sql. The app is
-- a BTP-style extension: it enriches SAP Business Partner master data with
-- local-only concerns (tags, free-text notes, access audit log) that never
-- touch SAP. This separation is the "clean core" pattern — local deltas
-- live here; SAP stays canonical for master data.
--
-- Differences from the Postgres variant are dialect-only:
--   * BIGSERIAL               -> BIGINT AUTO_INCREMENT
--   * TIMESTAMPTZ             -> TIMESTAMP (stored UTC; see Hibernate
--                                jdbc.time_zone=UTC in application.yml)
--   * TEXT                    -> TEXT (MySQL TEXT == up to 65 535 bytes,
--                                plenty for a note body; switch to
--                                MEDIUMTEXT if you expect longer)
--   * NOW()                   -> CURRENT_TIMESTAMP
--   * DESC index on happened_at expressed via a plain index — MySQL 8
--     accepts the DESC qualifier on B-tree indexes, and it is retained for
--     parity with the Postgres schema.
--
-- Tables:
--   customer_tag   — user-assigned labels on a BP ("vip", "delinquent", etc.)
--   customer_note  — free-text notes captured by CSRs during calls
--   audit_event    — every read/write, for compliance + usage analytics

CREATE TABLE customer_tag (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id VARCHAR(10)  NOT NULL,
    tag         VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(64)  NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_tag UNIQUE (customer_id, tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_customer_tag_customer_id ON customer_tag(customer_id);

CREATE TABLE customer_note (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id VARCHAR(10)  NOT NULL,
    body        TEXT         NOT NULL,
    author      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_customer_note_customer_id ON customer_note(customer_id);

CREATE TABLE audit_event (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id    VARCHAR(10),
    operation      VARCHAR(64)  NOT NULL,
    correlation_id VARCHAR(128),
    latency_ms     INTEGER,
    happened_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_audit_event_happened_at ON audit_event(happened_at DESC);
CREATE INDEX idx_audit_event_customer_id ON audit_event(customer_id);
