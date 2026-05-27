package com.vinplay.vbee.rmq.processor.crypto;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * RabbitMQ processor for crypto withdrawal events from the .NET USDT gateway.
 *
 * Handles two event types:
 * - withdrawal.completed: marks withdrawal as COMPLETED
 * - withdrawal.failed: marks as FAILED and refunds KRW balance
 *
 * Message format: UTF-8 JSON bytes (NOT Java serialized).
 * Command ID: 2002 (set via RabbitMQ message properties.messageId)
 */
public class CryptoWithdrawalEventProcessor implements BaseProcessor<byte[], Boolean> {

    private static final Logger logger = Logger.getLogger("vbee");
    private static final String POOL_NAME = "mysqlpoolname";

    @Override
    public Boolean execute(Param<byte[]> param) {
        String eventId = null;
        try {
            byte[] body = (byte[]) param.get();
            String jsonStr = new String(body, "UTF-8");
            logger.info("CryptoWithdrawalEventProcessor received: " + jsonStr);

            JSONObject json = new JSONObject(jsonStr);
            eventId = json.getString("eventId");
            String eventType = json.getString("eventType");
            String transactionId = json.getString("transactionId");
            String userId = json.getString("userId");
            double amount = json.getDouble("amount");
            String symbol = json.optString("symbol", "USDT");
            String timestamp = json.optString("timestamp", "");

            // Idempotency check
            if (isEventProcessed(eventId)) {
                logger.info("CryptoWithdrawalEventProcessor: event " + eventId + " already processed, skipping");
                return true;
            }

            if ("withdrawal.completed".equals(eventType)) {
                return handleCompleted(eventId, eventType, transactionId, userId, amount, json);
            } else if ("withdrawal.failed".equals(eventType)) {
                return handleFailed(eventId, eventType, transactionId, userId, amount, json);
            } else {
                logger.warn("CryptoWithdrawalEventProcessor: unknown eventType=" + eventType
                        + " eventId=" + eventId);
                // ACK to avoid infinite redelivery of unknown event types
                return true;
            }

        } catch (Exception e) {
            logger.error("CryptoWithdrawalEventProcessor: error processing eventId=" + eventId, e);
            return false;
        }
    }

    /**
     * Handle withdrawal.completed: update status to COMPLETED.
     */
    private Boolean handleCompleted(String eventId, String eventType, String transactionId,
                                     String userId, double amount, JSONObject json) {
        String txHash = json.optString("txHash", "");
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            conn.setAutoCommit(false);

            // Insert processed_events (idempotency)
            PreparedStatement psEvent = conn.prepareStatement(
                    "INSERT INTO processed_events (event_id, event_type) VALUES (?, ?)");
            psEvent.setString(1, eventId);
            psEvent.setString(2, eventType);
            try {
                psEvent.executeUpdate();
            } catch (java.sql.SQLIntegrityConstraintViolationException dupEx) {
                conn.rollback();
                logger.info("CryptoWithdrawalEventProcessor: concurrent duplicate for eventId=" + eventId);
                return true;
            } finally {
                psEvent.close();
            }

            // Update withdrawal status to COMPLETED
            PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE crypto_withdrawals SET status = 'COMPLETED', tx_hash = ?, processed_at = NOW() "
                    + "WHERE gateway_tx_id = ? AND status IN ('APPROVED', 'PENDING')");
            psUpdate.setString(1, txHash);
            psUpdate.setString(2, transactionId);
            int updated = psUpdate.executeUpdate();
            psUpdate.close();

            conn.commit();

            if (updated == 0) {
                logger.warn("CryptoWithdrawalEventProcessor: no rows updated for COMPLETED, gateway_tx_id="
                        + transactionId + " eventId=" + eventId
                        + " (may already be completed or not found)");
            } else {
                logger.info("CryptoWithdrawalEventProcessor: COMPLETED gateway_tx_id=" + transactionId
                        + " txHash=" + txHash + " eventId=" + eventId);
            }

