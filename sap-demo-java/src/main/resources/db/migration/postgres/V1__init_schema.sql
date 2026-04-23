-- Customer 360 local schema.
--
-- The app is a BTP-style extension: it enriches SAP Business Partner master
-- data with local-only concerns (tags, free-text notes, access audit log)
-- that never touch SAP. This separation is the "clean core" pattern —
-- local deltas live here; SAP stays canonical for master data.
--
-- Tables:
--   customer_tag   — user-assigned labels on a BP ("vip", "delinquent", etc.)
--   customer_note  — free-text notes captured by CSRs during calls
--   audit_event    — every read/write, for compliance + usage analytics

CREATE TABLE customer_tag (
    id          BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(10)  NOT NULL,
    tag         VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_customer_tag UNIQUE (customer_id, tag)
);
CREATE INDEX idx_customer_tag_customer_id ON customer_tag(customer_id);

CREATE TABLE customer_note (
    id          BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(10)  NOT NULL,
    body        TEXT         NOT NULL,
    author      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_customer_note_customer_id ON customer_note(customer_id);

CREATE TABLE audit_event (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    VARCHAR(10),
    operation      VARCHAR(64)  NOT NULL,
    correlation_id VARCHAR(128),
    latency_ms     INTEGER,
    happened_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_event_happened_at ON audit_event(happened_at DESC);
CREATE INDEX idx_audit_event_customer_id ON audit_event(customer_id);
