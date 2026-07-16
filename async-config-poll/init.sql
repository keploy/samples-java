-- Rule-engine schema + seed for the (ORDER_FLOW, ACME) use case.
-- The app reads these rows over the MySQL wire; Keploy captures that traffic
-- as mocks and serves it back on replay.
CREATE DATABASE IF NOT EXISTS ruledb;
USE ruledb;

CREATE TABLE IF NOT EXISTS rules (
  rule_id         BIGINT       PRIMARY KEY,
  use_case        VARCHAR(64)  NOT NULL,
  tenant          VARCHAR(64)  NOT NULL,
  constraint_expr TEXT         NOT NULL,
  rule_type       VARCHAR(16)  NOT NULL,
  INDEX idx_uc_tenant (use_case, tenant)
);

CREATE TABLE IF NOT EXISTS rule_actions (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  rule_id        BIGINT       NOT NULL,
  basic_action   VARCHAR(255) NOT NULL,
  action_details TEXT         NOT NULL,
  seq            INT          NOT NULL,
  INDEX idx_rule (rule_id)
);

INSERT INTO rules (rule_id, use_case, tenant, constraint_expr, rule_type) VALUES
 (14,'ORDER_FLOW','ACME','status == "COMPLETED" && type.equals("CHECKOUT")','POST'),
 (15,'ORDER_FLOW','ACME','status == "COMPLETED" && type.equals("PAYMENT")','POST'),
 (16,'ORDER_FLOW','ACME','status == "COMPLETED" && type.equals("SHIPMENT")','POST'),
 (17,'ORDER_FLOW','ACME','status == "IN_PROGRESS" && type.equals("REFUND")','POST'),
 (18,'ORDER_FLOW','ACME','status == "COMPLETED" && type.equals("FULFILLMENT")','PRE');

INSERT INTO rule_actions (rule_id, basic_action, action_details, seq) VALUES
 (14,'com.example.rules.handlers.ForceSyncHandler','{}',1),
 (15,'com.example.rules.handlers.PaymentTaskHandler','{}',1),
 (15,'com.example.rules.handlers.NotifyHandler','{"optional":"true","channel":"email"}',2),
 (16,'com.example.rules.handlers.ValidateHandler','{"optional":"true"}',1),
 (16,'com.example.rules.handlers.ShipmentHandler','{}',2),
 (17,'com.example.rules.handlers.RefundHandler','{}',1),
 (18,'com.example.rules.handlers.ImageSyncHandler','{"optional":"true"}',1);
