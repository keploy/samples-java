-- init.sql — schema + seed data for the hibernate-cache-tour repro.
--
-- Postgres mounts files in /docker-entrypoint-initdb.d/ at first start.
-- Both the schema and the seed are committed here so the docker-compose
-- run-dir contains exactly one file the DB needs to bootstrap.
--
-- The seed creates customers with ids 1..4 and 8 tags spread across them.
-- The exerciser hits each WHERE-id endpoint with `id = 1 + (i-1) % 4`
-- so the same id-space repeats every 4 calls — that's how a single SQL
-- handle gets its 5th execution on iteration 5 and trips the pgjdbc
-- prepareThreshold (default 5) into binary format.

CREATE TABLE IF NOT EXISTS customer (
    id    SERIAL PRIMARY KEY,
    name  TEXT NOT NULL,
    email TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_tag (
    id          SERIAL PRIMARY KEY,
    customer_id INT REFERENCES customer(id),
    tag         VARCHAR(64) NOT NULL,
    priority    INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_tag_customer_id ON customer_tag(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_tag_priority    ON customer_tag(priority);

INSERT INTO customer (name, email) VALUES
    ('alice',   'alice@example.com'),
    ('bob',     'bob@example.com'),
    ('carol',   'carol@example.com'),
    ('dave',    'dave@example.com')
ON CONFLICT DO NOTHING;

INSERT INTO customer_tag (customer_id, tag, priority) VALUES
    (1, 'gold',     10),
    (1, 'beta',     20),
    (2, 'silver',   10),
    (2, 'beta',     30),
    (3, 'bronze',   20),
    (3, 'preview',  30),
    (4, 'gold',     10),
    (4, 'preview',  20)
ON CONFLICT DO NOTHING;
