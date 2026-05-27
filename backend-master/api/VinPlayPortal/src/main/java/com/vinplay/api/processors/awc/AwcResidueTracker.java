package com.vinplay.api.processors.awc;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SUN-AWC-DECIMAL: per-user fractional residue tracker for AWC sub-VND amounts.
 *
 * <p>AWC sends amounts with up to 3 decimal places (e.g. winAmount=19.5).
 * users.vin is BIGINT (integer VND), so 0.500 fractions would be silently
 * dropped on every fractional settle. This tracker stores the 0-999
 * milli-VND residue per user in {@code vinplay.awc_user_residue} and carries
 * or borrows into the integer VND delta when the accumulated residue crosses
 * a whole-VND boundary.
 *
 * <p>AWC-only — other providers (GSC, native games) keep integer semantics.
 * Storage layer (users.vin BIGINT) is unchanged.
 *
 * <p>Worked example (SXB-1 sequence):
 * <pre>
 *   Initial: vin=27191150, residue=0   → balance 27191150.000
 *   Bet   10.000 (10000 milli): vinDelta=-10, residue stays 0
 *                                        → vin=27191140, residue=0
 *   Settle 19.500 (19500 milli): milliDelta=19500
 *     vinDelta   = 19500 / 1000 = 19
 *     residue    = 0 + 500 = 500
 *     → vin=27191159, residue=500  → balance 27191159.500 ✓
 * </pre>
 */
public final class AwcResidueTracker {

    private static final Logger logger = LoggerFactory.getLogger("awc");

    private AwcResidueTracker() {}

    /**
     * Atomically apply {@code milliDelta} (milli-VND, can be negative) to the
     * user's residue row and return the integer VND delta to pass to
     * MoneyGateway.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>UPSERT residue row (INSERT IGNORE then SELECT … FOR UPDATE)</li>
     *   <li>new_residue = current_residue + (milliDelta mod 1000), using
     *       floor-division so negatives are handled consistently</li>
     *   <li>vin_delta = milliDelta / 1000 (floor toward -∞)</li>
     *   <li>Carry:  if new_residue >= 1000 → vin_delta++, new_residue -= 1000</li>
     *   <li>Borrow: if new_residue &lt; 0    → vin_delta--, new_residue += 1000</li>
     *   <li>UPDATE residue row</li>
     *   <li>Return vin_delta (integer VND to credit/debit)</li>
     * </ol>
     *
     * @param userId     vinplay users.id (BIGINT)
     * @param milliDelta amount in milli-VND (×1000); positive = credit, negative = debit
     * @return integer VND delta to apply via MoneyGateway (can be 0)
     * @throws SQLException if the DB operation fails (caller should treat as fatal)
     */
    public static long applyMilliDelta(long userId, long milliDelta) throws SQLException {
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        try {
            conn.setAutoCommit(false);

            // Ensure row exists
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT IGNORE INTO vinplay.awc_user_residue (user_id, residue_milli_vnd) VALUES (?, 0)")) {
                ins.setLong(1, userId);
                ins.executeUpdate();
            }

            // Lock the row
            int currentResidue;
            try (PreparedStatement sel = conn.prepareStatement(
                    "SELECT residue_milli_vnd FROM vinplay.awc_user_residue WHERE user_id = ? FOR UPDATE")) {
                sel.setLong(1, userId);
                try (ResultSet rs = sel.executeQuery()) {
                    currentResidue = rs.next() ? rs.getInt(1) : 0;
                }
            }

            // milliDelta = wholePart*1000 + remainder  (floor division toward -inf)
            long vinDelta = Math.floorDiv(milliDelta, 1000L);
            long residueDelta = Math.floorMod(milliDelta, 1000L); // always 0..999

            long newResidue = currentResidue + residueDelta;

            // Carry
            if (newResidue >= 1000L) {
                vinDelta++;
                newResidue -= 1000L;
            }
            // Borrow (should not happen given floorMod gives 0..999, but guard anyway)
            if (newResidue < 0L) {
                vinDelta--;
                newResidue += 1000L;
            }

            try (PreparedStatement upd = conn.prepareStatement(
                    "UPDATE vinplay.awc_user_residue SET residue_milli_vnd = ? WHERE user_id = ?")) {
                upd.setInt(1, (int) newResidue);
                upd.setLong(2, userId);
                upd.executeUpdate();
            }

            conn.commit();
            return vinDelta;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException rb) { logger.warn("rollback failed: {}", rb.getMessage()); }
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Read the current residue (milli-VND) for a user. Returns 0 if no row exists.
     * Used for balance responses: returned balance = vin + residue/1000.
     *
     * @param userId vinplay users.id (BIGINT)
     * @return 0..999 milli-VND residue, or 0 on error
     */
    public static int getResidue(long userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT residue_milli_vnd FROM vinplay.awc_user_residue WHERE user_id = ? LIMIT 1")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            logger.warn("AwcResidueTracker.getResidue failed userId={}: {}", userId, e.getMessage());
            return 0;
        }
    }
}
