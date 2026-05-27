-- =============================================================================
-- SUN-1339 Phase A1 — provider-style settle contract columns
--
-- Adds the following to three real-time game bet tables:
--   • settle_status  ENUM('PENDING','SETTLED','VOIDED') NOT NULL DEFAULT 'PENDING'
--   • round_id       BIGINT NULL
--   • Composite UNIQUE index uk_<table>_round_ticket (round_id, id)
--
-- Tables affected:
--   vinplay_minigame.lode          — ADD columns + index (table pre-exists)
--   vinplay_minigame.taixiu_bet    — CREATE TABLE (new, mirrors lode shape)
--   vinplay_minigame.sicbo_bet     — CREATE TABLE (new, mirrors lode shape)
--
-- Backfill:
--   lode: rows where settled_at IS NOT NULL → SETTLED; remainder → PENDING.
--   taixiu_bet / sicbo_bet: new tables, no historical rows to backfill.
--   round_id stays NULL for all historical rows — the composite UNIQUE index
--   uses NULL semantics (multiple NULLs are allowed in MySQL UNIQUE indexes).
--
-- Rollback:
--   -- lode:
--   ALTER TABLE vinplay_minigame.lode
--       DROP INDEX  uk_lode_round_ticket,
--       DROP COLUMN round_id,
--       DROP COLUMN settle_status;
--   -- taixiu_bet / sicbo_bet:
--   DROP TABLE IF EXISTS vinplay_minigame.taixiu_bet;
--   DROP TABLE IF EXISTS vinplay_minigame.sicbo_bet;
--
-- Idempotency:
--   ADD COLUMN operations are guarded by INFORMATION_SCHEMA.COLUMNS checks
--   via PREPARE/EXECUTE (same pattern as 20260508_phase0b_gcr_constraints.sql).
--   ADD INDEX operations are guarded by INFORMATION_SCHEMA.STATISTICS checks.
--   CREATE TABLE uses IF NOT EXISTS.
--   Safe to re-run on an already-migrated schema — each block will skip.
--
-- Apply:
--   docker exec -i sunwinkr-mysql sh -c \
--     'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" vinplay_minigame' \
--     < install/config/mysql/migrations/20260515_provider_contract_round_settle.sql
-- =============================================================================

USE vinplay_minigame;

SET @schema := 'vinplay_minigame';

-- ---------------------------------------------------------------------------
-- 1. lode — ADD settle_status
-- ---------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema
       AND TABLE_NAME   = 'lode'
       AND COLUMN_NAME  = 'settle_status'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE lode ADD COLUMN settle_status ENUM(''PENDING'',''SETTLED'',''VOIDED'') NOT NULL DEFAULT ''PENDING'' AFTER updated_date',
    'SELECT ''lode.settle_status already present — skip'' AS migration_note'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------------
-- 2. lode — ADD round_id
-- ---------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema
       AND TABLE_NAME   = 'lode'
       AND COLUMN_NAME  = 'round_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE lode ADD COLUMN round_id BIGINT NULL AFTER settle_status',
    'SELECT ''lode.round_id already present — skip'' AS migration_note'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------------
-- 3. lode — ADD composite unique index uk_lode_round_ticket (round_id, id)
-- ---------------------------------------------------------------------------
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema
       AND TABLE_NAME   = 'lode'
       AND INDEX_NAME   = 'uk_lode_round_ticket'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE lode ADD UNIQUE INDEX uk_lode_round_ticket (round_id, id)',
    'SELECT ''lode.uk_lode_round_ticket already present — skip'' AS migration_note'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------------
-- 4. lode — backfill settle_status from settled_at
--    Rows with settled_at IS NOT NULL were already paid out → SETTLED.
--    Remaining rows keep the default PENDING.
--    round_id stays NULL for all historical rows.
-- ---------------------------------------------------------------------------
UPDATE lode
   SET settle_status = 'SETTLED'
 WHERE settled_at IS NOT NULL
   AND settle_status = 'PENDING';

