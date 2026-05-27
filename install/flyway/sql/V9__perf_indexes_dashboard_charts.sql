-- SUN-1108 Wave 2 — Tier 1 indexes for dashboard / chart / report-general queries.
--
-- Three new composite indexes that match real query shapes seen in:
--   * ReportGeneral4AgencyProcessor.getSumDeposit / getSumWithdraw
--   * ChartDepositWithdraw4AgencyProcessor (UNION of deposits + withdrawals)
--   * ReportGeneral4AgencyProcessor.getTotalSubAgents (parent_agent_id + dai_ly)
--
-- log_report_user(nick_name, time_report) already exists as `nickname_time` — skipped.
--
-- All adds are idempotent: stored procedure checks information_schema first.

DELIMITER $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
    IN p_schema VARCHAR(64),
    IN p_table  VARCHAR(64),
    IN p_index  VARCHAR(64),
    IN p_def    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = p_schema
          AND table_name   = p_table
          AND index_name   = p_index
        LIMIT 1
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD INDEX `', p_index, '` ', p_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_index_if_missing(
    'vinplay', 'deposit_transactions',
    'idx_deposit_user_status_created',
    '(user_id, status, created_at)'
);

CALL add_index_if_missing(
    'vinplay', 'bank_withdrawals',
    'idx_withdraw_user_status_created',
    '(user_id, status, created_at)'
);

CALL add_index_if_missing(
    'vinplay', 'users',
    'idx_users_parent_dai_ly',
    '(parent_agent_id, dai_ly)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;
