package com.vinplay.dal.withdraw;

import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Single source of truth for bank-withdrawal approve / reject.
 *
 * <p>Mirrors {@link com.vinplay.dal.crypto.CryptoWithdrawalApprovalService} and
 * {@link com.vinplay.dal.deposit.DepositApprovalService} so the four
 * approve/reject flows (bank deposit, crypto deposit*, bank withdraw,
 * crypto withdraw) all share the same shape. Today only the admin CMS
 * (c=9643 / c=9644) calls this service — but if we ever add a Telegram
 * approval channel for bank withdraws (parallel to what crypto withdraw
 * has), the channel-specific glue stays in the caller and the data
 * work stays here, exactly like the deposit service.
 *
 * <p>What used to be inline in each processor (~100 lines × 2):
 * <ul>
 *   <li>SELECT current row state</li>
 *   <li>Atomic UPDATE … WHERE status='PENDING' status flip</li>
 *   <li>{@link MoneyGateway#creditUser} refund on reject</li>
 *   <li>Race handling between SELECT and UPDATE</li>
 * </ul>
 *
 * <p>The service does <b>only</b> the data work: row lookup, status
 * validation, atomic state transitions, balance refund (on reject).
 * Channel-specific concerns — admin token validation, response shaping —
 * stay with the caller.
 *
 * <p>* Crypto deposits arrive via gateway events ({@code c=3026}
 * webhook → {@code CryptoDepositEventProcessor}) and are inherently
 * single-source — no admin approve/reject flow, so no shared service
 * needed.
 */
public final class BankWithdrawalApprovalService {

    private static final Logger logger = Logger.getLogger("dal");

    private BankWithdrawalApprovalService() {}

    /** Snapshot of a bank_withdrawals row. SUN-1220: includes bank
     *  details + telegram_message_id so callers can edit the bot
     *  message back to terminal state. */
    public static class WithdrawalRow {
        public long id;
        public long userId;
        public String nickName;
        public long amountKrw;
        public String txCode;
        public String status;
        public String bankName;
        public String bankNumber;
        public String customerName;
        public long telegramMessageId;   // 0 if not posted to Telegram

        public boolean isPending() { return "PENDING".equals(status); }
    }

    /** Approve / reject result. {@code row} is populated whenever the
     *  row was found, even on validation failure, so callers can render
     *  a useful response without re-querying. */
    public static class Result {
        public final boolean success;
        public final String errorCode;
        public final String errorMessage;
        public final WithdrawalRow row;

        private Result(boolean success, String errorCode, String errorMessage, WithdrawalRow row) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.row = row;
        }

        public static Result ok(WithdrawalRow row) {
            return new Result(true, null, null, row);
        }
        public static Result notFound() {
            return new Result(false, "WITHDRAWAL_NOT_FOUND", "Withdrawal not found", null);
        }
        public static Result alreadyProcessed(WithdrawalRow row) {
            return new Result(false, "NOT_PENDING",
                    "Withdrawal is " + (row != null ? row.status : "?") + ", not PENDING", row);
        }
        public static Result internalError(String msg) {
            return new Result(false, "INTERNAL_ERROR", msg, null);
        }
    }

    /** Look up a bank withdrawal row. Returns null if not found / DB error. */
    public static WithdrawalRow lookup(long txId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT bw.id, bw.nick_name, bw.amount_krw, bw.tx_code, bw.status, " +
                     "bw.bank_name, bw.bank_number, bw.customer_name, bw.telegram_message_id, " +
                     "u.id AS user_id " +
                     "FROM bank_withdrawals bw JOIN users u ON u.nick_name = bw.nick_name " +
                     "WHERE bw.id = ?")) {
            ps.setLong(1, txId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            logger.error("BankWithdrawalApprovalService.lookup error txId=" + txId, e);
        }
        return null;
    }

    /**
     * Approve a PENDING bank withdrawal. No balance touched (balance was
     * already debited when the player created the request via
     * {@code WithdrawBankProcessor}).
     */
    public static Result approve(long txId, String adminNickname) {
        WithdrawalRow row = lookup(txId);
        if (row == null) return Result.notFound();
        if (!row.isPending()) return Result.alreadyProcessed(row);

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bank_withdrawals SET status = 'APPROVED', admin_by = ?, " +
                     "processed_at = NOW() WHERE id = ? AND status = 'PENDING'")) {
            ps.setString(1, adminNickname);
            ps.setLong(2, txId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                WithdrawalRow current = lookup(txId);
                return Result.alreadyProcessed(current != null ? current : row);
            }
        } catch (Exception e) {
            logger.error("BankWithdrawalApprovalService.approve status update failed txId=" + txId, e);
            return Result.internalError("Status update failed: " + e.getMessage());
        }

        row.status = "APPROVED";

        // Report aggregates: bump users.t_rut / rut_times and the
        // log_report_user daily roll-up so the agent revenue panel
        // (c=9910 ListUsersProcessor) reflects the approved withdrawal.
        // The status flip above is the idempotency gate (UPDATE ...
        // WHERE status='PENDING' returned > 0 rows), so this fires at
        // most once per row in normal operation.
        try {
            com.vinplay.dal.service.DepositReportAggregator.applyWithdraw(
                    row.userId, row.nickName, row.amountKrw);
        } catch (Exception e) {
            logger.warn("BankWithdrawalApprovalService.approve aggregates failed txId=" + txId, e);
        }

        return Result.ok(row);
    }

    /**
     * Reject a PENDING bank withdrawal: flip status to REJECTED then
     * refund the frozen KRW via the canonical {@link MoneyGateway} path.
     *
     * <p>Refund failure is logged but does NOT fail the reject — status
     * is already REJECTED, refund can be recovered manually from
     * {@code money_gateway_log} if needed.
     */
    public static Result reject(long txId, String adminNickname, String reason) {
        if (reason == null || reason.isEmpty()) reason = "Rejected by admin";

        WithdrawalRow row = lookup(txId);
        if (row == null) return Result.notFound();
        if (!row.isPending()) return Result.alreadyProcessed(row);

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bank_withdrawals SET status = 'REJECTED', admin_by = ?, " +
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
            logger.error("BankWithdrawalApprovalService.reject status update failed txId=" + txId, e);
            return Result.internalError("Status update failed: " + e.getMessage());
        }

        try {
            MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                    row.userId, row.nickName, row.amountKrw,
                    MoneyGateway.SOURCE_REFUND_WITHDRAW,
                    row.txCode,
                    "Bank withdrawal rejected: " + reason);
            if (!cr.success) {
                logger.error("BankWithdrawalApprovalService.reject refund failed txId=" + txId
                        + " nickName=" + row.nickName + " amountKrw=" + row.amountKrw
                        + " error=" + cr.error);
            }
        } catch (Exception e) {
            logger.error("BankWithdrawalApprovalService.reject refund threw txId=" + txId, e);
        }

        row.status = "REJECTED";
        return Result.ok(row);
    }

    private static WithdrawalRow mapRow(ResultSet rs) throws java.sql.SQLException {
        WithdrawalRow r = new WithdrawalRow();
        r.id = rs.getLong("id");
        r.userId = rs.getLong("user_id");
        r.nickName = rs.getString("nick_name");
        r.amountKrw = rs.getLong("amount_krw");
        r.txCode = rs.getString("tx_code");
        r.status = rs.getString("status");
        r.bankName = rs.getString("bank_name");
        r.bankNumber = rs.getString("bank_number");
        r.customerName = rs.getString("customer_name");
        r.telegramMessageId = rs.getLong("telegram_message_id");   // 0 if SQL NULL
        return r;
    }
}
