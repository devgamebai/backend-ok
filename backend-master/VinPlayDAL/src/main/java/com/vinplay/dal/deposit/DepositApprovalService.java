package com.vinplay.dal.deposit;

import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.DepositTransactionDao;
import com.vinplay.dal.dao.impl.DepositTransactionDaoImpl;
import com.vinplay.dal.service.DepositLockService;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.dal.service.impl.DepositLockServiceImpl;
import com.vinplay.dal.withdraw.VolumeTrackingService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.NotiGameMessage;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * Single source of truth for deposit approve / reject.
 *
 * <p>Both the admin CMS endpoints (c=9543 / c=9544) and the Telegram
 * bot's approve/reject callbacks call into this service, so any new
 * step (volume tracking, promo handling, balance push) is added in
 * exactly one place.
 *
 * <p>Why this exists — drift incidents that motivated the extraction:
 * <ul>
 *   <li><b>SUN-1197</b> — {@code VolumeTrackingService.addRequiredVolume}
 *       was added to the CMS deposit-approve path but missed in the
 *       Telegram bot. Telegram-approved deposits never bumped the
 *       rolling-volume requirement; players could withdraw without
 *       clearing turnover.</li>
 *   <li>The two paths used <i>different</i> {@link MoneyGateway}
 *       sources ({@code DEPOSIT_BANK} vs {@code DEPOSIT_TELEGRAM}) and
 *       audit-log channels ({@code "CMS"} vs {@code "TELEGRAM"}) — the
 *       only legitimate channel-specific concerns. Those become enum
 *       values on {@link Channel}; everything else is shared.</li>
 *   <li>Each caller had its own balance-capture block (Hazelcast →
 *       MySQL fallback), insertAuditLog argument list, and partial
 *       error handling. Same story as crypto — the canonical path was
 *       drifting in five places at once.</li>
 * </ul>
 *
 * <p>The service does <b>only</b> the data work: lookup, lock-aware
 * status validation, atomic status transitions, balance credit via
 * {@link MoneyGateway}, volume tracking, balance-push RMQ message,
 * lock release, audit log. Channel-specific concerns — admin token
 * validation, Telegram message edits, response shaping, bot toast —
 * stay with the caller.
 */
public final class DepositApprovalService {

    private static final Logger logger = Logger.getLogger("dal");

    private DepositApprovalService() {}

    // ─────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────

    /** Where the approve/reject originated. Maps to the {@link MoneyGateway}
     *  source string and the audit-log platform tag. */
    public enum Channel {
        CMS("CMS", MoneyGateway.SOURCE_DEPOSIT_BANK,
                "Bank deposit approved", "Bank deposit rejected"),
        TELEGRAM("TELEGRAM", MoneyGateway.SOURCE_DEPOSIT_TELEGRAM,
                "Telegram deposit", "Telegram deposit rejected");

        public final String auditPlatform;
        public final String moneyGatewaySource;
        public final String creditDescriptionApprove;
        public final String creditDescriptionReject;

        Channel(String auditPlatform, String moneyGatewaySource,
                String creditDescriptionApprove, String creditDescriptionReject) {
            this.auditPlatform = auditPlatform;
            this.moneyGatewaySource = moneyGatewaySource;
            this.creditDescriptionApprove = creditDescriptionApprove;
            this.creditDescriptionReject = creditDescriptionReject;
        }
    }

    /** Snapshot of a deposit_transactions row at the moment the service
     *  handled it. Populated even on validation failure so callers can
     *  render a useful toast / Telegram update without re-querying. */
    public static class TransactionRow {
        public long id;
        public long userId;
        public long amount;
        public String nickName;
        public String txCode;
        public String status;       // status BEFORE the service mutated it
        public String bankName;     // user's bank info — needed for Telegram edit
        public String bankNumber;
        public String operatorName; // current lock holder (may be null)
        public long telegramMsgId;  // best-effort, 0 if none
    }

    /** Approve / reject result. */
    public static class Result {
        public final boolean success;
        public final String errorCode;          // null on success
        public final String errorMessage;       // null on success
        public final TransactionRow row;

