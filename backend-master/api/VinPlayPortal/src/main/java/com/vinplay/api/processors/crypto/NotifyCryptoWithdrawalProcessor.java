package com.vinplay.api.processors.crypto;

import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * c=3027 — Gateway → backend webhook for completed/failed USDT-TRC20
 * withdrawals (SUN-1171).
 *
 * <p>The sunkr-usdt-gateway (.NET) calls this after
 * {@code ProcessWithdrawalToNetworkJob} either successfully broadcasts
 * an on-chain TRX transaction (status=SUCCESS, with tx_hash) OR fails
 * permanently (status=FAILED, with reason). The mirror-image of c=3026
 * for the deposit direction.
 *
 * <p><b>Real-money invariants:</b>
 * <ul>
 *   <li><b>Auth:</b> shared secret in {@code aat} query param matches
 *       {@code TRON_GATEWAY_API_KEY} env. Refuse all requests if env
 *       unset (fail-closed).</li>
 *   <li><b>Lookup:</b> {@code crypto_withdrawals.gateway_tx_id} is the
 *       only acceptable join key — it's the GUID the gateway assigned
 *       at withdraw-creation time and is unique across the gateway DB.
 *       We do NOT trust user_id / amount alone for matching.</li>
 *   <li><b>Idempotency:</b> a row already in a terminal state
 *       ({@code COMPLETED} / {@code REJECTED}) is treated as a no-op
 *       success. Replay-safe.</li>
 *   <li><b>SUCCESS path:</b> stamp {@code tx_hash} (on-chain TRX hash) +
 *       {@code status='COMPLETED'} + {@code processed_at=NOW()}. Status
 *       guard {@code WHERE status IN ('PENDING','APPROVED')} prevents
 *       overwriting a row that lost a race to the failure path.</li>
 *   <li><b>FAILED path:</b> set {@code status='REJECTED'},
 *       {@code reject_reason} = gateway's failure reason, processed_at,
 *       AND refund {@code users.vin} via the same userService path the
 *       deduction used in {@code WithdrawCryptoProcessor}. Refund first,
 *       row-state-update second — same ordering invariant as the
 *       inline-fail path.</li>
 *   <li><b>Audit pairing:</b> refund description includes the row's
 *       {@code tx_code} so the original deduction (action_name=
 *       {@code CryptoWithdraw}) and refund (action_name=
 *       {@code CryptoWithdrawRefund}) can be paired in
 *       {@code log_money_user} reports.</li>
 * </ul>
 */
public class NotifyCryptoWithdrawalProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = param.get();

            // 1. Auth.
            String aat = request.getParameter("aat");
            String expected = System.getenv("TRON_GATEWAY_API_KEY");
            if (expected == null || expected.isEmpty()) {
                logger.error("NotifyCryptoWithdrawalProcessor: TRON_GATEWAY_API_KEY env not set — refusing all callbacks");
                return err(response, "9999", "Server misconfigured");
            }
            if (aat == null || !aat.equals(expected)) {
                logger.warn("NotifyCryptoWithdrawalProcessor: auth failed");
                return err(response, "1001", "Auth required");
            }

            // 2. Inputs.
            String gatewayTxId = request.getParameter("gateway_tx_id");
            String statusIn = request.getParameter("status"); // SUCCESS or FAILED
            String txHash = request.getParameter("tx_hash"); // on-chain hash, present on SUCCESS
            String reason = request.getParameter("reason"); // failure reason, present on FAILED

            if (gatewayTxId == null || gatewayTxId.isEmpty()) {
                return err(response, "4001", "gateway_tx_id required");
            }
            if (statusIn == null || statusIn.isEmpty()) {
                return err(response, "4002", "status required (SUCCESS or FAILED)");
            }
            boolean isSuccess = "SUCCESS".equalsIgnoreCase(statusIn);
            boolean isFailed = "FAILED".equalsIgnoreCase(statusIn);
            if (!isSuccess && !isFailed) {
                return err(response, "4003", "status must be SUCCESS or FAILED");
            }
            if (isSuccess && (txHash == null || txHash.isEmpty())) {
                return err(response, "4004", "tx_hash required for SUCCESS");
            }

            // 3. Look up the row by gateway_tx_id.
            long dbId = 0;
            long userId = 0;
            String nickName = null;
            long amountKrw = 0;
            String txCode = null;
            String currentStatus = null;
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, user_id, nick_name, amount_krw, tx_code, status " +
                         "FROM crypto_withdrawals WHERE gateway_tx_id = ? LIMIT 1")) {
                ps.setString(1, gatewayTxId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dbId = rs.getLong("id");
                        userId = rs.getLong("user_id");
                        nickName = rs.getString("nick_name");
                        amountKrw = rs.getLong("amount_krw");
                        txCode = rs.getString("tx_code");
                        currentStatus = rs.getString("status");
                    }
                }
            }
            if (dbId == 0) {
                logger.warn("NotifyCryptoWithdrawalProcessor: unknown gateway_tx_id=" + gatewayTxId);
                return err(response, "4005", "Withdrawal not found");
            }

            // 4. Idempotency — terminal states return success no-op.
            if ("COMPLETED".equals(currentStatus) || "REJECTED".equals(currentStatus)) {
                logger.info("NotifyCryptoWithdrawalProcessor: idempotent replay dbId=" + dbId
                        + " gatewayTxId=" + gatewayTxId + " currentStatus=" + currentStatus);
                response.put("success", true);
                response.put("errorCode", "0");
                response.put("idempotent", true);
                response.put("status", currentStatus);
                response.put("dbId", dbId);
                return response.toString();
            }

            if (isSuccess) {
                // 5a. Stamp tx_hash + COMPLETED. Status guard prevents
                //     racing with the FAILED path.
                try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                     PreparedStatement ps = conn.prepareStatement(
                             "UPDATE crypto_withdrawals SET tx_hash = ?, status = 'COMPLETED', " +
                             "processed_at = NOW() " +
                             "WHERE id = ? AND status IN ('PENDING','APPROVED')")) {
                    ps.setString(1, txHash);
                    ps.setLong(2, dbId);
                    int n = ps.executeUpdate();
                    if (n != 1) {
                        // Lost the race — row was already moved out of
                        // PENDING/APPROVED. Recheck and idempotent-respond.
                        logger.warn("NotifyCryptoWithdrawalProcessor: SUCCESS guard miss "
                                + "dbId=" + dbId + " gatewayTxId=" + gatewayTxId
                                + " txHash=" + txHash + " — row not in PENDING/APPROVED");
                    }
                }
                logger.info("NotifyCryptoWithdrawalProcessor: COMPLETED dbId=" + dbId
                        + " nick=" + nickName + " amountKrw=" + amountKrw
                        + " txCode=" + txCode + " gatewayTxId=" + gatewayTxId
                        + " txHash=" + txHash);

                response.put("success", true);
                response.put("errorCode", "0");
                response.put("dbId", dbId);
                response.put("status", "COMPLETED");
                response.put("txHash", txHash);
                return response.toString();
            }

            // 5b. FAILED path — refund first, row-state-update second.
            //     Mirrors the ordering invariant in WithdrawCryptoProcessor.
            String failureReason = (reason == null || reason.isEmpty())
                    ? "Gateway reported FAILED with no reason"
                    : reason;
            logger.warn("NotifyCryptoWithdrawalProcessor: FAILED dbId=" + dbId
                    + " nick=" + nickName + " amountKrw=" + amountKrw
                    + " txCode=" + txCode + " gatewayTxId=" + gatewayTxId
                    + " reason=" + failureReason);

            boolean refundOk = false;
            try {
                UserServiceImpl userService = new UserServiceImpl();
                BaseResponseModel refundRes = userService.updateMoneyFromAdmin(
                        nickName, amountKrw, "vin", "CryptoWithdrawRefund",
                        "crypto_refund_gateway_failed",
                        "Refund crypto withdraw txCode=" + txCode + " dbId=" + dbId
                                + " — gateway reported failure: " + failureReason);
                refundOk = refundRes != null && refundRes.isSuccess();
                if (refundOk) {
                    logger.info("NotifyCryptoWithdrawalProcessor: refund OK nick=" + nickName
                            + " dbId=" + dbId + " txCode=" + txCode + " amountKrw=" + amountKrw);
                }
            } catch (Exception refundErr) {
                logger.error("ALERT NotifyCryptoWithdrawalProcessor refund threw nick="
                        + nickName + " dbId=" + dbId + " txCode=" + txCode
                        + " amountKrw=" + amountKrw
                        + " — player KRW NOT restored, MANUAL FIX REQUIRED", refundErr);
            }
            if (!refundOk) {
                logger.error("ALERT NotifyCryptoWithdrawalProcessor refund FAILED nick="
                        + nickName + " dbId=" + dbId + " txCode=" + txCode
                        + " amountKrw=" + amountKrw
                        + " — player KRW NOT restored, MANUAL FIX REQUIRED");
            }

            // 5c. Mark REJECTED. Idempotent: only PENDING/APPROVED → REJECTED.
            String rejectReason = refundOk ? failureReason : "REFUND_FAILED — " + failureReason;
            if (rejectReason.length() > 500) rejectReason = rejectReason.substring(0, 500);
            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE crypto_withdrawals SET status = 'REJECTED', " +
                         "reject_reason = ?, processed_at = NOW() " +
                         "WHERE id = ? AND status IN ('PENDING','APPROVED')")) {
                ps.setString(1, rejectReason);
                ps.setLong(2, dbId);
                ps.executeUpdate();
            }

            response.put("success", true);
            response.put("errorCode", "0");
            response.put("dbId", dbId);
            response.put("status", "REJECTED");
            response.put("refunded", refundOk);
            return response.toString();

        } catch (Exception e) {
            logger.error("NotifyCryptoWithdrawalProcessor error", e);
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
