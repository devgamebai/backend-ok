package com.vinplay.api.processors.cashback;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Helper for the rolling 7-day cashback expiry rule (SUN-764 Q3).
 *
 * Rule (PM confirmed 2026-04-10):
 *   "Lần claim gần nhất sau đó có phát sinh số dư pending đầu tiên thì sẽ + 7 ngày,
 *    sau 7 ngày đó không claim thì sẽ xóa toàn bộ số dư pending và reset lại round."
 *
 * Translation:
 *   - For each user, find the OLDEST PENDING SELF rebate_log row (= start of the
 *     current 7-day cycle, because that's the first pending after their last claim).
 *   - If now - oldest >= 7 days, wipe ALL of that user's PENDING SELF rows. The
 *     next bet starts a fresh cycle automatically (the new INSERT becomes the new
 *     "oldest").
 *
 * Design note: instead of a scheduled cron, we run this lazily at the start of
 * every GetPending/Claim call for the calling user. This keeps state per-user
 * without requiring a separate background job, and the blast radius is bounded
 * to one user at a time. A dedicated admin sweep processor can be added later if
 * we need to force-wipe across the whole user base.
 */
public final class CashbackExpiryHelper {

    private static final Logger logger = Logger.getLogger("api");
    private static final int EXPIRY_DAYS = 7;

    private CashbackExpiryHelper() {}

    /**
     * Wipe this user's PENDING self-rebates if their oldest pending is older than
     * the 7-day window. Returns the number of rows deleted (0 if not yet expired).
     * Never throws — logs and returns 0 on any error to keep the caller clean.
     */
    public static int wipeIfExpired(String nickname) {
        if (nickname == null || nickname.isEmpty()) return 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM rebate_logs " +
                 "WHERE agent_nickname = ? " +
                 "AND rebate_type = 'SELF' " +
                 "AND status = 'PENDING' " +
                 "AND EXISTS ( " +
                 "  SELECT 1 FROM (SELECT MIN(created_at) AS oldest FROM rebate_logs " +
                 "                 WHERE agent_nickname = ? " +
                 "                 AND rebate_type = 'SELF' " +
                 "                 AND status = 'PENDING') x " +
                 "  WHERE x.oldest IS NOT NULL AND x.oldest < NOW() - INTERVAL ? DAY " +
                 ")")) {
            ps.setString(1, nickname);
            ps.setString(2, nickname);
            ps.setInt(3, EXPIRY_DAYS);
            int n = ps.executeUpdate();
            if (n > 0) {
                logger.info("CashbackExpiryHelper: wiped " + n
                        + " expired pending rebates for user=" + nickname
                        + " (rolling " + EXPIRY_DAYS + "d window)");
            }
            return n;
        } catch (Exception e) {
            logger.warn("CashbackExpiryHelper wipe failed user=" + nickname + " err=" + e.getMessage());
            return 0;
        }
    }

    public static int getExpiryDays() {
        return EXPIRY_DAYS;
    }
}
