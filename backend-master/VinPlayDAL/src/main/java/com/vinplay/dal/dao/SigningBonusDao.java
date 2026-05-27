package com.vinplay.dal.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DAO for Signing Bonus feature.
 * Handles device install tracking, bonus config, and bonus claim logs.
 */
public interface SigningBonusDao {

    // ── Device Install Tracking ──────────────────────────────────────

    /**
     * Check if a device fingerprint has been registered before.
     */
    boolean isDeviceRegistered(String deviceFingerprint) throws SQLException;

    /**
     * Register a new device first-install.
     * @return true if inserted (new device), false if duplicate (already registered)
     */
    boolean registerDevice(String deviceFingerprint, String platform,
                           String appVersion, String ipAddress) throws SQLException;

    /**
     * Link a registered device to a user after registration.
     */
    boolean linkDeviceToUser(String deviceFingerprint, long userId,
                             String nickName) throws SQLException;

    // ── Bonus Config ─────────────────────────────────────────────────

    /**
     * Get the currently active signing bonus config.
     * @return config map with keys: id, bonus_amount, status, etc. or null if none active
     */
    Map<String, Object> getActiveConfig() throws SQLException;

    /**
     * Get signing bonus config by ID.
     */
    Map<String, Object> getConfig(int configId) throws SQLException;

    /**
     * Update signing bonus config.
     */
    boolean updateConfig(int configId, long bonusAmount, int status,
                         int wagerEnabled, double wagerMultiplier,
                         String updatedBy) throws SQLException;

    /**
     * List all signing bonus configs.
     */
    List<Map<String, Object>> listConfigs() throws SQLException;

    // ── Bonus Claim Logs ─────────────────────────────────────────────

    /**
     * Check if a user has already received a signing bonus.
     */
    boolean hasUserReceivedBonus(long userId) throws SQLException;

    /**
     * Check if a device has already been used to claim a signing bonus.
     */
    boolean hasDeviceReceivedBonus(String deviceFingerprint) throws SQLException;

    /**
     * Insert a signing bonus claim log.
     * Uses INSERT IGNORE to handle race conditions via UNIQUE keys.
     * @return generated log ID, or -1 if duplicate/failed
     */
    long insertBonusLog(long userId, String nickName, String deviceFingerprint,
                        long bonusAmount, int configId) throws SQLException;

    /**
     * List signing bonus claim logs for admin report.
     */
    List<Map<String, Object>> listBonusLogs(String nickName, String dateFrom,
                                            String dateTo, int page,
                                            int limit) throws SQLException;

    /**
     * Count total signing bonus logs (for pagination).
     */
    int countBonusLogs(String nickName, String dateFrom,
                       String dateTo) throws SQLException;

    // ── Manual Payout ────────────────────────────────────────────────

    /**
     * Insert a signing bonus claim log for manual payout.
     * Uses INSERT IGNORE to handle race conditions via UNIQUE keys.
     * @return generated log ID, or -1 if duplicate/failed
     */
    long insertBonusLogManual(long userId, String nickName, String deviceFingerprint,
                              long bonusAmount, int configId,
                              String payoutBy, String reason) throws SQLException;

    /**
     * Update status of a signing bonus log entry.
     * Used to rollback status to 0 (pending) if wallet credit fails after insert.
     */
    boolean updateBonusLogStatus(long logId, int status) throws SQLException;

    /**
     * Delete a signing bonus log entry by ID.
     * Used as rollback when wallet credit fails after log insert.
     */
    boolean deleteBonusLog(long logId) throws SQLException;

    // ── Per-User Config ──────────────────────────────────────────────

    /**
     * Get user-level signing bonus config.
     * @return config map with keys: enabled, payout_mode, reason, etc. or null if no override
     */
    Map<String, Object> getUserConfig(long userId) throws SQLException;

    /**
     * Get user-level signing bonus config by nick_name.
     */
    Map<String, Object> getUserConfigByNickName(String nickName) throws SQLException;

    /**
     * Insert or update per-user signing bonus config.
     * @return true if upserted successfully
     */
    boolean upsertUserConfig(long userId, String nickName, int enabled,
                             String payoutMode, String reason,
                             String updatedBy) throws SQLException;
}
