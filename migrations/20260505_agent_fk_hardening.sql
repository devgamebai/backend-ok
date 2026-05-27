-- =============================================================================
-- 20260505_agent_fk_hardening.sql — schema hardening after the May-2 incident
--
-- BACKGROUND
--   On 2026-05-02 two users (8713 / tuankal1102 and 8727 / vanhoang0770)
--   silently re-attached to the wrong agents because:
--     1. parent_agent_id was a foreign key by convention (no FK constraint)
--     2. useragent IDs got freed (delete + restore) and recycled
--     3. no audit trail captured the silent shift
--
--   Forensic doc: see post-mortem in admin notes.
--
-- WHAT THIS MIGRATION DOES
--   Phase 5 (audit trail) and Phase 2 (FK on users.parent_agent_id) ARE ALREADY
--   APPLIED ON 2026-05-05 directly via psql. This file is the canonical record
--   so a fresh DB rebuild reproduces the same shape.
--
--   Phase 6 (FKs on commission/wallet/agency tables) is NOT applied yet —
--   each of those FK additions wants a maintenance window with the affected
--   service stopped (commission writers can lock the table). Run manually
--   after coordination, see HOW TO APPLY below.
--
-- HOW TO APPLY (Phase 6 only — Phases 2 + 5 are already live)
--   1. Run scripts/post-restore-reconcile-agents.sh first to ensure 0 dangling.
--   2. Pause the commission writer (vbee container) briefly.
--   3. Apply the ALTER TABLE statements below one at a time.
--   4. Resume vbee.
--
-- ROLLBACK
--   ALTER TABLE <t> DROP FOREIGN KEY <fk_name>;
--   (table_name + fk_name pairs at the bottom of this file)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Phase 5 (audit) — already applied 2026-05-05; idempotent re-create:
-- ---------------------------------------------------------------------------

USE vinplay;

CREATE TABLE IF NOT EXISTS users_parent_agent_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  old_parent_agent_id INT NULL,
  new_parent_agent_id INT NULL,
  old_referral_code   VARCHAR(128) NULL,
  new_referral_code   VARCHAR(128) NULL,
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  changed_by VARCHAR(128) NULL,
  source     VARCHAR(64)  NULL,
  INDEX idx_user_changed (user_id, changed_at),
  INDEX idx_changed_at   (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Audit log: parent_agent_id / referral_code mutations on users.';

DROP TRIGGER IF EXISTS trg_users_parent_agent_audit;

DELIMITER //
CREATE TRIGGER trg_users_parent_agent_audit
  AFTER UPDATE ON users
  FOR EACH ROW
BEGIN
  IF NOT (NEW.parent_agent_id <=> OLD.parent_agent_id)
     OR NOT (NEW.referral_code   <=> OLD.referral_code)
  THEN
    INSERT INTO users_parent_agent_history
      (user_id, old_parent_agent_id, new_parent_agent_id,
       old_referral_code, new_referral_code, changed_by, source)
    VALUES
      (OLD.id, OLD.parent_agent_id, NEW.parent_agent_id,
       OLD.referral_code, NEW.referral_code,
       CURRENT_USER(),
       COALESCE(@audit_source, 'app'));
  END IF;
END//
DELIMITER ;

-- ---------------------------------------------------------------------------
-- Phase 2 (FK on users.parent_agent_id) — already applied 2026-05-05:
-- ---------------------------------------------------------------------------

-- Pre-flight (idempotent — should report 0 by the time this runs):
-- SELECT COUNT(*) FROM users u
--   LEFT JOIN vinplay_admin.useragent a ON a.id = u.parent_agent_id
--  WHERE u.parent_agent_id IS NOT NULL AND a.id IS NULL;

-- (Constraint is added once. Wrapping in DROP+ADD to make the migration
--  idempotent if re-run on a fresh DB.)
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.referential_constraints
   WHERE CONSTRAINT_SCHEMA='vinplay' AND CONSTRAINT_NAME='fk_users_parent_agent');

SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE vinplay.users ADD CONSTRAINT fk_users_parent_agent
     FOREIGN KEY (parent_agent_id) REFERENCES vinplay_admin.useragent(id)
     ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- Phase 6 (commission / wallet / agency FKs) — APPLY MANUALLY
-- ---------------------------------------------------------------------------
-- All of these reference vinplay_admin.useragent(id). On a busy production
-- DB an ALTER TABLE ... ADD FOREIGN KEY can briefly lock the table for
-- writes (InnoDB online DDL covers most cases but not all). Apply with the
-- relevant writer paused.

-- agency_wallet — one row per agent. Strong FK with CASCADE makes sense:
-- if an agent is deleted, drop their wallet too (currently NOT NULL int).
-- ALTER TABLE vinplay.agency_wallet
--   ADD CONSTRAINT fk_agency_wallet_agent
--   FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE CASCADE;

-- agency_wallet_transactions — historical record. RESTRICT prevents
-- orphaning the audit chain when an agent is deleted (force soft-delete).
-- ALTER TABLE vinplay.agency_wallet_transactions
--   ADD CONSTRAINT fk_agency_wallet_tx_agent
--   FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE RESTRICT;

-- commission_history_outbox — historical, treat like a financial ledger:
-- ALTER TABLE vinplay.commission_history_outbox
--   ADD CONSTRAINT fk_commission_outbox_agent
--   FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE RESTRICT;

-- credit_wallet — one row per agent (same shape as agency_wallet):
-- ALTER TABLE vinplay.credit_wallet
--   ADD CONSTRAINT fk_credit_wallet_agent
--   FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE CASCADE;

-- credit_wallet_transactions — historical with optional related agent:
-- ALTER TABLE vinplay.credit_wallet_transactions
--   ADD CONSTRAINT fk_credit_wallet_tx_agent
--   FOREIGN KEY (agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE RESTRICT,
--   ADD CONSTRAINT fk_credit_wallet_tx_related_agent
--   FOREIGN KEY (related_agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE SET NULL;

-- log_tranfer_agent — both endpoints:
-- ALTER TABLE vinplay.log_tranfer_agent
--   ADD CONSTRAINT fk_log_tranfer_sender
--   FOREIGN KEY (sender_agent_id)   REFERENCES vinplay_admin.useragent(id)
--   ON DELETE SET NULL,
--   ADD CONSTRAINT fk_log_tranfer_receiver
--   FOREIGN KEY (receiver_agent_id) REFERENCES vinplay_admin.useragent(id)
--   ON DELETE SET NULL;

-- agent_code_history / agent_code_request / deposit_commission_logs —
-- same pattern, defer until commission system has a known maintenance window.

-- ---------------------------------------------------------------------------
-- ROLLBACK MAP (for ops emergencies — drop any of these to undo)
-- ---------------------------------------------------------------------------
-- vinplay.users                       → fk_users_parent_agent
-- vinplay.agency_wallet               → fk_agency_wallet_agent
-- vinplay.agency_wallet_transactions  → fk_agency_wallet_tx_agent
-- vinplay.commission_history_outbox   → fk_commission_outbox_agent
-- vinplay.credit_wallet               → fk_credit_wallet_agent
-- vinplay.credit_wallet_transactions  → fk_credit_wallet_tx_agent
--                                       fk_credit_wallet_tx_related_agent
-- vinplay.log_tranfer_agent           → fk_log_tranfer_sender
--                                       fk_log_tranfer_receiver
-- =============================================================================
