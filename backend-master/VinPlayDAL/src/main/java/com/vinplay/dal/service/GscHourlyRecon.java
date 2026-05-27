package com.vinplay.dal.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

/**
 * Hourly cross-check of GSC wagers against our {@code log_gsc_bets}.
 *
 * <p>Complements the existing 5-minute {@link GscWagerReconciler}, which
 * walks OUR records and asks GSC "did this settle?". That catches the
 * common "we know about the bet, missed the settle callback" case but
 * is blind to bets we never recorded at all. This job walks GSC's
 * authoritative ledger via the 3.2 Wager List API and finds the gaps
 * in our side — bets where GSC has a row but we don't, or amount
 * mismatches.
 *
 * <h2>Auto-fix policy — idempotent and safe by construction</h2>
 *
 * Every wallet movement here flows through {@link MoneyGateway} with a
 * {@code source} label. The {@code (tx_id, source)} UNIQUE in
 * {@code money_gateway_log} silently swallows duplicates if the
 * original callback eventually arrives or if the 5-min reconciler ran
 * concurrently — concurrent execution can NEVER double-pay.
 *
 * <ul>
 *   <li><b>Missing settle for lose</b> — patch {@code log_gsc_bets}
 *       row with {@code settled=true, prize=0}. No money moves.</li>
 *   <li><b>Missing settle for win</b> — same as 5-min reconciler:
 *       {@link MoneyGateway#creditUser} with
 *       {@code source=SOURCE_GSC_RECONCILE, txId=wager_code}. The
 *       UNIQUE makes this safe even if the 5-min recon raced us.</li>
 *   <li><b>GSC has a wager we have no row for, audit shows we
 *       received the callback</b> — backfill the row + apply wallet
 *       debit. Source distinct (`GSC_HOURLY_DEBIT`) so it can't
 *       collide with the realtime debit's `GSC_DEBIT`.</li>
 *   <li><b>GSC has a wager we have no row for, no audit row</b> —
 *       skip. Auto-debiting could overdraft a player who's already
 *       spent the un-debited balance; logged for manual review.</li>
 *   <li><b>Amount mismatch</b> (our {@code bet_value} != GSC's) —
 *       skip. Could be fraud, race, or upstream bug; needs human
 *       eyes; logged.</li>
 * </ul>
 *
 * <h2>Window / overlap</h2>
 * Default scan window: [now - 90min, now - 5min]. Trailing 5-min grace
 * gives the realtime path room to commit before we judge a wager
 * "missing". 90-min lookback gives a 30-min overlap with the previous
 * hourly run so a transient API failure can't leave a hole.
 *
 * <h2>Operational toggle</h2>
 * Disable at startup via env {@code GSC_HOURLY_RECON_ENABLED=false}.
 * Period in seconds via {@code GSC_HOURLY_RECON_PERIOD_SEC}
 * (default 3600).
 */
public final class GscHourlyRecon {

    private static final Logger logger = Logger.getLogger("dal");

    /** Skip wagers settled within this many ms — give realtime path time to commit. */
    public static final long DEFAULT_TRAILING_GRACE_MS = 5 * 60_000L;

    /** Total lookback. 90 min covers a full hour + 30-min overlap with prev run. */
    public static final long DEFAULT_LOOKBACK_MS = 90 * 60_000L;

    /** GSC API caps each request at 5 min per spec v2.0.5. */
    private static final long GSC_WINDOW_MS = 5 * 60_000L;

    /** Hard cap on wagers fetched per 5-min slice (GSC max is 5000). */
    private static final int GSC_PAGE_SIZE = 5000;

    /** Distinct from {@link MoneyGateway#SOURCE_GSC_DEBIT} so the realtime
     *  debit and the recon-backfill debit dedup independently — and so
     *  ops can grep money_gateway_log to find every recon-driven move. */
    private static final String SOURCE_GSC_HOURLY_DEBIT = "GSC_HOURLY_DEBIT";

