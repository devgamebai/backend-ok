package com.vinplay.vbee.deposit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * Long-polling thread that receives Telegram inline button callbacks.
 *
 * Parses callback_data in the format "action:txId" and dispatches to
 * the appropriate handler (pick, approve, reject, release).
 *
 * Uses Telegram getUpdates with timeout=30 (long-poll, NOT webhooks).
 */
public class DepositTelegramPoller implements Runnable {

    private static final Logger logger = Logger.getLogger("api");
    private static final String API_BASE = "https://api.telegram.org/bot";
    private static final int POLL_TIMEOUT = 30; // seconds
    private static final int ERROR_BACKOFF_MS = 5000;

    private final DepositTelegramBot bot;
    private final DepositTransactionDao dao;
    private volatile boolean running = true;
    private long lastUpdateId = 0;

    public DepositTelegramPoller(DepositTelegramBot bot, DepositTransactionDao dao) {
        this.bot = bot;
        this.dao = dao;
    }

    @Override
    public void run() {
        logger.info("DepositTelegramPoller started");

        while (running) {
            try {
                String response = pollUpdates();
                if (response == null) {
                    Thread.sleep(ERROR_BACKOFF_MS);
                    continue;
                }
                processUpdates(response);
            } catch (InterruptedException e) {
                logger.info("DepositTelegramPoller interrupted, stopping");
                running = false;
            } catch (Exception e) {
                logger.error("DepositTelegramPoller error", e);
                try { Thread.sleep(ERROR_BACKOFF_MS); } catch (InterruptedException ie) {
                    running = false;
                }
            }
        }

        logger.info("DepositTelegramPoller stopped");
    }

    public void stop() {
        running = false;
    }

    // -----------------------------------------------------------------------
    // Long-polling
    // -----------------------------------------------------------------------

