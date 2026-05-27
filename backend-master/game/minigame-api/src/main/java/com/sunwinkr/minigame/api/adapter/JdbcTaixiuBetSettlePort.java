package com.sunwinkr.minigame.api.adapter;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * JDBC adapter for {@code vinplay_minigame.taixiu_bet} settle-status operations.
 *
 * <h3>SUN-1339 Phase B1 — settle idempotency</h3>
 * <ul>
 *   <li>{@link #markSettled(long, long)} — flip {@code settle_status = 'SETTLED'},
 *       set {@code settled_at = NOW()}. Returns {@code false} (no-op) if the
 *       row is already {@code SETTLED} or {@code VOIDED}.</li>
 *   <li>{@link #markVoided(long)} — flip {@code settle_status = 'VOIDED'} on a
 *       {@code SETTLED} row. Returns {@code false} if not in {@code SETTLED}
 *       state (caller should 400).</li>
 *   <li>{@link #findById(long)} — look up a single bet row for the admin
 *       unsettleBet pre-check.</li>
 * </ul>
 *
 * <p>Pool name: {@code mysqlpool_minigame} (vinplay_minigame database).
 * All methods wrap failures in a checked {@link SettlePortException} so callers
 * can distinguish DB errors from business-rule rejections.
 *
 * <p>Plan SUN-1339 §B1/B2.
 */
@Component
public class JdbcTaixiuBetSettlePort {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcTaixiuBetSettlePort.class);

    private static final String POOL = "mysqlpool_minigame";

    /** Outcome returned by {@link #markSettled}. */
    public enum SettleOutcome {
        /** Row flipped PENDING → SETTLED. */
        SETTLED,
        /** Row was already SETTLED — no-op. */
        ALREADY_SETTLED,
        /** Row was VOIDED — rejected. */
        ALREADY_VOIDED,
        /** Row not found. */
        NOT_FOUND
    }

    /**
     * Row view returned by {@link #findById}.
     */
    public static final class BetRow {
        public final long id;
        public final String nickname;
        public final long betValue;
        public final long prize;
        public final long refund;
        public final String settleStatus;
        public final short moneyType;
        public final short betSide;
        public final long roundId;

        public BetRow(long id, String nickname, long betValue,
                      long prize, long refund, String settleStatus,
                      short moneyType, short betSide, long roundId) {
            this.id = id;
            this.nickname = nickname;
            this.betValue = betValue;
            this.prize = prize;
            this.refund = refund;
            this.settleStatus = settleStatus;
            this.moneyType = moneyType;
            this.betSide = betSide;
            this.roundId = roundId;
        }
    }

    /**
     * Checked exception for DB-level failures in settle operations.
     */
    public static final class SettlePortException extends Exception {
        public SettlePortException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * B1 — Mark a bet row SETTLED with idempotency guard.
     *
     * <p>Uses a conditional UPDATE: {@code WHERE id=? AND settle_status='PENDING'}.
     * Affected-rows count distinguishes "flipped" from "already settled/voided".
     *
     * @param betId   primary key of {@code taixiu_bet}
     * @param roundId round identifier (used for audit log correlation)
     * @return outcome enum; callers log + skip on {@code ALREADY_SETTLED},
     *         treat {@code NOT_FOUND} and {@code ALREADY_VOIDED} as errors.
     * @throws SettlePortException on DB failure
     */
    public SettleOutcome markSettled(long betId, long roundId) throws SettlePortException {
        // First check current state
        BetRow row;
        try {
            row = findById(betId);
        } catch (SettlePortException e) {
            throw e;
        }
        if (row == null) {
            return SettleOutcome.NOT_FOUND;
        }
        if ("SETTLED".equals(row.settleStatus)) {
            LOG.info("JdbcTaixiuBetSettlePort.markSettled: betId={} roundId={} already SETTLED — no-op",
                betId, roundId);
            return SettleOutcome.ALREADY_SETTLED;
        }
        if ("VOIDED".equals(row.settleStatus)) {
            LOG.warn("JdbcTaixiuBetSettlePort.markSettled: betId={} roundId={} is VOIDED — rejected",
                betId, roundId);
            return SettleOutcome.ALREADY_VOIDED;
        }

        // PENDING → SETTLED
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            ps = conn.prepareStatement(
                "UPDATE taixiu_bet SET settle_status='SETTLED', settled_at=? "
                + "WHERE id=? AND settle_status='PENDING'");
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setLong(2, betId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                // Race — another thread flipped it; re-read
                BetRow recheck = findById(betId);
                if (recheck != null && "SETTLED".equals(recheck.settleStatus)) {
                    return SettleOutcome.ALREADY_SETTLED;
                }
                return SettleOutcome.ALREADY_VOIDED;
            }
            return SettleOutcome.SETTLED;
        } catch (Throwable t) {
            throw new SettlePortException(
                "markSettled failed betId=" + betId + " roundId=" + roundId, t);
        } finally {
            close(ps, conn);
        }
    }

    /**
     * B2 — Flip a SETTLED row to VOIDED (for admin unsettleBet).
     *
     * <p>Uses conditional UPDATE {@code WHERE id=? AND settle_status='SETTLED'}
     * so it is race-safe.
     *
     * @param betId primary key of {@code taixiu_bet}
     * @return {@code true} if the row was flipped; {@code false} if not in SETTLED state
     * @throws SettlePortException on DB failure or row not found
     */
    public boolean markVoided(long betId) throws SettlePortException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            ps = conn.prepareStatement(
                "UPDATE taixiu_bet SET settle_status='VOIDED', updated_date=? "
                + "WHERE id=? AND settle_status='SETTLED'");
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setLong(2, betId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (Throwable t) {
            throw new SettlePortException("markVoided failed betId=" + betId, t);
        } finally {
            close(ps, conn);
        }
    }

    /**
     * Look up a single bet row by primary key.
     *
     * @param betId primary key
     * @return row or {@code null} if not found
     * @throws SettlePortException on DB failure
     */
    public BetRow findById(long betId) throws SettlePortException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL);
            ps = conn.prepareStatement(
                "SELECT id, nick_name, bet_value, prize, refund, settle_status, "
                + "money_type, bet_side, round_id "
                + "FROM taixiu_bet WHERE id=? LIMIT 1");
            ps.setLong(1, betId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return new BetRow(
                rs.getLong("id"),
                rs.getString("nick_name"),
                rs.getLong("bet_value"),
                rs.getLong("prize"),
                rs.getLong("refund"),
                rs.getString("settle_status"),
                rs.getShort("money_type"),
                rs.getShort("bet_side"),
                rs.getLong("round_id"));
        } catch (Throwable t) {
            throw new SettlePortException("findById failed betId=" + betId, t);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (Throwable ignored) { /* no-op */ }
            }
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
