package com.sunwinkr.minigame.api.adapter;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * JDBC adapter writing audit rows for admin TaiXiu unsettleBet operations.
 *
 * <p>Table: {@code vinplay_minigame.taixiu_bet_unsettle_audit}
 *
 * <p>Schema (auto-created on first write if absent — the migration that
 * creates it lives in {@code install/config/mysql/migrations/
 * 20260515_taixiu_unsettle_audit.sql}).  The INSERT uses
 * {@code CREATE TABLE IF NOT EXISTS} semantics via a one-time DDL guard
 * (see {@link #ensureTable()}).
 *
 * <h3>Columns written</h3>
 * <ul>
 *   <li>{@code bet_id}      — FK to taixiu_bet.id</li>
 *   <li>{@code actor}       — admin nickname who called unsettle</li>
 *   <li>{@code reason}      — free-text reason from request body</li>
 *   <li>{@code old_status}  — always {@code 'SETTLED'}</li>
 *   <li>{@code new_status}  — always {@code 'VOIDED'}</li>
 *   <li>{@code debit_amount} — prize amount reversed (credited back to house)</li>
 *   <li>{@code created_at}  — wall-clock timestamp</li>
 * </ul>
 *
 * <p>Plan SUN-1339 §B2.
 */
@Component
public class JdbcTaixiuUnsettleAuditPort {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcTaixiuUnsettleAuditPort.class);

    private static final String POOL = "mysqlpool_minigame";
    private static final String TABLE = "taixiu_bet_unsettle_audit";

    private volatile boolean tableEnsured = false;

    /**
     * Write one audit row for an admin unsettle action.
     *
     * @param betId       taixiu_bet.id
     * @param actor       admin nickname (from security context)
     * @param reason      free-text reason supplied in request body
     * @param prizeAmount prize that was reversed (the amount debited back)
     */
    public void writeAudit(long betId, String actor, String reason, long prizeAmount) {
        ensureTable();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            ps = conn.prepareStatement(
                "INSERT INTO " + TABLE
                + " (bet_id, actor, reason, old_status, new_status, debit_amount, created_at) "
                + "VALUES (?, ?, ?, 'SETTLED', 'VOIDED', ?, ?)");
            ps.setLong(1, betId);
            ps.setString(2, actor != null ? actor : "unknown");
            ps.setString(3, reason != null ? reason : "");
            ps.setLong(4, prizeAmount);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (Throwable t) {
            // Audit failure must not block the unsettle — log WARN and continue
            LOG.warn("JdbcTaixiuUnsettleAuditPort.writeAudit failed betId={} actor={}", betId, actor, t);
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Create the audit table if it does not already exist.
     * Runs at most once per JVM lifetime (volatile flag).
     */
    private void ensureTable() {
        if (tableEnsured) {
            return;
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "  id           BIGINT       NOT NULL AUTO_INCREMENT,"
                + "  bet_id       BIGINT       NOT NULL,"
                + "  actor        VARCHAR(90)  NOT NULL,"
                + "  reason       TEXT         NULL,"
                + "  old_status   VARCHAR(20)  NOT NULL DEFAULT 'SETTLED',"
                + "  new_status   VARCHAR(20)  NOT NULL DEFAULT 'VOIDED',"
                + "  debit_amount BIGINT       NOT NULL DEFAULT 0,"
                + "  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "  PRIMARY KEY (id),"
                + "  KEY idx_unsettle_audit_bet_id (bet_id),"
                + "  KEY idx_unsettle_audit_actor  (actor),"
                + "  KEY idx_unsettle_audit_created (created_at)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
                + "  COMMENT='SUN-1339 B2: admin TaiXiu unsettleBet audit log'");
            ps.executeUpdate();
            tableEnsured = true;
        } catch (Throwable t) {
            LOG.warn("JdbcTaixiuUnsettleAuditPort.ensureTable failed — audit writes may fail", t);
        } finally {
            close(ps, conn);
        }
    }

    private static void close(PreparedStatement ps, Connection conn) {
        if (ps != null) {
            try { ps.close(); } catch (Throwable ignored) { /* no-op */ }
        }
        if (conn != null) {
            try { conn.close(); } catch (Throwable ignored) { /* no-op */ }
        }
    }
}