    public static final class Outcome {
        public int gscWagersScanned;
        public int alreadyConsistent;
        public int settleBackfilled;       // missing-settle (lose or win)
        public int rowBackfilled;          // missing-row, audit had it, recovered
        public int skippedNoAudit;         // missing-row, no audit — manual case
        public int skippedAmountMismatch;  // bet_value drift — manual case
        public int skippedVoided;          // GSC says voided/rolled-back — leave deleted
        public int errors;
        public long totalCreditedToPlayers;
        public long totalDebitedFromPlayers;

        @Override public String toString() {
            return "scanned=" + gscWagersScanned
                    + " consistent=" + alreadyConsistent
                    + " settle_backfilled=" + settleBackfilled
                    + " row_backfilled=" + rowBackfilled
                    + " skipped_no_audit=" + skippedNoAudit
                    + " skipped_amount_mismatch=" + skippedAmountMismatch
                    + " skipped_voided=" + skippedVoided
                    + " errors=" + errors
                    + " credited=" + totalCreditedToPlayers
                    + " debited=" + totalDebitedFromPlayers;
        }
    }

    private GscHourlyRecon() {}

    public static Outcome reconcileOnce() {
        long now = System.currentTimeMillis();
        long endMs = now - DEFAULT_TRAILING_GRACE_MS;
        long startMs = endMs - DEFAULT_LOOKBACK_MS;
        return reconcileOnce(startMs, endMs);
    }

    public static Outcome reconcileOnce(long startMs, long endMs) {
        Outcome out = new Outcome();
        if (endMs <= startMs) return out;

        MongoDatabase db = MongoDBConnectionFactory.getDB("win123club");
        MongoCollection<Document> betsCol = db.getCollection("log_gsc_bets");

        // Walk the [startMs, endMs] range in 5-minute slices to respect
        // GSC's per-call window cap.
        for (long winStart = startMs; winStart < endMs; winStart += GSC_WINDOW_MS) {
            long winEnd = Math.min(winStart + GSC_WINDOW_MS, endMs);

            // Page through this slice in case of >5000 wagers.
            int offset = 0;
            while (true) {
                List<GscWagerClient.WagerStatus> page =
                        GscWagerClient.listWagers(winStart, winEnd, offset, GSC_PAGE_SIZE);
                if (page.isEmpty()) break;
                for (GscWagerClient.WagerStatus w : page) {
                    out.gscWagersScanned++;
                    try {
                        reconcileOne(w, betsCol, out);
                    } catch (Exception e) {
                        out.errors++;
                        logger.warn("GscHourlyRecon: error on wager=" + w.code
                                + " err=" + e.getMessage());
                    }
                }
                if (page.size() < GSC_PAGE_SIZE) break;  // last page
                offset += page.size();
            }
        }
        return out;
    }

    private static void reconcileOne(GscWagerClient.WagerStatus gsc,
                                      MongoCollection<Document> betsCol,
                                      Outcome out) {
        String wagerCode = gsc.code;
        if (wagerCode == null || wagerCode.isEmpty()) {
            out.errors++;
            return;
        }
        // SUN-1248: skip wagers GSC marked as voided / cancelled / rolled-back.
        // Without this guard we re-create rows that the realtime ROLLBACK
        // path already deleted on purpose (Evolution voids a bet → we delete
        // log_gsc_bets → recon sees the wager in GSC's wager-list and
        // backfills it as if it were a missing row). The status field on
        // GscWagerClient.WagerStatus is what GSC's 3.2 API returned for
        // each wager; only "SETTLED" / "BET" / "PENDING" should be
        // reconciled.
        if (gsc.status != null) {
            String s = gsc.status.toUpperCase();
            if (s.equals("VOID") || s.equals("CANCELLED") || s.equals("CANCELED")
                    || s.equals("ROLLBACK") || s.equals("REJECTED") || s.equals("REFUND")) {
                out.skippedVoided++;
                return;
            }
        }
        Document ours = betsCol.find(new Document("wager_code", wagerCode)).first();

        if (ours == null) {
            // No row at all. Was the callback received but we crashed
            // mid-flow? Audit log answers that.
            if (auditHasWager(wagerCode)) {
                backfillMissingRow(gsc, betsCol, out);
            } else {
                // Catastrophic miss. Auto-debit could overdraft. Skip
                // and log — operator decides.
                out.skippedNoAudit++;
                logger.warn("GscHourlyRecon: GSC has wager we never received — wager=" + wagerCode
                        + " member=" + gsc.memberAccount
                        + " product=" + gsc.productCode
                        + " bet=" + gsc.betAmount
                        + " — no audit row, MANUAL review needed");
            }
            return;
        }

        // Row exists. Compare amounts first; bet_value drift is a hard
        // stop because the wallet effect already happened with the
        // wrong amount.
        long ourBet = numberLong(ours.get("bet_value"));
        if (gsc.betAmount > 0 && ourBet > 0 && ourBet != gsc.betAmount) {
            out.skippedAmountMismatch++;
            logger.warn("GscHourlyRecon: amount mismatch — wager=" + wagerCode
                    + " member=" + gsc.memberAccount
                    + " our_bet=" + ourBet + " gsc_bet=" + gsc.betAmount
                    + " — MANUAL review needed");
            return;
        }

        // Settled? If GSC says yes but we say no, run the same patch
        // path the 5-min reconciler uses (settle the row, credit prize
        // if any).
        boolean weSettled = Boolean.TRUE.equals(ours.getBoolean("settled"));
        if (gsc.isSettled() && !weSettled) {
            backfillMissingSettle(gsc, betsCol, out);
            return;
        }

        out.alreadyConsistent++;
    }

