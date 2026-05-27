package com.vinplay.dal.service;

import com.vinplay.dal.dao.SigningBonusDao;
import com.vinplay.dal.dao.impl.SigningBonusDaoImpl;
import com.vinplay.dal.withdraw.VolumeTrackingService;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Signing Bonus Service.
 * Evaluates eligibility and grants bonus for first-time app install + new account registration.
 *
 * Supports two payout modes:
 *   - Auto: bonus granted automatically on registration if eligible
 *   - Manual: admin manually triggers payout (for flagged/suspicious users)
 *
 * Anti-duplicate: INSERT IGNORE on UNIQUE keys prevents double payouts.
 */
public class SigningBonusService {

    private static final Logger logger = Logger.getLogger("api");

    private final SigningBonusDao dao;

    public SigningBonusService() {
        this.dao = new SigningBonusDaoImpl();
    }

    public SigningBonusService(SigningBonusDao dao) {
        this.dao = dao;
    }

    /**
     * Track a device install. Called when app is opened for the first time.
     *
     * @return true if this is a first-time install (new device), false if device was already registered
     */
    public boolean trackDeviceInstall(String deviceFingerprint, String platform,
                                      String appVersion, String ipAddress) {
        try {
            if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
                logger.warn("SigningBonus.trackDeviceInstall: empty device_fingerprint");
                return false;
            }
            boolean isNew = dao.registerDevice(deviceFingerprint.trim(), platform, appVersion, ipAddress);
            logger.info("SigningBonus.trackDeviceInstall: device=" + deviceFingerprint +
                    " platform=" + platform + " isNew=" + isNew);
            return isNew;
        } catch (Exception e) {
            logger.error("SigningBonus.trackDeviceInstall error device=" + deviceFingerprint, e);
            return false;
        }
    }

    /**
     * Evaluate and grant signing bonus after successful user registration (AUTO mode).
     *
     * Flow:
     * 1. Check feature is active (config exists with status=1)
     * 2. Check per-user config: if user is blocked (enabled=0) → skip; if payout_mode=manual → skip
     * 3. Check device was registered via track-install API (is first-time install)
     * 4. Check user hasn't already received bonus
     * 5. Check device hasn't already been used to claim bonus
     * 6. Link device to user
     * 7. Insert bonus log (INSERT IGNORE for race condition safety)
     * 8. Return bonus amount for caller to credit to user's wallet
     *
     * @return bonus amount if eligible, 0 if not eligible
     */
    public long evaluateAndGrant(long userId, String nickName, String deviceFingerprint) {
        try {
            if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
                logger.info("SigningBonus.evaluate: userId=" + userId + " no device_fp provided, skipping");
                return 0;
            }

            String fp = deviceFingerprint.trim();

            // 1. Check config active
            Map<String, Object> config = dao.getActiveConfig();
            if (config == null) {
                logger.info("SigningBonus.evaluate: feature is OFF (no active config)");
                return 0;
            }

            int configId = ((Number) config.get("id")).intValue();
            long bonusAmount = ((Number) config.get("bonus_amount")).longValue();

            if (bonusAmount <= 0) {
                logger.info("SigningBonus.evaluate: bonus_amount is 0, skipping");
                return 0;
            }

            // 2. Check per-user config (if exists)
            try {
                Map<String, Object> userConfig = dao.getUserConfig(userId);
                if (userConfig != null) {
                    int enabled = ((Number) userConfig.get("enabled")).intValue();
                    if (enabled == 0) {
                        logger.info("SigningBonus.evaluate: userId=" + userId +
                                " signing bonus DISABLED by admin");
                        return 0;
                    }
                    String payoutMode = (String) userConfig.get("payout_mode");
                    if ("manual".equals(payoutMode)) {
                        logger.info("SigningBonus.evaluate: userId=" + userId +
                                " set to MANUAL payout mode, skipping auto payout");
                        return 0;
                    }
                }
            } catch (Exception e) {
                logger.warn("SigningBonus.evaluate: error checking user config for userId=" + userId +
                        ", proceeding with auto", e);
            }

            // 3. Check device was registered (must have called track-install before)
            if (!dao.isDeviceRegistered(fp)) {
                logger.info("SigningBonus.evaluate: userId=" + userId +
                        " device=" + fp + " NOT registered (not first-time install or track-install not called)");
                return 0;
            }

            // 4. Check user hasn't already received bonus
            if (dao.hasUserReceivedBonus(userId)) {
                logger.info("SigningBonus.evaluate: userId=" + userId + " already received bonus");
                return 0;
            }

            // 5. Check device hasn't already claimed bonus (another user on same device)
            if (dao.hasDeviceReceivedBonus(fp)) {
                logger.info("SigningBonus.evaluate: device=" + fp +
                        " already used for bonus claim by another user");
                return 0;
            }

            // 6. Link device to user
            dao.linkDeviceToUser(fp, userId, nickName);

            // 7. Insert bonus log (INSERT IGNORE handles race condition)
            long logId = dao.insertBonusLog(userId, nickName, fp, bonusAmount, configId);
            if (logId <= 0) {
                logger.warn("SigningBonus.evaluate: userId=" + userId +
                        " insertBonusLog failed (possible race condition duplicate)");
                return 0;
            }

            logger.info("SigningBonus.evaluate: SUCCESS userId=" + userId +
                    " nickName=" + nickName + " device=" + fp +
                    " bonusAmount=" + bonusAmount + " logId=" + logId);

            // 8. Return bonus amount — caller is responsible for crediting user's wallet
            return bonusAmount;

        } catch (Exception e) {
            logger.error("SigningBonus.evaluate error userId=" + userId +
                    " device=" + deviceFingerprint, e);
            return 0;
        }
    }

    /**
     * Manual payout signing bonus by admin.
     * Used when user has suspicious flag but admin confirms they are legitimate.
     *
     * Flow:
     * 1. Check user hasn't already received bonus (anti-duplicate)
     * 2. Insert bonus log with payout_mode='manual'
     * 3. Return log ID for caller to credit wallet
     *
     * @param userId user ID
     * @param nickName user's nickname
     * @param bonusAmount custom bonus amount (admin can change)
     * @param adminUser admin who performed the payout
     * @param reason reason for manual payout
     * @return log ID if successful, -1 if duplicate or failed
     */
    public long manualPayout(long userId, String nickName, long bonusAmount,
                             String adminUser, String reason) {
        try {
            // 1. Check user hasn't already received bonus
            if (dao.hasUserReceivedBonus(userId)) {
                logger.warn("SigningBonus.manualPayout: userId=" + userId +
                        " already received bonus, DUPLICATE prevented");
                return -1;
            }

            // 2. Insert bonus log (INSERT IGNORE for race condition safety)
            // config_id = 0 for manual payout (not linked to any auto config)
            long logId = dao.insertBonusLogManual(userId, nickName, "manual_" + userId,
                    bonusAmount, 0, adminUser, reason);

            if (logId <= 0) {
                logger.warn("SigningBonus.manualPayout: userId=" + userId +
                        " insertBonusLogManual failed (possible duplicate)");
                return -1;
            }

            logger.info("SigningBonus.manualPayout: SUCCESS userId=" + userId +
                    " nickName=" + nickName + " bonusAmount=" + bonusAmount +
                    " by=" + adminUser + " logId=" + logId);

            return logId;

        } catch (Exception e) {
            logger.error("SigningBonus.manualPayout error userId=" + userId +
                    " admin=" + adminUser, e);
            return -1;
        }
    }

    /**
     * Calculate required wagering volume from bonus amount using active signing bonus config.
     * Formula: required_volume = bonus_amount * wager_multiplier
     */
    public long calculateWageringRequiredVolume(long bonusAmount) {
        if (bonusAmount <= 0) {
            return 0;
        }
        try {
            int wagerEnabled = 1;
            BigDecimal multiplier = new BigDecimal("3");

            Map<String, Object> config = dao.getActiveConfig();
            if (config != null) {
                Object enabledObj = config.get("wager_enabled");
                if (enabledObj != null) {
                    wagerEnabled = Integer.parseInt(String.valueOf(enabledObj));
                }
                Object multiplierObj = config.get("wager_multiplier");
                if (multiplierObj != null) {
                    multiplier = new BigDecimal(String.valueOf(multiplierObj));
                }
            }

            if (wagerEnabled != 1) {
                return 0;
            }
            if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
                return 0;
            }

            return BigDecimal.valueOf(bonusAmount)
                    .multiply(multiplier)
                    .setScale(0, RoundingMode.DOWN)
                    .longValue();
        } catch (Exception e) {
            logger.warn("SigningBonus.calculateWageringRequiredVolume failed, skip wagering", e);
            return 0;
        }
    }

    /**
     * Apply wagering requirement to withdrawal volume tracking after bonus is credited.
     * Returns the required volume added.
     */
    public long applyWageringRequirement(int userId, String nickName, long bonusAmount) {
        long requiredVolume = calculateWageringRequiredVolume(bonusAmount);
        if (requiredVolume <= 0) {
            return 0;
        }
        try {
            VolumeTrackingService.addRequiredVolumeDirect(userId, nickName, requiredVolume);
            VolumeTrackingService.updateWithdrawalStatus(userId);
            logger.info("SigningBonus.applyWageringRequirement userId=" + userId +
                    " nickName=" + nickName + " bonusAmount=" + bonusAmount +
                    " requiredVolumeAdded=" + requiredVolume);
        } catch (Exception e) {
            logger.warn("SigningBonus.applyWageringRequirement failed userId=" + userId, e);
        }
        return requiredVolume;
    }
}
