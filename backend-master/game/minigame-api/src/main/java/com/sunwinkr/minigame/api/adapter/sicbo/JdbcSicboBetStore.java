package com.sunwinkr.minigame.api.adapter.sicbo;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * JDBC adapter for the {@code vinplay_minigame.sicbo_bet} table introduced
 * by migration {@code 20260515_provider_contract_round_settle.sql} (SUN-1339 A1).
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>INSERT a new bet row on every accepted Sicbo bet (round-settle pipeline).</li>
 *   <li>UPDATE {@code settle_status='SETTLED'} + {@code prize}, {@code settled_at}
 *       on settlement — idempotent: returns {@code false} and logs when the row
 *       is already SETTLED.</li>
 *   <li>UPDATE {@code settle_status='VOIDED'} on admin unsettleBet — callers
 *       must verify the prior status is SETTLED before calling.</li>
 *   <li>Lookup a single bet row by primary key for the admin unsettle path.</li>
 * </ul>
 *
 * <h3>Connection pool</h3>
 * Uses {@code mysqlpool_minigame} — same pool as {@code JdbcBetStore} in
 * lottery-api. Connections are returned via try-with-resources.
 *
 * <h3>SQL injection</h3>
 * All bindings go through {@link PreparedStatement} — no string-concat SQL.
 *
 * <p>Plan SUN-1339 §B1.
 */
@Component("jdbcSicboBetStore")
public class JdbcSicboBetStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSicboBetStore.class);

    private static final String POOL = "mysqlpool_minigame";

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Fetch a single bet row by primary key.
     *
     * @param ticketId primary key ({@code sicbo_bet.id})
     * @return a {@link SicboBetRow} or {@code null} when not found
     */
    public SicboBetRow findById(long ticketId) {
        final String sql =
            "SELECT id, nick_name, user_id, bet_value, prize, money_type, " +
            "       settle_status, round_id, reference_id " +
            "  FROM sicbo_bet " +
            " WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        } catch (Exception e) {
            LOG.warn("JdbcSicboBetStore.findById failed ticketId={}", ticketId, e);
            return null;
        }
    }

    /**
     * Mark a bet row as SETTLED (idempotent).
     *
     * <p>If the row is already SETTLED this method logs a warning and returns
     * {@code false} — callers should treat that as a no-op, not an error.
     * The composite unique key {@code uk_sicbo_bet_round_ticket(round_id, id)}
     * added in A1 provides additional row-level dedup at the DB layer.
     *
     * @param ticketId  primary key
     * @param prize     gross prize credited to the user (0 for losers)
     * @return {@code true} when the UPDATE applied; {@code false} when already SETTLED or row missing
     */
    public boolean markSettled(long ticketId, long prize) {
        final String sql =
            "UPDATE sicbo_bet " +
            "   SET settle_status = 'SETTLED', prize = ?, settled_at = ? " +
            " WHERE id = ? AND settle_status = 'PENDING'";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, prize);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setLong(3, ticketId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                LOG.warn("JdbcSicboBetStore.markSettled no-op ticketId={} — already SETTLED or missing", ticketId);
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.error("JdbcSicboBetStore.markSettled failed ticketId={}", ticketId, e);
            return false;
        }
    }

    /**
     * Mark a bet row as VOIDED (admin unsettleBet path).
     *
     * <p>Callers MUST verify that the row is SETTLED before calling this;
     * the WHERE clause enforces it at the DB level so concurrent calls are
     * safe.
     *
     * @param ticketId primary key
     * @return {@code true} when the UPDATE applied
     */
    public boolean markVoided(long ticketId) {
        final String sql =
            "UPDATE sicbo_bet " +
            "   SET settle_status = 'VOIDED', updated_date = ? " +
            " WHERE id = ? AND settle_status = 'SETTLED'";
        try (Connection conn = ConnectionPool.getInstance().getConnection(POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setLong(2, ticketId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                LOG.warn("JdbcSicboBetStore.markVoided no-op ticketId={} — not SETTLED or missing", ticketId);
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.error("JdbcSicboBetStore.markVoided failed ticketId={}", ticketId, e);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Row mapping
    // -----------------------------------------------------------------------

    private static SicboBetRow mapRow(ResultSet rs) throws SQLException {
        SicboBetRow row = new SicboBetRow();
        row.id           = rs.getLong("id");
        row.nickname     = rs.getString("nick_name");
        row.userId       = rs.getLong("user_id");
        row.betValue     = rs.getLong("bet_value");
        row.prize        = rs.getLong("prize");
        row.moneyType    = rs.getShort("money_type");
        row.settleStatus = rs.getString("settle_status");
        row.roundId      = rs.getLong("round_id");
        row.referenceId  = rs.getLong("reference_id");
        return row;
    }

    // -----------------------------------------------------------------------
    // Value object
    // -----------------------------------------------------------------------

    /**
     * Projection of a {@code sicbo_bet} row used by the settle + unsettle paths.
     */
    public static final class SicboBetRow {
        public long   id;
        public String nickname;
        public long   userId;
        public long   betValue;
        public long   prize;
        public short  moneyType;
        public String settleStatus;
        public long   roundId;
        public long   referenceId;

        /** {@code true} when {@link #settleStatus} equals {@code "SETTLED"}. */
        public boolean isSettled() {
            return "SETTLED".equals(settleStatus);
        }

        /** {@code true} when {@link #settleStatus} equals {@code "PENDING"}. */
        public boolean isPending() {
            return "PENDING".equals(settleStatus);
        }

        /** {@code true} when {@link #settleStatus} equals {@code "VOIDED"}. */
        public boolean isVoided() {
            return "VOIDED".equals(settleStatus);
        }

        /** Money type string for {@link com.sunwinkr.minigame.engine.port.WalletPort}. */
        public String moneyTypeStr() {
            return moneyType == 1 ? "vin" : "xu";
        }
    }
}
