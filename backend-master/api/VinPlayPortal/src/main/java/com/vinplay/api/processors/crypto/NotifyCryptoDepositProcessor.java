package com.vinplay.api.processors.crypto;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * c=3026 — Gateway → backend webhook for completed USDT deposits (SUN-1171).
 *
 * <p>The sunkr-usdt-gateway (.NET) calls this after it commits a confirmed
 * on-chain transfer to one of its user-share addresses. Without this hook
 * the gateway updates only its own PostgreSQL ledger; player-visible state
 * (KRW balance, deposit history) lives in the Java backend's MySQL and
 * never moves. This processor closes that loop.
 *
 * <p>Auth: gateway sends the shared {@code TRON_GATEWAY_API_KEY} as the
 * {@code aat} query param. Same key the backend uses outbound in
 * {@link com.vinplay.dal.crypto.TronGatewayClient} — set once in
 * {@code .env} and shared by both directions.
 *
 * <p>Idempotency: matched on {@code crypto_deposits.gateway_tx_id}. A
 * duplicate retry returns success without re-crediting.
 *
 * <p>Wallet credit follows the canonical Hazelcast lock + RMQ pattern from
 * {@code ClaimCashbackProcessor} and {@code RechargeServiceImpl.rechargeByCard}
 * so the credit lands consistently in {@code log_money_user} and stays in
 * sync with any in-progress bets on this user.
 */