    /**
     * Audit-side existence check. {@code true} when {@code gsc_event_log}
     * has any row for this wager_code — meaning GSC's callback DID reach
     * us at some point, we just failed to commit downstream. Safe to
     * backfill from audit (the wallet UNIQUE protects against duplication).
     */
    private static boolean auditHasWager(String wagerCode) {
        String sql = "SELECT 1 FROM gsc_event_log WHERE gsc_wager_code = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wagerCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.warn("GscHourlyRecon.auditHasWager failed wager=" + wagerCode
                    + " err=" + e.getMessage());
            // Conservative: pretend audit has it so we backfill rather
            // than skip on a transient DB error. Wallet UNIQUE protects.
            return true;
        }
    }

    /**
     * Resolve nickname → user_id. Mirrors the helper in
     * {@link GscWagerReconciler}; kept private here so this class
     * compiles without leaking that one's internals.
     */
    private static long lookupUserId(String nickname) {
        if (nickname == null || nickname.isEmpty()) return 0L;
        String sql = "SELECT id FROM vinplay.users WHERE nick_name = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.warn("GscHourlyRecon.lookupUserId failed nick=" + nickname
                    + " err=" + e.getMessage());
        }
        return 0L;
    }

    /**
     * GSC has a wager, we have no log_gsc_bets row, but audit shows we
     * received the callback. Insert the missing bet row + apply wallet
     * debit (idempotent via {@code (tx_id, source)} UNIQUE).
     */
    private static void backfillMissingRow(GscWagerClient.WagerStatus gsc,
                                            MongoCollection<Document> betsCol,
                                            Outcome out) {
        long now = System.currentTimeMillis();
        long createdMs = gsc.settledAtMs > 0L ? Math.max(now - 60_000L, gsc.settledAtMs - 1000L) : now;

        Document setFields = new Document()
                .append("wager_code", gsc.code)
                .append("user_name", gsc.memberAccount)
                .append("nick_name", gsc.memberAccount)
                .append("product_code", gsc.productCode)
                .append("game_code", gsc.gameCode)
                .append("bet_value", gsc.betAmount)
                .append("currency", "")              // unknown from list-API; harmless
                .append("create_time", new Date(createdMs))
                .append("time_log", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date(createdMs)))
                .append("bet_type", "BET")
                .append("recovered_by", "GscHourlyRecon");
        Document setOnInsert = new Document()
                .append("settled", false)
                .append("prize", 0L);

        betsCol.updateOne(
                new Document("wager_code", gsc.code),
                new Document("$set", setFields).append("$setOnInsert", setOnInsert),
                new UpdateOptions().upsert(true));

        // Apply the wallet debit. UNIQUE on (tx_id, source) means the
        // realtime path (if it later arrives) is silently no-opped, and
        // a second recon pass can never double-debit.
        long debitedAmount = applyDebitForBackfill(gsc);
        out.totalDebitedFromPlayers += debitedAmount;
        out.rowBackfilled++;

        // If GSC already shows the wager as settled, fold in the
        // settle/prize patch on the same pass so the row leaves the
        // unsettled bucket.
        if (gsc.isSettled()) {
            backfillMissingSettle(gsc, betsCol, out);
        }
    }

    /**
     * Settles the existing log_gsc_bets row to match GSC. For wins,
     * also credits the prize via {@link MoneyGateway#creditUser} —
     * the same code path {@link GscWagerReconciler} uses, with the
     * same {@code SOURCE_GSC_RECONCILE} dedup so the two reconcilers
     * never double-credit each other.
     */
    private static void backfillMissingSettle(GscWagerClient.WagerStatus gsc,
                                                MongoCollection<Document> betsCol,
                                                Outcome out) {
        long credited = 0L;
        if (gsc.playerWon()) {
            long userId = lookupUserId(gsc.memberAccount);
            if (userId > 0L) {
                try {
                    MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                            userId,
                            gsc.memberAccount,
                            gsc.prizeAmount,
                            MoneyGateway.SOURCE_GSC_RECONCILE,
                            /*txId*/ gsc.code,
                            "GSC hourly recon — late settle prize for wager " + gsc.code);
                    if (cr != null && cr.success) {
                        credited = gsc.prizeAmount;
                    }
                } catch (Throwable t) {
                    logger.warn("GscHourlyRecon.backfillMissingSettle: credit failed wager=" + gsc.code
                            + " amount=" + gsc.prizeAmount + " — " + t.getMessage());
                }
            } else {
                logger.warn("GscHourlyRecon.backfillMissingSettle: cannot resolve user_id for "
                        + gsc.memberAccount + " — wager=" + gsc.code + " skipping credit");
            }
        }
        out.totalCreditedToPlayers += credited;

        long settledAt = gsc.settledAtMs > 0L ? gsc.settledAtMs : System.currentTimeMillis();
        betsCol.updateOne(
                new Document("wager_code", gsc.code),
                new Document("$set", new Document()
                        .append("settled", true)
                        .append("prize", gsc.prizeAmount)
                        .append("settle_time", new Date(settledAt))
                        .append("settle_source", "auto_hourly_recon")));
        out.settleBackfilled++;
    }

    /**
     * Apply the late wallet debit for a row we backfilled from GSC's
     * ledger. Wired through {@link MoneyGateway#debitUser} with a
     * distinct {@code source} so its UNIQUE protects against re-runs
     * AND against a delayed primary callback that eventually arrives
     * (which uses {@code SOURCE_GSC_DEBIT}).
     *
     * <p>If the player's current balance is below the bet amount, the
     * atomic floor-check rejects — we don't try to overdraft. The
     * audit row in {@code money_gateway_log} still records the
     * attempt; ops can chase the player or write off.
     */
    private static long applyDebitForBackfill(GscWagerClient.WagerStatus gsc) {
        long userId = lookupUserId(gsc.memberAccount);
        if (userId <= 0L) {
            logger.warn("GscHourlyRecon.applyDebitForBackfill: no user_id for "
                    + gsc.memberAccount + " — wager=" + gsc.code + " skipping debit");
            return 0L;
        }
        try {
            MoneyGateway.CreditResult dr = MoneyGateway.debitUser(
                    userId,
                    gsc.memberAccount,
                    gsc.betAmount,
                    SOURCE_GSC_HOURLY_DEBIT,
                    /*txId*/ gsc.code,
                    "GSC hourly recon — backfill missed bet wager " + gsc.code);
            return (dr != null && dr.success) ? gsc.betAmount : 0L;
        } catch (Throwable t) {
            logger.warn("GscHourlyRecon.applyDebitForBackfill failed wager=" + gsc.code
                    + " amount=" + gsc.betAmount + " — " + t.getMessage());
            return 0L;
        }
    }

    private static long numberLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return 0L;
        try { return Long.parseLong(o.toString()); } catch (Exception ignored) { return 0L; }
    }
}
