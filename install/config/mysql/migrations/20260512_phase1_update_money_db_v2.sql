-- SUN-13xx Phase 1 — additive SP that stops writing users.vin_total / users.xu_total
--
-- Drop-in companion to legacy `update_money_db(user_id, money, money_type)`.
-- The legacy SP (which mutates BOTH vin AND vin_total in a single UPDATE)
-- stays installed for the duration of Phase 1.  Phase 4
-- (20260601_phase4_drop_legacy_sp_and_total_columns.sql) is what finally
-- drops the legacy SP and the *_total columns.
--
-- Routing between v1 and v2 is decided at the Java DAO layer using the
-- env-var `UNIFIED_WALLET_PHASE_1` (off / shadow / on).  See
-- UserDaoImpl.updateMoney() for the gate logic.
--
-- Idempotent: DROP PROCEDURE IF EXISTS + CREATE.
USE vinplay;

DROP PROCEDURE IF EXISTS update_money_db_v2;

DELIMITER //
CREATE PROCEDURE update_money_db_v2(
    IN p_user_id    INT,
    IN p_money      BIGINT,
    IN p_money_type VARCHAR(5)
)
BEGIN
    -- v2 contract: write ONLY the current-balance column.
    -- Cumulative P&L (vin_total / xu_total) is now derived from
    -- v_derived_player_pnl, not stored on users.*.
    --
    -- Note: we deliberately keep the same (id, money, money_type) signature
    -- as v1 so the Java DAO can swap CALL targets behind a flag without
    -- touching any parameter shape.
    -- SUN-13xx Phase 3a: xu column dropped. All money_type values now write vin.
    UPDATE users
       SET vin = vin + p_money
     WHERE id = p_user_id;
END //
DELIMITER ;

-- Sanity probe: list both SPs after install (useful for the operator running
-- the manual `docker exec ... mysql < file.sql` apply step).
SELECT ROUTINE_NAME, CREATED, LAST_ALTERED
  FROM information_schema.ROUTINES
 WHERE ROUTINE_SCHEMA = 'vinplay'
   AND ROUTINE_NAME IN ('update_money_db', 'update_money_db_v2')
 ORDER BY ROUTINE_NAME;