public class NotifyCryptoDepositProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // 1. Auth — shared secret with the gateway.
            String aat = request.getParameter("aat");
            String expected = System.getenv("TRON_GATEWAY_API_KEY");
            if (expected == null || expected.isEmpty()) {
                logger.error("NotifyCryptoDepositProcessor: TRON_GATEWAY_API_KEY env not set — refusing all callbacks");
                return err(response, "9999", "Server misconfigured");
            }
            if (aat == null || !aat.equals(expected)) {
                logger.warn("NotifyCryptoDepositProcessor: auth failed");
                return err(response, "1001", "Auth required");
            }

            // 2. Parse + validate inputs.
            String userIdStr = request.getParameter("user_id");
            String txId = request.getParameter("tx_id");
            String amountUsdtStr = request.getParameter("amount_usdt");
            String address = request.getParameter("address");
            String gatewayTxId = request.getParameter("gateway_tx_id");
            if (gatewayTxId == null || gatewayTxId.isEmpty()) gatewayTxId = txId;

            if (userIdStr == null || userIdStr.isEmpty()
                    || txId == null || txId.isEmpty()
                    || amountUsdtStr == null || amountUsdtStr.isEmpty()) {
                return err(response, "4001", "Missing required parameters (user_id, tx_id, amount_usdt)");
            }
            long userId;
            try { userId = Long.parseLong(userIdStr.trim()); }
            catch (NumberFormatException nfe) { return err(response, "4002", "Invalid user_id"); }

            BigDecimal amountUsdt;
            try { amountUsdt = new BigDecimal(amountUsdtStr.trim()); }
            catch (NumberFormatException nfe) { return err(response, "4003", "Invalid amount_usdt"); }
            if (amountUsdt.signum() <= 0) {
                return err(response, "4003", "amount_usdt must be > 0");
            }

            // 3. Resolve user.
            String nickName = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT nick_name FROM users WHERE id = ? LIMIT 1")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) nickName = rs.getString("nick_name");
                }
            }
            if (nickName == null) {
                logger.warn("NotifyCryptoDepositProcessor: unknown user_id=" + userId + " gatewayTxId=" + gatewayTxId);
                return err(response, "4004", "User not found");
            }

            // 4. Detect existing crypto_deposits row for this gateway_tx_id.
            //    Informational only — we still run through MoneyGateway.creditUser
            //    below so that legacy rows from before the
            //    money-gateway-canonical refactor (where crypto_deposits was
            //    inserted but users.vin never credited because the cache-miss
            //    path bailed out) get their credit reconciled on replay.
            //    MoneyGateway's own dedup (money_gateway_log) is the source of
            //    truth for "did the money actually move", so a replay where
            //    everything previously succeeded returns "Duplicate transaction"
            //    from creditUser and lands in the dedup branch below — never
            //    double-credits.
            long existingDepositId = 0;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id FROM crypto_deposits WHERE gateway_tx_id = ? LIMIT 1")) {
                ps.setString(1, gatewayTxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existingDepositId = rs.getLong("id");
                }
            }

            // 5. Fetch live USDT→KRW rate (same Upbit/Bithumb/CryptoCompare cascade
            //    as c=3024). Refuse if every source is down — wrong-rate credit
            //    is worse than delayed credit.
            double rate = GetExchangeRateProcessor.getCurrentRate();
            if (rate <= 0) {
                logger.error("NotifyCryptoDepositProcessor: rate fetch failed userId=" + userId
                        + " gatewayTxId=" + gatewayTxId + " — refusing to credit");
                return err(response, "5001", "Exchange rate unavailable");
            }
            long amountKrw = amountUsdt.multiply(BigDecimal.valueOf(rate))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();

            // 6. Insert crypto_deposits row (status=COMPLETED) — only if not
            //    already there from a previous (possibly failed) attempt.
            //    Was 'SUCCESS' historically; player FE doesn't recognise that
            //    enum value (bank uses APPROVED/COMPLETED, bonus uses COMPLETED)
            //    and renders unrecognised statuses as 'Đang chờ'. Aligning to
            //    COMPLETED here closes the divergence at write time.
            //    CryptoDepositHistoryProcessor's read query also normalises
            //    legacy SUCCESS rows on the fly so there is no migration window.
            long depositId = existingDepositId;
            if (existingDepositId == 0) {
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO crypto_deposits (user_id, nick_name, amount_krw, amount_usdt, "
                             + "address, tx_hash, gateway_tx_id, status) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, 'COMPLETED')",
                             Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickName);
                    ps.setLong(3, amountKrw);
                    ps.setBigDecimal(4, amountUsdt);
                    ps.setString(5, address == null ? "" : address);
                    ps.setString(6, txId);
                    ps.setString(7, gatewayTxId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) depositId = rs.getLong(1);
                    }
                }
            }

            // 7. Credit users.vin via MoneyGateway — the only path the
            //    backend-master Dockerfile lint allows for users.vin writes.
            //    creditUser handles BOTH cache states uniformly:
            //      * SQL UPDATE users.vin (+ recharge_money for deposit sources)
            //      * updateCacheAndPush → Hazelcast cache refresh if user is
            //        cached, plus action-message broadcast to portal/minigame
            //        queues so any in-progress sessions see the new balance
            //      * money_gateway_log audit row + log_money_user RMQ publish
            //      * dedup on (tx_id, source) — a replay of the same
            //        gateway_tx_id is a no-op (returns CreditResult.fail
            //        with "Duplicate transaction"), which we treat as success
            //        because the original credit already landed.
            String description = "Nạp Crypto USDT-TRC20 ("
                    + amountUsdt.toPlainString() + " USDT @ " + rate + " KRW)";
            com.vinplay.dal.service.MoneyGateway.CreditResult cr =
                    com.vinplay.dal.service.MoneyGateway.creditUser(
                            userId,
                            nickName,
                            amountKrw,
                            com.vinplay.dal.service.MoneyGateway.SOURCE_DEPOSIT_CRYPTO,
                            gatewayTxId,
                            description);
            long newBalance;
            if (cr != null && cr.success) {
                newBalance = cr.newBalance;
            } else if (cr != null && cr.error != null && cr.error.contains("Duplicate")) {
                // Replay of a tx that already credited — recover newBalance
                // from MySQL so the response is informative.
                logger.info("NotifyCryptoDepositProcessor: replay of already-credited tx, returning idempotent success "
                        + "nick=" + nickName + " gatewayTxId=" + gatewayTxId);
                newBalance = -1;
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = conn.prepareStatement("SELECT vin FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) newBalance = rs.getLong("vin");
                    }
                } catch (Exception ignored) {}
            } else {
                String reason = (cr != null && cr.error != null) ? cr.error : "unknown";
                logger.error("NotifyCryptoDepositProcessor: MoneyGateway.creditUser FAILED nick="
                        + nickName + " amountKrw=" + amountKrw + " reason=" + reason);
                return err(response, "9998",
                        "Deposit recorded but wallet update failed; please contact support");
            }

            logger.info("NotifyCryptoDepositProcessor: OK userId=" + userId + " nick=" + nickName
                    + " usdt=" + amountUsdt.toPlainString() + " rate=" + rate + " krw=" + amountKrw
                    + " depositId=" + depositId + " newBalance=" + newBalance
                    + " gatewayTxId=" + gatewayTxId);

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("depositId", depositId);
            response.put("amountKrw", amountKrw);
            response.put("rate", rate);
            response.put("newBalance", newBalance);
            return response.toString();
        } catch (Exception e) {
            logger.error("NotifyCryptoDepositProcessor error", e);
            return err(response, "9999", "Internal: " + e.getMessage());
        }
    }

    private static String err(JSONObject r, String code, String msg) {
        r.put("success", false);
        r.put("errorCode", code);
        r.put("message", msg);
        return r.toString();
    }
}
