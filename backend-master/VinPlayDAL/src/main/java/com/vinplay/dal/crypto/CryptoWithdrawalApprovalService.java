package com.vinplay.dal.crypto;

import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Single source of truth for crypto-withdrawal approve / reject.
 *
 * <p>Both the admin CMS endpoints (c=9631 / c=9632) and the Telegram bot's
 * approve/reject callbacks call into this service, so any new step (volume
 * tracking, gateway-create, refund destination, audit log) is added in
 * exactly one place.
 *
 * <p>History — what motivated extracting this:
 * <ul>
 *   <li><b>SUN-1186</b> — moved gateway-side {@code createWithdrawal} from the
 *       player-request endpoint to admin-approve time. The admin processor
 *       got a create-then-approve branch; the Telegram bot was missed.
 *       Telegram-approved rows landed status=APPROVED in MySQL but the
 *       gateway never heard about them, so no on-chain transfer ever
 *       happened (KwonUSD_2 / row id=7, 2026-04-30).</li>
 *   <li>Drift discovered while fixing the above: admin c=9631 SELECT
 *       referenced a non-existent {@code telegram_message_id} column —
 *       meaning every call returned "Withdrawal not found". The admin
 *       web panel had been silently unable to approve crypto for some time;
 *       only the bot was working. This service uses one canonical query
 *       so both callers stay aligned with the actual schema.</li>
 * </ul>
 *
 * <p>The service does <b>only</b> the data work: row lookup, status
 * validation, gateway calls, DB state transitions, balance refund (on
 * reject). Channel-specific concerns — admin token validation, Telegram
 * message edits, response shaping — stay with the caller.
 */
public final class CryptoWithdrawalApprovalService {

    private static final Logger logger = Logger.getLogger("dal");

    private CryptoWithdrawalApprovalService() {}

    // ─────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────

    /** Snapshot of a crypto_withdrawals row at the moment the service handled it.
     *  amountUsdt is BigDecimal because the source column is decimal(20,6) —
     *  using double would silently lose sub-cent precision on large amounts. */
    public static class WithdrawalRow {
        public long id;
        public long userId;
        public String nickName;
        public String toAddress;
        public long amountKrw;
        public BigDecimal amountUsdt;
        public String txCode;
        public String gatewayTxId;
        public String status;

        public boolean isPending() { return "PENDING".equals(status); }
    }

    /** Approve / reject result. {@code row} is populated whenever the row was
     *  found, even on validation failure, so callers can render a useful
     *  toast / Telegram update without re-querying. */
    public static class Result {
        public final boolean success;
        public final String errorCode;
        public final String errorMessage;
        public final WithdrawalRow row;
        // Populated only on HOT_WALLET_LOW — gives the bot enough
        // context to render a helpful "please top up" alert.
        public final String hotWalletAddress;
        public final BigDecimal hotWalletBalance;
        public final BigDecimal requiredAmount;

        private Result(boolean success, String errorCode, String errorMessage, WithdrawalRow row,
                       String hotWalletAddress, BigDecimal hotWalletBalance, BigDecimal requiredAmount) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.row = row;
            this.hotWalletAddress = hotWalletAddress;
            this.hotWalletBalance = hotWalletBalance;
            this.requiredAmount = requiredAmount;
        }

