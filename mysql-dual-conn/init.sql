CREATE DATABASE IF NOT EXISTS myntra_oms;
CREATE DATABASE IF NOT EXISTS camunda;

CREATE USER IF NOT EXISTS 'omsAppUser'@'%' IDENTIFIED BY 'omsPassword';
GRANT ALL PRIVILEGES ON myntra_oms.* TO 'omsAppUser'@'%';

CREATE USER IF NOT EXISTS 'stagebuster'@'%' IDENTIFIED BY 'camundaPassword';
GRANT ALL PRIVILEGES ON camunda.* TO 'stagebuster'@'%';

FLUSH PRIVILEGES;

-- Column-type fidelity fixture (keploy/keploy#4426).
--
-- The OMS datasource runs with useServerPrepStmts=true, so a SELECT over
-- this table comes back as a binary-protocol result set — the wire format
-- whose FLOAT/DOUBLE columns keploy decoded as their raw IEEE-754 bit
-- pattern rather than their value, corrupting mocks.yaml at record time.
--
-- The BIGINT UNSIGNED column covers the other half: values above MaxInt64
-- have no lossless float64 form, so a mock format that routes them through
-- one collapses distinct rows onto the same number.
USE myntra_oms;

CREATE TABLE IF NOT EXISTS numeric_fidelity (
  id           INT PRIMARY KEY,
  label        VARCHAR(32)      NOT NULL,
  price_f      FLOAT            NOT NULL,
  ratio_d      DOUBLE           NOT NULL,
  big_u        BIGINT UNSIGNED  NOT NULL
);

INSERT INTO numeric_fidelity (id, label, price_f, ratio_d, big_u) VALUES
  -- 9.99 is the value from the bug report: read as a numeric cast it
  -- surfaces as 1.0926057e+09 (FLOAT) / 4.621813488089437e+18 (DOUBLE).
  (1, 'nine-ninety-nine', 9.99,     9.99,               18446744073709551615),
  (2, 'negative',         -0.5,     -1234.5678,         9223372036854775808),
  (3, 'zero',             0,        0,                  0),
  (4, 'whole',            10,       10,                 4294967296),
  (5, 'small',            1.5,      2.2250738585072014e-308, 1)
ON DUPLICATE KEY UPDATE label = VALUES(label);
