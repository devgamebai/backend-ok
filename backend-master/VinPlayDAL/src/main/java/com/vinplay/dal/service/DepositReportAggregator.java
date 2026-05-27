package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Maintains the cumulative deposit/withdraw counters that drive the
 * admin "Quản lý user" panel and the {@code log_report_user} daily
 * roll-up consumed by the agent revenue report.
 *
 * <p>Mirrors the {@link MoneyGateway} ledger pattern: idempotency is
 * the caller's responsibility. The deposit/withdraw approval services
 * already gate the row flip with {@code WHERE status='PENDING'} so
 * each transaction's {@code applyDeposit}/{@code applyWithdraw} call
 * fires at most once per row in normal operation. A retry triggered
 * after a successful flip will double-count the aggregates — accepted
 * trade-off for now since the alternative (extra audit table or
 * {@code aggregates_applied_at} column) would require a migration and
 * the canonical wallet balance is unaffected (only the
 * {@code users.t_nap} / {@code users.t_rut} reporting columns).
 *
 * <p>Both methods write inside a single Connection so the per-user
 * cumulative bump and the per-day {@code log_report_user} upsert
 * commit atomically. On any DB error the change is rolled back and
 * the caller receives a {@code false} return — failure is logged but
 * not propagated, matching {@code MoneyGateway} convention (an
 * aggregate-write blip must NEVER mask a successful credit).
 *
 * <p>Tables touched:
 * <ul>
 *   <li>{@code vinplay.users} — {@code t_nap += amount, nap_times += 1}
 *       (deposit) or {@code t_rut += amount, rut_times += 1} (withdraw).</li>
 *   <li>{@code vinplay.log_report_user} — UPSERT keyed on
 *       {@code (nick_name, time_report)} (composite UNIQUE
 *       {@code nickname_time}). Inserts a fresh row for the player's
 *       day if absent, else accumulates {@code deposit}/{@code withdraw}.</li>
 * </ul>
 */
public final class DepositReportAggregator {

    private static final Logger logger = Logger.getLogger("backend");

    private DepositReportAggregator() {}

    /** Bump deposit aggregates for an APPROVED deposit. */
    public static boolean applyDeposit(long userId, String nickName, long amount) {
        return apply(userId, nickName, amount, true);
    }

    /** Bump withdraw aggregates for an APPROVED withdrawal. */
    public static boolean applyWithdraw(long userId, String nickName, long amount) {
        return apply(userId, nickName, amount, false);
    }

    private static boolean apply(long userId, String nickName, long amount, boolean isDeposit) {
        if (amount <= 0L || userId <= 0L || nickName == null || nickName.isEmpty()) {
            return false;
        }
        Connection conn = null;
        boolean prevAuto = true;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            prevAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            String userSql = isDeposit
                    ? "UPDATE users SET t_nap = COALESCE(t_nap,0) + ?, nap_times = COALESCE(nap_times,0) + 1 WHERE id = ?"
                    : "UPDATE users SET t_rut = COALESCE(t_rut,0) + ?, rut_times = COALESCE(rut_times,0) + 1 WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setLong(1, amount);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }

            // log_report_user has UNIQUE (nick_name, time_report). On the
            // FIRST hit for the day the row is inserted with deposit OR
            // withdraw populated and the rest at 0 (matches the legacy
            // schema's nullable bigints). On subsequent hits for the same
            // day the matching column accumulates while the others stay
            // untouched — including any prior deposit if this call is a
            // withdraw, and vice versa.
            String reportSql = isDeposit
                    ? "INSERT INTO log_report_user (time_report, nick_name, user_id, deposit, withdraw, t_bonus) "
                            + "VALUES (CURDATE(), ?, ?, ?, 0, 0) "
                            + "ON DUPLICATE KEY UPDATE deposit = COALESCE(deposit,0) + VALUES(deposit), "
                            + "user_id = COALESCE(user_id, VALUES(user_id))"
                    : "INSERT INTO log_report_user (time_report, nick_name, user_id, deposit, withdraw, t_bonus) "
                            + "VALUES (CURDATE(), ?, ?, 0, ?, 0) "
                            + "ON DUPLICATE KEY UPDATE withdraw = COALESCE(withdraw,0) + VALUES(withdraw), "
                            + "user_id = COALESCE(user_id, VALUES(user_id))";
            try (PreparedStatement ps = conn.prepareStatement(reportSql)) {
                ps.setString(1, nickName);
                ps.setLong(2, userId);
                ps.setLong(3, amount);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignore) {}
            }
            logger.warn("DepositReportAggregator." + (isDeposit ? "applyDeposit" : "applyWithdraw")
                    + " failed userId=" + userId + " nick=" + nickName + " amount=" + amount
                    + ": " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(prevAuto); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }
        }
    }
}
