package com.vinplay.dal.service;

import com.vinplay.dal.rebate.RebateService;
import com.vinplay.dal.service.DepositPromotionService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.ledger.MoneyLedger;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.hazelcast.core.IMap;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MoneyGateway — the ONLY way to credit money to a user's game wallet.
 * All money-in sources MUST call this instead of direct DB updates.
 *
 * Centralizes: vin credit, recharge_money tracking, promotion evaluation,
 * commission (future), balance push (WebSocket + game server), audit logging.
 */
public class MoneyGateway {

    private static final Logger logger = Logger.getLogger("backend");

    // Feature flag: populated ONCE at class-load time from env (not on every call — perf).
    // Default OFF. Enable with MONEY_LEDGER_DUAL_WRITE=true env var + JVM restart.
    // Set to "false" for instant rollback without code changes.
    //
    // Volatile (not final) so integration tests can flip it via reflection to actually
    // exercise the OFF path. Production callers still see a value frozen at class-load
    // because no production code ever writes to this field — only the static initializer
    // below and (occasionally) a test does. The gate logic in creditUser reads this field
    // on every call, so the test's reflection toggle takes effect immediately.
    static volatile boolean DUAL_WRITE_ENABLED;

    static {
        DUAL_WRITE_ENABLED = "true".equalsIgnoreCase(
                System.getenv().getOrDefault("MONEY_LEDGER_DUAL_WRITE", "false"));
    }

    // Canonical source tags. Use these constants in callers instead of raw strings
    // so a typo becomes a compile error rather than a silent new audit category.
    public static final String SOURCE_DEPOSIT_BANK = "DEPOSIT_BANK";
    public static final String SOURCE_DEPOSIT_CRYPTO = "DEPOSIT_CRYPTO";
    public static final String SOURCE_DEPOSIT_TELEGRAM = "DEPOSIT_TELEGRAM";
    public static final String SOURCE_CARD_RECHARGE = "CARD_RECHARGE";
    public static final String SOURCE_ADMIN_TOPUP = "ADMIN_TOPUP";
    public static final String SOURCE_ADMIN_DEDUCT = "ADMIN_DEDUCT";
    public static final String SOURCE_AGENT_TOPUP = "AGENT_TOPUP";
    public static final String SOURCE_PROMO_BONUS = "PROMO_BONUS";
    public static final String SOURCE_CREDIT_WALLET_DEPOSIT = "CREDIT_WALLET_DEPOSIT";
    public static final String SOURCE_REFUND_WITHDRAW = "REFUND_WITHDRAW";
    // CONVERT_AGENCY_TO_VIN is a CREDIT source despite the verb "convert"
    // — see WithdrawAgencyWalletProcessor which calls creditUser(). Keep
    // it grouped with credit-side constants above the SUN-1200 debit
    // section below so the call direction is unambiguous.
    public static final String SOURCE_CONVERT_AGENCY_TO_VIN = "CONVERT_AGENCY_TO_VIN";
    // SUN-1200: bank/crypto withdraw deduct previously went through
    // UserServiceImpl.updateMoneyFromAdmin, which mutated the Hazelcast
    // cache without persisting `users.vin` to MySQL — only the async
    // RMQ consumer was supposed to sync the DB and that never landed
    // for these actions. Refund (REFUND_WITHDRAW) used MoneyGateway
    // .creditUser which DOES write the DB directly, so a debit-then-
    // refund cycle would leave `users.vin` strictly higher than before
    // (the refund credited DB by 100k while the debit only ever
    // touched the cache). KwonUSD was up 300k after 3 reject loops.
    // Routing the deduct through MoneyGateway.debitUser closes the
    // exploit — DB-first, dedup-keyed, atomic floor-checked.
    public static final String SOURCE_WITHDRAW_BANK = "WITHDRAW_BANK";
    public static final String SOURCE_WITHDRAW_CRYPTO = "WITHDRAW_CRYPTO";
    public static final String SOURCE_AWC_DEBIT = "AWC_DEBIT";
    public static final String SOURCE_AWC_CREDIT = "AWC_CREDIT";
    /**
     * GSC seamless wallet — bet/withdraw event. Used by the SeamlessWalletAggregator
     * refactor (see docs/SEAMLESS_WALLET_AGGREGATOR_DESIGN.md). Phase 1 places the
     * constant; first caller lands in Phase 3.
     *
     * TODO(Phase 3): add to {@link #mapDebitSourceToLedgerType} +
     * {@link #mapDebitSourceToSystemAccount} BEFORE the first GSC aggregator
     * subclass starts dual-writing — otherwise the dual-write helper logs a WARN
     * and silently skips, and the ledger drifts from the legacy table.
     */
    public static final String SOURCE_GSC_DEBIT  = "GSC_DEBIT";
    /**
     * GSC seamless wallet — settle/win/credit event. Same notes as SOURCE_GSC_DEBIT.
     *
     * TODO(Phase 3): add to {@link #mapSourceToLedgerType} +
     * {@link #mapSourceToSystemAccount} BEFORE first caller lands.
     */
    public static final String SOURCE_GSC_CREDIT = "GSC_CREDIT";
    /**
     * Compensating credit issued by {@link
     * com.vinplay.dal.service.seamless.gsc.GscStuckRowReconciler} for
     * GSC withdraw rows stuck at processing_status='RECEIVED' past the
     * timeout window. tx_id format: "stuck_refund_&lt;gsc_transaction_id&gt;".
     * Idempotent at the wallet layer via the unique (tx_id, source) constraint.
     */
    public static final String SOURCE_GSC_STUCK_REFUND = "GSC_STUCK_REFUND";
    public static final String SOURCE_SYSTEM_RECOVERY_RESET = "SYSTEM_RECOVERY_RESET";
    /**
     * SUN-1387 — player cashback claim. Routed through the gateway as of
     * 2026-05-18 after the legacy Hazelcast-lock + setVin pattern in
     * {@code ClaimCashbackProcessor} was found to silently mark
     * rebate_logs PAID without crediting users.vin when the player wasn't
     * in the {@code users} Hazelcast cache (26 players, 9.2M KRW owed at
     * audit time). tx_id format: {@code "rebate_claim_<nick>_<batchMs>"}
     * — idempotent against accidental double-click via the gateway's
     * unique (tx_id, source) constraint.
     */
    public static final String SOURCE_REBATE_CLAIM = "REBATE_CLAIM";
    /**
     * SUN-1387 — operator-initiated re-credit for the 26 players whose
     * SELF rebate claims hit the cache-miss bug. Distinct from
     * {@link #SOURCE_REBATE_CLAIM} so audit can tell normal player
     * claims from one-time recovery flows. tx_id format:
     * {@code "rebate_recovery_<nick>_<batch>"}.
     */
    public static final String SOURCE_REBATE_RECOVERY = "REBATE_RECOVERY";
    /**
     * User → user (or user → agent / agent → user) transfer through
     * {@link #transferBetweenUsers}. Symmetric source string used on BOTH the
     * src-side (negative-amount) and dest-side (positive-amount) audit rows,
     * so dedup on {@code (tx_id, source)} short-circuits a retry of either side.
     */
    public static final String SOURCE_INTER_USER_TRANSFER = "INTER_USER_TRANSFER";
    /**
     * SUN-1204: credit applied by {@code GscWagerReconciler} when GSC's
     * settle event was never delivered (Dream/Pragmatic/Evolution silent
     * push fail) and we discover the actual prize via the wager-detail
     * API. Dedup on {@code tx_id = wager_code} keeps automated retries
     * idempotent across both the scheduler and the one-shot manual
     * reconcile path.
     */
    public static final String SOURCE_GSC_RECONCILE = "GSC_RECONCILE";

    /**
     * Phase 2 — non-vin currency sources used by the migrated
     * MoneyInGameDaoImpl callers.  These exist on the credit path:
     *   - SAFE_FREEZE_DRAIN: agent-transfer freezes drain users.safe to the
     *     point where the freezable amount is exhausted; the legacy code set
     *     safe to its new absolute value via updateSafeMoney().
     *   - VIPPOINT_UPDATE: minigame/board-game vp accumulator updates
     *     users.vip_point + users.vip_point_save (both deltas) and money_vp
     *     (set absolute).  We route the money_vp side through the gateway;
     *     the vip_point* columns are NOT currencies and stay as direct SQL.
     */
    public static final String SOURCE_SAFE_FREEZE_DRAIN = "SAFE_FREEZE_DRAIN";
    public static final String SOURCE_VIPPOINT_UPDATE   = "VIPPOINT_UPDATE";
    public static final String SOURCE_RECHARGE_TOTAL_UPDATE = "RECHARGE_TOTAL_UPDATE";

    /**
     * Phase 2 unified vault sources. Used by {@link #lockFunds} / {@link #unlockFunds}
     * to move money between a player's main wallet (PLAYER_VIN) and their
     * vault (PLAYER_VAULT). Replaces:
     *   * Direct UPDATE of {@code users.safe} (legacy MySQL vault column)
     *   * Direct write to MongoDB collection {@code safe_box} (legacy shadow vault)
     * Both legacy storages are drained into PLAYER_VAULT by
     * {@code 20260512_phase2_safe_migration.sql}.
     */
    public static final String SOURCE_LOCK_FUND   = "LOCK_FUND";
    public static final String SOURCE_UNLOCK_FUND = "UNLOCK_FUND";
    /**
     * SUN-1235: every UserServiceImpl.updateMoney call (deposit/withdraw +
     * 16 game servers' win/loss flow). Differentiated from per-flow sources
     * (DEPOSIT_BANK / WITHDRAW_BANK / GSC_DEBIT / etc) because UserServiceImpl
     * doesn't have visibility into the flow's high-level source — the caller
     * passes serviceName for description but the canonical SOURCE_* must be
     * derived. Use this as the safe default when the caller's intent is
     * ambiguous (game wins/losses inside an offline game session).
     */
    public static final String SOURCE_USERSERVICE_GAME = "USERSERVICE_GAME";

    /**
     * SUN-1054 / Phase 5b — BanCa unified-wallet session settle sources.
     * Posted via c=9998 BanCaSettleProcessor (HTTP bridge from the C#
     * BanCa MoneyGatewayClient). Mapped to WAGER_DEBIT / WAGER_CREDIT
     * with HOUSE_GAME_POT as the counterparty.
     *
     * <p>{@link #SOURCE_WAGER_DEBIT_BANCA} — player lost the session (or
     * a periodic-flush slice of it); debit PLAYER_VIN.
     * <p>{@link #SOURCE_WAGER_CREDIT_BANCA} — player won the session; credit PLAYER_VIN.
     * <p>{@link #SOURCE_EMERGENCY_BANCA} — Revive crash-recovery debit
     * (collected per-player profit batch after a BanCa container restart).
     */
    public static final String SOURCE_WAGER_DEBIT_BANCA  = "WAGER_DEBIT_BANCA";
    public static final String SOURCE_WAGER_CREDIT_BANCA = "WAGER_CREDIT_BANCA";
    public static final String SOURCE_EMERGENCY_BANCA    = "EMERGENCY_BANCA";

    // Sources that trigger deposit promotions (first-deposit, daily-deposit)
    private static final Set<String> PROMO_SOURCES = new HashSet<>(Arrays.asList(
            SOURCE_DEPOSIT_BANK, SOURCE_DEPOSIT_CRYPTO, SOURCE_DEPOSIT_TELEGRAM,
            SOURCE_CARD_RECHARGE, SOURCE_CREDIT_WALLET_DEPOSIT
    ));

