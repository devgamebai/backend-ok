package com.vinplay.vbee.rmq.processor.crypto;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * RabbitMQ processor for crypto deposit confirmed events from the .NET USDT gateway.
 *
 * Message format: UTF-8 JSON bytes (NOT Java serialized).
 * Command ID: 2001 (set via RabbitMQ message properties.messageId)
 *
 * Flow:
 * 1. Parse JSON from raw bytes
 * 2. Idempotency check via processed_events table
 * 3. Resolve nickname from userId
 * 4. Convert USDT to KRW (Upbit API with fallback rate)
 * 5. Insert crypto_deposits record
 * 6. Credit KRW balance via UserServiceImpl
 * 7. Record processed event
 */
public class CryptoDepositEventProcessor implements BaseProcessor<byte[], Boolean> {

    private static final Logger logger = Logger.getLogger("vbee");
    private static final String POOL_NAME = "mysqlpoolname";
    private static final long FALLBACK_KRW_PER_USDT = 1350L;
    private static final int UPBIT_TIMEOUT_MS = 3000;

    @Override
    public Boolean execute(Param<byte[]> param) {
        String eventId = null;
        try {
            byte[] body = (byte[]) param.get();
            String jsonStr = new String(body, "UTF-8");

            // Skip non-JSON messages (queue_payment receives mixed message types)
            if (jsonStr == null || jsonStr.isEmpty() || jsonStr.charAt(0) != '{') {
                return true; // ack and skip silently
            }

            logger.info("CryptoDepositEventProcessor received: " + jsonStr);

            JSONObject json = new JSONObject(jsonStr);
            eventId = json.getString("eventId");
            String eventType = json.optString("eventType", "deposit.confirmed");
            String transactionId = json.getString("transactionId");
            String userId = json.getString("userId");
            // Inbound JSON amount → BigDecimal via toString to avoid double-precision loss.
            BigDecimal amountUsdt = new BigDecimal(json.get("amount").toString());
            String fromAddress = json.optString("fromAddress", "");
            String symbol = json.optString("symbol", "USDT");
            String timestamp = json.optString("timestamp", "");

            // Step 1: Idempotency check
            if (isEventProcessed(eventId)) {
                logger.info("CryptoDepositEventProcessor: event " + eventId + " already processed, skipping");
                return true;
            }

            // Step 2: Resolve nickname from userId
            String nickname = resolveNickname(Long.parseLong(userId));
            if (nickname == null || nickname.isEmpty()) {
                logger.error("CryptoDepositEventProcessor: cannot resolve nickname for userId=" + userId
                        + ", eventId=" + eventId);
                // Return false to NACK so it retries (user may not be cached yet)
                return false;
            }

            // Step 3: Get exchange rate and convert USDT to KRW
            long krwPerUsdt = getUpbitKrwPerUsdt();
            long amountKrw = amountUsdt.multiply(BigDecimal.valueOf(krwPerUsdt))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (amountKrw <= 0) {
                logger.error("CryptoDepositEventProcessor: invalid amountKrw=" + amountKrw
                        + " for amountUsdt=" + amountUsdt + ", eventId=" + eventId);
                return false;
            }

            // Step 4: Single transaction — insert records and credit balance
            Connection conn = null;
            boolean committed = false;
            try {
                conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
                conn.setAutoCommit(false);

                // 4a: Insert into processed_events (idempotency key)
                PreparedStatement psEvent = conn.prepareStatement(
                        "INSERT INTO processed_events (event_id, event_type) VALUES (?, ?)");
                psEvent.setString(1, eventId);
                psEvent.setString(2, eventType);
                try {
                    psEvent.executeUpdate();
                } catch (java.sql.SQLIntegrityConstraintViolationException dupEx) {
                    // Duplicate — another thread processed it between our check and insert
                    conn.rollback();
                    logger.info("CryptoDepositEventProcessor: concurrent duplicate for eventId=" + eventId);
                    return true;
                } finally {
                    psEvent.close();
                }

                // 4b: Insert into crypto_deposits
                PreparedStatement psDeposit = conn.prepareStatement(
                        "INSERT INTO crypto_deposits (user_id, nick_name, address, tx_hash, amount_usdt, amount_krw, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'CONFIRMED', NOW())");
                psDeposit.setLong(1, Long.parseLong(userId));
                psDeposit.setString(2, nickname);
                psDeposit.setString(3, fromAddress);
                psDeposit.setString(4, transactionId);
                psDeposit.setBigDecimal(5, amountUsdt);
                psDeposit.setLong(6, amountKrw);
                psDeposit.executeUpdate();
                psDeposit.close();

                conn.commit();
                committed = true;

            } catch (Exception dbEx) {
                if (conn != null) {
                    try { conn.rollback(); } catch (Exception rbEx) { logger.error("Rollback failed", rbEx); }
                }
                throw dbEx;
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                    try { ConnectionPool.getInstance().releaseConnection(conn); } catch (Exception ignored) {}
                }
            }

            // Step 5: Credit KRW balance (outside DB transaction — uses its own internal tx)
            if (committed) {
                try {
                    UserServiceImpl userService = new UserServiceImpl();
                    BaseResponseModel creditResult = userService.updateMoneyFromAdmin(
                            nickname, amountKrw, "vin", "CryptoDeposit", "crypto_deposit",
                            "USDT deposit " + amountUsdt + " (" + transactionId + ")");

                    if (!creditResult.isSuccess()) {
                        logger.error("CryptoDepositEventProcessor: credit failed for nickname=" + nickname
                                + " amountKrw=" + amountKrw + " eventId=" + eventId
                                + " errorCode=" + creditResult.getErrorCode());
                        // Record is in DB as CONFIRMED but balance not credited
                        // Mark deposit with credit_failed flag for manual reconciliation
                        markCreditFailed(eventId, transactionId);
                        // Still return true to ACK — the event is recorded, needs manual fix
                        return true;
                    }

                    logger.info("CryptoDepositEventProcessor: SUCCESS eventId=" + eventId
                            + " userId=" + userId + " nickname=" + nickname
                            + " amountUsdt=" + amountUsdt + " amountKrw=" + amountKrw
                            + " rate=" + krwPerUsdt);

                } catch (Exception creditEx) {
                    logger.error("CryptoDepositEventProcessor: credit exception for eventId=" + eventId, creditEx);
                    markCreditFailed(eventId, transactionId);
                    return true;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("CryptoDepositEventProcessor: error processing eventId=" + eventId, e);
            // Return false to NACK — message will be redelivered
            return false;
        }
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
            logger.error("CryptoDepositEventProcessor: idempotency check failed for eventId=" + eventId, e);
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
            logger.error("CryptoDepositEventProcessor: resolveNickname failed for userId=" + userId, e);
            return null;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Get KRW/USDT rate from Upbit API. Falls back to FALLBACK_KRW_PER_USDT on any error.
     */
    private long getUpbitKrwPerUsdt() {
        try {
            URL url = new URL("https://api.upbit.com/v1/ticker?markets=KRW-USDT");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setConnectTimeout(UPBIT_TIMEOUT_MS);
            httpConn.setReadTimeout(UPBIT_TIMEOUT_MS);

            if (httpConn.getResponseCode() == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(httpConn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                // Response is a JSON array: [{"trade_price":1350.0,...}]
                String resp = sb.toString().trim();
                if (resp.startsWith("[")) {
                    org.json.JSONArray arr = new org.json.JSONArray(resp);
                    if (arr.length() > 0) {
                        double tradePrice = arr.getJSONObject(0).getDouble("trade_price");
                        if (tradePrice > 0) {
                            logger.debug("CryptoDepositEventProcessor: Upbit KRW/USDT rate=" + tradePrice);
                            return Math.round(tradePrice);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("CryptoDepositEventProcessor: Upbit API failed, using fallback rate", e);
        }
        return FALLBACK_KRW_PER_USDT;
    }

    /**
     * Mark a deposit as having a failed credit for manual reconciliation.
     */
    private void markCreditFailed(String eventId, String txHash) {
        Connection conn = null;
        try {
            conn = ConnectionPool.getInstance().getConnection(POOL_NAME);
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE crypto_deposits SET status = 'CREDIT_FAILED' WHERE tx_hash = ?");
            ps.setString(1, txHash);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            logger.error("CryptoDepositEventProcessor: markCreditFailed failed for txHash=" + txHash, e);
        } finally {
            releaseConnection(conn);
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
