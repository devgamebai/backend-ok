package com.vinplay.dal.withdraw;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Volume tracking service for withdrawal eligibility.
 * Tracks how much "volume" (bet turnover) a user has generated
 * compared to how much is required based on their deposits.
 */
public class VolumeTrackingService {

    private static final Logger logger = Logger.getLogger("dal");

    /**
     * Add bet volume for a user based on a game bet.
     * Looks up game_volume_config for the serviceName to get the volume_percentage,
     * then adds (betAmount * volume_percentage / 100) to user_volume_tracking.total_actual_volume.
     */
    public static void addBetVolume(int userId, String nickname, String serviceName, long betAmount) {
        try {
            int volumePercentage = getGameVolumePercentage(serviceName);
            long volumeToAdd = betAmount * volumePercentage / 100;
            if (volumeToAdd <= 0) {
                return;
            }

            // Upsert: insert if not exists, otherwise add to total_actual_volume and total_commission_volume
            String sql = "INSERT INTO user_volume_tracking (user_id, nick_name, total_required_volume, total_actual_volume, withdrawal_status, last_reset_at, total_commission_volume) " +
                    "VALUES (?, ?, 0, ?, 'DISABLED', NOW(), ?) " +
                    "ON DUPLICATE KEY UPDATE total_actual_volume = total_actual_volume + VALUES(total_actual_volume), " +
                    "total_commission_volume = COALESCE(total_commission_volume, 0) + VALUES(total_commission_volume), " +
                    "nick_name = VALUES(nick_name)";
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, nickname);
                ps.setLong(3, volumeToAdd);
                ps.setLong(4, volumeToAdd);
                ps.executeUpdate();
            }