    // Sources that count as real deposits for recharge_money tracking
    private static final Set<String> DEPOSIT_SOURCES = new HashSet<>(Arrays.asList(
            SOURCE_DEPOSIT_BANK, SOURCE_DEPOSIT_CRYPTO, SOURCE_DEPOSIT_TELEGRAM,
            SOURCE_CARD_RECHARGE, SOURCE_ADMIN_TOPUP, SOURCE_AGENT_TOPUP,
            SOURCE_CREDIT_WALLET_DEPOSIT
    ));

    /**
     * Hazelcast cache update + balance push, shared by creditUser and debitUser.
     * Best-effort: failures here are logged but never propagate; the SQL write
     * is the system of record.
     *
     * <p>SUN-1248: was async-dispatched to a daemon executor to drop GSC
     * seamless-wallet latency from 595→128 ms. Reverted 2026-05-08: the lag
     * between SQL commit and the queue_action_minigame push (200-1000 ms)
     * caused offline-game bet responses (TaiXiu/SicBo betTaiXiu) to reach
     * the client before BalanceUpdateProcessor pushed the new wallet over
     * cmd=2003. The client UI was overwriting the freshly debited balance
     * on the lagging push. Re-introduce async only when the push can be
     * scoped to the latency-sensitive GSC/AWC seamless paths (via plumbed
     * source) — until then, sync is the safe contract.
     */
    private static void updateCacheAndPush(String nickname, long newBalance) {
        updateCacheAndPushSync(nickname, newBalance);
    }

    private static void updateCacheAndPushSync(String nickname, long newBalance) {
        try {
            IMap<String, UserCacheModel> userMap = HazelcastClientFactory.getInstance().getMap("users");
            if (userMap != null) {
                // Fast path: entry already cached → mutate in place under a short lock.
                // Fall-through path: entry absent, lock timeout, or null model → evict so
                // the next read canonically reloads from MySQL. Old code skipped silently
                // when containsKey was false — a credit committed to MySQL while the cache
                // held a stale (or missing) entry, and the player saw outdated balance
                // until a manual eviction (TuyenCo3939, 2026-05-10 23:49 +07).
                boolean updated = false;
                if (userMap.containsKey(nickname)) {
                    if (userMap.tryLock(nickname, 5, java.util.concurrent.TimeUnit.SECONDS)) {
                        try {
                            UserCacheModel uc = userMap.get(nickname);
                            if (uc != null) {
                                uc.setVin(newBalance);
                                userMap.put(nickname, uc, 1L, java.util.concurrent.TimeUnit.HOURS);
                                updated = true;
                            }
                        } finally {
                            userMap.unlock(nickname);
                        }
                    } else {
                        logger.warn("MoneyGateway: Hazelcast lock timeout for " + nickname
                                + " — falling back to evict");
                    }
                }
                if (!updated) {
                    try {
                        userMap.evict(nickname);
                    } catch (Exception evictErr) {
                        logger.warn("MoneyGateway: cache evict fallback failed for " + nickname
                                + ": " + evictErr.getMessage());
                    }
                }
            }
        } catch (Exception cacheErr) {
            logger.warn("MoneyGateway: Hazelcast cache update failed (SQL OK): " + cacheErr.getMessage());
        }
        publishBalanceUpdate(nickname);
    }

    /**
     * Single canonical entry point for "balance changed for this user — push
     * the new value to the FE WebSocket session". Fires notifications on
     * {@code queue_action_portal} (consumed by PortalBalanceConsumer →
     * BalanceWebSocketServlet) and {@code queue_action_minigame} (consumed
     * by per-game-server BalanceUpdateConsumer → game-side balance refresh).
     *
     * <p><b>Pattern contract:</b> every money mutation in the codebase MUST
     * end with a call to this method on success — no exceptions. The helper
     * is fire-and-forget; failures are logged at WARN and never propagate to
     * the caller because the underlying SQL write has already committed and
     * is the system of record.
     *
     * <p>The notification payload carries only the nickname; consumers re-read
     * the fresh balance from Hazelcast (or DB on cache miss) before pushing
     * the JSON frame to the WebSocket. So callers do NOT need to invalidate
     * the cache before calling — but they DO need to ensure their own cache
     * write has committed first, otherwise the consumer reads stale.
     *
     * <p>Existing call sites that should funnel through this helper:
     * <ul>
     *   <li>{@link #updateCacheAndPushSync} — GSC/AWC/online-user MoneyGateway path</li>
     *   <li>{@code UserServiceImpl.updateMoney} — minigame bet/win/refund path
     *     (BauCua, TaiXiu, TaiXiuMD5, PokeGo, ChatWorld, …)</li>
     *   <li>{@code BackendUtils.sendUpdateUserMoneyInfo} — admin processors
     *     (c=100 UpdateMoneyUser, ChuyenTienDaiLy, ResentBankSms, etc.)</li>
     *   <li>{@code DepositApprovalService}, {@code DepositTelegramPoller},
     *     {@code AwcCallbackProcessor} — deposit / seamless wallet credit paths</li>
     * </ul>
     */
    public static void publishBalanceUpdate(String nickname) {
        if (nickname == null || nickname.isEmpty()) return;
        try {
            java.util.ArrayList<String> list = new java.util.ArrayList<>();
            list.add(nickname);
            com.vinplay.vbee.common.messages.NotiGameMessage msg = createNotiMessage(list);
            MessageBusFactory.get("queue_action_minigame").publish("queue_action_minigame", msg, 1);
            MessageBusFactory.get("queue_action_portal").publish("queue_action_portal", msg, 1);
        } catch (Exception pushErr) {
            logger.warn("MoneyGateway.publishBalanceUpdate failed for " + nickname + ": " + pushErr.getMessage());
        }
    }