-- ---------------------------------------------------------------------------
-- 5. taixiu_bet — CREATE TABLE (new; mirrors lode provider-contract shape +
--    taixiu_record engine columns: taixiu_id FK, betamount, winamount,
--    bet_side, bettime, money_type, nick_name)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS taixiu_bet (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    taixiu_id      BIGINT       NULL COMMENT 'FK to taixiu.id (round)',
    user_id        BIGINT       NOT NULL,
    nick_name      VARCHAR(90)  NOT NULL,
    bet_value      BIGINT       NOT NULL,
    bet_unit       BIGINT       NULL     COMMENT 'unit multiplier at purchase time (provider contract)',
    rate_at_purchase INT        NULL     COMMENT 'odds/rate snapshotted at purchase time (provider contract)',
    bet_side       TINYINT      NULL     COMMENT '0=Tai,1=Xiu (engine convention)',
    prize          BIGINT       NULL     DEFAULT 0,
    refund         BIGINT       NULL     DEFAULT 0,
    money_type     TINYINT      NULL,
    settle_status  ENUM('PENDING','SETTLED','VOIDED') NOT NULL DEFAULT 'PENDING',
    round_id       BIGINT       NULL     COMMENT 'canonical provider-contract round identifier',
    settled_at     TIMESTAMP    NULL,
    created_date   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP    NULL     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE  KEY uk_taixiu_bet_round_ticket (round_id, id),
    KEY idx_taixiu_bet_taixiu_id  (taixiu_id),
    KEY idx_taixiu_bet_user_id    (user_id),
    KEY idx_taixiu_bet_nick_name  (nick_name),
    KEY idx_taixiu_bet_settled_at (settled_at),
    KEY idx_taixiu_bet_created    (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='SUN-1339: provider-style bet contract for TaiXiu real-time game';

-- ---------------------------------------------------------------------------
-- 6. sicbo_bet — CREATE TABLE (same provider-contract shape as taixiu_bet;
--    existing sicbo data lives in transaction_tai_xiu_sicbo — this table is
--    the new provider-contract write target for the settle pipeline)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sicbo_bet (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    reference_id   BIGINT       NULL COMMENT 'FK to result_tai_xiu_sicbo.id (round)',
    user_id        BIGINT       NOT NULL,
    nick_name      VARCHAR(90)  NOT NULL,
    bet_value      BIGINT       NOT NULL,
    bet_unit       BIGINT       NULL     COMMENT 'unit multiplier at purchase time (provider contract)',
    rate_at_purchase INT        NULL     COMMENT 'odds/rate snapshotted at purchase time (provider contract)',
    bet_side       TINYINT      NULL     COMMENT 'bet side enum (engine convention)',
    prize          BIGINT       NULL     DEFAULT 0,
    refund         BIGINT       NULL     DEFAULT 0,
    money_type     TINYINT      NULL,
    settle_status  ENUM('PENDING','SETTLED','VOIDED') NOT NULL DEFAULT 'PENDING',
    round_id       BIGINT       NULL     COMMENT 'canonical provider-contract round identifier',
    settled_at     TIMESTAMP    NULL,
    created_date   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP    NULL     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE  KEY uk_sicbo_bet_round_ticket (round_id, id),
    KEY idx_sicbo_bet_reference_id (reference_id),
    KEY idx_sicbo_bet_user_id      (user_id),
    KEY idx_sicbo_bet_nick_name    (nick_name),
    KEY idx_sicbo_bet_settled_at   (settled_at),
    KEY idx_sicbo_bet_created      (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='SUN-1339: provider-style bet contract for Sicbo real-time game';

-- ---------------------------------------------------------------------------
-- Verification — show new columns on all three tables
-- ---------------------------------------------------------------------------
SELECT 'lode' AS tbl, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'vinplay_minigame'
   AND TABLE_NAME   = 'lode'
   AND COLUMN_NAME IN ('settle_status','round_id')
UNION ALL
SELECT 'taixiu_bet', COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'vinplay_minigame'
   AND TABLE_NAME   = 'taixiu_bet'
   AND COLUMN_NAME IN ('settle_status','round_id')
UNION ALL
SELECT 'sicbo_bet', COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'vinplay_minigame'
   AND TABLE_NAME   = 'sicbo_bet'
   AND COLUMN_NAME IN ('settle_status','round_id')
ORDER BY tbl, COLUMN_NAME;
