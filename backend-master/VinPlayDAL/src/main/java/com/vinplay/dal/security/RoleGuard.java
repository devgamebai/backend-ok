package com.vinplay.dal.security;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SUN-1099: Role-based deny guard for the wallet system.
 *
 * Per PM clarification (2026-04-25), SpecialAccount is read-only — every
 * money-related action must be blocked, including:
 *   - withdrawals (bank, crypto, agency wallet)
 *   - cashback claim
 *   - cashback batch payout (skip rows belonging to SpecialAccount)
 *   - credit transfer (as actor or recipient)
 *   - credit topup to game wallet (as actor or recipient)
 *
 * SpecialAccount is identified structurally by useragent.code='0' (per
 * agency_enhancement_migration.sql, SUN-765). The deployed system has
 * exactly one such row by design.
 *
 * The check is deliberately strict: on DB error we fail-CLOSED and treat
 * the lookup as "is SpecialAccount" so a transient outage cannot let a
 * forbidden action through. SpecialAccount has zero throughput in normal
 * operations, so the false-positive cost is negligible.
 */
public final class RoleGuard {

    private static final Logger logger = Logger.getLogger("dal");

    /**
     * Error code returned from processors when a SpecialAccount tries to
     * perform a money action. FE / ops can match on this to render the
     * Vietnamese "Tài khoản chỉ xem" message.
     */
    public static final String ERR_CODE_SPECIAL_ACCOUNT = "1099";
    public static final String ERR_MSG_SPECIAL_ACCOUNT  = "SpecialAccount cannot perform money actions";

    private RoleGuard() {}

    /**
     * Returns true if the given nickname belongs to a SpecialAccount.
     *
     * Lookup walks: users.nick_name -> useragent (joined by nickname) and
     * checks code='0'. We can't simply match the constant string
     * "SpecialAccount" because the nickname column is editable and the
     * spec keys off the structural code='0' marker, not display name.
     *
     * Fail-closed: any exception returns true. The cost of a stuck
     * mysqlpool_admin connection is at most a few denied money actions
     * for legitimate users, recoverable with a retry. Letting a forbidden
     * action through on a DB hiccup is unrecoverable.
     */
    public static boolean isSpecialAccount(String nickname) {
        if (nickname == null || nickname.isEmpty()) return false;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
            ps = conn.prepareStatement(
                    "SELECT 1 FROM useragent WHERE code = '0' AND nickname = ? LIMIT 1");
            ps.setString(1, nickname);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.warn("RoleGuard.isSpecialAccount fail-closed for nick=" + nickname
                    + " (DB error treated as SpecialAccount): " + e.getMessage());
            return true;
        } finally {
            close(rs, ps, conn);
        }
    }

    private static void close(AutoCloseable... rs) {
        for (AutoCloseable r : rs) {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
    }
}
