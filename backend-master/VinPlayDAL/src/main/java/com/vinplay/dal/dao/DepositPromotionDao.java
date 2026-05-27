package com.vinplay.dal.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DAO for deposit promotion configuration and claim logs.
 */
public interface DepositPromotionDao {

    // ── Config CRUD ──────────────────────────────────────────────────

    /**
     * Create a new promotion config.
     * @param gate Kênh nạp áp dụng: ALL, BANK, CRYPTO
     * @return generated ID, or -1 on failure
     */
    long createPromotion(int promoType, String gate, double bonusPercent, int maxUsers,
                         double turnoverFactor, long maxBonusPerTx,
                         Integer maxClaimsDaily, Long maxBonusDaily,
                         String startTime, String endTime,
                         String title, String description,
                         String conditionText) throws SQLException;

    /**
     * Get a single promotion config by ID.
     */
    Map<String, Object> getPromotion(long promoId) throws SQLException;

    /**
     * List all promotion configs, optionally filtered by type and/or gate.
     * @param promoType 0 = all, 1 = first deposit, 2 = daily
     * @param gate      null or empty = all gates; "BANK", "CRYPTO", "ALL" = filter
     */
    List<Map<String, Object>> listPromotions(int promoType, String gate) throws SQLException;

    /**
     * Update promotion config fields.
     * @param gate Kênh nạp: ALL, BANK, CRYPTO
     */
    boolean updatePromotion(long promoId, String gate, double bonusPercent, int maxUsers,
                            double turnoverFactor, long maxBonusPerTx,
                            Integer maxClaimsDaily, Long maxBonusDaily,
                            String startTime, String endTime, int status,
                            String title,
                            String description,
                            String conditionText)
                            throws SQLException;

    /**
     * @param promoType promotion type
     * @param gate      payment gate (ALL/BANK/CRYPTO); null or "ALL" = no gate filter
     * Returns null if none active.
     */
    Map<String, Object> getActivePromotion(int promoType, String gate) throws SQLException;

    /**
     * Find all active promotions for a given type and gate, ordered from newest to oldest.
     * @param promoType promotion type
     * @param gate      payment gate (ALL/BANK/CRYPTO); null or "ALL" = no gate filter
     */
    List<Map<String, Object>> getActivePromotions(int promoType, String gate) throws SQLException;

    /**
     * Hard delete a promotion config.
     * @param promoId ID of the promotion to delete
     * @return true if deleted, false otherwise
     */
    boolean deletePromotion(long promoId) throws SQLException;

    // ── Eligibility queries ──────────────────────────────────────────

    /**
     * Count distinct users who have claimed a specific promotion.
     * Used to check {B} max_users limit.
     */
    int countDistinctUsers(long promoId) throws SQLException;

    /**
     * Check if a user has ever received a First Deposit promotion.
     */
    boolean hasFirstDepositClaim(long userId) throws SQLException;

    /**
     * Check if a user received a First Deposit promotion today.
     */
    boolean hasFirstDepositClaimToday(long userId) throws SQLException;

    /**
     * Count how many times a user has claimed a specific promotion today.
     * Used to check {E} max_claims_daily.
     */
    int countUserClaimsToday(long userId, long promoId) throws SQLException;

    /**
     * Sum of bonus_amount a user has received from a specific promotion today.
     * Used to check {F} max_bonus_daily.
     */
    long sumUserBonusToday(long userId, long promoId) throws SQLException;

    // ── Log operations ───────────────────────────────────────────────

    /**
     * Insert a claim log after bonus is credited.
     * @return generated log ID
     */
    long insertClaimLog(long promoId, int promoType, long userId, String nickName,
                        long depositTxId, long depositAmount, long bonusAmount) throws SQLException;

    /**
     * List claim logs for admin report.
     * @param nickName optional nickname filter (LIKE %nickName%)
     */
    List<Map<String, Object>> listClaimLogs(Long promoId, Long userId, String nickName,
                                            String dateFrom, String dateTo,
                                            int page, int limit) throws SQLException;

    /**
     * Mark a claim log as turnover-completed.
     */
    boolean markCompleted(long logId) throws SQLException;
}