            logger.debug("addBetVolume userId=" + userId + " serviceName=" + serviceName +
                    " betAmount=" + betAmount + " volumePct=" + volumePercentage + " volumeAdded=" + volumeToAdd);
        } catch (Exception e) {
            logger.error("addBetVolume error userId=" + userId, e);
        }
    }

    /**
     * Rollback bet volume for a user when a bet is cancelled or game is rolled back.
     */
    public static void rollbackBetVolume(int userId, String nickname, String serviceName, long betAmount) {
        try {
            int volumePercentage = getGameVolumePercentage(serviceName);
            long volumeToRemove = betAmount * volumePercentage / 100;
            if (volumeToRemove <= 0) {
                return;
            }

            // Decrease total_actual_volume and total_commission_volume, ensure it doesn't go below 0
            String sql = "UPDATE user_volume_tracking SET total_actual_volume = GREATEST(CAST(total_actual_volume AS SIGNED) - ?, 0), " +
                         "total_commission_volume = GREATEST(CAST(COALESCE(total_commission_volume, 0) AS SIGNED) - ?, 0) " +
                         "WHERE user_id = ?";
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, volumeToRemove);
                ps.setLong(2, volumeToRemove);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }

            logger.debug("rollbackBetVolume userId=" + userId + " serviceName=" + serviceName +
                    " betAmount=" + betAmount + " volumeRemoved=" + volumeToRemove);
        } catch (Exception e) {
            logger.error("rollbackBetVolume error userId=" + userId, e);
        }
    }

    /**
     * Add required volume when a deposit is approved.
     * Reads default_coefficient from withdrawal_settings,
     * calculates depositAmount * coefficient, sets total_required_volume.
     *
     * SUN-1376 (PROD bug VietTu68):
     * - QC rule (ticket note): "Khi số dư về 0 hoặc có khoản tiền nạp mới
     *   thì rolling yêu cầu rút tiền reset lại tính từ đầu" — every deposit
     *   resets the rolling counter from zero, regardless of pre-deposit
     *   balance.
     * - Old behaviour (balance>0 ⇒ accumulate) let a user with massive
     *   historical actual_volume (5M from prior bets, 200k old required
     *   already satisfied) bypass the new requirement: deposit 800k,
     *   required becomes 200k+800k=1M, actual stays 5M ≥ 1M → status
     *   ENABLED → withdraw succeeded with only 250k bet against the
     *   800k deposit. Production bug VietTu68 2026-05-17.
     * - New behaviour: every deposit triggers a fresh reset — actual=0,
     *   required = depositAmount × coefficient. User MUST roll the new
     *   deposit before withdrawing.
     *
     * Examples (post-SUN-1376):
     *   TH1: balance=0, deposit 100k, coeff=1 → required=100k actual=0
     *   TH2: balance=10k, deposit 100k, coeff=1 → required=100k actual=0
     *        (NOT 110k accumulated — old required is discarded, fresh start)
     *   With promo: deposit 100k + 50k KM, turnover x3 → required=(100k+50k)*3=450k
     *
     * For non-deposit rolling sources (signing bonus, giftcode) that
     * legitimately need accumulation without reset, use
     * {@link #addRequiredVolumeDirect(int, String, long)} (shouldReset=false)
     * — that contract is unchanged.
     */
    /**
     * Overload for callers that don't have pre-credit balance (legacy).
     */
    public static void addRequiredVolume(int userId, String nickname, long depositAmount) {
        addRequiredVolume(userId, nickname, depositAmount, -1);
    }

    /**
     * Add required volume when a deposit is approved.
     * @param balanceBeforeCredit user's vin balance BEFORE deposit was credited.
     *        If <= 0 → RESET volumes (TH1/TH3). If > 0 → ACCUMULATE (TH2).
     *        Pass -1 if unknown (will check from cache/DB).
     */
    public static void addRequiredVolume(int userId, String nickname, long depositAmount, long balanceBeforeCredit) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {

            // Read default_coefficient from withdrawal_settings
            long coefficient = 1;
            try (PreparedStatement psSelect = conn.prepareStatement("SELECT setting_value FROM withdrawal_settings WHERE setting_key = 'default_coefficient'");
                 ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    try {
                        coefficient = Long.parseLong(rs.getString("setting_value"));
                    } catch (NumberFormatException nfe) {
                        coefficient = 1;
                    }
                }
            }

            long requiredToAdd = depositAmount * coefficient;

            // SUN-1: If caller didn't provide balance, look it up
            if (balanceBeforeCredit < 0) {
                balanceBeforeCredit = 0;
                try {
                    com.hazelcast.core.IMap<String, com.vinplay.vbee.common.models.cache.UserCacheModel> userMap =
                            com.vinplay.vbee.common.hazelcast.HazelcastClientFactory.getInstance().getMap("users");
                    com.vinplay.vbee.common.models.cache.UserCacheModel uc = userMap.get(nickname);
                    if (uc != null) balanceBeforeCredit = uc.getVin();
                } catch (Exception cacheErr) {
                    try (PreparedStatement psBalance = conn.prepareStatement("SELECT vin FROM users WHERE id = ?")) {
                        psBalance.setInt(1, userId);
                        try (ResultSet rsB = psBalance.executeQuery()) {
                            if (rsB.next()) balanceBeforeCredit = rsB.getLong("vin");
                        }
                    }
                }
            }

            // SUN-1376: deposit always resets the rolling counter — see method
            // javadoc for the production bug rationale. Pre-credit balance is
            // kept in the log line for ops audit but no longer gates reset.
            boolean shouldReset = true;
            addRequiredVolumeDirect(userId, nickname, requiredToAdd, shouldReset);
            logger.info("addRequiredVolume userId=" + userId + " depositAmount=" + depositAmount +
                    " coefficient=" + coefficient + " requiredAdded=" + requiredToAdd +
                    " balanceBeforeCredit=" + balanceBeforeCredit + " reset=" + shouldReset);
        } catch (Exception e) {
            logger.error("addRequiredVolume error userId=" + userId, e);
        }
    }

    /**
     * Add required volume directly.
     * Used for non-deposit sources such as signing bonus wagering requirements.
     */
    public static void addRequiredVolumeDirect(int userId, String nickname, long requiredToAdd) {
        addRequiredVolumeDirect(userId, nickname, requiredToAdd, false);
    }

    /**
     * Add required volume directly, with optional reset.
     *
     * @param shouldReset if true, reset volumes to zero first (user had 0 balance = fresh start)
     */
    public static void addRequiredVolumeDirect(int userId, String nickname, long requiredToAdd, boolean shouldReset) {
        if (requiredToAdd <= 0) {
            return;
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            boolean rowExists;
            try (PreparedStatement psSelect = conn.prepareStatement("SELECT total_required_volume, total_actual_volume FROM user_volume_tracking WHERE user_id = ?")) {
                psSelect.setInt(1, userId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    rowExists = rs.next();
                }
            }

            if (rowExists) {
                if (shouldReset) {
                    // SUN-1 TH1/TH3: user balance was 0 → fresh start, reset both volumes
                    try (PreparedStatement psUpdate = conn.prepareStatement(
                            "UPDATE user_volume_tracking SET total_required_volume = ?, total_actual_volume = 0, " +
                            "total_commission_volume = 0, withdrawal_status = 'DISABLED', last_reset_at = NOW(), nick_name = ? WHERE user_id = ?")) {
                        psUpdate.setLong(1, requiredToAdd);
                        psUpdate.setString(2, nickname);
                        psUpdate.setInt(3, userId);
                        psUpdate.executeUpdate();
                    }
                } else {
                    // SUN-1 TH2: user still has balance → accumulate required volume
                    try (PreparedStatement psUpdate = conn.prepareStatement(
                            "UPDATE user_volume_tracking SET total_required_volume = total_required_volume + ?, " +
                            "withdrawal_status = 'DISABLED', nick_name = ? WHERE user_id = ?")) {
                        psUpdate.setLong(1, requiredToAdd);
                        psUpdate.setString(2, nickname);
                        psUpdate.setInt(3, userId);
                        psUpdate.executeUpdate();
                    }
                }
            } else {
                try (PreparedStatement psUpdate = conn.prepareStatement(
                        "INSERT INTO user_volume_tracking (user_id, nick_name, total_required_volume, total_actual_volume, " +
                        "total_commission_volume, withdrawal_status, last_reset_at) " +
                        "VALUES (?, ?, ?, 0, 0, 'DISABLED', NOW())")) {
                    psUpdate.setInt(1, userId);
                    psUpdate.setString(2, nickname);
                    psUpdate.setLong(3, requiredToAdd);
                    psUpdate.executeUpdate();
                }
            }

            logger.debug("addRequiredVolumeDirect userId=" + userId + " requiredAdded=" + requiredToAdd + " reset=" + shouldReset);
        } catch (Exception e) {
            logger.error("addRequiredVolumeDirect error userId=" + userId, e);
        }
    }

    /**
     * Compare actual vs required volume and update withdrawal_status.
     * ENABLED if actual >= required, DISABLED otherwise.
     */
    public static void updateWithdrawalStatus(int userId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE user_volume_tracking SET withdrawal_status = " +
                     "CASE WHEN total_actual_volume >= total_required_volume THEN 'ENABLED' ELSE 'DISABLED' END " +
                     "WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("updateWithdrawalStatus error userId=" + userId, e);
        }
    }

    /**
     * Get volume percentage for a game by serviceName.
     * Queries game_volume_config where game_name LIKE '%serviceName%' and is_active=1.
     * Returns volume_percentage, or 100 if not found (default: 100% counts toward volume).
     */
    public static int getGameVolumePercentage(String serviceName) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT volume_percentage FROM game_volume_config WHERE game_name LIKE ? AND is_active = 1 LIMIT 1")) {
            ps.setString(1, "%" + serviceName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("volume_percentage");
                }
            }
        } catch (Exception e) {
            logger.error("getGameVolumePercentage error serviceName=" + serviceName, e);
        }
        return 100; // default
    }
}
