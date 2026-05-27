package com.vinplay.vbee.deposit;

import java.util.List;
import org.apache.log4j.Logger;

/**
 * Periodic recovery worker for deposit transactions.
 * Runs every 5 minutes via ScheduledExecutorService.
 *
 * Four sweeps:
 *   1. Expire old PENDING (30 min) — update Telegram message
 *   2. Re-notify orphaned PENDING (no telegram_msg_id, < 30 min old)
 *   3. Release stale PROCESSING (lock expired > 31 min) — update Telegram message
 *   4. Retry failed credits
 */
public class DepositRecoveryWorker implements Runnable {

    private static final Logger logger = Logger.getLogger("api");

    private static final int EXPIRE_MINUTES = 30;
    private static final int STALE_LOCK_MINUTES = 31;

    private final DepositTelegramBot bot;
    private final DepositTransactionDao dao;

    public DepositRecoveryWorker(DepositTelegramBot bot, DepositTransactionDao dao) {
        this.bot = bot;
        this.dao = dao;
    }

    @Override
    public void run() {
        logger.info("DepositRecoveryWorker: starting sweep");

        try {
            sweepExpirePending();
        } catch (Exception e) {
            logger.error("DepositRecoveryWorker: sweepExpirePending error", e);
        }

        try {
            sweepOrphanedPending();
        } catch (Exception e) {
            logger.error("DepositRecoveryWorker: sweepOrphanedPending error", e);
        }

        try {
            sweepStaleProcessing();
        } catch (Exception e) {
            logger.error("DepositRecoveryWorker: sweepStaleProcessing error", e);
        }

        try {
            sweepFailedCredits();
        } catch (Exception e) {
            logger.error("DepositRecoveryWorker: sweepFailedCredits error", e);
        }

        logger.info("DepositRecoveryWorker: sweep complete");
    }

    // -----------------------------------------------------------------------
    // Sweep 1: Expire old PENDING (30 min)
    // -----------------------------------------------------------------------

    private void sweepExpirePending() {
        List<DepositTransactionDao.DepositTx> expired = dao.expireOldTransactions(EXPIRE_MINUTES);

        for (DepositTransactionDao.DepositTx tx : expired) {
            if (tx.telegramMsgId > 0 && bot.isConfigured()) {
                try {
                    bot.editMessageExpired(tx.telegramMsgId, tx.id, tx.txCode);
                } catch (Exception e) {
                    logger.error("Failed to edit Telegram message for expired txId=" + tx.id, e);
                }
            }
        }

        if (!expired.isEmpty()) {
            logger.info("DepositRecoveryWorker: expired " + expired.size() + " PENDING transactions");
        }
    }

    // -----------------------------------------------------------------------
    // Sweep 2: Re-notify orphaned PENDING (no telegram_msg_id, < 30 min old)
    // -----------------------------------------------------------------------

    private void sweepOrphanedPending() {
        if (!bot.isConfigured()) return;

        List<DepositTransactionDao.DepositTx> orphaned = dao.getOrphanedPending(EXPIRE_MINUTES);

        for (DepositTransactionDao.DepositTx tx : orphaned) {
            try {
                long msgId = bot.sendDepositNotification(
                        tx.id, tx.txCode, tx.nickName, tx.amount, tx.bankName, tx.bankNumber);
                if (msgId > 0) {
                    dao.setTelegramMessageId(tx.id, msgId, bot.getChatId());
                    logger.info("DepositRecoveryWorker: re-notified orphaned txId=" + tx.id + " msgId=" + msgId);
                }
            } catch (Exception e) {
                logger.error("Failed to re-notify orphaned txId=" + tx.id, e);
            }
        }

        if (!orphaned.isEmpty()) {
            logger.info("DepositRecoveryWorker: re-notified " + orphaned.size() + " orphaned transactions");
        }
    }

    // -----------------------------------------------------------------------
    // Sweep 3: Release stale PROCESSING (lock expired > 31 min)
    // -----------------------------------------------------------------------

    private void sweepStaleProcessing() {
        List<DepositTransactionDao.DepositTx> stale = dao.releaseStaleProcessing(STALE_LOCK_MINUTES);

        for (DepositTransactionDao.DepositTx tx : stale) {
            if (tx.telegramMsgId > 0 && bot.isConfigured()) {
                try {
                    bot.editMessageReleased(tx.telegramMsgId, tx.id, tx.txCode,
                            tx.nickName, tx.amount, tx.bankName, tx.bankNumber);
                } catch (Exception e) {
                    logger.error("Failed to edit Telegram message for released txId=" + tx.id, e);
                }
            }
        }

        if (!stale.isEmpty()) {
            logger.info("DepositRecoveryWorker: released " + stale.size() + " stale PROCESSING transactions");
        }
    }

    // -----------------------------------------------------------------------
    // Sweep 4: Retry failed credits
    // -----------------------------------------------------------------------

    private void sweepFailedCredits() {
        List<DepositTransactionDao.DepositTx> failed = dao.getFailedCredits();

        int retried = 0;
        for (DepositTransactionDao.DepositTx tx : failed) {
            try {
                boolean credited = dao.creditUserBalance(tx.userId, tx.amount, tx.txCode);
                if (credited) {
                    dao.markCreditOk(tx.id);
                    retried++;
                    logger.info("DepositRecoveryWorker: retried credit for txId=" + tx.id + " OK");
                } else {
                    logger.warn("DepositRecoveryWorker: retry credit for txId=" + tx.id + " still failed");
                }
            } catch (Exception e) {
                logger.error("Failed to retry credit for txId=" + tx.id, e);
            }
        }

        if (!failed.isEmpty()) {
            logger.info("DepositRecoveryWorker: retried " + retried + "/" + failed.size() + " failed credits");
        }
    }
}
