package com.vinplay.dal.service;

import com.vinplay.dal.dao.DepositPromotionDao;
import com.vinplay.dal.dao.impl.DepositPromotionDaoImpl;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Deposit Promotion Engine.
 * Determines eligibility and calculates bonus for deposit transactions.
 *
 * Usage:
 *   DepositPromotionService promoService = new DepositPromotionService();
 *   DepositPromotionService.BonusResult result = promoService.evaluate(userId, nickName, depositTxId, depositAmount);
 *   if (result != null) {
 *       // Credit result.bonusAmount to user, then log
 *       promoService.recordClaim(result);
 *   }
 */
public class DepositPromotionService {

    private static final Logger logger = Logger.getLogger("api");

    /** Promo type constants */
    public static final int TYPE_FIRST_DEPOSIT = 1;
    public static final int TYPE_DAILY_DEPOSIT = 2;

    private final DepositPromotionDao promoDao;

    public DepositPromotionService() {
        this.promoDao = new DepositPromotionDaoImpl();
    }

    public DepositPromotionService(DepositPromotionDao promoDao) {
        this.promoDao = promoDao;
    }

    /**
     * Result object returned when a bonus is calculated.
     */
    public static class BonusResult {
        public final long promoId;
        public final int promoType;
        public final long userId;
        public final String nickName;
        public final long depositTxId;
        public final long depositAmount;
        public final long bonusAmount;
        public final double turnoverFactor;

        public BonusResult(long promoId, int promoType, long userId, String nickName,
                           long depositTxId, long depositAmount, long bonusAmount,
                           double turnoverFactor) {
            this.promoId = promoId;
            this.promoType = promoType;
            this.userId = userId;
            this.nickName = nickName;
            this.depositTxId = depositTxId;
            this.depositAmount = depositAmount;
            this.bonusAmount = bonusAmount;
            this.turnoverFactor = turnoverFactor;
        }

        @Override
        public String toString() {
            return "BonusResult{promoId=" + promoId + ", type=" + promoType +
                    ", userId=" + userId + ", deposit=" + depositAmount +
                    ", bonus=" + bonusAmount + "}";
        }
    }

    /**
     * Main entry point: evaluate a completed deposit and return bonus info if eligible.
     *
     * @param gate Mệnh giá cổng nạp: BANK, CRYPTO, etc.
     * @return BonusResult if user is eligible for promotion, null otherwise.
     */
    public BonusResult evaluate(long userId, String nickName, long depositTxId, long depositAmount, String gate) {
        try {
            // Step 1: Determine promotion type
            boolean hasFirstClaim = promoDao.hasFirstDepositClaim(userId);

            if (!hasFirstClaim) {
                BonusResult result = tryPromotion(TYPE_FIRST_DEPOSIT, gate, userId, nickName, depositTxId, depositAmount);
                if (result != null) {
                    return result;
                }
            }

            if (promoDao.hasFirstDepositClaimToday(userId)) {
                logger.info("DepositPromotion: userId=" + userId +
                        " received First Deposit today, skipping Daily Deposit");
                return null;
            }

            return tryPromotion(TYPE_DAILY_DEPOSIT, gate, userId, nickName, depositTxId, depositAmount);

        } catch (Exception e) {
            logger.error("DepositPromotionService.evaluate error userId=" + userId +
                    " txId=" + depositTxId, e);
            return null;
        }
    }

    /**
     * Try to apply a specific promotion type.
     */
    private BonusResult tryPromotion(int promoType, String gate, long userId, String nickName,
                                     long depositTxId, long depositAmount) throws Exception {
        // 1. Get ALL active promotion configs
        java.util.List<Map<String, Object>> promos = promoDao.getActivePromotions(promoType, gate);
        if (promos == null || promos.isEmpty()) {
            logger.debug("DepositPromotion: no active promo for type=" + promoType);
            return null;
        }

        for (Map<String, Object> promo : promos) {
            long promoId = ((Number) promo.get("id")).longValue();
            double bonusPercent = ((Number) promo.get("bonus_percent")).doubleValue();
            int maxUsers = ((Number) promo.get("max_users")).intValue();
            double turnoverFactor = ((Number) promo.get("turnover_factor")).doubleValue();
            long maxBonusPerTx = ((Number) promo.get("max_bonus_per_tx")).longValue();

            Integer maxClaimsDaily = promo.get("max_claims_daily") != null
                    ? ((Number) promo.get("max_claims_daily")).intValue() : null;
            Long maxBonusDaily = promo.get("max_bonus_daily") != null
                    ? ((Number) promo.get("max_bonus_daily")).longValue() : null;

            // 2. Check {B} max_users limit
            if (maxUsers > 0) {
                int distinctUsers = promoDao.countDistinctUsers(promoId);
                if (distinctUsers >= maxUsers) {
                    logger.info("DepositPromotion: promoId=" + promoId +
                            " max_users reached (" + distinctUsers + "/" + maxUsers + ") - skipping to next");
                    continue;
                }
            }

            // 3. Check {E} max_claims_daily (Daily promo only)
            if (maxClaimsDaily != null && promoType == TYPE_DAILY_DEPOSIT) {
                int claimsToday = promoDao.countUserClaimsToday(userId, promoId);
                if (claimsToday >= maxClaimsDaily) {
                    logger.info("DepositPromotion: userId=" + userId + " promoId=" + promoId +
                            " max_claims_daily reached (" + claimsToday + "/" + maxClaimsDaily + ") - skipping to next");
                    continue;
                }
            }

            // 4. Calculate bonus
            long bonus = Math.round(depositAmount * bonusPercent / 100.0);

            // Apply {D} cap per transaction
            if (bonus > maxBonusPerTx) {
                bonus = maxBonusPerTx;
            }

            // Apply {F} daily bonus cap (Daily promo only)
            if (maxBonusDaily != null && promoType == TYPE_DAILY_DEPOSIT) {
                long sumToday = promoDao.sumUserBonusToday(userId, promoId);
                long remaining = maxBonusDaily - sumToday;
                if (remaining <= 0) {
                    logger.info("DepositPromotion: userId=" + userId + " promoId=" + promoId +
                            " max_bonus_daily reached (sum=" + sumToday + "/" + maxBonusDaily + ") - skipping to next");
                    continue;
                }
                if (bonus > remaining) {
                    bonus = remaining;
                }
            }

            if (bonus <= 0) {
                continue;
            }

            return new BonusResult(promoId, promoType, userId, nickName,
                    depositTxId, depositAmount, bonus, turnoverFactor);
        }
        
        return null;
    }

    /**
     * Record a successful bonus claim into the log table.
     * Call this AFTER the bonus has been credited to the user's wallet.
     *
     * @return log ID, or -1 on failure
     */
    public long recordClaim(BonusResult result) {
        try {
            return promoDao.insertClaimLog(
                    result.promoId, result.promoType,
                    result.userId, result.nickName,
                    result.depositTxId, result.depositAmount,
                    result.bonusAmount);
        } catch (Exception e) {
            logger.error("DepositPromotionService.recordClaim error " + result, e);
            return -1;
        }
    }
}