    /**
     * Multi-nickname overload for callers that change several wallets in one
     * operation (deposit-batch, agent transfer, etc.). Same fire-and-forget
     * contract as {@link #publishBalanceUpdate(String)}.
     */
    public static void publishBalanceUpdate(java.util.List<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) return;
        try {
            com.vinplay.vbee.common.messages.NotiGameMessage msg = createNotiMessage(nicknames);
            MessageBusFactory.get("queue_action_minigame").publish("queue_action_minigame", msg, 1);
            MessageBusFactory.get("queue_action_portal").publish("queue_action_portal", msg, 1);
        } catch (Exception pushErr) {
            logger.warn("MoneyGateway.publishBalanceUpdate(list) failed: " + pushErr.getMessage());
        }
    }

    public static class CreditResult {
        public static final String ERROR_DUPLICATE_TRANSACTION = "DUPLICATE_TRANSACTION";
        public static final String ERROR_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
        public static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
        public static final String ERROR_INTERNAL = "INTERNAL_ERROR";

        public boolean success;
        public long newBalance;
        public boolean promoApplied;
        public long bonusAmount;
        public String promoType;
        public String failureCode;
        public String error;

        public CreditResult(boolean success, long newBalance) {
            this.success = success;
            this.newBalance = newBalance;
        }

        public static CreditResult fail(String error) {
            CreditResult r = new CreditResult(false, 0);
            r.error = error;
            return r;
        }

        public static CreditResult fail(String failureCode, String error) {
            CreditResult r = fail(error);
            r.failureCode = failureCode;
            return r;
        }
    }

    /**
     * Result for {@link #creditUserWithCumulative}: extends {@link CreditResult}
     * with the cumulative-column post-write value so callers (UserServiceImpl)
     * can stamp both balance + total into the cache without a second SELECT.
     */
    public static class CreditResultWithCumulative extends CreditResult {
        public long newTotal;

        public CreditResultWithCumulative(boolean success, long newBalance, long newTotal) {
            super(success, newBalance);
            this.newTotal = newTotal;
        }

        public static CreditResultWithCumulative fail(String error) {
            CreditResultWithCumulative r = new CreditResultWithCumulative(false, 0, 0);
            r.error = error;
            return r;
        }
    }

    /**
     * SUN-1235: atomic credit/debit on (col, col_total) — single SQL UPDATE
     * with race-safe floor check on debit. Audit row in money_gateway_log.
     *
     * <p>Does NOT touch Hazelcast cache. Caller MUST hold the userMap lock
     * and stamp the returned newBalance/newTotal into the cache themselves.
     * This is the only canonical-allow gateway method that skips the cache
     * step — UserServiceImpl.updateMoney owns cross-field cache coherence
     * (VipPoint + balance + total in one Hazelcast tx) and a second cache
     * write here would race with that.
     *
     * <p>Replaces the legacy direct UPDATE in UserServiceImpl.updateMoney
     * (last entry on Dockerfile legacy_allow before this method landed).
     *
     * @param userId   users.id
     * @param nickname users.nick_name
     * @param col      MUST be "vin" since Phase 3a (Option A) collapsed xu → vin.
     *                 col_total is derived as col + "_total". The "xu" value is
     *                 rejected at runtime; callers passing it indicate a missed
     *                 Phase 3 migration site — fix the caller, do not re-enable
     *                 the branch.
     * @param delta    signed VND delta — negative = debit (with floor check
     *                 {@code col + delta >= 0})
     * @param source   {@link #SOURCE_*} constant
     * @param txId     idempotency key (unique with source); null OK
     * @param description audit log description
     * @return CreditResultWithCumulative — {@code success}, {@code newBalance},
     *         {@code newTotal}, {@code error} on failure
     */
    public static CreditResultWithCumulative creditUserWithCumulative(
            long userId, String nickname, String col, long delta,
            String source, String txId, String description) {
        if (delta == 0) return CreditResultWithCumulative.fail("Delta must be non-zero");
        if (nickname == null || nickname.isEmpty()) return CreditResultWithCumulative.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResultWithCumulative.fail("Source required");
        // Phase 3a (Option A): xu retired, collapsed into vin. Only "vin" is
        // accepted; callers passing "xu" hit a hard-fail so the unmigrated
        // site shows up in logs immediately rather than silently writing to a
        // dropped column.
        if (col == null || !col.equals("vin")) {
            return CreditResultWithCumulative.fail("col must be 'vin' (xu retired in Phase 3a)");
        }
        // SUN-13xx Phase 4 dropped users.vin_total / users.xu_total. Cumulative
        // P&L now lives in the ledger (v_derived_player_pnl). This method keeps
        // its signature for backward compatibility but writes ONLY the current-
        // balance column; the returned `newTotal` mirrors `newBalance` so
        // callers that still report a "totalCash" field get the live balance.

        try {
            if (txId != null && !txId.isEmpty() && isDuplicate(txId, source, userId)) {
                logger.warn("MoneyGateway.creditUserWithCumulative: duplicate txId=" + txId + " source=" + source);
                return CreditResultWithCumulative.fail("Duplicate transaction");
            }

            long newBalance = 0;
            long newTotal = 0;
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                // Atomic UPDATE on the live-balance column only. Debit gets a
                // floor check so vin can never go negative.
                String sql = (delta < 0)
                        ? "UPDATE users SET " + col + " = " + col + " + ? WHERE id = ? AND " + col + " + ? >= 0"
                        : "UPDATE users SET " + col + " = " + col + " + ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, delta);
                    ps.setLong(2, userId);
                    if (delta < 0) ps.setLong(3, delta);
                    if (ps.executeUpdate() == 0) {
                        try (PreparedStatement existPs = conn.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
                            existPs.setLong(1, userId);
                            try (ResultSet rs = existPs.executeQuery()) {
                                conn.rollback();
                                if (!rs.next()) return CreditResultWithCumulative.fail("User not found: id=" + userId);
                            }
                        }
                        return CreditResultWithCumulative.fail(delta < 0 ? "Insufficient balance" : "Update failed");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT " + col + " FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            newBalance = rs.getLong(1);
                            newTotal = newBalance; // vin_total dropped; mirror live balance
                        }
                    }
                }

                // Audit row INSIDE the transaction — UNIQUE(tx_id, source) is the dedup.
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, delta); // signed: negative for debit, positive for credit
                    ps.setString(4, source);
                    ps.setString(5, txId);
                    ps.setString(6, description);
                    ps.setLong(7, newBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.creditUserWithCumulative: race-dedup rollback txId=" + txId + " source=" + source);
                        return CreditResultWithCumulative.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            // NOTE: cache update intentionally skipped — UserServiceImpl owns
            // userMap.lock + setMoney/setTotalPnl + VipPoint cache coherence.

            // Phase 1 dual-write to money_ledger.
            if (DUAL_WRITE_ENABLED) {
                try {
                    if (delta < 0) {
                        dualWriteDebitToLedger(userId, nickname, -delta, source, txId, description);
                    } else {
                        dualWriteToLedger(userId, nickname, delta, source, txId, description);
                    }
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway.creditUserWithCumulative dual-write Throwable (legacy OK): user="
                            + nickname + " source=" + source + " delta=" + delta, ledgerErr);
                }
            }

            logger.info("MoneyGateway.creditUserWithCumulative OK: user=" + nickname
                    + " col=" + col + " delta=" + delta
                    + " newBalance=" + newBalance + " newTotal=" + newTotal);
            return new CreditResultWithCumulative(true, newBalance, newTotal);

        } catch (Exception e) {
            logger.error("MoneyGateway.creditUserWithCumulative error user=" + nickname, e);
            return CreditResultWithCumulative.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Credit money to a user's game wallet.
     *
     * @param userId      user ID
     * @param nickname    user nickname
     * @param amount      amount to credit (must be positive)
     * @param source      ADMIN_TOPUP, DEPOSIT_BANK, DEPOSIT_CRYPTO, DEPOSIT_TELEGRAM,
     *                    AGENT_TOPUP, CARD_RECHARGE, PROMO_BONUS
     * @param txId        transaction ID for dedup + audit (can be null for admin topup)
     * @param description human-readable description
     * @return CreditResult
     */
    public static CreditResult creditUser(long userId, String nickname, long amount,
                                           String source, String txId, String description) {
        if (amount <= 0) return CreditResult.fail("Amount must be positive");
        if (nickname == null || nickname.isEmpty()) return CreditResult.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResult.fail("Source required");

        logger.info("MoneyGateway.creditUser: user=" + nickname + " amount=" + amount
                + " source=" + source + " txId=" + txId);

        try {
            // === 1. Dedup check (if txId provided) — fast-path optimization ===
            // The SELECT below is racy on its own (two concurrent webhook retries
            // can BOTH pass it), but the authoritative dedup is the (tx_id, source)
            // UNIQUE constraint enforced inside the transactional block below.
            // Keeping the pre-check avoids a wasted UPDATE+rollback in the common
            // (already-seen) case where the duplicate is fully visible.
            if (txId != null && !txId.isEmpty()) {
                if (isDuplicate(txId, source, userId)) {
                    logger.warn("MoneyGateway: duplicate txId=" + txId + " source=" + source + " — skipping");
                    return CreditResult.fail("Duplicate transaction");
                }
            }

            // === 2. Credit vin + audit row INSIDE a single SQL transaction ===
            //   UPDATE users.vin            (mutates wallet)
            //   INSERT money_gateway_log    (audit row; UNIQUE(tx_id, source) is the dedup gate)
            // If the INSERT trips errno 1062 (duplicate key) — meaning a racing
            // call slipped past the SELECT-based pre-check above and committed
            // its audit row first — the wallet UPDATE is rolled back so neither
            // call double-credits.  This is the standard finance pattern: the
            // audit row is the source of truth for "did this happen", and if
            // the audit can't go through, the wallet movement is reversed.
            long newBalance = 0;
            boolean trackDeposit = DEPOSIT_SOURCES.contains(source);
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                // SUN-13xx Phase 7: recharge_money column dropped; deposit total now
                // derived from money_gateway_log via v_derived_deposit_total view.
                String sql = "UPDATE users SET vin = vin + ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int idx = 1;
                    ps.setLong(idx++, amount);
                    ps.setLong(idx, userId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return CreditResult.fail("User not found: id=" + userId);
                    }
                }
                // Read new balance inside the same transaction.
                try (PreparedStatement ps = conn.prepareStatement("SELECT vin FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) newBalance = rs.getLong("vin");
                    }
                }

                // Audit INSERT — uniqueness check is the race-safe dedup.
                // INSERT IGNORE turns errno 1062 into a 0-rowcount return so the
                // catch-and-rollback path below fires uniformly whether the
                // duplicate is detected here or by an earlier exception.
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, amount);
                    ps.setString(4, source);
                    ps.setString(5, txId);
                    ps.setString(6, description);
                    ps.setLong(7, newBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        // Race: another caller's audit row landed between our
                        // SELECT-based pre-check and this INSERT. Roll back the
                        // wallet UPDATE so the duplicate webhook does not
                        // double-credit.
                        conn.rollback();
                        logger.warn("MoneyGateway.creditUser: race-dedup rollback txId=" + txId + " source=" + source);
                        return CreditResult.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            updateCacheAndPush(nickname, newBalance);

            CreditResult result = new CreditResult(true, newBalance);

            // === 4. Deposit promotion (only for real deposits) ===
            if (PROMO_SOURCES.contains(source)) {
                try {
                    DepositPromotionService promoService = new DepositPromotionService();
                    String gate = sourceToGate(source);
                    // SUN-1171 follow-up: bank deposit txIds are numeric, but
                    // crypto txIds are 32-byte hex hashes (e.g.
                    // "fa7527a20f0cf4ea0..."). Long.parseLong threw on every
                    // crypto deposit, the inner catch swallowed it, and crypto
                    // deposits silently got no promo bonus. Parse defensively
                    // and fall back to 0 (used only as a tracer in the promo
                    // claim record — not a primary key).
                    long depositTxIdNumeric = 0L;
                    if (txId != null && !txId.isEmpty()) {
                        try { depositTxIdNumeric = Long.parseLong(txId); }
                        catch (NumberFormatException ignore) { /* non-numeric txId (crypto) → use 0 */ }
                    }
                    DepositPromotionService.BonusResult bonus = promoService.evaluate(
                            userId, nickname, depositTxIdNumeric, amount, gate);
                    if (bonus != null && bonus.bonusAmount > 0) {
                        // Credit the bonus (recursive call with PROMO_BONUS source — no promo on promo)
                        CreditResult bonusCr = creditUser(userId, nickname, bonus.bonusAmount, "PROMO_BONUS", null,
                                "Deposit promo bonus from " + source);
                        if (bonusCr.success) {
                            // Record the promo claim so hasFirstDepositClaim returns true next time
                            promoService.recordClaim(bonus);
                        }
                        result.promoApplied = true;
                        result.bonusAmount = bonus.bonusAmount;
                        result.promoType = bonus.promoType == 1 ? "FIRST_DEPOSIT" : "DAILY_DEPOSIT";
                        logger.info("MoneyGateway: promo applied user=" + nickname
                                + " type=" + result.promoType + " bonus=" + bonus.bonusAmount);
                    }
                } catch (Exception promoErr) {
                    logger.error("MoneyGateway: promotion evaluation FAILED user=" + nickname + " source=" + source, promoErr);
                }
            }

            // Audit row (money_gateway_log) is now written inside the transactional
            // block above so the (tx_id, source) UNIQUE constraint can race-protect
            // the wallet UPDATE. Do NOT call logTransaction here — that would write
            // a duplicate row when txId is null (which has no UNIQUE protection).

            // Phase 1 dual-write: also record in money_ledger (additive, behind feature flag).
            // On failure: log warning but DON'T fail the credit — legacy path is source of truth in Phase 1.
            // Defense-in-depth: dualWriteToLedger has its own try/catch for runtime errors,
            // but a class-load failure of MoneyLedger (NoClassDefFoundError, missing JDBC driver,
            // static-initializer failure) would bypass that internal catch and propagate as an
            // ExceptionInInitializerError / Error, killing the legacy credit return path.
            // Catch Throwable here so the legacy write is NEVER broken by ledger code.
            // Yes this swallows OOM / StackOverflow / ThreadDeath — accepted: legacy credit
            // is already committed, propagating an Error here would only abort our return.
            if (DUAL_WRITE_ENABLED) {
                try {
                    dualWriteToLedger(userId, nickname, amount, source, txId, description);
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write call site Throwable (legacy credit OK): userId="
                            + userId + " user=" + nickname + " source=" + source
                            + " amount=" + amount + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.creditUser OK: user=" + nickname + " newBalance=" + newBalance
                    + " promo=" + result.promoApplied);
            return result;

        } catch (Exception e) {
            logger.error("MoneyGateway.creditUser error user=" + nickname, e);
            return CreditResult.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Check for duplicate transaction.
     */
    private static boolean isDuplicate(String txId, String source, long userId) {
        // 2026-05-08: previously this query filtered only on (tx_id, source).
        // The actual UNIQUE constraint uk_tx_source is (tx_id, source, user_id,
        // currency) — INCLUDES user_id. UserServiceImpl.updateMoney builds
        // txId = "userservice:" + roundId, so for any round-keyed game (SicBo,
        // TaiXiu, BauCua, ...) the same txId is shared by every player in
        // that round. Without the user_id filter, the second player onward
        // hit a false-positive dup and every bet returned 1031 "Duplicate
        // transaction" — visible regression on staging where laviai's SicBo
        // bets all failed because another player got into the round first.
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM money_gateway_log WHERE tx_id = ? AND source = ? AND user_id = ? LIMIT 1")) {
            ps.setString(1, txId);
            ps.setString(2, source);
            ps.setLong(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            // Table may not exist yet — not fatal, skip dedup
            return false;
        }
    }

    /**
     * Log transaction for audit trail.
     */
    private static void logTransaction(long userId, String nickname, long amount,
                                        String source, String txId, String description, long balanceAfter) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
            ps.setLong(1, userId);
            ps.setString(2, nickname);
            ps.setLong(3, amount);
            ps.setString(4, source);
            ps.setString(5, txId);
            ps.setString(6, description);
            ps.setLong(7, balanceAfter);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("MoneyGateway: logTransaction failed (credit OK): " + e.getMessage());
        }
    }

    /**
     * Debit money from a user's game wallet — atomic floor-checked deduction.
     * Audit row written with NEGATIVE amount so SUM(amount) over money_gateway_log
     * yields net flow without needing a direction column.
     */
    public static CreditResult debitUser(long userId, String nickname, long amount,
                                          String source, String txId, String description) {
        if (amount <= 0) return CreditResult.fail("Amount must be positive");
        if (nickname == null || nickname.isEmpty()) return CreditResult.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResult.fail("Source required");

        logger.info("MoneyGateway.debitUser: user=" + nickname + " amount=" + amount
                + " source=" + source + " txId=" + txId);

        try {
            // Fast-path dedup check; the authoritative dedup is the
            // (tx_id, source) UNIQUE constraint enforced inside the
            // transactional block below.
            if (txId != null && !txId.isEmpty() && isDuplicate(txId, source, userId)) {
                logger.warn("MoneyGateway.debitUser: duplicate txId=" + txId + " source=" + source);
                return CreditResult.fail(CreditResult.ERROR_DUPLICATE_TRANSACTION, "Duplicate transaction");
            }

            long newBalance = 0;
            // UPDATE users.vin (atomic floor check) + INSERT audit row in a
            // single transaction. If the audit INSERT trips the unique key
            // (a racing call slipped past the SELECT pre-check), the debit
            // is rolled back so the duplicate webhook does not double-debit.
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET vin = vin - ? WHERE id = ? AND vin >= ?")) {
                    ps.setLong(1, amount);
                    ps.setLong(2, userId);
                    ps.setLong(3, amount);
                    if (ps.executeUpdate() == 0) {
                        // Disambiguate: missing user vs insufficient balance.
                        try (PreparedStatement existPs = conn.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
                            existPs.setLong(1, userId);
                            try (ResultSet rs = existPs.executeQuery()) {
                                conn.rollback();
                                if (!rs.next()) {
                                    return CreditResult.fail(CreditResult.ERROR_USER_NOT_FOUND,
                                            "User not found: id=" + userId);
                                }
                            }
                        }
                        return CreditResult.fail(CreditResult.ERROR_INSUFFICIENT_BALANCE, "Insufficient balance");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT vin FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        newBalance = rs.next() ? rs.getLong("vin") : 0;
                    }
                }

                // Audit row INSIDE the transaction — UNIQUE(tx_id, source) is
                // the race-safe dedup. INSERT IGNORE → 0 rowcount on duplicate
                // key, which we treat as "another caller already won; roll back
                // our debit and surface fail("Duplicate transaction")".
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, -amount); // debit recorded as NEGATIVE so SUM(amount) yields net flow
                    ps.setString(4, source);
                    ps.setString(5, txId);
                    ps.setString(6, description);
                    ps.setLong(7, newBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.debitUser: race-dedup rollback txId=" + txId + " source=" + source);
                        return CreditResult.fail(CreditResult.ERROR_DUPLICATE_TRANSACTION, "Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            updateCacheAndPush(nickname, newBalance);

            // Phase 1 dual-write: also record the debit in money_ledger (additive, behind feature flag).
            // On failure: log warning but DON'T fail the debit — legacy path is source of truth in Phase 1.
            // Defense-in-depth: dualWriteDebitToLedger has its own try/catch for runtime errors,
            // but a class-load failure of MoneyLedger (NoClassDefFoundError, missing JDBC driver,
            // static-initializer failure) would bypass that internal catch and propagate as an
            // ExceptionInInitializerError / Error, killing the legacy debit return path.
            // Catch Throwable here so the legacy write is NEVER broken by ledger code.
            // Yes this swallows OOM / StackOverflow / ThreadDeath — accepted: legacy debit
            // is already committed, propagating an Error here would only abort our return.
            if (DUAL_WRITE_ENABLED) {
                try {
                    dualWriteDebitToLedger(userId, nickname, amount, source, txId, description);
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write call site Throwable (legacy debit OK): userId="
                            + userId + " user=" + nickname + " source=" + source
                            + " amount=" + amount + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.debitUser OK: user=" + nickname + " newBalance=" + newBalance);
            return new CreditResult(true, newBalance);

        } catch (Exception e) {
            logger.error("MoneyGateway.debitUser error user=" + nickname, e);
            return CreditResult.fail(CreditResult.ERROR_INTERNAL, "Internal error: " + e.getMessage());
        }
    }

    /**
     * SUN-AWC-NEGATIVE-BALANCE: debit a user even when the result goes negative.
     *
     * <p>Same flow as {@link #debitUser} (audit row in money_gateway_log,
     * Hazelcast cache invalidation, money_ledger dual-write) — but the
     * UPDATE drops the {@code AND vin >= ?} floor check. Only AWC seamless
     * wallet's settle clawback / resettle-lose / voidSettle paths should call
     * this — the operator wallet is contractually allowed to owe the player
     * during reversal of a previously-paid win.
     *
     * <p>Do NOT use this for normal player-initiated debits.
     */
    public static CreditResult debitUserAllowNegative(long userId, String nickname, long amount,
                                                      String source, String txId, String description) {
        if (amount <= 0) return CreditResult.fail("Amount must be positive");
        if (nickname == null || nickname.isEmpty()) return CreditResult.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResult.fail("Source required");

        logger.info("MoneyGateway.debitUserAllowNegative: user=" + nickname + " amount=" + amount
                + " source=" + source + " txId=" + txId);

        try {
            if (txId != null && !txId.isEmpty() && isDuplicate(txId, source, userId)) {
                logger.warn("MoneyGateway.debitUserAllowNegative: duplicate txId=" + txId + " source=" + source);
                return CreditResult.fail("Duplicate transaction");
            }

            long newBalance = 0;
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET vin = vin - ? WHERE id = ?")) {
                    ps.setLong(1, amount);
                    ps.setLong(2, userId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return CreditResult.fail("User not found: id=" + userId);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT vin FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        newBalance = rs.next() ? rs.getLong("vin") : 0;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, -amount);
                    ps.setString(4, source);
                    ps.setString(5, txId);
                    ps.setString(6, description);
                    ps.setLong(7, newBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.debitUserAllowNegative: race-dedup rollback txId=" + txId + " source=" + source);
                        return CreditResult.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            updateCacheAndPush(nickname, newBalance);

            if (DUAL_WRITE_ENABLED) {
                try {
                    dualWriteDebitToLedger(userId, nickname, amount, source, txId, description);
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write call site Throwable (legacy debit OK): userId="
                            + userId + " user=" + nickname + " source=" + source
                            + " amount=" + amount + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.debitUserAllowNegative OK: user=" + nickname + " newBalance=" + newBalance);
            return new CreditResult(true, newBalance);

        } catch (Exception e) {
            logger.error("MoneyGateway.debitUserAllowNegative error user=" + nickname, e);
            return CreditResult.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Loyalty point increment — atomic UPDATE on vip_point + vip_point_save.
     * Wraps the legacy direct UPDATE that MoneyInGameDaoImpl.updateVippoint
     * was doing, so the call goes through the canonical gateway path.
     *
     * <p>vip_point / vip_point_save are loyalty counters, not money — no
     * insufficient-balance check, no ledger dual-write. Audit row in
     * money_gateway_log only (with amount = pointDelta as a tag).
     *
     * <p>SUN-13xx Phase 6: also dual-writes to {@code vip_points} +
     * {@code vip_point_log} via {@link VipPointsService} so the new
     * reader surface is consistent with legacy columns before the
     * 14-day drop migration runs. Failures in the dual-write side are
     * swallowed (legacy UPDATE remains system of record).
     *
     * @param userId          users.id
     * @param nickname        users.nick_name
     * @param pointDelta      delta to add to vip_point
     * @param pointSaveDelta  delta to add to vip_point_save (often == pointDelta but
     *                        differs for agent paths)
     * @return success true / false
     */
    public static boolean addLoyaltyPoints(long userId, String nickname, int pointDelta, int pointSaveDelta) {
        // SUN-13xx: VIP system retired. No-op kept for API compatibility — every
        // caller still compiles, but vip_point/vip_point_save are no longer
        // tracked anywhere. Returns true so success-path branches don't bail.
        return true;
    }

    /**
     * Recharge cumulative total — sets 0 to an absolute value.
     * Wraps the legacy direct UPDATE that UserDaoImpl.updateRechargeMoney was
     * doing. Pure tracking column (sum of historical deposits), not balance —
     * no audit row needed beyond the original deposit's money_gateway_log entry.
     *
     * @param nickname  users.nick_name (lookup key in legacy callers)
     * @param totalAmount  absolute value to set in recharge_money
     * @return success true / false
     */
    public static boolean updateRechargeTotal(String nickname, long totalAmount) {
        // SUN-13xx Phase 7: recharge_money column dropped. No-op for API compat.
        return true;
    }

    /**
     * Result of {@link #transferBetweenUsers}: extends {@link CreditResult} with the
     * destination user's post-transfer balance so callers can render both sides.
     * On failure {@code success=false}, both balances are 0, {@code error} is set.
     */
    public static class TransferResult extends CreditResult {
        /** Destination user's balance after a successful transfer; 0 on failure. */
        public long destNewBalance;

        public TransferResult(boolean success, long srcNewBalance, long destNewBalance) {
            super(success, srcNewBalance);
            this.destNewBalance = destNewBalance;
        }

        public static TransferResult fail(String error) {
            TransferResult r = new TransferResult(false, 0, 0);
            r.error = error;
            return r;
        }
    }

    /**
     * Atomically transfer vin from one user to another. Mirrors {@link #debitUser}
     * (atomic floor-checked debit on src) followed by an additive credit on dest,
     * audited as TWO money_gateway_log rows under the same {@code (tx_id, source)}
     * (negative on src, positive on dest) so {@code SUM(amount)} reconciles to 0.
     *
     * <p>If the dest credit fails, the src debit is refunded best-effort so the
     * transfer is atomic from the user's perspective.
     *
     * <p>Phase 1 dual-write: when {@code DUAL_WRITE_ENABLED} is true, also posts a
     * single ledger transaction with two entries (DEBIT src PLAYER_VIN, CREDIT dest
     * PLAYER_VIN). Idempotent on the same {@code (source, txId)}.
     *
     * @param srcUserId    source user id (vin debited)
     * @param srcNickname  source nickname (for cache push + audit)
     * @param destUserId   destination user id (vin credited)
     * @param destNickname destination nickname (for cache push + audit)
     * @param amount       positive amount in vin units
     * @param source       canonical source tag — typically {@link #SOURCE_INTER_USER_TRANSFER}
     * @param txId         transaction id for dedup + audit; required for idempotency
     * @param description  human-readable memo (used on both audit rows)
     * @return TransferResult with both balances, or fail() with an error string
     */
    public static TransferResult transferBetweenUsers(long srcUserId, String srcNickname,
                                                      long destUserId, String destNickname,
                                                      long amount, String source,
                                                      String txId, String description) {
        if (amount <= 0) return TransferResult.fail("Amount must be positive");
        if (srcNickname == null || srcNickname.isEmpty()) return TransferResult.fail("Source nickname required");
        if (destNickname == null || destNickname.isEmpty()) return TransferResult.fail("Destination nickname required");
        if (source == null || source.isEmpty()) return TransferResult.fail("Source required");
        if (srcUserId == destUserId) return TransferResult.fail("Cannot transfer to self");

        logger.info("MoneyGateway.transferBetweenUsers: src=" + srcNickname + " dest=" + destNickname
                + " amount=" + amount + " source=" + source + " txId=" + txId);

        try {
            // Dedup against either side of a previous attempt (same tx_id, source, srcUserId).
            // We key on the source user — a transfer is uniquely owned by who initiated it.
            if (txId != null && !txId.isEmpty() && isDuplicate(txId, source, srcUserId)) {
                logger.warn("MoneyGateway.transferBetweenUsers: duplicate txId=" + txId + " source=" + source);
                return TransferResult.fail("Duplicate transaction");
            }

            long srcNewBalance;
            long destNewBalance;
            // Wrap debit + credit + balance read in a single SQL transaction.
            // A JVM crash between steps would otherwise destroy money irrecoverably:
            // step 1's row-level commit can't be undone after step 2 fails. The
            // application-level "refund-on-dest-miss" branch only fires when dest
            // legitimately doesn't exist (rowcount=0, no exception); a thrown
            // exception (connection drop mid-statement, deadlock victim) needs
            // rollback() to undo step 1.
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);
                // Step 1: atomic floor-checked debit of src.
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET vin = vin - ? WHERE id = ? AND vin >= ?")) {
                    ps.setLong(1, amount);
                    ps.setLong(2, srcUserId);
                    ps.setLong(3, amount);
                    if (ps.executeUpdate() == 0) {
                        // Disambiguate: src missing vs insufficient balance.
                        try (PreparedStatement existPs = conn.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
                            existPs.setLong(1, srcUserId);
                            try (ResultSet rs = existPs.executeQuery()) {
                                conn.rollback();
                                if (!rs.next()) return TransferResult.fail("Source user not found: id=" + srcUserId);
                            }
                        }
                        return TransferResult.fail("Insufficient balance");
                    }
                }

                // Step 2: credit dest. If dest doesn't exist, rollback the whole tx so src is auto-refunded.
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET vin = vin + ? WHERE id = ?")) {
                    ps.setLong(1, amount);
                    ps.setLong(2, destUserId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        logger.warn("MoneyGateway.transferBetweenUsers: dest not found id=" + destUserId
                                + " — rolled back src debit for=" + srcNickname);
                        return TransferResult.fail("Destination user not found: id=" + destUserId);
                    }
                }

                // Read both new balances inside the same transaction.
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, vin FROM users WHERE id IN (?, ?)")) {
                    ps.setLong(1, srcUserId);
                    ps.setLong(2, destUserId);
                    long src = 0;
                    long dest = 0;
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            long vin = rs.getLong("vin");
                            if (id == srcUserId) src = vin;
                            else if (id == destUserId) dest = vin;
                        }
                    }
                    srcNewBalance = src;
                    destNewBalance = dest;
                }

                // Audit rows INSIDE the transaction — both share (tx_id, source)
                // but have different user_id, which is exactly what the
                // uk_tx_source(tx_id, source, user_id) UNIQUE key permits.
                // Negative on src, positive on dest, so SUM(amount) reconciles to 0.
                // A racing webhook retry of EITHER side hits a uniqueness conflict
                // (same tx_id+source+user_id), INSERT IGNORE returns 0 rowcount,
                // we roll back both wallet UPDATEs. Two callers cannot both produce
                // a transfer.
                String srcDesc = description != null ? description : ("Transfer to " + destNickname);
                String destDesc = description != null ? description : ("Transfer from " + srcNickname);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, srcUserId);
                    ps.setString(2, srcNickname);
                    ps.setLong(3, -amount);
                    ps.setString(4, source);
                    ps.setString(5, txId);
                    ps.setString(6, srcDesc);
                    ps.setLong(7, srcNewBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.transferBetweenUsers: race-dedup rollback (src side) txId="
                                + txId + " source=" + source);
                        return TransferResult.fail("Duplicate transaction");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, destUserId);
                    ps.setString(2, destNickname);
                    ps.setLong(3, amount);
                    ps.setString(4, source);
                    ps.setString(5, txId);
                    ps.setString(6, destDesc);
                    ps.setLong(7, destNewBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.transferBetweenUsers: race-dedup rollback (dest side) txId="
                                + txId + " source=" + source);
                        return TransferResult.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            // Cache push for both users (after commit; cache is best-effort).
            updateCacheAndPush(srcNickname, srcNewBalance);
            updateCacheAndPush(destNickname, destNewBalance);

            // Phase 1 dual-write: also record a single transfer transaction in money_ledger
            // (two entries: DEBIT src PLAYER_VIN, CREDIT dest PLAYER_VIN). Additive, behind feature flag.
            // On failure: log warning but DON'T fail the transfer — legacy path is source of truth in Phase 1.
            // Defense-in-depth: dualWriteTransferToLedger has its own try/catch for runtime errors,
            // but a class-load failure of MoneyLedger (NoClassDefFoundError, missing JDBC driver,
            // static-initializer failure) would bypass that internal catch and propagate as an
            // ExceptionInInitializerError / Error, killing the legacy transfer return path.
            // Catch Throwable here so the legacy write is NEVER broken by ledger code.
            // Yes this swallows OOM / StackOverflow / ThreadDeath — accepted: legacy transfer
            // is already committed, propagating an Error here would only abort our return.
            if (DUAL_WRITE_ENABLED) {
                try {
                    dualWriteTransferToLedger(srcUserId, srcNickname, destUserId, destNickname,
                            amount, source, txId, description);
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write call site Throwable (legacy transfer OK):"
                            + " srcUserId=" + srcUserId + " srcUser=" + srcNickname
                            + " destUserId=" + destUserId + " destUser=" + destNickname
                            + " source=" + source + " amount=" + amount + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.transferBetweenUsers OK: src=" + srcNickname + " srcBal=" + srcNewBalance
                    + " dest=" + destNickname + " destBal=" + destNewBalance);
            return new TransferResult(true, srcNewBalance, destNewBalance);

        } catch (Exception e) {
            logger.error("MoneyGateway.transferBetweenUsers error src=" + srcNickname + " dest=" + destNickname, e);
            return TransferResult.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Bulk restore vin = vin_total + reapply active FreezeMoneyTranferAgent
     * locks. Called on backend boot when Hazelcast `users` cache is empty
     * (BackendUtils.init). Writes a single SYSTEM_RECOVERY_RESET audit row.
     *
     * @return rows affected by the bulk vin restore, 0 on rollback.
     */
    public static int systemRecoveryReset(java.util.List<String> sessionBlockList) {
        // SUN-13xx: Phase 1 made vin_total / xu_total obsolete; Phase 4 dropped
        // those columns. This boot-time hook used to restore `vin = vin_total`
        // which is now impossible AND incorrect (the ledger PLAYER_VIN is the
        // source of truth, not a P&L counter). Reduced to clearing stale
        // freeze_money rows only — no wallet rewrite.
        StringBuilder ssBNotIn = new StringBuilder();
        if (sessionBlockList != null && !sessionBlockList.isEmpty()) {
            ssBNotIn.append("AND session_id NOT IN (");
            for (int i = 0; i < sessionBlockList.size(); ++i) ssBNotIn.append("?,");
            ssBNotIn.deleteCharAt(ssBNotIn.length() - 1).append(")");
        }
        int rowsAffected = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(
                    " UPDATE vinplay.freeze_money SET money = 0, status = 0 " +
                    " WHERE game_name <> 'FreezeMoneyTranferAgent' AND status = 1 " + ssBNotIn)) {
            int idx = 1;
            if (sessionBlockList != null) for (String s : sessionBlockList) stm.setString(idx++, s);
            rowsAffected = stm.executeUpdate();
        } catch (Exception e) {
            logger.error("MoneyGateway.systemRecoveryReset (freeze_money clear) failed", e);
        }
        logger.info("MoneyGateway.systemRecoveryReset OK (freeze_money only) rowsAffected=" + rowsAffected);
        return rowsAffected;
    }

    private static com.vinplay.vbee.common.messages.NotiGameMessage createNotiMessage(java.util.List<String> nicknames) {
        com.vinplay.vbee.common.messages.NotiGameMessage msg = new com.vinplay.vbee.common.messages.NotiGameMessage();
        msg.nicknames = nicknames;
        return msg;
    }

    private static String sourceToGate(String source) {
        switch (source) {
            case "DEPOSIT_BANK": return "BANK";
            case "DEPOSIT_CRYPTO": return "CRYPTO";
            case "CARD_RECHARGE": return "CARD";
            case "CREDIT_WALLET_DEPOSIT": return "ALL"; // nhận tất cả KM không giới hạn cổng
            default: return "ALL";
        }
    }

    // =========================================================================
    // Phase 2 — multi-currency credit/debit/setAbsolute
    //
    // The legacy creditUser/debitUser methods only ever touch users.vin.  These
    // generalised entry points cover xu, safe, and money_vp without changing
    // the existing 30+ vin call sites (those keep calling creditUser/debitUser).
    //
    // Design notes:
    //   - Currency is validated against ALLOWED_CURRENCIES at the top of every
    //     method.  An invalid currency returns CreditResult.fail("Unknown
    //     currency: ...") — there is NO path where an attacker-controlled string
    //     reaches the SQL.  The internal column-name lookup is a switch on the
    //     pre-validated value, so the SQL is built from a fixed set of literals.
    //   - The legacy `safe` column is set-absolute in every existing caller
    //     (see MoneyInGameDaoImpl.updateSafeMoney) — we expose that as a
    //     separate `setSafeAbsolute` method rather than overloading the
    //     credit/debit semantics.  money_vp is also set-absolute in vippoint
    //     paths but mixed with delta-style vp/vp_save updates; we expose
    //     `setMoneyVpAbsolute` for that specific shape.
    //   - Audit rows go to the same money_gateway_log table with a `currency`
    //     column added by 2026_05_02_money_gateway_log_currency_column.sql.
    //     The UNIQUE key was extended to (tx_id, source, user_id, currency) so
    //     a single deposit can credit multiple currencies under one external
    //     txId without triggering false-positive dedup.
    //   - Ledger dual-write maps vin/xu/safe/money_vp →
    //     PLAYER_VIN/PLAYER_XU/PLAYER_SAFE/PLAYER_VP money_account types.
    // =========================================================================

    /**
     * Currencies accepted by creditCurrency/debitCurrency/setSafeAbsolute.
     * <p>Phase 3a (Option A) removed "xu" — it now collapses into "vin" at
     * the SQL layer. Any caller still passing currency="xu" gets a hard
     * "Unknown currency" failure so the unmigrated callsite surfaces in
     * logs.
     */
    private static final Set<String> ALLOWED_CURRENCIES = new HashSet<>(Arrays.asList(
            "vin", "safe", "money_vp"));

    /**
     * Map MoneyGateway currency tag → {@code users} table column name. The
     * value is taken from a fixed dictionary, never from user input — once
     * {@link #ALLOWED_CURRENCIES} validates the parameter, this lookup is
     * effectively a compile-time constant.
     *
     * @return the column name, or null if currency was not pre-validated
     */
    private static String currencyToColumn(String currency) {
        if (currency == null) return null;
        switch (currency) {
            case "vin":      return "vin";
            // Phase 3a: "xu" intentionally absent — column `users.xu` is dropped
            // in 20260615_phase3_drop_users_xu.sql. Returning null forces an
            // explicit "Unknown currency" error path in callers.
            case "safe":     return "safe";
            case "money_vp": return "money_vp";
            default:         return null;
        }
    }

    /**
     * Map MoneyGateway currency tag → MoneyLedger {@code account_type}.
     */
    static String currencyToLedgerAccountType(String currency) {
        if (currency == null) return null;
        switch (currency) {
            case "vin":      return "PLAYER_VIN";
            // Phase 3a (Option A): xu retired. The `PLAYER_XU` ledger account
            // remains in money_account as historical record (balance = 0 after
            // 20260601_phase3a migration) but no new entries are posted to it.
            case "safe":     return "PLAYER_SAFE";
            case "money_vp": return "PLAYER_VP";
            default:         return null;
        }
    }

    /**
     * Credit any of the 4 supported player currencies.  vin currency just
     * delegates to {@link #creditUser} so the existing deposit-promotion +
     * recharge_money tracking still fires.  Other currencies follow the
     * race-safe transactional pattern (UPDATE + INSERT IGNORE audit row, with
     * the currency column included in the UNIQUE dedup key) and skip the
     * promo path which is vin-only by design.
     *
     * @param userId      user ID
     * @param nickname    user nickname
     * @param currency    one of {@code Consts.MONEY_VIN/MONEY_XU/MONEY_SAFE/MONEY_VP}
     * @param amount      positive amount to add to the balance
     * @param source      canonical source tag (e.g. ADMIN_TOPUP)
     * @param txId        transaction id for dedup + audit; nullable
     * @param description human-readable memo
     */
    public static CreditResult creditCurrency(long userId, String nickname, String currency,
                                              long amount, String source, String txId, String description) {
        if (currency == null || !ALLOWED_CURRENCIES.contains(currency)) {
            return CreditResult.fail("Unknown currency: " + currency);
        }
        // Delegate the vin path to the original method so its bespoke
        // promotion/recharge_money/Hazelcast logic runs unchanged.
        if ("vin".equals(currency)) {
            return creditUser(userId, nickname, amount, source, txId, description);
        }

        if (amount <= 0) return CreditResult.fail("Amount must be positive");
        if (nickname == null || nickname.isEmpty()) return CreditResult.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResult.fail("Source required");

        String column = currencyToColumn(currency);
        // currencyToColumn cannot return null after the ALLOWED_CURRENCIES gate,
        // but be defensive against future refactors.
        if (column == null) return CreditResult.fail("Unknown currency: " + currency);

        logger.info("MoneyGateway.creditCurrency: user=" + nickname + " currency=" + currency
                + " amount=" + amount + " source=" + source + " txId=" + txId);

        try {
            if (txId != null && !txId.isEmpty() && isDuplicateForCurrency(txId, source, currency)) {
                logger.warn("MoneyGateway.creditCurrency: duplicate txId=" + txId
                        + " source=" + source + " currency=" + currency + " — skipping");
                return CreditResult.fail("Duplicate transaction");
            }

            long newBalance = 0;
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                // Column name is from a closed dictionary — safe to interpolate.
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET " + column + " = " + column + " + ? WHERE id = ?")) {
                    ps.setLong(1, amount);
                    ps.setLong(2, userId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return CreditResult.fail("User not found: id=" + userId);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT " + column + " FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) newBalance = rs.getLong(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, amount);
                    ps.setString(4, currency);
                    ps.setString(5, source);
                    ps.setString(6, txId);
                    ps.setString(7, description);
                    ps.setLong(8, newBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.creditCurrency: race-dedup rollback txId=" + txId
                                + " source=" + source + " currency=" + currency);
                        return CreditResult.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            // Phase 1 dual-write parity for non-vin currencies.
            if (DUAL_WRITE_ENABLED) {
                try {
                    dualWriteCurrencyCreditToLedger(userId, nickname, currency, amount,
                            source, txId, description);
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write currency-credit Throwable (legacy OK): userId="
                            + userId + " user=" + nickname + " currency=" + currency
                            + " source=" + source + " amount=" + amount + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.creditCurrency OK: user=" + nickname + " currency=" + currency
                    + " newBalance=" + newBalance);
            return new CreditResult(true, newBalance);

        } catch (Exception e) {
            logger.error("MoneyGateway.creditCurrency error user=" + nickname
                    + " currency=" + currency, e);
            return CreditResult.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Debit any of the 4 supported player currencies with an atomic
     * floor-checked deduction.  Vin delegates to {@link #debitUser}; the rest
     * follow the same race-safe pattern as creditCurrency.  Audit row uses
     * NEGATIVE amount so SUM(amount) GROUP BY currency yields net flow.
     */
    public static CreditResult debitCurrency(long userId, String nickname, String currency,
                                             long amount, String source, String txId, String description) {
        if (currency == null || !ALLOWED_CURRENCIES.contains(currency)) {
            return CreditResult.fail("Unknown currency: " + currency);
        }
        if ("vin".equals(currency)) {
            return debitUser(userId, nickname, amount, source, txId, description);
        }

        if (amount <= 0) return CreditResult.fail("Amount must be positive");
        if (nickname == null || nickname.isEmpty()) return CreditResult.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResult.fail("Source required");

        String column = currencyToColumn(currency);
        if (column == null) return CreditResult.fail("Unknown currency: " + currency);

        logger.info("MoneyGateway.debitCurrency: user=" + nickname + " currency=" + currency
                + " amount=" + amount + " source=" + source + " txId=" + txId);

        try {
            if (txId != null && !txId.isEmpty() && isDuplicateForCurrency(txId, source, currency)) {
                logger.warn("MoneyGateway.debitCurrency: duplicate txId=" + txId
                        + " source=" + source + " currency=" + currency);
                return CreditResult.fail("Duplicate transaction");
            }

            long newBalance = 0;
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET " + column + " = " + column + " - ? WHERE id = ? AND "
                        + column + " >= ?")) {
                    ps.setLong(1, amount);
                    ps.setLong(2, userId);
                    ps.setLong(3, amount);
                    if (ps.executeUpdate() == 0) {
                        try (PreparedStatement existPs = conn.prepareStatement("SELECT 1 FROM users WHERE id = ?")) {
                            existPs.setLong(1, userId);
                            try (ResultSet rs = existPs.executeQuery()) {
                                conn.rollback();
                                if (!rs.next()) return CreditResult.fail("User not found: id=" + userId);
                            }
                        }
                        return CreditResult.fail("Insufficient balance");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT " + column + " FROM users WHERE id = ?")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        newBalance = rs.next() ? rs.getLong(1) : 0;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, -amount);
                    ps.setString(4, currency);
                    ps.setString(5, source);
                    ps.setString(6, txId);
                    ps.setString(7, description);
                    ps.setLong(8, newBalance);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.debitCurrency: race-dedup rollback txId=" + txId
                                + " source=" + source + " currency=" + currency);
                        return CreditResult.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            if (DUAL_WRITE_ENABLED) {
                try {
                    dualWriteCurrencyDebitToLedger(userId, nickname, currency, amount,
                            source, txId, description);
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write currency-debit Throwable (legacy OK): userId="
                            + userId + " user=" + nickname + " currency=" + currency
                            + " source=" + source + " amount=" + amount + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.debitCurrency OK: user=" + nickname + " currency=" + currency
                    + " newBalance=" + newBalance);
            return new CreditResult(true, newBalance);

        } catch (Exception e) {
            logger.error("MoneyGateway.debitCurrency error user=" + nickname
                    + " currency=" + currency, e);
            return CreditResult.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Set a currency to an absolute value (for callers that pass the new value
     * pre-computed from cache state, e.g. legacy {@code updateSafeMoney}).
     * The audit row records the DELTA from the previous balance so SUM(amount)
     * still reconciles to the column.
     *
     * <p>Only meaningful for {@code safe} and {@code money_vp} today —
     * {@code vin} should use creditCurrency/debitCurrency to preserve its
     * delta semantics. {@code xu} retired in Phase 3a (Option A).
     */
    public static CreditResult setCurrencyAbsolute(long userId, String nickname, String currency,
                                                   long newValue, String source, String txId, String description) {
        if (currency == null || !ALLOWED_CURRENCIES.contains(currency)) {
            return CreditResult.fail("Unknown currency: " + currency);
        }
        if (nickname == null || nickname.isEmpty()) return CreditResult.fail("Nickname required");
        if (source == null || source.isEmpty()) return CreditResult.fail("Source required");
        if (newValue < 0) return CreditResult.fail("New value must be non-negative");

        String column = currencyToColumn(currency);
        if (column == null) return CreditResult.fail("Unknown currency: " + currency);

        logger.info("MoneyGateway.setCurrencyAbsolute: user=" + nickname + " currency=" + currency
                + " newValue=" + newValue + " source=" + source + " txId=" + txId);

        try {
            if (txId != null && !txId.isEmpty() && isDuplicateForCurrency(txId, source, currency)) {
                logger.warn("MoneyGateway.setCurrencyAbsolute: duplicate txId=" + txId
                        + " source=" + source + " currency=" + currency);
                return CreditResult.fail("Duplicate transaction");
            }

            long oldBalance = 0;
            long delta;
            Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            try {
                conn.setAutoCommit(false);

                // Read old balance INSIDE the transaction so the delta is
                // computed against the row state we're about to overwrite.
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT " + column + " FROM users WHERE id = ? FOR UPDATE")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return CreditResult.fail("User not found: id=" + userId);
                        }
                        oldBalance = rs.getLong(1);
                    }
                }
                delta = newValue - oldBalance;

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET " + column + " = ? WHERE id = ?")) {
                    ps.setLong(1, newValue);
                    ps.setLong(2, userId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return CreditResult.fail("User not found: id=" + userId);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO money_gateway_log (user_id, nick_name, amount, currency, source, tx_id, description, balance_after, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, nickname);
                    ps.setLong(3, delta);
                    ps.setString(4, currency);
                    ps.setString(5, source);
                    ps.setString(6, txId);
                    ps.setString(7, description);
                    ps.setLong(8, newValue);
                    int audited = ps.executeUpdate();
                    if (audited == 0 && txId != null && !txId.isEmpty()) {
                        conn.rollback();
                        logger.warn("MoneyGateway.setCurrencyAbsolute: race-dedup rollback txId=" + txId
                                + " source=" + source + " currency=" + currency);
                        return CreditResult.fail("Duplicate transaction");
                    }
                }

                conn.commit();
            } catch (Exception txErr) {
                try { conn.rollback(); } catch (Exception ignore) {}
                throw txErr;
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
                try { conn.close(); } catch (Exception ignore) {}
            }

            // Dual-write the DELTA as a credit or debit to keep the ledger
            // consistent with the legacy column.  No-op when delta == 0.
            if (DUAL_WRITE_ENABLED && delta != 0) {
                try {
                    if (delta > 0) {
                        dualWriteCurrencyCreditToLedger(userId, nickname, currency, delta,
                                source, txId, description);
                    } else {
                        dualWriteCurrencyDebitToLedger(userId, nickname, currency, -delta,
                                source, txId, description);
                    }
                } catch (Throwable ledgerErr) {
                    logger.error("MoneyGateway dual-write set-absolute Throwable (legacy OK): userId="
                            + userId + " user=" + nickname + " currency=" + currency
                            + " source=" + source + " delta=" + delta + " txId=" + txId, ledgerErr);
                }
            }

            logger.info("MoneyGateway.setCurrencyAbsolute OK: user=" + nickname + " currency="
                    + currency + " old=" + oldBalance + " new=" + newValue + " delta=" + delta);
            return new CreditResult(true, newValue);

        } catch (Exception e) {
            logger.error("MoneyGateway.setCurrencyAbsolute error user=" + nickname
                    + " currency=" + currency, e);
            return CreditResult.fail("Internal error: " + e.getMessage());
        }
    }

    /**
     * Currency-aware dedup pre-check: with the {@code currency} column added
     * to {@code money_gateway_log}, the same {@code (tx_id, source, user_id)}
     * can legitimately appear once per currency.  Filter the SELECT by
     * currency too so a vin row doesn't shadow a separate xu credit.
     */
    private static boolean isDuplicateForCurrency(String txId, String source, String currency) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM money_gateway_log WHERE tx_id = ? AND source = ? AND currency = ? LIMIT 1")) {
            ps.setString(1, txId);
            ps.setString(2, source);
            ps.setString(3, currency);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dual-write a non-vin currency credit to the ledger.  Vin credits use
     * the existing {@link #dualWriteToLedger} via creditUser; this method is
     * only reached for xu/safe/money_vp.
     */
    private static void dualWriteCurrencyCreditToLedger(long userId, String nickname, String currency,
                                                        long amount, String source, String txId,
                                                        String description) {
        try {
            String txType = mapSourceToLedgerType(source);
            if (txType == null) {
                logger.warn("MoneyGateway dual-write currency-credit: no ledger type for source="
                        + source + " currency=" + currency);
                return;
            }
            String accountType = currencyToLedgerAccountType(currency);
            Long playerAccId = MoneyLedger.findPlayerAccount(userId, accountType);
            if (playerAccId == null) {
                logger.warn("MoneyGateway dual-write currency-credit: no " + accountType
                        + " account for userId=" + userId);
                return;
            }
            String systemAcctType = mapSourceToSystemAccount(source);
            Long systemAccId = MoneyLedger.findSystemAccount(systemAcctType);
            if (systemAccId == null) {
                logger.warn("MoneyGateway dual-write currency-credit: no system account for type="
                        + systemAcctType);
                return;
            }
            String externalRef = (txId != null && !txId.isEmpty())
                    ? txId + ":" + currency  // disambiguate per-currency posts under same txId
                    : "mgw:" + System.nanoTime();

            MoneyLedger.LedgerResult result = MoneyLedger.credit(
                    playerAccId, systemAccId, amount,
                    txType, externalRef,
                    description != null ? description : "MoneyGateway " + source + "/" + currency,
                    null);

            if (result.status == MoneyLedger.Status.POSTED || result.status == MoneyLedger.Status.DUPLICATE) {
                logger.info("MoneyGateway dual-write currency-credit OK: currency=" + currency
                        + " ref=" + externalRef + " status=" + result.status);
            } else {
                logger.error("MoneyGateway dual-write currency-credit FAILED: status=" + result.status
                        + " userId=" + userId + " currency=" + currency + " amount=" + amount
                        + " source=" + source + " txId=" + txId
                        + (result.errorMessage != null ? " err=" + result.errorMessage : ""));
            }
        } catch (Exception e) {
            logger.error("MoneyGateway dual-write currency-credit threw: userId=" + userId
                    + " currency=" + currency + " amount=" + amount + " source=" + source
                    + " txId=" + txId, e);
        }
    }

    private static void dualWriteCurrencyDebitToLedger(long userId, String nickname, String currency,
                                                       long amount, String source, String txId,
                                                       String description) {
        try {
            // Try debit-source map first; fall back to credit-source map for
            // ambidextrous sources (e.g. ADMIN_TOPUP can be either direction).
            String txType = mapDebitSourceToLedgerType(source);
            String systemAcctType = mapDebitSourceToSystemAccount(source);
            if (txType == null) {
                txType = mapSourceToLedgerType(source);
                systemAcctType = mapSourceToSystemAccount(source);
            }
            if (txType == null) {
                logger.warn("MoneyGateway dual-write currency-debit: no ledger type for source="
                        + source + " currency=" + currency);
                return;
            }
            String accountType = currencyToLedgerAccountType(currency);
            Long playerAccId = MoneyLedger.findPlayerAccount(userId, accountType);
            if (playerAccId == null) {
                logger.warn("MoneyGateway dual-write currency-debit: no " + accountType
                        + " account for userId=" + userId);
                return;
            }
            Long systemAccId = MoneyLedger.findSystemAccount(systemAcctType);
            if (systemAccId == null) {
                logger.warn("MoneyGateway dual-write currency-debit: no system account for type="
                        + systemAcctType);
                return;
            }
            String externalRef = (txId != null && !txId.isEmpty())
                    ? txId + ":" + currency
                    : "mgw:" + System.nanoTime();

            MoneyLedger.LedgerResult result = MoneyLedger.debit(
                    playerAccId, systemAccId, amount,
                    txType, externalRef,
                    description != null ? description : "MoneyGateway " + source + "/" + currency,
                    null);

            if (result.status == MoneyLedger.Status.POSTED || result.status == MoneyLedger.Status.DUPLICATE) {
                logger.info("MoneyGateway dual-write currency-debit OK: currency=" + currency
                        + " ref=" + externalRef + " status=" + result.status);
            } else {
                logger.error("MoneyGateway dual-write currency-debit FAILED: status=" + result.status
                        + " userId=" + userId + " currency=" + currency + " amount=" + amount
                        + " source=" + source + " txId=" + txId
                        + (result.errorMessage != null ? " err=" + result.errorMessage : ""));
            }
        } catch (Exception e) {
            logger.error("MoneyGateway dual-write currency-debit threw: userId=" + userId
                    + " currency=" + currency + " amount=" + amount + " source=" + source
                    + " txId=" + txId, e);
        }
    }

    // =========================================================================
    // Phase 1 dual-write helpers
    // =========================================================================

    /**
     * Write a corresponding ledger row after a successful legacy credit.
     * CRITICAL: any exception is caught and logged — must never propagate to the
     * legacy credit caller. The legacy path is the source of truth in Phase 1.
     */
    private static void dualWriteToLedger(long userId, String nickname, long amount,
                                           String source, String txId, String description) {
        try {
            // SUN-13xx: bots are house-side liquidity, not real money. Skip dual-write.
            if (isBotUser(userId)) {
                return;
            }

            // Map MoneyGateway source → MoneyLedger transaction_type
            String txType = mapSourceToLedgerType(source);
            if (txType == null) {
                logger.warn("MoneyGateway dual-write: no ledger type for source=" + source);
                return;
            }

            // Resolve player VIN account
            Long playerAccId = MoneyLedger.findPlayerAccount(userId, "PLAYER_VIN");
            if (playerAccId == null) {
                logger.warn("MoneyGateway dual-write: no PLAYER_VIN account for userId=" + userId);
                return;
            }

            // Resolve counterparty system account
            String systemAcctType = mapSourceToSystemAccount(source);
            Long systemAccId = MoneyLedger.findSystemAccount(systemAcctType);
            if (systemAccId == null) {
                logger.warn("MoneyGateway dual-write: no system account for type=" + systemAcctType);
                return;
            }

            // Idempotent external_ref: use txId when available, otherwise synthesise one from nanoTime
            String externalRef = (txId != null && !txId.isEmpty()) ? txId : "mgw:" + System.nanoTime();

            // Post the credit: DEBIT system account, CREDIT player account
            MoneyLedger.LedgerResult result = MoneyLedger.credit(
                    playerAccId, systemAccId, amount,
                    txType, externalRef,
                    description != null ? description : "MoneyGateway " + source,
                    null  // metadata; could be enriched with {source, originalTxId} in future
            );

            if (result.status == MoneyLedger.Status.POSTED) {
                logger.info("MoneyGateway dual-write OK: type=" + txType + " amount=" + amount
                        + " ref=" + externalRef + " ledger_tx_id=" + result.transactionId);
            } else if (result.status == MoneyLedger.Status.DUPLICATE) {
                // Already in ledger — fine, the write is idempotent
                logger.debug("MoneyGateway dual-write duplicate (already in ledger): ref=" + externalRef);
            } else {
                // Insufficient balance / frozen / error — log but don't fail the credit.
                // Include all 5 fields so an operator can find the divergent legacy row.
                logger.error("MoneyGateway dual-write FAILED: status=" + result.status
                        + " userId=" + userId + " user=" + nickname
                        + " amount=" + amount + " source=" + source + " txId=" + txId
                        + (result.errorMessage != null ? " err=" + result.errorMessage : ""));
            }
        } catch (Exception e) {
            // CRITICAL: dual-write failures must NOT propagate to the legacy credit caller.
            logger.error("MoneyGateway dual-write threw: userId=" + userId + " user=" + nickname
                    + " amount=" + amount + " source=" + source + " txId=" + txId, e);
        }
    }

    /**
     * Map a MoneyGateway source string to a MoneyLedger transaction_type.
     * Returns null for unmapped sources (dual-write will be skipped with a warning).
     * Covers all values in DEPOSIT_SOURCES and PROMO_SOURCES plus known extras.
     */
    /**
     * SUN-13xx: cheap is_bot lookup so dual-write can short-circuit on bot
     * traffic. Bots are house-side liquidity, not real money — they should
     * not pollute the player ledger. 30-second TTL cache to avoid hammering
     * the DB on the hot game path.
     */
    private static final java.util.concurrent.ConcurrentHashMap<Long, Long> BOT_CACHE
            = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Long, Boolean> BOT_FLAG
            = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long BOT_CACHE_TTL_MS = 30_000L;

    static boolean isBotUser(long userId) {
        if (userId <= 0) return false;
        long now = System.currentTimeMillis();
        Long ts = BOT_CACHE.get(userId);
        if (ts != null && (now - ts) < BOT_CACHE_TTL_MS) {
            Boolean cached = BOT_FLAG.get(userId);
            if (cached != null) return cached;
        }
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement("SELECT is_bot FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean isBot = rs.next() && rs.getInt(1) == 1;
                BOT_FLAG.put(userId, isBot);
                BOT_CACHE.put(userId, now);
                return isBot;
            }
        } catch (Exception e) {
            // Fail-open: on error, treat as non-bot so real users are not silently skipped.
            logger.warn("MoneyGateway.isBotUser lookup failed userId=" + userId + ": " + e.getMessage());
            return false;
        }
    }

    static String mapSourceToLedgerType(String source) {
        if (source == null) return null;
        switch (source) {
            case "DEPOSIT_BANK":           return "DEPOSIT_BANK";
            case "DEPOSIT_TELEGRAM":       return "DEPOSIT_BANK";   // unified under DEPOSIT_BANK in ledger
            case "DEPOSIT_CRYPTO":         return "DEPOSIT_CRYPTO";
            case "CARD_RECHARGE":          return "DEPOSIT_BANK";   // card top-up treated as bank deposit
            case "CREDIT_WALLET_DEPOSIT":  return "DEPOSIT_BANK";
            case "ADMIN_TOPUP":            return "ADMIN_TOPUP";
            case "AGENT_TOPUP":            return "AGENT_TOPUP";
            case "PROMO_BONUS":            return "DEPOSIT_PROMO";
            case "SIGNUP_BONUS":           return "SIGNUP_BONUS";
            case "CASHBACK":               return "CASHBACK";
            case "JACKPOT_WIN":            return "JACKPOT_WIN";
            case "GSC_RECONCILE":          return "DEPOSIT_BANK";
            case "GSC_CREDIT":             return "WAGER_CREDIT";   // SUN-1248: GSC settle/win credit
            case "AWC_CREDIT":             return "WAGER_CREDIT";   // SUN-13xx: AWC settle credit is a wager payout, not a deposit
            case "USERSERVICE_GAME":       return "WAGER_CREDIT";   // SUN-13xx: legacy SP game win path (positive delta)
            case "WAGER_CREDIT_BANCA":     return "WAGER_CREDIT";   // SUN-1054 / Phase 5b: BanCa session settle win → PLAYER_VIN
            case "EMERGENCY_BANCA":        return "WAGER_CREDIT";   // SUN-1054 / Phase 5c: BanCa direction-agnostic emergency credit (daily bonus, IAP, refund, admin)
            case "REFUND_WITHDRAW":        return "REFUND_WITHDRAW";
            case "CONVERT_AGENCY_TO_VIN":  return "AGENT_TOPUP";   // agency wallet → vin treated as agent top-up
            case "VIPPOINT_UPDATE":        return "VIPPOINT_UPDATE";  // money_vp credit/debit on vp accumulator
            case "SAFE_FREEZE_DRAIN":      return "SAFE_FREEZE_DRAIN"; // safe set-absolute when freezing
            // Add more mappings as new DEPOSIT_SOURCES / PROMO_SOURCES are introduced.
            default:                       return null; // unknown sources skipped (warned above)
        }
    }

    /**
     * Map a MoneyGateway source string to the corresponding system account type
     * used as the counterparty in the ledger credit entry.
     */
    static String mapSourceToSystemAccount(String source) {
        if (source == null) return null;
        switch (source) {
            case "DEPOSIT_BANK":           return "BANK_INBOX";
            case "DEPOSIT_TELEGRAM":       return "BANK_INBOX";
            case "DEPOSIT_CRYPTO":         return "CRYPTO_INBOX";
            case "CARD_RECHARGE":          return "BANK_INBOX";
            case "CREDIT_WALLET_DEPOSIT":  return "BANK_INBOX";
            case "ADMIN_TOPUP":            return "PROMO_POOL";
            case "AGENT_TOPUP":            return "PROMO_POOL";
            case "PROMO_BONUS":            return "PROMO_POOL";
            case "SIGNUP_BONUS":           return "PROMO_POOL";
            case "CASHBACK":               return "PROMO_POOL";
            case "JACKPOT_WIN":            return "JACKPOT_POOL";
            case "GSC_RECONCILE":          return "BANK_INBOX";
            case "GSC_CREDIT":             return "HOUSE_GAME_POT"; // SUN-1248: settle credit comes from the game pool
            case "AWC_CREDIT":             return "HOUSE_GAME_POT"; // SUN-13xx: AWC settle credit comes from the game pool
            case "USERSERVICE_GAME":       return "HOUSE_GAME_POT"; // SUN-13xx: legacy SP game win (positive delta)
            case "WAGER_CREDIT_BANCA":     return "HOUSE_GAME_POT"; // SUN-1054 / Phase 5b: BanCa win comes out of the game pool
            case "EMERGENCY_BANCA":        return "HOUSE_GAME_POT"; // SUN-1054 / Phase 5c: direction-agnostic; on credit path money flows from game pot to player
            case "REFUND_WITHDRAW":        return "BANK_INBOX";
            case "CONVERT_AGENCY_TO_VIN":  return "PROMO_POOL";   // pairs with AGENT_TOPUP credit type
            case "VIPPOINT_UPDATE":        return "PROMO_POOL";  // money_vp lives in the promo budget
            case "SAFE_FREEZE_DRAIN":      return "SUSPENSE";    // drained safe parks in suspense until restore
            default:                       return null;
        }
    }

    // -------------------------------------------------------------------------
    // Phase 1 dual-write: debit side
    // -------------------------------------------------------------------------

    /**
     * Write a corresponding ledger debit row after a successful legacy debit.
     * Symmetric counterpart of {@link #dualWriteToLedger}: posts
     * DEBIT player_account → CREDIT system_account (money flows OUT of player).
     *
     * <p>CRITICAL: any exception is caught and logged — must never propagate to the
     * legacy debit caller. The legacy path is the source of truth in Phase 1.
     */
    private static void dualWriteDebitToLedger(long userId, String nickname, long amount,
                                                String source, String txId, String description) {
        try {
            // SUN-13xx: bots are house-side liquidity, not real money. Skip dual-write.
            if (isBotUser(userId)) {
                return;
            }

            // Map MoneyGateway debit source → MoneyLedger transaction_type
            String txType = mapDebitSourceToLedgerType(source);
            if (txType == null) {
                logger.warn("MoneyGateway dual-write debit: no ledger type for source=" + source);
                return;
            }

            // Resolve player VIN account
            Long playerAccId = MoneyLedger.findPlayerAccount(userId, "PLAYER_VIN");
            if (playerAccId == null) {
                logger.warn("MoneyGateway dual-write debit: no PLAYER_VIN account for userId=" + userId);
                return;
            }

            // Resolve counterparty system account (the destination pool the money flows TO)
            String systemAcctType = mapDebitSourceToSystemAccount(source);
            Long systemAccId = MoneyLedger.findSystemAccount(systemAcctType);
            if (systemAccId == null) {
                logger.warn("MoneyGateway dual-write debit: no system account for type=" + systemAcctType);
                return;
            }

            // Idempotent external_ref: use txId when available, otherwise synthesise one from nanoTime
            String externalRef = (txId != null && !txId.isEmpty()) ? txId : "mgw:" + System.nanoTime();

            // Post the debit: DEBIT player account, CREDIT system account
            MoneyLedger.LedgerResult result = MoneyLedger.debit(
                    playerAccId, systemAccId, amount,
                    txType, externalRef,
                    description != null ? description : "MoneyGateway " + source,
                    null  // metadata; could be enriched with {source, originalTxId} in future
            );

            if (result.status == MoneyLedger.Status.POSTED) {
                logger.info("MoneyGateway dual-write debit OK: type=" + txType + " amount=" + amount
                        + " ref=" + externalRef + " ledger_tx_id=" + result.transactionId);
            } else if (result.status == MoneyLedger.Status.DUPLICATE) {
                // Already in ledger — fine, the write is idempotent
                logger.debug("MoneyGateway dual-write debit duplicate (already in ledger): ref=" + externalRef);
            } else {
                // Insufficient balance / frozen / error — log but don't fail the debit.
                // Note: INSUFFICIENT_BALANCE here means the legacy debit succeeded but the
                // ledger balance is out of sync — Phase 2 reconciliation will catch this.
                logger.error("MoneyGateway dual-write debit FAILED: status=" + result.status
                        + " userId=" + userId + " user=" + nickname
                        + " amount=" + amount + " source=" + source + " txId=" + txId
                        + (result.errorMessage != null ? " err=" + result.errorMessage : ""));
            }
        } catch (Exception e) {
            // CRITICAL: dual-write failures must NOT propagate to the legacy debit caller.
            logger.error("MoneyGateway dual-write debit threw: userId=" + userId + " user=" + nickname
                    + " amount=" + amount + " source=" + source + " txId=" + txId, e);
        }
    }

    /**
     * Map a MoneyGateway debit source string to a MoneyLedger transaction_type.
     * Returns null for unmapped sources (dual-write will be skipped with a warning).
     * Covers all debit-side SOURCE_ constants currently passed to {@link #debitUser}.
     */
    static String mapDebitSourceToLedgerType(String source) {
        if (source == null) return null;
        switch (source) {
            case "WITHDRAW_BANK":          return "WITHDRAW_BANK";
            case "WITHDRAW_CRYPTO":        return "WITHDRAW_CRYPTO";
            case "ADMIN_DEDUCT":           return "ADMIN_DEDUCT";
            case "AWC_DEBIT":              return "WAGER_DEBIT";   // game-pot transfer (seamless wallet)
            case "GSC_DEBIT":              return "WAGER_DEBIT";   // SUN-1248: GSC bet/withdraw → game pool
            case "GSC_HOURLY_DEBIT":       return "WAGER_DEBIT";   // SUN-1248: hourly recon backfill of missing BET
            case "USERSERVICE_GAME":       return "WAGER_DEBIT";   // SUN-13xx: legacy SP game loss path (negative delta)
            case "WAGER_DEBIT_BANCA":      return "WAGER_DEBIT";   // SUN-1054 / Phase 5b: BanCa session settle loss → PLAYER_VIN
            case "EMERGENCY_BANCA":        return "WAGER_DEBIT";   // SUN-1054 / Phase 5b: BanCa Revive emergency save / crash recovery debit
            // Add more mappings as new debit sources are routed through debitUser.
            default:                       return null;
        }
    }

    /**
     * Map a MoneyGateway debit source string to the destination system account type.
     * The player's wallet is debited and this system pool is credited (money flows OUT of player).
     */
    static String mapDebitSourceToSystemAccount(String source) {
        if (source == null) return null;
        switch (source) {
            case "WITHDRAW_BANK":          return "BANK_OUTBOX";
            case "WITHDRAW_CRYPTO":        return "CRYPTO_OUTBOX";
            case "ADMIN_DEDUCT":           return "PROMO_POOL";
            case "AWC_DEBIT":              return "HOUSE_GAME_POT";
            case "GSC_DEBIT":              return "HOUSE_GAME_POT";  // SUN-1248
            case "GSC_HOURLY_DEBIT":       return "HOUSE_GAME_POT";  // SUN-1248
            case "USERSERVICE_GAME":       return "HOUSE_GAME_POT";  // SUN-13xx: legacy SP game loss
            case "WAGER_DEBIT_BANCA":      return "HOUSE_GAME_POT";  // SUN-1054 / Phase 5b
            case "EMERGENCY_BANCA":        return "HOUSE_GAME_POT";  // SUN-1054 / Phase 5b: Revive crash recovery
            default:                       return null;
        }
    }

    // -------------------------------------------------------------------------
    // Phase 1 dual-write: transfer side (user → user)
    // -------------------------------------------------------------------------

    /**
     * Write a corresponding ledger transaction after a successful legacy transfer.
     * Composes ONE transaction with two entries: DEBIT src PLAYER_VIN, CREDIT dest
     * PLAYER_VIN — atomic via {@link MoneyLedger#transfer}, idempotent on
     * {@code (transaction_type, external_ref)}.
     *
     * <p>CRITICAL: any exception is caught and logged — must never propagate to
     * the legacy transfer caller. The legacy path is the source of truth in Phase 1.
     */
    private static void dualWriteTransferToLedger(long srcUserId, String srcNickname,
                                                   long destUserId, String destNickname,
                                                   long amount, String source,
                                                   String txId, String description) {
        try {
            // Map MoneyGateway transfer source → MoneyLedger transaction_type
            String txType = mapTransferSourceToLedgerType(source);
            if (txType == null) {
                logger.warn("MoneyGateway dual-write transfer: no ledger type for source=" + source);
                return;
            }

            // Resolve src player VIN account
            Long srcAccId = MoneyLedger.findPlayerAccount(srcUserId, "PLAYER_VIN");
            if (srcAccId == null) {
                logger.warn("MoneyGateway dual-write transfer: no PLAYER_VIN account for srcUserId=" + srcUserId);
                return;
            }
            // Resolve dest player VIN account
            Long destAccId = MoneyLedger.findPlayerAccount(destUserId, "PLAYER_VIN");
            if (destAccId == null) {
                logger.warn("MoneyGateway dual-write transfer: no PLAYER_VIN account for destUserId=" + destUserId);
                return;
            }

            // Idempotent external_ref: use txId when available, otherwise synthesise one from nanoTime
            String externalRef = (txId != null && !txId.isEmpty()) ? txId : "mgw:" + System.nanoTime();

            // Single ledger transaction: DEBIT src, CREDIT dest, both PLAYER_VIN.
            MoneyLedger.LedgerResult result = MoneyLedger.transfer(
                    srcAccId, destAccId, amount,
                    txType, externalRef,
                    description != null ? description : "MoneyGateway " + source,
                    null  // metadata; could be enriched with {srcUserId, destUserId} in future
            );

            if (result.status == MoneyLedger.Status.POSTED) {
                logger.info("MoneyGateway dual-write transfer OK: type=" + txType + " amount=" + amount
                        + " ref=" + externalRef + " ledger_tx_id=" + result.transactionId);
            } else if (result.status == MoneyLedger.Status.DUPLICATE) {
                // Already in ledger — fine, the write is idempotent
                logger.debug("MoneyGateway dual-write transfer duplicate (already in ledger): ref=" + externalRef);
            } else {
                // Insufficient balance / frozen / error — log but don't fail the transfer.
                // Note: INSUFFICIENT_BALANCE here means the legacy transfer succeeded but the
                // ledger src balance is out of sync — Phase 2 reconciliation will catch this.
                logger.error("MoneyGateway dual-write transfer FAILED: status=" + result.status
                        + " srcUserId=" + srcUserId + " srcUser=" + srcNickname
                        + " destUserId=" + destUserId + " destUser=" + destNickname
                        + " amount=" + amount + " source=" + source + " txId=" + txId
                        + (result.errorMessage != null ? " err=" + result.errorMessage : ""));
            }
        } catch (Exception e) {
            // CRITICAL: dual-write failures must NOT propagate to the legacy transfer caller.
            logger.error("MoneyGateway dual-write transfer threw:"
                    + " srcUserId=" + srcUserId + " srcUser=" + srcNickname
                    + " destUserId=" + destUserId + " destUser=" + destNickname
                    + " amount=" + amount + " source=" + source + " txId=" + txId, e);
        }
    }

    /**
     * Map a MoneyGateway transfer source string to a MoneyLedger transaction_type.
     * Returns null for unmapped sources (dual-write will be skipped with a warning).
     * Keep separate from credit/debit maps because transfers are PLAYER↔PLAYER —
     * neither side touches a system account, so the credit/debit system-account
     * mappings simply do not apply.
     */
    static String mapTransferSourceToLedgerType(String source) {
        if (source == null) return null;
        switch (source) {
            case "INTER_USER_TRANSFER":    return "INTER_USER_TRANSFER";
            // Add more transfer-style sources here (e.g. AGENT_TO_USER) as they emerge.
            default:                       return null;
        }
    }
}