    /**
     * GET getUpdates with long polling.
     */
    private String pollUpdates() {
        HttpURLConnection conn = null;
        try {
            String token = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
            if (token == null || token.isEmpty()) return null;

            String urlStr = API_BASE + token + "/getUpdates?offset="
                    + (lastUpdateId + 1) + "&timeout=" + POLL_TIMEOUT
                    + "&allowed_updates=[\"callback_query\"]";

            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            // Read timeout must be longer than the long-poll timeout
            conn.setReadTimeout((POLL_TIMEOUT + 10) * 1000);

            int code = conn.getResponseCode();
            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                logger.error("Telegram getUpdates HTTP " + code + ": " + sb.toString());
                return null;
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (java.net.SocketTimeoutException e) {
            // Normal for long polling — no updates available
            return "{\"ok\":true,\"result\":[]}";
        } catch (Exception e) {
            logger.error("Telegram getUpdates error: " + e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // -----------------------------------------------------------------------
    // Update processing — minimal JSON parsing (no library)
    // -----------------------------------------------------------------------

    /**
     * Process the getUpdates response.
     */
    private void processUpdates(String response) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(response);
            if (!json.optBoolean("ok", false)) return;
            org.json.JSONArray result = json.optJSONArray("result");
            if (result == null) return;

            for (int i = 0; i < result.length(); i++) {
                org.json.JSONObject update = result.optJSONObject(i);
                if (update == null) continue;

                long updateId = update.optLong("update_id", 0);
                if (updateId > lastUpdateId) {
                    lastUpdateId = updateId;
                }

                org.json.JSONObject cbQuery = update.optJSONObject("callback_query");
                if (cbQuery != null) {
                    try {
                        handleCallbackQuery(cbQuery);
                    } catch (Exception e) {
                        logger.error("Error handling callback query", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Telegram updates JSON", e);
        }
    }

    /**
     * Handle a single callback_query JSON block.
     */
    // SUN-1xxx (2026-05-11): package-accessible so TelegramWebhookServer can
    // dispatch the same callback handling logic when Telegram delivers via
    // webhook instead of long-poll. Original visibility was `private`.
    void handleCallbackQuery(org.json.JSONObject cbQuery) {
        String callbackId = cbQuery.optString("id");
        String data = cbQuery.optString("data");

        org.json.JSONObject message = cbQuery.optJSONObject("message");
        long messageId = message != null ? message.optLong("message_id", 0) : 0;

        // Extract operator name from callback_query.from
        org.json.JSONObject from = cbQuery.optJSONObject("from");
        String operatorName = "unknown";
        if (from != null) {
            operatorName = from.optString("username", "");
            if (operatorName.isEmpty()) {
                operatorName = from.optString("first_name", "unknown");
            }
        }

        if (data == null || data.isEmpty() || callbackId == null || callbackId.isEmpty()) {
            logger.warn("Callback query missing data or id");
            return;
        }

        // Parse callback_data: "action:txId"
        int colonIdx = data.indexOf(':');
        if (colonIdx < 0) {
            bot.answerCallback(callbackId, "Invalid callback data");
            return;
        }

        String action = data.substring(0, colonIdx);
        long txId;
        try {
            txId = Long.parseLong(data.substring(colonIdx + 1));
        } catch (NumberFormatException e) {
            bot.answerCallback(callbackId, "Invalid transaction ID");
            return;
        }

        logger.info("Telegram callback: action=" + action + " txId=" + txId
                + " operator=" + operatorName + " messageId=" + messageId);

        switch (action) {
            case "pick":
                handlePick(callbackId, txId, messageId, operatorName);
                break;
            case "approve":
                handleApprove(callbackId, txId, messageId, operatorName);
                break;
            case "reject":
                handleReject(callbackId, txId, messageId, operatorName);
                break;
            case "release":
                handleRelease(callbackId, txId, messageId, operatorName);
                break;
            case "crypto_approve":
                handleCryptoApprove(callbackId, txId, messageId, operatorName);
                break;
            case "crypto_reject":
                handleCryptoReject(callbackId, txId, messageId, operatorName);
                break;
            // SUN-1220 — bank withdraw approve/reject from Telegram bot.
            // Mirrors the crypto pair: button payload is
            // "bank_approve:<dbId>" / "bank_reject:<dbId>", set by
            // TelegramBankWithdrawNotifier.sendNewWithdrawal.
            case "bank_approve":
                handleBankApprove(callbackId, txId, messageId, operatorName);
                break;
            case "bank_reject":
                handleBankReject(callbackId, txId, messageId, operatorName);
                break;
            case "force_approve":
                handleForceApproveRequest(callbackId, txId, messageId, operatorName);
                break;
            case "force_approve_confirm":
                handleForceApproveConfirm(callbackId, txId, messageId, operatorName);
                break;
            case "force_approve_close":
                handleForceApproveClose(callbackId, txId, messageId, operatorName);
                break;
            default:
                bot.answerCallback(callbackId, "Unknown action: " + action);
        }
    }

    // -----------------------------------------------------------------------
    // Action handlers
    // -----------------------------------------------------------------------

    private void handlePick(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Processing...");

        try {
            // Try to pick the transaction in the DB (atomic)
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            if (tx == null) {
                bot.answerCallback(callbackId, "Transaction not found");
                return;
            }

            if (!"PENDING".equals(tx.status)) {
                bot.answerCallback(callbackId, "Transaction is " + tx.status + ", cannot pick");
                return;
            }

            boolean picked = dao.pickTransaction(txId, operatorName, "TELEGRAM");
            if (!picked) {
                bot.answerCallback(callbackId, "Transaction already being processed");
                return;
            }

            // Refresh to get updated data
            tx = dao.getTransaction(txId);
            bot.editMessagePicked(messageId, txId, tx.txCode, tx.nickName, tx.amount, operatorName);
            logger.info("Deposit " + txId + " picked by " + operatorName + " via Telegram");

        } catch (Exception e) {
            logger.error("handlePick error for txId=" + txId, e);
        }
    }

    private void handleApprove(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Approving...");

        // SUN-1xxx (2026-05-11): Dat2lit tx 483 incident. The original
        // catch block here just logged on exception, so when approve()
        // partially completed (money credited but post-credit DB writes
        // failed) and then threw, the Telegram message stayed at
        // "Đang xử lý" forever. Operator saw no resolution; player got
        // the money but admin state was inconsistent. Fix: re-read DB
        // state after approve() and edit the message to a definitive
        // final state regardless of how the body ended.
        com.vinplay.dal.deposit.DepositApprovalService.Result result = null;
        Throwable approveThrew = null;
        try {
            result = com.vinplay.dal.deposit.DepositApprovalService.approve(
                    txId, operatorName,
                    com.vinplay.dal.deposit.DepositApprovalService.Channel.TELEGRAM);
        } catch (Throwable t) {
            approveThrew = t;
            logger.error("handleApprove DepositApprovalService.approve threw txId=" + txId, t);
        }

        // Surface the error toast for the operator if approve clearly
        // rejected (not-found, terminal status, locked by other, etc.).
        if (result != null && !result.success && approveThrew == null) {
            String toast;
            if ("TX_NOT_FOUND".equals(result.errorCode))         toast = "Transaction not found";
            else if ("TERMINAL_STATUS".equals(result.errorCode)) toast = "Cannot approve a " + (result.row != null ? result.row.status : "?") + " tx";
            else if ("LOCKED_BY_OTHER".equals(result.errorCode)) toast = "Locked by " + (result.row != null ? result.row.operatorName : "another op") + ", not you";
            else if ("CANNOT_PICK".equals(result.errorCode))     toast = "Cannot pick — locked by another admin";
            else if ("CANNOT_APPROVE".equals(result.errorCode))  toast = "Failed to approve transaction";
            else                                                  toast = "Error: " + result.errorMessage;
            bot.answerCallback(callbackId, toast);
            return;
        }

        // Reconcile the Telegram message against the live DB state, not
        // against the approve() result. If approve threw between the
        // money credit and the post-credit DB writes, the row may be
        // status=APPROVED + credit_status=PENDING_CREDIT — but the player
        // already has the money. The operator needs the message edited
        // either way, with content that matches reality.
        com.vinplay.dal.deposit.DepositApprovalService.TransactionRow finalRow = null;
        try {
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            if (tx != null) {
                finalRow = new com.vinplay.dal.deposit.DepositApprovalService.TransactionRow();
                finalRow.txCode = tx.txCode;
                finalRow.nickName = tx.nickName;
                finalRow.amount = tx.amount;
                finalRow.bankName = tx.bankName;
                finalRow.bankNumber = tx.bankNumber;
                finalRow.userId = tx.userId;
                finalRow.status = tx.status;
            }
        } catch (Exception readErr) {
            logger.error("handleApprove final DB re-read failed txId=" + txId, readErr);
        }

        try {
            if (finalRow != null && "APPROVED".equals(finalRow.status)) {
                String holderName = lookupHolderName((int) finalRow.userId, finalRow.bankNumber);
                bot.editMessageApproved(messageId, finalRow.txCode, finalRow.amount, operatorName,
                        finalRow.nickName, finalRow.bankName, finalRow.bankNumber, holderName);
                logger.info("Deposit " + txId + " approved by " + operatorName
                        + " via Telegram"
                        + (result != null ? ", credited=" + result.credited : ", approve threw but row is APPROVED in DB"));
            } else if (approveThrew != null) {
                bot.answerCallback(callbackId,
                        "Approve failed (" + approveThrew.getClass().getSimpleName() + ") — retry or use admin panel");
            }
        } catch (Throwable editErr) {
            logger.error("handleApprove editMessageApproved failed txId=" + txId
                    + " (DB row finalized but Telegram message edit threw)", editErr);
        }
    }

    private void handleReject(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Rejecting...");

        try {
            String reason = "Rejected via Telegram";
            com.vinplay.dal.deposit.DepositApprovalService.Result result =
                    com.vinplay.dal.deposit.DepositApprovalService.reject(
                            txId, operatorName, reason,
                            com.vinplay.dal.deposit.DepositApprovalService.Channel.TELEGRAM);

            if (!result.success) {
                String toast;
                if ("TX_NOT_FOUND".equals(result.errorCode))         toast = "Transaction not found";
                else if ("TERMINAL_STATUS".equals(result.errorCode)) toast = "Cannot reject a " + (result.row != null ? result.row.status : "?") + " tx";
                else if ("LOCKED_BY_OTHER".equals(result.errorCode)) toast = "Locked by " + (result.row != null ? result.row.operatorName : "another op") + ", not you";
                else if ("CANNOT_PICK".equals(result.errorCode))     toast = "Cannot pick — locked by another admin";
                else if ("CANNOT_REJECT".equals(result.errorCode))   toast = "Failed to reject transaction";
                else                                                  toast = "Error: " + result.errorMessage;
                bot.answerCallback(callbackId, toast);
                return;
            }

            com.vinplay.dal.deposit.DepositApprovalService.TransactionRow row = result.row;
            bot.editMessageRejected(messageId, row.txCode, row.amount, operatorName, reason);
            logger.info("Deposit " + txId + " rejected by " + operatorName + " via Telegram");

        } catch (Exception e) {
            logger.error("handleReject error for txId=" + txId, e);
        }
    }

    private void handleRelease(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Releasing...");

        try {
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            if (tx == null) {
                bot.answerCallback(callbackId, "Transaction not found");
                return;
            }

            if (!"PROCESSING".equals(tx.status)) {
                bot.answerCallback(callbackId, "Transaction is " + tx.status + ", cannot release");
                return;
            }

            // Verify this operator holds the lock
            if (tx.operatorName != null && !tx.operatorName.equals(operatorName)) {
                bot.answerCallback(callbackId, "Locked by " + tx.operatorName + ", not you");
                return;
            }

            boolean released = dao.releaseTransaction(txId);
            if (!released) {
                bot.answerCallback(callbackId, "Failed to release transaction");
                return;
            }

            bot.editMessageReleased(messageId, txId, tx.txCode, tx.nickName, tx.amount,
                    tx.bankName, tx.bankNumber);
            logger.info("Deposit " + txId + " released by " + operatorName + " via Telegram");

        } catch (Exception e) {
            logger.error("handleRelease error for txId=" + txId, e);
        }
    }

    // -----------------------------------------------------------------------
    // Force Approve handlers
    // -----------------------------------------------------------------------

    /**
     * Step 1: Operator clicks "Force Approve" on expired message.
     * Shows confirmation dialog with [Confirm] [Close] buttons.
     */
    private void handleForceApproveRequest(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "X\u00e1c nh\u1eadn Force Approve...");

        try {
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            if (tx == null) {
                bot.answerCallback(callbackId, "Transaction not found");
                return;
            }

            if (!"EXPIRED".equals(tx.status) && !"REJECTED".equals(tx.status)) {
                bot.answerCallback(callbackId, "Only expired/rejected transactions can be force approved");
                return;
            }

            bot.editMessageForceApproveConfirm(messageId, txId, tx.txCode, tx.nickName, tx.amount);
            logger.info("Force approve confirmation shown for txId=" + txId + " by " + operatorName);

        } catch (Exception e) {
            logger.error("handleForceApproveRequest error for txId=" + txId, e);
        }
    }

    /**
     * Step 2: Operator clicks "Confirm" — execute force approve.
     * Updates status, credits balance, writes audit log.
     */
    private void handleForceApproveConfirm(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Force approving...");

        try {
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            if (tx == null) {
                bot.answerCallback(callbackId, "Transaction not found");
                return;
            }

            if (!"EXPIRED".equals(tx.status) && !"REJECTED".equals(tx.status)) {
                bot.answerCallback(callbackId, "Transaction is " + tx.status + ", cannot force approve");
                return;
            }

            boolean approved = dao.forceApproveTransaction(txId, operatorName, tx.amount);
            if (!approved) {
                bot.answerCallback(callbackId, "Failed to force approve — may have been processed already");
                return;
            }

            // Credit balance via MoneyGateway (handles promo, push, recharge_money, audit)
            boolean credited = false;
            try {
                com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                    com.vinplay.dal.service.MoneyGateway.creditUser(
                        tx.userId, tx.nickName, tx.amount, "DEPOSIT_TELEGRAM",
                        String.valueOf(txId), "Telegram force-approve " + tx.txCode);
                credited = cr.success;
            } catch (Exception creditErr) {
                logger.error("Force approve credit exception txId=" + txId, creditErr);
            }
            if (!credited) {
                // Fallback to stored procedure
                credited = dao.creditUserBalance(tx.userId, tx.amount, tx.txCode);
            }
            if (credited) {
                dao.markCreditOk(txId);
                // Push real-time balance update to connected client + portal
                // WS via the canonical MoneyGateway helper. Previously only
                // published to queue_action_minigame, leaving the portal
                // top-bar wallet stale; helper now fires both queues.
                com.vinplay.dal.service.MoneyGateway.publishBalanceUpdate(tx.nickName);
            } else {
                dao.markCreditFailed(txId);
                logger.error("Force approve credit failed txId=" + txId + " userId=" + tx.userId);
            }

            // Audit log
            try {
                dao.insertAuditLog(txId, tx.txCode, "FORCE_APPROVED", operatorName,
                        "TELEGRAM", "Force approved via Telegram by " + operatorName
                                + ", amount=" + tx.amount + ", previous_status=" + tx.status);
            } catch (Exception auditErr) {
                logger.error("Force approve audit log error txId=" + txId, auditErr);
            }

            String holderName = lookupHolderName(tx.userId, tx.bankNumber);
            bot.editMessageForceApproved(messageId, tx.txCode, tx.amount, operatorName,
                    tx.nickName, tx.bankName, tx.bankNumber, holderName);
            logger.info("Deposit " + txId + " force approved by " + operatorName
                    + " via Telegram, credited=" + credited);

        } catch (Exception e) {
            logger.error("handleForceApproveConfirm error for txId=" + txId, e);
        }
    }

    /**
     * Step 3: Operator clicks "Close" — cancel confirmation, restore Force Approve button.
     */
    private void handleForceApproveClose(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Closed");

        try {
            DepositTransactionDao.DepositTx tx = dao.getTransaction(txId);
            String txCode = (tx != null) ? tx.txCode : "N/A";

            // Restore the expired message with [Force Approve] button (not no-buttons)
            bot.editMessageExpired(messageId, txId, txCode);
            logger.info("Force approve cancelled for txId=" + txId + " by " + operatorName);

        } catch (Exception e) {
            logger.error("handleForceApproveClose error for txId=" + txId, e);
        }
    }

    // -----------------------------------------------------------------------
    // Crypto withdrawal handlers
    // -----------------------------------------------------------------------

    private void handleCryptoApprove(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Approving crypto withdrawal...");
        try {
            // Delegate the data work to the shared service so this stays
            // bug-for-bug aligned with c=9631. The bot's only remaining
            // responsibilities here are the answerCallback toast and the
            // Telegram message-edit (it owns the messageId).
            com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.Result result =
                    com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.approve(txId, operatorName);

            if (!result.success) {
                // SUN-1171 follow-up (QC): hot-wallet-low isn't a real
                // failure — ops just needs to top up. Toast briefly,
                // post a separate alert message in the chat, and leave
                // the original pending message + buttons untouched so
                // a re-click after top-up works without reissuing the
                // withdrawal.
                if ("HOT_WALLET_LOW".equals(result.errorCode) && result.row != null) {
                    bot.answerCallback(callbackId, "Hot wallet thiếu USDT — xem cảnh báo trong chat");
                    try {
                        new com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier()
                                .sendHotWalletLowAlert(
                                        result.row.txCode,
                                        result.requiredAmount,
                                        result.hotWalletBalance,
                                        result.hotWalletAddress,
                                        operatorName);
                    } catch (Exception alertErr) {
                        logger.warn("handleCryptoApprove hot-wallet-low alert failed txId="
                                + txId, alertErr);
                    }
                    return;
                }

                String toast;
                if ("WITHDRAWAL_NOT_FOUND".equals(result.errorCode))      toast = "Withdrawal not found";
                else if ("NOT_PENDING".equals(result.errorCode))          toast = "Already " + (result.row != null ? result.row.status : "processed");
                else if ("GATEWAY_ERROR".equals(result.errorCode))        toast = result.errorMessage;
                else                                                       toast = "Error: " + result.errorMessage;
                bot.answerCallback(callbackId, toast);
                return;
            }

            com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.WithdrawalRow row = result.row;
            com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier notifier =
                    new com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier();
            notifier.editMessageApproved(messageId, row.txCode, row.amountKrw, row.amountUsdt,
                    row.nickName, row.toAddress, operatorName);

            logger.info("Crypto withdrawal " + txId + " approved by " + operatorName + " via Telegram");
        } catch (Exception e) {
            logger.error("handleCryptoApprove error txId=" + txId, e);
            bot.answerCallback(callbackId, "Error: " + e.getMessage());
        }
    }

    private void handleCryptoReject(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Rejecting crypto withdrawal...");
        try {
            String reason = "Rejected via Telegram by " + operatorName;

            com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.Result result =
                    com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.reject(txId, operatorName, reason);

            if (!result.success) {
                String toast;
                if ("WITHDRAWAL_NOT_FOUND".equals(result.errorCode))      toast = "Withdrawal not found";
                else if ("NOT_PENDING".equals(result.errorCode))          toast = "Already " + (result.row != null ? result.row.status : "processed");
                else                                                       toast = "Error: " + result.errorMessage;
                bot.answerCallback(callbackId, toast);
                return;
            }

            com.vinplay.dal.crypto.CryptoWithdrawalApprovalService.WithdrawalRow row = result.row;
            com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier notifier =
                    new com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier();
            notifier.editMessageRejected(messageId, row.txCode, row.amountKrw, row.amountUsdt,
                    row.nickName, row.toAddress, operatorName, reason);

            logger.info("Crypto withdrawal " + txId + " rejected by " + operatorName + " via Telegram");
        } catch (Exception e) {
            logger.error("handleCryptoReject error txId=" + txId, e);
            bot.answerCallback(callbackId, "Error: " + e.getMessage());
        }
    }

    private void handleBankApprove(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Approving bank withdrawal...");
        try {
            com.vinplay.dal.withdraw.BankWithdrawalApprovalService.Result result =
                    com.vinplay.dal.withdraw.BankWithdrawalApprovalService.approve(txId, operatorName);

            if (!result.success) {
                String toast;
                if ("WITHDRAWAL_NOT_FOUND".equals(result.errorCode))      toast = "Withdrawal not found";
                else if ("NOT_PENDING".equals(result.errorCode))           toast = "Already " + (result.row != null ? result.row.status : "processed");
                else                                                       toast = "Error: " + result.errorMessage;
                bot.answerCallback(callbackId, toast);
                return;
            }

            com.vinplay.dal.withdraw.BankWithdrawalApprovalService.WithdrawalRow row = result.row;
            new com.vinplay.dal.withdraw.TelegramBankWithdrawNotifier()
                    .editMessageApproved(messageId, row.txCode, row.amountKrw,
                            row.nickName, row.bankName, row.bankNumber,
                            row.customerName, operatorName);
            logger.info("Bank withdrawal " + txId + " approved by " + operatorName + " via Telegram");
        } catch (Exception e) {
            logger.error("handleBankApprove error txId=" + txId, e);
            bot.answerCallback(callbackId, "Error: " + e.getMessage());
        }
    }

    private void handleBankReject(String callbackId, long txId, long messageId, String operatorName) {
        bot.answerCallback(callbackId, "Rejecting bank withdrawal...");
        try {
            String reason = "Rejected via Telegram by " + operatorName;
            com.vinplay.dal.withdraw.BankWithdrawalApprovalService.Result result =
                    com.vinplay.dal.withdraw.BankWithdrawalApprovalService.reject(txId, operatorName, reason);

            if (!result.success) {
                String toast;
                if ("WITHDRAWAL_NOT_FOUND".equals(result.errorCode))      toast = "Withdrawal not found";
                else if ("NOT_PENDING".equals(result.errorCode))           toast = "Already " + (result.row != null ? result.row.status : "processed");
                else                                                       toast = "Error: " + result.errorMessage;
                bot.answerCallback(callbackId, toast);
                return;
            }

            com.vinplay.dal.withdraw.BankWithdrawalApprovalService.WithdrawalRow row = result.row;
            new com.vinplay.dal.withdraw.TelegramBankWithdrawNotifier()
                    .editMessageRejected(messageId, row.txCode, row.amountKrw,
                            row.nickName, row.bankName, row.bankNumber,
                            row.customerName, operatorName, reason);
            logger.info("Bank withdrawal " + txId + " rejected by " + operatorName + " via Telegram");
        } catch (Exception e) {
            logger.error("handleBankReject error txId=" + txId, e);
            bot.answerCallback(callbackId, "Error: " + e.getMessage());
        }
    }

    /**
     * Look up the player's bank account holder name (users_bank.customer_name)
     * for the bank number recorded on the deposit. Returns null on miss or
     * any error — Telegram edit helpers print "-" for null.
     */
    private String lookupHolderName(int userId, String bankNumber) {
        if (userId <= 0 || bankNumber == null || bankNumber.isEmpty()) return null;
        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT customer_name FROM vinplay.users_bank "
                     + "WHERE user_id = ? AND bank_number = ? LIMIT 1")) {
            ps.setInt(1, userId);
            ps.setString(2, bankNumber);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            logger.warn("lookupHolderName failed userId=" + userId + " bankNumber=" + bankNumber + ": " + e.getMessage());
        }
        return null;
    }
}