            return true;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception rbEx) { logger.error("Rollback failed", rbEx); }
            }
            logger.error("CryptoWithdrawalEventProcessor: handleCompleted error eventId=" + eventId, e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                releaseConnection(conn);
            }
        }
    }

    /**
     * Handle withdrawal.failed: update status to FAILED, refund KRW balance, send Telegram alert.
     */
    private Boolean handleFailed(String eventId, String eventType, String transactionId,
                                  String userId, double amount, JSONObject json) {
        String reason = json.optString("reason", "Unknown");

        // Look up the withdrawal record to get nickname and amountKrw
        String nickName = null;
        long amountKrw = 0;
        long withdrawalId = 0;
        long telegramMsgId = 0;
        String txCode = null;
        BigDecimal amountUsdt = BigDecimal.ZERO;

        Connection lookupConn = null;
        try {
            lookupConn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            PreparedStatement ps = lookupConn.prepareStatement(
                    "SELECT id, nick_name, amount_krw, amount_usdt, tx_code, telegram_message_id "
                    + "FROM crypto_withdrawals WHERE gateway_tx_id = ?");
            ps.setString(1, transactionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                withdrawalId = rs.getLong("id");
                nickName = rs.getString("nick_name");
                amountKrw = rs.getLong("amount_krw");
                amountUsdt = rs.getBigDecimal("amount_usdt");
                txCode = rs.getString("tx_code");
                telegramMsgId = rs.getLong("telegram_message_id");
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            logger.error("CryptoWithdrawalEventProcessor: lookup failed for gateway_tx_id=" + transactionId, e);
            return false;
        } finally {
            releaseConnection(lookupConn);
        }

        if (nickName == null || nickName.isEmpty()) {
            // Fallback: resolve from userId
            nickName = resolveNickname(Long.parseLong(userId));
        }

        if (nickName == null || nickName.isEmpty()) {
            logger.error("CryptoWithdrawalEventProcessor: cannot resolve nickname for userId=" + userId
                    + " gateway_tx_id=" + transactionId + " eventId=" + eventId);
            return false;
        }

        // Update DB status in a transaction with idempotency
        Connection conn = null;
        boolean committed = false;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            conn.setAutoCommit(false);

            // Insert processed_events
            PreparedStatement psEvent = conn.prepareStatement(
                    "INSERT INTO processed_events (event_id, event_type) VALUES (?, ?)");
            psEvent.setString(1, eventId);
            psEvent.setString(2, eventType);
            try {
                psEvent.executeUpdate();
            } catch (java.sql.SQLIntegrityConstraintViolationException dupEx) {
                conn.rollback();
                logger.info("CryptoWithdrawalEventProcessor: concurrent duplicate for eventId=" + eventId);
                return true;
            } finally {
                psEvent.close();
            }

            // Update withdrawal status to FAILED
            PreparedStatement psUpdate = conn.prepareStatement(
                    "UPDATE crypto_withdrawals SET status = 'FAILED', fail_reason = ?, processed_at = NOW() "
                    + "WHERE gateway_tx_id = ?");
            psUpdate.setString(1, reason);
            psUpdate.setString(2, transactionId);
            int updated = psUpdate.executeUpdate();
            psUpdate.close();

            conn.commit();
            committed = true;

            if (updated == 0) {
                logger.warn("CryptoWithdrawalEventProcessor: no rows updated for FAILED, gateway_tx_id="
                        + transactionId + " eventId=" + eventId);
            }

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception rbEx) { logger.error("Rollback failed", rbEx); }
            }
            logger.error("CryptoWithdrawalEventProcessor: handleFailed DB error eventId=" + eventId, e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                releaseConnection(conn);
            }
        }

        // Refund KRW balance (outside DB transaction)
        if (committed && amountKrw > 0) {
            try {
                UserServiceImpl userService = new UserServiceImpl();
                BaseResponseModel refundResult = userService.updateMoneyFromAdmin(
                        nickName, amountKrw, "vin", "CryptoWithdrawRefund", "crypto_refund",
                        "Withdrawal failed: " + reason + " (tx:" + transactionId + ")");

                if (!refundResult.isSuccess()) {
                    logger.error("CryptoWithdrawalEventProcessor: REFUND FAILED for nickname=" + nickName
                            + " amountKrw=" + amountKrw + " eventId=" + eventId
                            + " errorCode=" + refundResult.getErrorCode());
                    // CRITICAL: refund failed — needs manual intervention
                    sendTelegramAlert("CRITICAL REFUND FAILED\n"
                            + "EventId: " + eventId + "\n"
                            + "User: " + nickName + " (id:" + userId + ")\n"
                            + "Amount: " + amountKrw + " KRW\n"
                            + "Gateway TX: " + transactionId + "\n"
                            + "Reason: " + reason);
                } else {
                    logger.info("CryptoWithdrawalEventProcessor: refund SUCCESS nickname=" + nickName
                            + " amountKrw=" + amountKrw + " eventId=" + eventId);
                }
            } catch (Exception refundEx) {
                logger.error("CryptoWithdrawalEventProcessor: REFUND EXCEPTION for eventId=" + eventId, refundEx);
                sendTelegramAlert("CRITICAL REFUND EXCEPTION\n"
                        + "EventId: " + eventId + "\n"
                        + "User: " + nickName + " (id:" + userId + ")\n"
                        + "Amount: " + amountKrw + " KRW\n"
                        + "Error: " + refundEx.getMessage());
            }
        }

        // Send Telegram notification about the failure
        sendTelegramAlert("Crypto Withdrawal FAILED\n"
                + "User: " + nickName + " (id:" + userId + ")\n"
                + "Amount: " + amountUsdt + " USDT / " + amountKrw + " KRW\n"
                + "Gateway TX: " + transactionId + "\n"
                + "Reason: " + reason + "\n"
                + (committed && amountKrw > 0 ? "Refund: issued" : "Refund: N/A"));

        // Update Telegram message if exists
        if (telegramMsgId > 0) {
            try {
                com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier notifier =
                        new com.vinplay.dal.crypto.TelegramCryptoWithdrawNotifier();
                if (notifier.isEnabled()) {
                    notifier.editMessageRejected(telegramMsgId, txCode, amountKrw, amountUsdt,
                            nickName, "N/A", "SYSTEM", reason);
                }
            } catch (Exception tgEx) {
                logger.warn("CryptoWithdrawalEventProcessor: Telegram edit failed for eventId=" + eventId, tgEx);
            }
        }

        return true;
    }

    /**
     * Check if an event has already been processed.
     */
    private boolean isEventProcessed(String eventId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM processed_events WHERE event_id = ?");
            ps.setString(1, eventId);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        } catch (Exception e) {
            logger.error("CryptoWithdrawalEventProcessor: idempotency check failed for eventId=" + eventId, e);
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Resolve nickname from userId via DB lookup.
     */
    private String resolveNickname(long userId) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT nick_name FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            String nickname = null;
            if (rs.next()) {
                nickname = rs.getString("nick_name");
            }
            rs.close();
            ps.close();
            return nickname;
        } catch (Exception e) {
            logger.error("CryptoWithdrawalEventProcessor: resolveNickname failed for userId=" + userId, e);
            return null;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Send a Telegram alert message for critical events (refund failures, withdrawal failures).
     * Uses the same bot token/chat as deposit notifications.
     */
    private void sendTelegramAlert(String message) {
        try {
            String token = System.getenv("TELEGRAM_DEPOSIT_BOT_TOKEN");
            String chatId = System.getenv("TELEGRAM_DEPOSIT_CHAT_ID");
            if (token == null || token.isEmpty() || chatId == null || chatId.isEmpty()) {
                logger.warn("CryptoWithdrawalEventProcessor: Telegram not configured, skipping alert");
                return;
            }

            String apiUrl = "https://api.telegram.org/bot" + token + "/sendMessage";
            String payload = "chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                    + "&text=" + URLEncoder.encode(message, "UTF-8")
                    + "&parse_mode=HTML";

            URL url = new URL(apiUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setConnectTimeout(5000);
            httpConn.setReadTimeout(5000);
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStream os = httpConn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = httpConn.getResponseCode();
            if (responseCode != 200) {
                logger.warn("CryptoWithdrawalEventProcessor: Telegram alert returned " + responseCode);
            }
            httpConn.disconnect();
        } catch (Exception e) {
            logger.warn("CryptoWithdrawalEventProcessor: Telegram alert failed", e);
        }
    }

    private void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                ConnectionPool.getInstance().releaseConnection(conn);
            } catch (Exception e) {
                logger.error("Error releasing connection", e);
            }
        }
    }
}
