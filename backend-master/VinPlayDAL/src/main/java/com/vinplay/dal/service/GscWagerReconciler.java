package com.vinplay.dal.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.vinplay.dal.audit.GscEventLogger;
import com.vinplay.dal.rebate.RealTimeCommission;
import com.vinplay.dal.rebate.RebateService;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Catches GSC live-casino wagers that our {@code DepositProcess} never
 * received the settle/deposit event for, and reconciles them by
 * pulling the authoritative status from GSC's wager-detail API and
 * applying the missed prize credit via {@link MoneyGateway}.
 *
 * <p>Why this exists — see SUN-1204:
 * <ul>
 *   <li>Dream Gaming and other live-casino vendors sometimes silently
 *       fail to push the deposit/settle event after a wager. The
 *       withdraw (bet) push lands fine, so {@code log_gsc_bets} ends up
 *       with {@code prize=0, settled=false} even when the player won.
 *       Without recovery, agency LSC + player history both render the
 *       round as a loss, the player's vin balance stays short of the
 *       won prize, and over time we accumulate stuck rounds (audit
 *       2026-05-01: 58 stuck across 5 vendors, 23 with unbooked wins
 *       totalling ₩751,910 owed to 14 players).</li>
 *   <li>The same class of failure can recur on any future provider
 *       integration whose seamless push isn't fully reliable. A
 *       vendor-agnostic poller is the only durable fix.</li>
 * </ul>
 *
 * <p>Algorithm (idempotent, safe to run on overlap):
 * <ol>
 *   <li>Query Mongo {@code log_gsc_bets} for {@code settled=false AND
 *       create_time < NOW() - graceWindowMs}. Grace window keeps us
 *       from racing the legitimate settle path on freshly-placed bets.</li>
 *   <li>For each row, call {@link GscWagerClient#fetch(String)} on the
 *       wager_code.</li>
 *   <li>If GSC returns {@code SETTLED} with prize > 0: call
 *       {@link MoneyGateway#creditUser} with
 *       {@code source=SOURCE_GSC_RECONCILE} and {@code txId=wagerCode}.
 *       The dedup on {@code (tx_id, source)} guarantees double-runs
 *       can't double-credit.</li>
 *   <li>Update Mongo: {@code prize, settled=true, settle_time=now,
 *       settle_source="auto_reconcile"}. Independent of credit
 *       success — a credit failure doesn't block the Mongo flag flip
 *       (the audit row in {@code money_gateway_log} is the
 *       source-of-truth).</li>
 *   <li>If GSC returns {@code BET}/{@code PENDING}: leave for the
 *       next cycle. If still unsettled after 1 hour absolute age,
 *       log a warn so ops can investigate.</li>
 * </ol>
 */
public final class GscWagerReconciler {

    private static final Logger logger = Logger.getLogger("dal");

    /** Skip rows newer than this. The legitimate Dream/Pragmatic settle
     *  path has its own window; reconciling too early would race it. */
    public static final long DEFAULT_GRACE_WINDOW_MS = 5 * 60_000L;

    /** Cap rows per cycle to keep one bad provider from monopolizing
     *  the GSC API budget. */
    public static final int DEFAULT_BATCH_LIMIT = 200;

    /** Stuck this long → log a warn alongside the regular processing. */
    public static final long ALERT_AGE_MS = 60 * 60_000L;

    public static final class Outcome {
        public int scanned;
        public int recovered;
        public int alreadySettledOnVendor;   // SETTLED but prize=0
        public int stillPending;
        public int errors;
        public long totalCredited;
        /**
         * SUN-1212 — wagers we declined to credit because a row already
         * exists in {@code gsc_event_log} for the deposit/settle endpoint.
         * That row may be {@code RECEIVED} (handler crashed before applying
         * wallet credit and before writing the response) or {@code COMPLETED}
         * (handler applied credit successfully but our reconciler scan was
         * already in flight). Either way, paying again risks a double-credit;
         * a separate force-complete recovery job (out of scope here) drives
         * the stuck row to terminal state and applies the missed credit if
         * the original deposit handler never made it that far. The Mongo
         * {@code settled} flag is still flipped so the wager leaves the
         * stuck-bucket queue and a human can inspect it via the deposit row.
         */
        public int skippedDepositRowExists;
        @Override public String toString() {
            return "scanned=" + scanned + " recovered=" + recovered
                    + " no_prize_settled=" + alreadySettledOnVendor
                    + " still_pending=" + stillPending
                    + " errors=" + errors
                    + " skipped_deposit_row=" + skippedDepositRowExists
                    + " total_credited=" + totalCredited;
        }
    }

    private GscWagerReconciler() {}

    /** One-shot scan + reconcile. Returns counters for telemetry. */
    public static Outcome reconcileOnce() {
        return reconcileOnce(DEFAULT_GRACE_WINDOW_MS, DEFAULT_BATCH_LIMIT);
    }

    public static Outcome reconcileOnce(long graceWindowMs, int batchLimit) {
        Outcome out = new Outcome();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            MongoCollection<Document> col = db.getCollection("log_gsc_bets");
            Document filter = new Document("settled", false)
                    .append("bet_value", new Document("$gt", 0L))
                    .append("create_time", new Document("$lt",
                            new Date(System.currentTimeMillis() - graceWindowMs)));
            for (Document d : col.find(filter)
                    .sort(new Document("create_time", 1))   // oldest first
                    .limit(batchLimit)) {
                out.scanned++;
                String wagerCode = d.getString("wager_code");
                String userName = d.getString("user_name");
                long ourBet = numberLong(d.get("bet_value"));
                Date created = d.getDate("create_time");
                long ageMs = System.currentTimeMillis()
                        - (created != null ? created.getTime() : System.currentTimeMillis());

                if (wagerCode == null || wagerCode.isEmpty()) {
                    out.errors++;
                    continue;
                }
                GscWagerClient.WagerStatus s = GscWagerClient.fetch(wagerCode);
                if (s == null) {
                    out.errors++;
                    if (ageMs > ALERT_AGE_MS) {
                        logger.warn("GscWagerReconciler ALERT: stuck " + (ageMs/60_000L)
                                + "m without resolvable status — wager=" + wagerCode
                                + " user=" + userName + " bet=" + ourBet);
                    }
                    continue;
                }

                if (!s.isSettled()) {
                    out.stillPending++;
                    if (ageMs > ALERT_AGE_MS) {
                        logger.warn("GscWagerReconciler ALERT: vendor still " + s.status
                                + " after " + (ageMs/60_000L) + "m — wager=" + wagerCode
                                + " user=" + userName);
                    }
                    continue;
                }

                long prize = s.prizeAmount;
                long userId = lookupUserId(userName);

                // SUN-1212 — double-pay safety guard. If gsc_event_log
                // already has a deposit row for this wager (regardless of
                // processing_status), the original DepositProcess handler
                // ran far enough to insert the audit row. We CANNOT prove
                // from here whether it also applied the wallet credit
                // before crashing. Paying via reconcile when the deposit
                // already credited would double-pay the player. Skip the
                // credit, flip Mongo settled=true so the wager leaves the
                // stuck-bucket queue, and let a separate force-complete
                // recovery job drive the audit row to terminal state +
                // apply any missing credit.
                //
                // Trade-off: a wager whose deposit row is RECEIVED but
                // whose wallet credit never landed will be left unpaid
                // until that recovery job runs. Better unpaid + visible
                // (operator can investigate via the audit row) than
                // double-paid + invisible.
                GscEventLogger.DepositRowInfo info = (prize > 0L && userId > 0L)
                        ? GscEventLogger.findDepositRowForWager(wagerCode)
                        : null;
                if (info != null) {
                    out.skippedDepositRowExists++;
                    String timeZ = info.receivedAt != null ? info.receivedAt.toString() : "UNKNOWN";
                    logger.warn("Skipping reconcile for wager " + wagerCode
                            + ": deposit row id=" + info.id + " state=" + info.processingStatus
                            + " at time " + timeZ
                            + " (user=" + userName + " prize=" + prize
                            + " gsc_status=" + s.status + ")");

                    // Still flip Mongo settled=true so the round leaves the
                    // stuck bucket. Mark settle_source distinctly so ops
                    // can find these rows for follow-up via the recovery
                    // job, separate from genuinely-recovered rows.
                    try {
                        Document update = new Document("$set", new Document()
                                .append("settled", true)
                                .append("prize", prize)
                                .append("settle_time", new Date())
                                .append("settle_source", "auto_reconcile_skipped_deposit_row"));
                        col.updateOne(new Document("wager_code", wagerCode), update,
                                new UpdateOptions().upsert(false));
                    } catch (Exception me) {
                        out.errors++;
                        logger.warn("GscWagerReconciler Mongo flag-flip failed wager="
                                + wagerCode + " err=" + me.getMessage());
                    }
                    continue;
                }

                // Apply credit only when player won. Losers just need
                // the Mongo flag flipped so the round leaves the stuck
                // bucket and renders correctly in agency LSC + player
                // history (which the SUN-1204 Step 1 read-side filter
                // currently hides).
                if (prize > 0L && userId > 0L) {
                    try {
                        MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                                userId, userName, prize,
                                MoneyGateway.SOURCE_GSC_RECONCILE,
                                wagerCode,
                                "GSC reconcile (settle event missed): wager=" + wagerCode
                                        + " bet=" + ourBet + " prize=" + prize);
                        if (cr.success) {
                            out.totalCredited += prize;
                        } else {
                            // Dedup hit (already credited) is treated as
                            // "recovered" for accounting; only true
                            // failures bump the error counter.
                            String err = cr.error != null ? cr.error : "";
                            if (!err.toLowerCase().contains("already")
                                    && !err.toLowerCase().contains("dedup")) {
                                out.errors++;
                                logger.warn("GscWagerReconciler credit failed wager="
                                        + wagerCode + " err=" + err);
                            }
                        }
                    } catch (Exception ce) {
                        out.errors++;
                        logger.warn("GscWagerReconciler credit threw wager="
                                + wagerCode + " err=" + ce.getMessage());
                    }
                } else if (prize == 0L) {
                    out.alreadySettledOnVendor++;
                } else if (userId <= 0L) {
                    out.errors++;
                    logger.warn("GscWagerReconciler: user lookup failed for "
                            + userName + " wager=" + wagerCode + " — leaving Mongo unchanged");
                    continue;
                }

                // Update Mongo once credit decision is made.
                try {
                    Document update = new Document("$set", new Document()
                            .append("settled", true)
                            .append("prize", prize)
                            .append("settle_time", new Date())
                            .append("settle_source", "auto_reconcile"));
                    col.updateOne(new Document("wager_code", wagerCode), update,
                            new UpdateOptions().upsert(false));
                    out.recovered++;
                } catch (Exception me) {
                    out.errors++;
                    logger.warn("GscWagerReconciler Mongo update failed wager="
                            + wagerCode + " err=" + me.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("GscWagerReconciler.reconcileOnce error", e);
        }
        return out;
    }

    /** Resolve nickname → user_id via {@code users.nick_name} lookup.
     *  Returns 0 on miss / DB error so the caller can skip safely. */
    private static long lookupUserId(String nickname) {
        if (nickname == null || nickname.isEmpty()) return 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM users WHERE nick_name = ? LIMIT 1")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("GscWagerReconciler.lookupUserId failed nick="
                    + nickname + " err=" + e.getMessage());
        }
        return 0L;
    }

    private static long numberLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }

    // ---------------------------------------------------------------
    // SUN-1205/1206 — deferred rebate posting for hedge-prone providers
    //
    // Dream Gaming (product_code=1052) ships valid_bet_amount =
    // bet_amount in BOTH the seamless BET push AND the deposit/settle
    // push as a placeholder; the post-hedge truth (e.g. valid_bet=0 on
    // a full Banker+Player draw refund, or valid_bet=200k on a
    // partial-hedge 600k bet) is only available via GSC's wager-detail
    // PULL API, with a ~4s settle-to-update lag.
    //
    // Pipeline:
    //   1. WithdrawProcess + LogMoneyUserExtraProcessor SKIP rebate
    //      creation for action prefix "gsc_1052_" at BET time.
    //   2. DepositProcess applies the prize credit and marks the
    //      log_gsc_bets row settled=true (existing path, unchanged).
    //   3. This reconciler scans Mongo log_gsc_bets for settled Dream
    //      rounds with no rebate_posted flag, pulls the authoritative
    //      valid_bet_amount from GSC, and calls
    //      RealTimeCommission.calculate(nick, valid_bet, action) ONCE
    //      with the truth — no reverses, no wrong-state window.
    // ---------------------------------------------------------------

    public static final class DriftOutcome {
        public int scanned;
        public int posted;            // rebate created with GSC-truth volume
        public int zeroBetSkipped;    // valid_bet=0 (full hedge) → no rebate to post
        public int stillPending;      // GSC says not SETTLED yet
        public int reconFailed;
        public long totalVolumePosted; // sum of valid_bet across POSTED rows
        @Override public String toString() {
            return "scanned=" + scanned + " posted=" + posted
                    + " zero_bet=" + zeroBetSkipped + " still_pending=" + stillPending
                    + " recon_failed=" + reconFailed
                    + " volume_posted=" + totalVolumePosted;
        }
    }

    /** Default scope for the deferred-rebate pass: only consider wagers
     *  whose log_gsc_bets row landed in the last 6h. Older un-posted
     *  rounds require the manual one-shot endpoint. */
    public static final long DEFAULT_DRIFT_LOOKBACK_MS = 6 * 60 * 60_000L;

    /** Deferred-rebate pass for the given productCode using default windows. */
    public static DriftOutcome reconcileRebateDriftOnce(int productCode) {
        return reconcileRebateDriftOnce(productCode,
                DEFAULT_GRACE_WINDOW_MS, DEFAULT_DRIFT_LOOKBACK_MS, DEFAULT_BATCH_LIMIT);
    }

    /**
     * One scan pass over Mongo {@code log_gsc_bets} for settled but
     * un-posted Dream-style rebates of the given productCode. For each:
     * <ol>
     *   <li>Skip if already in {@link #AUDIT_TABLE} (idempotency).</li>
     *   <li>Pull wager-detail from GSC. If status is not SETTLED, leave
     *       for the next cycle.</li>
     *   <li>If valid_bet_amount &gt; 0: post via {@link
     *       RealTimeCommission#calculate} with the GSC-truth volume.</li>
     *   <li>If valid_bet_amount == 0 (full hedge): record SKIP_ZERO_BET
     *       — no rebate posted, by design.</li>
     * </ol>
     * The audit row is written last so a partial failure leaves the
     * wager re-discoverable on the next cycle (via RECON_FAILED status).
     */
    public static DriftOutcome reconcileRebateDriftOnce(int productCode,
                                                          long graceWindowMs,
                                                          long lookbackMs,
                                                          int batchLimit) {
        DriftOutcome out = new DriftOutcome();
        // Pre-load already-audited wager_codes so the Mongo scan can
        // filter them out client-side (Mongo collection doesn't carry
        // the audit flag — it's in MySQL).
        java.util.Set<String> alreadyAudited = loadAuditedWagerCodes(productCode, lookbackMs);

        List<Candidate> candidates = new ArrayList<>();
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
            MongoCollection<Document> col = db.getCollection("log_gsc_bets");
            Document filter = new Document("product_code", productCode)
                    .append("settled", true)
                    .append("create_time", new Document("$lt",
                            new Date(System.currentTimeMillis() - graceWindowMs))
                                                   .append("$gt",
                            new Date(System.currentTimeMillis() - lookbackMs)));
            int collected = 0;
            for (Document d : col.find(filter)
                    .sort(new Document("create_time", 1))
                    .limit(batchLimit * 4)) {                // headroom for already-audited filter
                String wager = d.getString("wager_code");
                if (wager == null || wager.isEmpty()) continue;
                if (alreadyAudited.contains(wager)) continue;
                Candidate c = new Candidate();
                c.wagerCode = wager;
                c.playerNickname = d.getString("user_name");
                c.gameAction = d.getString("game_key");      // already in "gsc_<pc>_<gc>" form
                candidates.add(c);
                if (++collected >= batchLimit) break;
            }
        } catch (Exception e) {
            logger.error("GscWagerReconciler deferred-rebate Mongo scan failed pc="
                    + productCode, e);
            return out;
        }

        for (Candidate c : candidates) {
            out.scanned++;
            try {
                processDeferredCandidate(productCode, c, out);
            } catch (Throwable t) {
                out.reconFailed++;
                logger.warn("GscWagerReconciler deferred-rebate threw wager=" + c.wagerCode
                        + " err=" + t.getMessage());
                writeAudit(productCode, c, "RECON_FAILED", 0L, t.getMessage());
            }
        }
        return out;
    }

    private static final class Candidate {
        String wagerCode;
        String playerNickname;
        String gameAction;
    }

    private static void processDeferredCandidate(int productCode, Candidate c, DriftOutcome out) {
        GscWagerClient.WagerStatus s = GscWagerClient.fetch(c.wagerCode);
        if (s == null) {
            out.reconFailed++;
            // Don't write audit — leave for next cycle to retry transient HTTP issues.
            return;
        }
        if (!s.isSettled()) {
            out.stillPending++;
            return;
        }

        if (s.validBetAmount <= 0L) {
            // Full hedge — no real-money risk → no commission. Record
            // for audit and move on.
            writeAudit(productCode, c, "SKIP_ZERO_BET", s.validBetAmount, "valid_bet=0 full hedge");
            out.zeroBetSkipped++;
            return;
        }

        if (c.playerNickname == null || c.playerNickname.isEmpty()
                || c.gameAction == null || c.gameAction.isEmpty()) {
            out.reconFailed++;
            writeAudit(productCode, c, "RECON_FAILED", s.validBetAmount,
                    "missing player/action in log_gsc_bets row");
            return;
        }

        // Defensive check: skip wagers that ALREADY have AUTO_COMMISSION
        // rows in rebate_logs (e.g. pre-deploy bets created at BET time
        // before the gsc_1052_* skip went live). Without this guard the
        // reconciler double-creates and double-credits wallets.
        if (rebateAlreadyExists(productCode, c.wagerCode)) {
            writeAudit(productCode, c, "POSTED", s.validBetAmount,
                    "skipped — pre-existing AUTO_COMMISSION rows found");
            out.posted++;
            return;
        }

        // SUN-1205/1206/1208 — post via AutoCommissionPipeline, NOT
        // RealTimeCommission. The legacy path uses
        // useragent.commission_rate (global) which is 0% for Dream
        // agents whose real rates live in vinplay.game_commission_rate
        // (per-game). The pipeline below mirrors vbee's
        // triggerAutoCommission logic so deferred post matches eager
        // post for non-Dream providers.
        String sourceKey = "gsc:" + productCode + ":"
                + extractGameCode(c.gameAction) + ":" + c.wagerCode;
        com.vinplay.dal.rebate.AutoCommissionPipeline.Outcome o =
                com.vinplay.dal.rebate.AutoCommissionPipeline.post(
                        c.playerNickname,
                        c.gameAction,
                        sourceKey,
                        String.valueOf(productCode),
                        s.validBetAmount);
        if (!o.ok) {
            out.reconFailed++;
            writeAudit(productCode, c, "RECON_FAILED", s.validBetAmount,
                    "pipeline failed: " + o.error);
            logger.warn("GscWagerReconciler deferred post failed wager=" + c.wagerCode
                    + " err=" + o.error);
            return;
        }

        writeAudit(productCode, c, "POSTED", s.validBetAmount,
                "gsc_valid_bet=" + s.validBetAmount + " rows=" + o.rowsCreated
                        + " total=" + o.totalAmount);
        out.posted++;
        out.totalVolumePosted += s.validBetAmount;
        logger.info("GscWagerReconciler deferred POSTED wager=" + c.wagerCode
                + " user=" + c.playerNickname + " action=" + c.gameAction
                + " valid_bet=" + s.validBetAmount
                + " rows=" + o.rowsCreated + " total=" + o.totalAmount);
    }

    /**
     * SUN-1370 (2026-05-17): inline-post Dream-style deferred commission
     * when the SETTLE payload already carries the truthful
     * {@code valid_bet_amount}. Skips the wager-detail PULL API call
     * (we already have the number) and writes the same {@code POSTED}
     * audit row the scheduler-pass writes, so the next reconciler tick
     * sees it as already-done and short-circuits.
     *
     * <p>Caller: {@link com.vinplay.dal.service.seamless.gsc.GscDepositAggregator#afterCredit}
     * fires this for {@code productCode=1052} on every settle whose
     * {@code valid_bet_amount > 0}. Dream changed their wire on
     * 2026-05-16 to emit real valid_bet on the deposit/settle push
     * instead of the legacy {@code valid_bet = bet} placeholder, so
     * the lag-free path is now possible.
     *
     * <p>Idempotency: writes the {@code gsc_wager_drift_audit} row with
     * status {@code POSTED} BEFORE the rebate post, using INSERT IGNORE.
     * If the audit row already exists (another path posted), the method
     * short-circuits and does NOT call {@link AutoCommissionPipeline}.
     * This protects against double-post when the reconciler scheduler
     * also picks the same wager up.
     *
     * @return true if this call performed the inline post; false if a
     *         prior path already handled the wager.
     */
    public static boolean postFromSettlePayload(int productCode,
                                                 String wagerCode,
                                                 String playerNickname,
                                                 String gameAction,
                                                 long validBetSubunit) {
        if (wagerCode == null || wagerCode.isEmpty()) return false;
        if (playerNickname == null || playerNickname.isEmpty()) return false;
        if (gameAction == null || gameAction.isEmpty()) return false;
        if (validBetSubunit <= 0L) return false;

        // Claim the wager via INSERT IGNORE on drift_audit; if INSERT
        // returns 0 rows the audit already exists → reconciler or a
        // prior settle has already handled it.
        if (!claimAuditRow(productCode, wagerCode, playerNickname, gameAction, validBetSubunit)) {
            return false;
        }
        if (rebateAlreadyExists(productCode, wagerCode)) {
            updateAuditNote(productCode, wagerCode,
                    "POSTED — pre-existing AUTO_COMMISSION rows found (inline)");
            return false;
        }

        String sourceKey = "gsc:" + productCode + ":"
                + extractGameCode(gameAction) + ":" + wagerCode;
        com.vinplay.dal.rebate.AutoCommissionPipeline.Outcome o;
        try {
            o = com.vinplay.dal.rebate.AutoCommissionPipeline.post(
                    playerNickname, gameAction, sourceKey,
                    String.valueOf(productCode), validBetSubunit);
        } catch (Throwable t) {
            updateAuditNote(productCode, wagerCode,
                    "RECON_FAILED inline: " + t.getMessage());
            logger.warn("GscWagerReconciler.postFromSettlePayload threw wager="
                    + wagerCode + " err=" + t.getMessage());
            return false;
        }
        if (!o.ok) {
            updateAuditNote(productCode, wagerCode,
                    "RECON_FAILED inline: " + o.error);
            logger.warn("GscWagerReconciler.postFromSettlePayload failed wager="
                    + wagerCode + " err=" + o.error);
            return false;
        }
        updateAuditNote(productCode, wagerCode,
                "gsc_valid_bet=" + validBetSubunit
                        + " rows=" + o.rowsCreated
                        + " total=" + o.totalAmount + " (inline)");
        logger.info("GscWagerReconciler inline POSTED wager=" + wagerCode
                + " user=" + playerNickname + " action=" + gameAction
                + " valid_bet=" + validBetSubunit
                + " rows=" + o.rowsCreated + " total=" + o.totalAmount);
        return true;
    }

    /** INSERT IGNORE into drift_audit. Returns true if the row was newly inserted. */
    private static boolean claimAuditRow(int productCode, String wagerCode,
                                          String playerNickname, String gameAction,
                                          long validBetSubunit) {
        String sql = "INSERT IGNORE INTO " + AUDIT_TABLE
                + " (wager_code, product_code, player_nickname, game_action, "
                + "recorded_volume, gsc_valid_bet, status, reversed_rows, note) "
                + "VALUES (?, ?, ?, ?, 0, ?, 'POSTED', 0, 'claimed-inline')";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wagerCode);
            ps.setInt(2, productCode);
            ps.setString(3, playerNickname);
            ps.setString(4, gameAction);
            ps.setLong(5, validBetSubunit);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            logger.warn("GscWagerReconciler.claimAuditRow failed wager=" + wagerCode
                    + " err=" + e.getMessage());
            return false;
        }
    }

    /** Update the note column on an existing audit row (claim + result two-step). */
    private static void updateAuditNote(int productCode, String wagerCode, String note) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE " + AUDIT_TABLE + " SET note = ? "
                             + "WHERE product_code = ? AND wager_code = ?")) {
            ps.setString(1, note.length() > 255 ? note.substring(0, 255) : note);
            ps.setInt(2, productCode);
            ps.setString(3, wagerCode);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("GscWagerReconciler.updateAuditNote failed wager=" + wagerCode
                    + " err=" + e.getMessage());
        }
    }

    /** Check whether AUTO_COMMISSION rows already exist for this wager.
     *  True ⇒ skip create (avoid duplicate credit); false ⇒ safe to post. */
    private static boolean rebateAlreadyExists(int productCode, String wagerCode) {
        if (wagerCode == null || wagerCode.isEmpty()) return false;
        String pattern = "AUTO_COMMISSION source=gsc:" + productCode + ":%:" + wagerCode + " %";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM vinplay.rebate_logs "
                             + "WHERE note LIKE ? AND status NOT IN ('REVERSED','REJECTED') "
                             + "LIMIT 1")) {
            ps.setString(1, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.warn("GscWagerReconciler.rebateAlreadyExists failed wager=" + wagerCode
                    + " err=" + e.getMessage());
            return false;
        }
    }

    /** Extract the game_code segment from action like "gsc_1052_10302" → "10302". */
    private static String extractGameCode(String gameAction) {
        if (gameAction == null) return "";
        int p1 = gameAction.indexOf('_');
        if (p1 < 0) return "";
        int p2 = gameAction.indexOf('_', p1 + 1);
        if (p2 < 0) return "";
        return gameAction.substring(p2 + 1);
    }

    private static java.util.Set<String> loadAuditedWagerCodes(int productCode, long lookbackMs) {
        java.util.Set<String> set = new java.util.HashSet<>();
        // Only need recent audit rows — anything older than lookbackMs
        // is also outside the candidate window, so loading it would be
        // wasted memory.
        String sql = "SELECT wager_code FROM vinplay.gsc_wager_drift_audit "
                + "WHERE product_code = ? AND checked_at > NOW() - INTERVAL ? SECOND";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productCode);
            ps.setLong(2, lookbackMs / 1000L);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.warn("GscWagerReconciler.loadAuditedWagerCodes failed pc="
                    + productCode + " err=" + e.getMessage());
        }
        return set;
    }

    private static final String AUDIT_TABLE = "vinplay.gsc_wager_drift_audit";

    private static void writeAudit(int productCode, Candidate c, String status,
                                    long gscValidBet, String note) {
        // recorded_volume is always 0 in the deferred-post model — there
        // was no BET-time rebate, so nothing was "recorded". Kept on the
        // schema for backward compat (drift_amount = 0 - valid_bet on
        // POSTED rows; consult valid_bet directly for "what did we
        // commission against").
        String sql =
                "INSERT INTO " + AUDIT_TABLE + " " +
                "  (wager_code, product_code, player_nickname, game_action, " +
                "   recorded_volume, gsc_valid_bet, status, reversed_rows, note) " +
                "VALUES (?,?,?,?,0,?,?,0,?) " +
                "ON DUPLICATE KEY UPDATE status=VALUES(status), " +
                "  gsc_valid_bet=VALUES(gsc_valid_bet), " +
                "  note=VALUES(note), checked_at=CURRENT_TIMESTAMP";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.wagerCode);
            ps.setInt(2, productCode);
            ps.setString(3, c.playerNickname);
            ps.setString(4, c.gameAction);
            ps.setLong(5, gscValidBet);
            ps.setString(6, status);
            ps.setString(7, note);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("GscWagerReconciler.writeAudit failed wager=" + c.wagerCode
                    + " err=" + e.getMessage());
        }
    }
}