        // Approve-only outputs:
        public final boolean credited;          // false on credit failure (status already moved to APPROVED, ops reconciles)
        public final boolean promoApplied;
        public final long bonusAmount;
        public final String promoType;

        private Result(boolean success, String errorCode, String errorMessage, TransactionRow row,
                       boolean credited, boolean promoApplied, long bonusAmount, String promoType) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.row = row;
            this.credited = credited;
            this.promoApplied = promoApplied;
            this.bonusAmount = bonusAmount;
            this.promoType = promoType;
        }

        // ── Success factories ──────────────────────────────────────────
        public static Result approved(TransactionRow row, boolean credited,
                                      boolean promoApplied, long bonusAmount, String promoType) {
            return new Result(true, null, null, row, credited, promoApplied, bonusAmount, promoType);
        }
        public static Result rejected(TransactionRow row) {
            return new Result(true, null, null, row, false, false, 0, null);
        }

        // ── Failure factories ─────────────────────────────────────────
        public static Result notFound() {
            return new Result(false, "TX_NOT_FOUND", "Transaction not found",
                    null, false, false, 0, null);
        }
        public static Result terminalStatus(TransactionRow row) {
            return new Result(false, "TERMINAL_STATUS",
                    "Cannot mutate a " + (row != null ? row.status : "?")
                            + " transaction. Use Force Approve (c=9615) with explicit confirmation.",
                    row, false, false, 0, null);
        }
        public static Result lockedByOther(TransactionRow row, String lockedBy) {
            return new Result(false, "LOCKED_BY_OTHER",
                    "Locked by " + lockedBy, row, false, false, 0, null);
        }
        public static Result cannotPick(TransactionRow row) {
            return new Result(false, "CANNOT_PICK",
                    "Cannot pick transaction — may be locked by another admin",
                    row, false, false, 0, null);
        }
        public static Result cannotApprove(TransactionRow row) {
            return new Result(false, "CANNOT_APPROVE",
                    "Cannot approve transaction (status race?)", row,
                    false, false, 0, null);
        }
        public static Result cannotReject(TransactionRow row) {
            return new Result(false, "CANNOT_REJECT",
                    "Cannot reject transaction (status race?)", row,
                    false, false, 0, null);
        }
        public static Result internalError(String msg) {
            return new Result(false, "INTERNAL_ERROR", msg, null,
                    false, false, 0, null);
        }
    }

    /** Look up a deposit transaction. Returns null if not found / DB error. */
    public static TransactionRow lookup(long txId) {
        try {
            DepositTransactionDao dao = new DepositTransactionDaoImpl();
            Map<String, Object> tx = dao.getTransaction(txId);
            if (tx == null) return null;
            return mapRow(tx);
        } catch (Exception e) {
            logger.error("DepositApprovalService.lookup error txId=" + txId, e);
            return null;
        }
    }

    /**
     * Approve a PENDING / PROCESSING deposit.
     *
     * <p>Auto-picks a PENDING row so callers don't need a separate
     * pick-then-approve flow. Refuses REJECTED / EXPIRED rows — those
     * require {@code c=9615 ForceApproveExpired} with explicit
     * confirmation (incident on tx_id=59 where a rejected deposit was
     * silently re-approved 5 minutes later).
     *
     * <p>Credit failures do NOT flip the status back to PENDING — the
     * row stays APPROVED with credit_status=CREDIT_FAILED so the
     * failed-credits drainer can retry without an operator picking
     * the same row twice.
     */
    public static Result approve(long txId, String operatorName, Channel channel) {
        // Revert a successful pickTransaction if anything between then and reaching
        // APPROVED fails. Without this, the row sits at status=PROCESSING + locked_by=op
        // forever, blocking every subsequent retry from any admin (Duccot8386 tx 368
        // incident, 2026-05-10). dao + lockService are hoisted out of the inner try
        // so the finally can see them; weDidPick / fullyApproved track progress.
        DepositTransactionDao dao = new DepositTransactionDaoImpl();
        DepositLockService lockService = new DepositLockServiceImpl();
        boolean weDidPick = false;
        boolean fullyApproved = false;
        try {
            Map<String, Object> tx = dao.getTransaction(txId);
            if (tx == null) return Result.notFound();
            TransactionRow row = mapRow(tx);

            // GitLab #28: Reject is final. Re-approving REJECTED / EXPIRED
            // requires the explicit Force Approve UX — the same "Approve"
            // button silently re-running force-approve already caused an
            // incident (tx_id=59, money credited twice).
            if ("REJECTED".equals(row.status) || "EXPIRED".equals(row.status)) {
                return Result.terminalStatus(row);
            }

            // Lock check. If held by someone else, refuse — both CMS and
            // Telegram require the operator to hold the lock.
            Map<String, Object> lockInfo = lockService.getLockInfo(txId);
            if (lockInfo != null) {
                String lockedBy = (String) lockInfo.get("operator");
                if (lockedBy != null && !lockedBy.equals(operatorName)) {
                    return Result.lockedByOther(row, lockedBy);
                }
            }

            // Auto-pick PENDING — atomic; fails if someone else picked
            // between our read and our update.
            if ("PENDING".equals(row.status)) {
                boolean picked = dao.pickTransaction(txId, operatorName, channel.auditPlatform);
                if (!picked) return Result.cannotPick(row);
                weDidPick = true;
                logger.info("DepositApprovalService.approve auto-picked txId=" + txId
                        + " by " + operatorName + " channel=" + channel.auditPlatform);
            }

            // Atomic status flip: WHERE status='PROCESSING' AND locked_by=op
            boolean approved = dao.approveTransaction(txId, operatorName, row.amount);
            if (!approved) return Result.cannotApprove(row);
            fullyApproved = true;

            // Capture pre-credit balance for VolumeTrackingService reset/
            // accumulate decision (SUN-1: balance==0 → reset, balance>0 →
            // accumulate). Hazelcast first, MySQL fallback so we never
            // miss a real balance.
            long balanceBeforeCredit = readPreCreditBalance(row.userId, row.nickName);

            // Credit balance via the canonical MoneyGateway path.
            // creditUser already does: SQL UPDATE users.vin, recharge_money
            // bump, Hazelcast cache refresh, action-message broadcast,
            // promo evaluation, and money_gateway_log audit row, all
            // dedup-keyed on (tx_id, source).
            boolean credited = false;
            boolean promoApplied = false;
            long bonusAmount = 0L;
            String promoType = null;
            try {
                MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                        row.userId, row.nickName, row.amount,
                        channel.moneyGatewaySource,
                        String.valueOf(txId),
                        channel.creditDescriptionApprove + " " + (row.txCode != null ? row.txCode : ""));
                credited = cr != null && cr.success;
                if (credited) {
                    promoApplied = cr.promoApplied;
                    bonusAmount = cr.bonusAmount;
                    promoType = cr.promoType;
                    if (promoApplied) {
                        logger.info("DepositApprovalService.approve promo CREDITED userId=" + row.userId
                                + " type=" + promoType + " bonus=" + bonusAmount + " txId=" + txId);
                    }
                } else {
                    logger.error("DepositApprovalService.approve credit failed txId=" + txId
                            + " nickName=" + row.nickName + " amount=" + row.amount
                            + " error=" + (cr != null ? cr.error : "null result"));
                }
            } catch (Exception e) {
                logger.error("DepositApprovalService.approve credit threw txId=" + txId, e);
            }

            // SUN-1xxx (2026-05-11): retry the post-credit status update.
            // Dat2lit tx 483 incident: MoneyGateway.creditUser committed
            // money to the player wallet (mgl row + users.vin updated) but
            // dao.updateCreditStatus failed silently here, leaving the
            // deposit row at credit_status=PENDING_CREDIT. The Telegram
            // approval message then stayed at "Đang xử lý" forever because
            // the bot reads the DB state to decide what to edit. Three
            // attempts with 200ms backoff covers transient DB pool / HZ
            // contention without slowing the happy path.
            String desiredCreditStatus = credited ? "CREDITED" : "CREDIT_FAILED";
            boolean creditStatusUpdated = false;
            Exception lastCreditStatusErr = null;
            for (int attempt = 0; attempt < 3 && !creditStatusUpdated; attempt++) {
                try {
                    dao.updateCreditStatus(txId, desiredCreditStatus);
                    creditStatusUpdated = true;
                } catch (Exception e) {
                    lastCreditStatusErr = e;
                    if (attempt < 2) {
                        try { Thread.sleep(200L); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    }
                }
            }
            if (!creditStatusUpdated) {
                logger.error("DepositApprovalService.approve updateCreditStatus FAILED after 3 attempts txId="
                        + txId + " desiredStatus=" + desiredCreditStatus
                        + " credited=" + credited
                        + " — deposit row will remain at credit_status=PENDING_CREDIT;"
                        + " the deposit-reconciliation drainer will retry asynchronously",
                        lastCreditStatusErr);
            }

            // Report aggregates: bump users.t_nap / nap_times and the
            // log_report_user daily roll-up that the agent revenue panel
            // (c=9910 ListUsersProcessor) reads. The credit ledger handles
            // wallet balance; this fires only on a real credit so a
            // failed/duplicate credit does not inflate the totals.
            if (credited) {
                try {
                    com.vinplay.dal.service.DepositReportAggregator.applyDeposit(
                            row.userId, row.nickName, row.amount);
                } catch (Exception e) {
                    logger.warn("DepositApprovalService.approve aggregates failed txId=" + txId, e);
                }
            }

            // SUN-1197: bump rolling-volume requirement. Without this the
            // user can withdraw before clearing the deposit's turnover.
            if (credited) {
                try {
                    VolumeTrackingService.addRequiredVolume(
                            (int) row.userId, row.nickName, row.amount, balanceBeforeCredit);
                    VolumeTrackingService.updateWithdrawalStatus((int) row.userId);
                } catch (Exception e) {
                    logger.warn("DepositApprovalService.approve volume tracking failed txId=" + txId, e);
                }

                // Push real-time balance update to any connected client
                // session — keeps the in-game wallet + portal top-bar in
                // sync without a refresh. Routed through the canonical
                // MoneyGateway.publishBalanceUpdate helper so this stays
                // aligned with every other money-mutation path.
                com.vinplay.dal.service.MoneyGateway.publishBalanceUpdate(row.nickName);
            }

            // Release the Hazelcast lock.
            try {
                lockService.release(txId, operatorName);
            } catch (Exception e) {
                logger.error("DepositApprovalService.approve release lock failed txId=" + txId, e);
            }

            // Audit log — retry on the same pattern as updateCreditStatus.
            // Without retry, a transient DB error here leaves the deposit
            // history page missing the APPROVED row (Dat2lit incident).
            boolean auditWritten = false;
            Exception lastAuditErr = null;
            for (int attempt = 0; attempt < 3 && !auditWritten; attempt++) {
                try {
                    dao.insertAuditLog(txId, row.txCode != null ? row.txCode : "", "APPROVED",
                            operatorName, operatorName, channel.auditPlatform,
                            "Transaction approved by " + operatorName + ", amount=" + row.amount);
                    auditWritten = true;
                } catch (Exception e) {
                    lastAuditErr = e;
                    if (attempt < 2) {
                        try { Thread.sleep(200L); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    }
                }
            }
            if (!auditWritten) {
                logger.error("DepositApprovalService.approve audit log FAILED after 3 attempts txId=" + txId,
                        lastAuditErr);
            }

            row.status = "APPROVED";
            return Result.approved(row, credited, promoApplied, bonusAmount, promoType);

        } catch (Exception e) {
            logger.error("DepositApprovalService.approve unexpected error txId=" + txId, e);
            return Result.internalError(e.getMessage());
        } finally {
            // Rollback the pick if approveTransaction never succeeded. Covers both
            // (a) approveTransaction returned false → cannotApprove, and (b) anything
            // else threw between pick and approve. Without this the row stays at
            // status=PROCESSING + locked_by=op forever and no admin can retry.
            if (weDidPick && !fullyApproved) {
                try {
                    dao.unlockTransaction(txId);
                    logger.warn("DepositApprovalService.approve rolled back stuck pick txId=" + txId
                            + " operator=" + operatorName);
                } catch (Exception e) {
                    logger.error("DepositApprovalService.approve rollback (DB) failed txId=" + txId, e);
                }
                try {
                    lockService.release(txId, operatorName);
                } catch (Exception e) {
                    logger.warn("DepositApprovalService.approve rollback (HZ) failed txId=" + txId, e);
                }
            }
        }
    }

    /**
     * Reject a PENDING / PROCESSING deposit. Auto-picks PENDING for the
     * same one-click UX as approve. No balance touched (deposit money
     * is only credited on approve).
     */
    public static Result reject(long txId, String operatorName, String reason, Channel channel) {
        if (reason == null) reason = "";
        try {
            DepositTransactionDao dao = new DepositTransactionDaoImpl();
            DepositLockService lockService = new DepositLockServiceImpl();

            Map<String, Object> tx = dao.getTransaction(txId);
            if (tx == null) return Result.notFound();
            TransactionRow row = mapRow(tx);

            // Lock check identical to approve.
            Map<String, Object> lockInfo = lockService.getLockInfo(txId);
            if (lockInfo != null) {
                String lockedBy = (String) lockInfo.get("operator");
                if (lockedBy != null && !lockedBy.equals(operatorName)) {
                    return Result.lockedByOther(row, lockedBy);
                }
            }

            if ("PENDING".equals(row.status)) {
                boolean picked = dao.pickTransaction(txId, operatorName, channel.auditPlatform);
                if (!picked) return Result.cannotPick(row);
                logger.info("DepositApprovalService.reject auto-picked txId=" + txId
                        + " by " + operatorName + " channel=" + channel.auditPlatform);
            }

            boolean rejected = dao.rejectTransaction(txId, operatorName, reason);
            if (!rejected) return Result.cannotReject(row);

            try {
                lockService.release(txId, operatorName);
            } catch (Exception e) {
                logger.error("DepositApprovalService.reject release lock failed txId=" + txId, e);
            }

            try {
                dao.insertAuditLog(txId, row.txCode != null ? row.txCode : "", "REJECTED",
                        operatorName, operatorName, channel.auditPlatform,
                        "Transaction rejected by " + operatorName + ", reason=" + reason);
            } catch (Exception e) {
                logger.error("DepositApprovalService.reject audit log failed txId=" + txId, e);
            }

            row.status = "REJECTED";
            return Result.rejected(row);

        } catch (Exception e) {
            logger.error("DepositApprovalService.reject unexpected error txId=" + txId, e);
            return Result.internalError(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Internals
    // ─────────────────────────────────────────────────────────────────────

    private static TransactionRow mapRow(Map<String, Object> tx) {
        TransactionRow r = new TransactionRow();
        r.id = numberAsLong(tx.get("id"));
        r.userId = numberAsLong(tx.get("user_id"));
        r.amount = numberAsLong(tx.get("amount"));
        r.nickName = (String) tx.get("nick_name");
        r.txCode = (String) tx.get("tx_code");
        r.status = (String) tx.get("status");
        r.bankName = (String) tx.get("user_bank_name");
        r.bankNumber = (String) tx.get("user_bank_number");
        // Lock-holder column is `locked_by` in the deposit_transactions
        // table. The vbee local DAO exposes it as DepositTx.operatorName,
        // which only adds confusion — the field on the row reflects who
        // currently holds the Hazelcast lock, not the original operator
        // who created the row.
        r.operatorName = (String) tx.get("locked_by");
        r.telegramMsgId = numberAsLong(tx.get("telegram_msg_id"));
        return r;
    }

    private static long numberAsLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { return 0L; }
    }

    /** Hazelcast-first, MySQL-fallback read of {@code users.vin}. Used
     *  to pick the volume-tracking branch (reset vs accumulate). */
    private static long readPreCreditBalance(long userId, String nickName) {
        try {
            IMap<String, UserCacheModel> userMap =
                    HazelcastClientFactory.getInstance().getMap("users");
            UserCacheModel uc = userMap.get(nickName);
            if (uc != null) return uc.getVin();
        } catch (Exception ignore) {
            // fall through to DB
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT vin FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("vin");
            }
        } catch (Exception e) {
            logger.warn("DepositApprovalService.readPreCreditBalance fallback failed userId=" + userId, e);
        }
        return 0L;
    }
}