        public static Result ok(WithdrawalRow row) {
            return new Result(true, null, null, row, null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        public static Result notFound() {
            return new Result(false, "WITHDRAWAL_NOT_FOUND", "Withdrawal not found",
                    null, null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        public static Result alreadyProcessed(WithdrawalRow row) {
            return new Result(false, "NOT_PENDING",
                    "Withdrawal is " + (row != null ? row.status : "?") + ", not PENDING",
                    row, null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        public static Result gatewayError(String msg, WithdrawalRow row) {
            return new Result(false, "GATEWAY_ERROR", msg, row, null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        public static Result hotWalletLow(WithdrawalRow row, String address,
                                          BigDecimal balance, BigDecimal required) {
            return new Result(false, "HOT_WALLET_LOW",
                    "Hot wallet has " + balance.setScale(2, RoundingMode.HALF_UP).toPlainString() + " USDT but "
                            + required.setScale(2, RoundingMode.HALF_UP).toPlainString() + " USDT is needed. "
                            + "Top up " + (address != null ? address : "the hot wallet")
                            + " then click Approve again.",
                    row, address, balance, required);
        }
        public static Result internalError(String msg) {
            return new Result(false, "INTERNAL_ERROR", msg, null, null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /** Look up a withdrawal row. Returns null if not found or on DB error. */
    public static WithdrawalRow lookup(long txId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_id, nick_name, to_address, amount_krw, amount_usdt, " +
                     "tx_code, gateway_tx_id, status " +
                     "FROM crypto_withdrawals WHERE id = ?")) {
            ps.setLong(1, txId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            logger.error("CryptoWithdrawalApprovalService.lookup error txId=" + txId, e);
        }
        return null;
    }

    /**
     * Approve a PENDING crypto withdrawal: ensure a gateway record exists,
     * tell the gateway to broadcast it on-chain, then flip the local row to
     * APPROVED. The on-chain tx hash arrives later via c=3027 callback,
     * which flips APPROVED → COMPLETED.
     */
    public static Result approve(long txId, String adminNickname) {
        WithdrawalRow row = lookup(txId);
        if (row == null) return Result.notFound();
        if (!row.isPending()) return Result.alreadyProcessed(row);

        // SUN-1171 follow-up: pre-check hot wallet balance. The gateway's
        // ProcessWithdrawalToNetworkJob is what actually pushes USDT
        // on-chain — it runs async and just LOGS the failure if the hot
        // wallet is short, leaving the local row at status=APPROVED and
        // the gateway record at status=Approved (count_failed_attempts
        // creeps up forever, no retry-on-topup mechanism, no operator
        // alert). Detect early so the bot can refuse the click and
        // post a "blance not enough" warning in chat instead. Skip for
        // gateway-record-already-exists rows (re-approve scenario): the
        // job is already queued, and re-checking here would mis-block
        // the retry case. Best-effort: if the gateway lookup fails, fall
        // through and let the legacy path proceed.
        if (row.gatewayTxId == null || row.gatewayTxId.isEmpty()) {
            try {
                JSONObject hwResp = TronGatewayClient.getInstance().getHotWalletInfo();
                if (hwResp != null && hwResp.optBoolean("isSuccess", false)) {
                    JSONObject hwData = hwResp.optJSONObject("data");
                    if (hwData != null) {
                        // Gateway returns balance as a JSON number; parse via String to avoid
                        // JSON-double precision loss for large balances.
                        BigDecimal hwBalance;
                        try {
                            hwBalance = new BigDecimal(hwData.opt("balance") == null ? "-1"
                                    : hwData.get("balance").toString());
                        } catch (NumberFormatException nfe) {
                            hwBalance = new BigDecimal("-1");
                        }
                        String hwAddress = hwData.optString("address", null);
                        if (hwBalance.signum() >= 0 && hwBalance.compareTo(row.amountUsdt) < 0) {
                            logger.warn("CryptoWithdrawalApprovalService.approve hot wallet low — "
                                    + "txId=" + txId + " need=" + row.amountUsdt
                                    + " have=" + hwBalance + " address=" + hwAddress);
                            return Result.hotWalletLow(row, hwAddress, hwBalance, row.amountUsdt);
                        }
                    }
                }
            } catch (Exception e) {
                // Don't fail the approve on a balance-lookup glitch.
                // Worst case we proceed and the gateway logs the same
                // "Balance hot wallet smaller" error it would have
                // logged anyway.
                logger.warn("CryptoWithdrawalApprovalService.approve hot wallet check threw txId="
                        + txId + " err=" + e.getMessage());
            }
        }

        // Ensure the gateway has a withdrawal record. SUN-1186 deferred this
        // from the player-request c=3021 to admin-approve time — meaning the
        // common case here is gateway_tx_id == null.
        if (row.gatewayTxId == null || row.gatewayTxId.isEmpty()) {
            try {
                TronGatewayClient.getInstance().getDepositAddress(row.userId);
            } catch (Exception ignore) {
                // Address may already exist; createWithdrawal will surface
                // any real failure.
            }

            JSONObject createResp = TronGatewayClient.getInstance().createWithdrawal(
                    row.toAddress, row.amountUsdt, row.userId, row.txCode);

            if (!createResp.optBoolean("isSuccess", false)) {
                JSONObject err = createResp.optJSONObject("error");
                String errMsg = err != null ? err.optString("message", "Gateway error") : "Gateway error";
                logger.error("CryptoWithdrawalApprovalService.approve gateway create failed txId="
                        + txId + ": " + errMsg);
                return Result.gatewayError("Gateway create failed: " + errMsg, row);
            }

            JSONObject createData = createResp.optJSONObject("data");
            String newGatewayTxId = createData != null ? createData.optString("txId", "") : "";
            if (newGatewayTxId == null || newGatewayTxId.isEmpty()) {
                logger.error("CryptoWithdrawalApprovalService.approve gateway returned empty txId for "
                        + txId);
                return Result.gatewayError("Gateway returned empty txId", row);
            }

            try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE crypto_withdrawals SET gateway_tx_id = ? WHERE id = ?")) {
                ps.setString(1, newGatewayTxId);
                ps.setLong(2, txId);
                ps.executeUpdate();
            } catch (Exception e) {
                // Gateway record exists but we can't persist the link locally.
                // Fail loud: a retry will hit "already exists" on the gateway
                // side, but ops can manually resolve via the gateway_tx_id in
                // the log message above.
                logger.error("CryptoWithdrawalApprovalService.approve persist gateway_tx_id failed txId="
                        + txId + " gateway_tx_id=" + newGatewayTxId, e);
                return Result.internalError("Persist gateway_tx_id failed: " + e.getMessage());
            }
            row.gatewayTxId = newGatewayTxId;
        }

        // Tell the gateway to broadcast. Parity with c=9631 (pre-refactor):
        // a gateway-approve failure does NOT fail the overall approve — the
        // record exists, ProcessWithdrawalToNetworkJob retries hourly, and
        // ops can manually re-trigger if needed.
        try {
            JSONObject gwResp = TronGatewayClient.getInstance().approveWithdrawal(row.gatewayTxId);
            if (!gwResp.optBoolean("isSuccess", false)) {
                JSONObject err = gwResp.optJSONObject("error");
                String errMsg = err != null ? err.optString("message", "Gateway error") : "Gateway error";
                logger.warn("CryptoWithdrawalApprovalService.approve gateway approve soft-failed txId="
                        + txId + " gateway_tx_id=" + row.gatewayTxId + " err=" + errMsg);
            }
        } catch (Exception e) {
            logger.warn("CryptoWithdrawalApprovalService.approve gateway approve threw txId="
                    + txId + " gateway_tx_id=" + row.gatewayTxId, e);
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE crypto_withdrawals " +
                     "SET status = 'APPROVED', admin_by = ?, processed_at = NOW() " +
                     "WHERE id = ? AND status = 'PENDING'")) {
            ps.setString(1, adminNickname);
            ps.setLong(2, txId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // Race — someone else approved/rejected between our lookup
                // and our update.
                WithdrawalRow current = lookup(txId);
                return Result.alreadyProcessed(current != null ? current : row);
            }
        } catch (Exception e) {
            logger.error("CryptoWithdrawalApprovalService.approve status update failed txId=" + txId, e);
            return Result.internalError("Status update failed: " + e.getMessage());
        }

        row.status = "APPROVED";
        return Result.ok(row);
    }

    /**
     * Reject a PENDING crypto withdrawal: tell the gateway to cancel any
     * record (best effort), flip the local row to REJECTED, refund the
     * frozen balance via MoneyGateway.
     */
    public static Result reject(long txId, String adminNickname, String reason) {
        if (reason == null || reason.isEmpty()) reason = "Rejected by admin";

        WithdrawalRow row = lookup(txId);
        if (row == null) return Result.notFound();
        if (!row.isPending()) return Result.alreadyProcessed(row);

        // Best-effort gateway reject. If the gateway has no record yet
        // (gateway_tx_id null because never approved) there's nothing to
        // cancel there.
        if (row.gatewayTxId != null && !row.gatewayTxId.isEmpty()) {
            try {
                JSONObject gwResp = TronGatewayClient.getInstance().rejectWithdrawal(row.gatewayTxId, reason);
                if (!gwResp.optBoolean("isSuccess", false)) {
                    JSONObject err = gwResp.optJSONObject("error");
                    String errMsg = err != null ? err.optString("message", "Gateway error") : "Gateway error";
                    logger.warn("CryptoWithdrawalApprovalService.reject gateway reject soft-failed txId="
                            + txId + " gateway_tx_id=" + row.gatewayTxId + " err=" + errMsg);
                }
            } catch (Exception e) {
                logger.warn("CryptoWithdrawalApprovalService.reject gateway reject threw txId="
                        + txId + " gateway_tx_id=" + row.gatewayTxId, e);
            }
        }

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE crypto_withdrawals SET status = 'REJECTED', admin_by = ?, " +
                     "reject_reason = ?, processed_at = NOW() " +
                     "WHERE id = ? AND status = 'PENDING'")) {
            ps.setString(1, adminNickname);
            ps.setString(2, reason);
            ps.setLong(3, txId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                WithdrawalRow current = lookup(txId);
                return Result.alreadyProcessed(current != null ? current : row);
            }
        } catch (Exception e) {
            logger.error("CryptoWithdrawalApprovalService.reject status update failed txId=" + txId, e);
            return Result.internalError("Status update failed: " + e.getMessage());
        }

        // Refund the frozen vin via canonical MoneyGateway path. Failure is
        // logged but does NOT fail the reject — status is already REJECTED,
        // refund can be recovered manually if needed.
        try {
            MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                    row.userId, row.nickName, row.amountKrw, MoneyGateway.SOURCE_REFUND_WITHDRAW,
                    row.txCode, "Crypto withdrawal rejected: " + reason);
            if (!cr.success) {
                logger.error("CryptoWithdrawalApprovalService.reject refund failed txId=" + txId
                        + " nickName=" + row.nickName + " amountKrw=" + row.amountKrw
                        + " error=" + cr.error);
            }
        } catch (Exception e) {
            logger.error("CryptoWithdrawalApprovalService.reject refund threw txId=" + txId, e);
        }

        row.status = "REJECTED";
        return Result.ok(row);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Internals
    // ─────────────────────────────────────────────────────────────────────

    private static WithdrawalRow mapRow(ResultSet rs) throws java.sql.SQLException {
        WithdrawalRow r = new WithdrawalRow();
        r.id = rs.getLong("id");
        r.userId = rs.getLong("user_id");
        r.nickName = rs.getString("nick_name");
        r.toAddress = rs.getString("to_address");
        r.amountKrw = rs.getLong("amount_krw");
        r.amountUsdt = rs.getBigDecimal("amount_usdt");
        r.txCode = rs.getString("tx_code");
        r.gatewayTxId = rs.getString("gateway_tx_id");
        r.status = rs.getString("status");
        return r;
    }
}
