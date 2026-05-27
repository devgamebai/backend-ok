package com.vinplay.dal.service.seamless.gsc;

import com.vinplay.dal.audit.TelegramOpsNotifier;
import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic compensating-refund sweep for GSC seamless {@code /withdraw}
 * rows that got stuck at {@code processing_status='RECEIVED'} —
 * i.e. the request landed, was audit-logged, the wallet debit committed,
 * but the JVM died (or the response writer hung) before
 * {@link com.vinplay.dal.audit.GscEventLogger#tryLogResponse} could
 * flip the row to {@code COMPLETED}.
 *
 * <p>Without this reconciler, GSC times out and rejects the bet on its
 * side while we keep the player's vin debited — net player loss with no
 * automatic recovery.
 *
 * <h2>Safety properties</h2>
 * <ul>
 *   <li><b>Idempotent</b>. The {@code (tx_id, source)} UNIQUE on
 *       {@code money_gateway_log} guarantees a second run cannot
 *       double-refund — relied on instead of a TOCTOU pre-check.</li>
 *   <li><b>Conservative</b>. We refund ONLY when a {@code GSC_DEBIT}
 *       row exists. If the lookup is ambiguous (DB error), we skip and
 *       re-try next tick.</li>
 *   <li><b>Bounded</b>. {@code BATCH_LIMIT=50} caps per-tick work; the
 *       7-day {@code received_at} upper bound prevents a whole-table
 *       sweep on first run.</li>
 *   <li><b>Daemon-safe</b>. All exceptions caught at the run-loop
 *       boundary because {@link ScheduledExecutorService} silently
 *       cancels a task that throws — which would leave the reconciler
 *       dead until the next restart.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * Singleton, started by bootstrap in {@code VinPlayMain}. Feature-flagged
 * via env {@code GSC_STUCK_RECONCILER_ENABLED} (default {@code false});
 * read once at {@link #start()}, toggling requires a restart. Single-thread
 * {@link ScheduledExecutorService#scheduleWithFixedDelay} so a slow tick
 * defers rather than stacks.
 */
public class GscStuckRowReconciler {

    private static final Logger logger = Logger.getLogger("dal");

    /** SQL connection pool name shared across the DAL. */
    private static final String DB_POOL = "mysqlpoolname";

    /** Per-tick row cap. Keeps a sustained backlog from monopolising. */
    private static final int BATCH_LIMIT = 50;

    /** Bounded lookback so the first run doesn't sweep the whole table. */
    private static final int LOOKBACK_DAYS = 7;

    /** Default seconds-of-stuckness before a row is considered eligible. */
    private static final int DEFAULT_STUCK_THRESHOLD_SECONDS = 60;

    /** Default fixed-delay between ticks. */
    private static final int DEFAULT_PERIOD_SECONDS = 60;

    /** Initial delay before the first tick — keeps startup logs clean. */
    private static final int DEFAULT_INITIAL_DELAY_SECONDS = 90;

    /**
     * Wallet credit functional interface — production wires this to
     * {@link MoneyGateway#creditUserWithCumulative}. Tests inject a fake
     * to capture credit calls and return synthesized success/failure
     * without touching MySQL. Package-private and non-final so the test
     * subclass can override; production behaviour is unchanged because
     * no production code writes to this field.
     */
    @FunctionalInterface
    interface WalletCreditClient {
        MoneyGateway.CreditResultWithCumulative credit(
                long userId, String nickname, String col, long delta,
                String source, String txId, String description);
    }

    /**
     * Ops alert functional interface — production wires this to
     * {@link TelegramOpsNotifier#alert}. Tests inject a recorder.
     */
    @FunctionalInterface
    interface OpsAlerter {
        void alert(String alertKey, String text);
    }

    /** Default credit-client — delegates straight to the static gateway. */
    WalletCreditClient walletCreditClient = MoneyGateway::creditUserWithCumulative;

    /** Default alerter — delegates straight to the static notifier. */
    OpsAlerter opsAlerter = TelegramOpsNotifier::alert;

    /** Singleton instance — lazily constructed in {@link #getInstance()}. */
    private static volatile GscStuckRowReconciler INSTANCE;

    /** The scheduled executor; null until {@link #start()} is called and enabled. */
    private ScheduledExecutorService scheduler;

    /** True once {@link #start()} has wired the schedule (or no-op'd because disabled). */
    private boolean started;

    GscStuckRowReconciler() {}

    public static GscStuckRowReconciler getInstance() {
        GscStuckRowReconciler local = INSTANCE;
        if (local != null) return local;
        synchronized (GscStuckRowReconciler.class) {
            if (INSTANCE == null) INSTANCE = new GscStuckRowReconciler();
            return INSTANCE;
        }
    }

    private static boolean envEnabled() {
        String v = System.getenv("GSC_STUCK_RECONCILER_ENABLED");
        // Default OFF — operator must opt in. This is a recovery path
        // that issues compensating wallet credits; we don't want it
        // running by default until ops has reviewed the alerts.
        if (v == null) return false;
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    private static int envInt(String name, int dflt, int floor) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) return dflt;
        try {
            int n = Integer.parseInt(v.trim());
            return Math.max(floor, n);
        } catch (NumberFormatException ignored) {
            return dflt;
        }
    }

    /**
     * Wire the schedule. Idempotent — calling twice is a no-op. If the
     * feature flag {@code GSC_STUCK_RECONCILER_ENABLED} is not
     * {@code true}/{@code 1}, this method logs and returns without
     * scheduling — the daemon thread is never created.
     */
    public synchronized void start() {
        if (started) return;
        started = true;

        if (!envEnabled()) {
            logger.info("GscStuckRowReconciler: disabled by env "
                    + "GSC_STUCK_RECONCILER_ENABLED (default false) — daemon NOT started");
            return;
        }

        final int periodSec = envInt("GSC_STUCK_RECONCILER_PERIOD_SEC",
                DEFAULT_PERIOD_SECONDS, 10);
        final int initialDelaySec = envInt("GSC_STUCK_RECONCILER_INITIAL_DELAY_SEC",
                DEFAULT_INITIAL_DELAY_SECONDS, 0);
        final int stuckThresholdSec = envInt("GSC_STUCK_THRESHOLD_SECONDS",
                DEFAULT_STUCK_THRESHOLD_SECONDS, 30);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gsc-stuck-row-reconciler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(
                () -> safeSweep(stuckThresholdSec),
                initialDelaySec, periodSec, TimeUnit.SECONDS);

        // JVM shutdown hook so a clean exit cancels the schedule rather
        // than the daemon thread getting yanked mid-credit. Best-effort —
        // if the JVM is already shutting down, addShutdownHook throws
        // IllegalStateException which we swallow.
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "gsc-stuck-row-reconciler-shutdown"));
        } catch (IllegalStateException ignored) { /* JVM already shutting down */ }

        logger.info("GscStuckRowReconciler: scheduled — first run in "
                + initialDelaySec + "s, period " + periodSec
                + "s, stuck threshold " + stuckThresholdSec + "s");
    }

    /** Shut the schedule down. Safe to call from JVM shutdown hook. */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /** Outermost catch — a tick throwing kills the schedule otherwise. */
    private void safeSweep(int stuckThresholdSec) {
        long t0 = System.currentTimeMillis();
        try {
            Outcome out = sweepOnce(stuckThresholdSec);
            long ms = System.currentTimeMillis() - t0;
            if (out.scanned > 0 || out.refunded > 0 || out.errors > 0) {
                logger.info("GscStuckRowReconciler tick: " + out + " in " + ms + "ms");
            }
        } catch (Throwable t) {
            logger.error("GscStuckRowReconciler tick threw — schedule continues", t);
        }
    }

    /** Per-tick result counters for the log line. */
    static final class Outcome {
        int scanned;        // RECEIVED rows pulled this tick
        int refunded;       // compensating credits issued
        int alreadyRefunded;// gateway returned "Duplicate transaction"
        int noDebit;        // transactions with no GSC_DEBIT row
        int markedFailed;   // gsc_event_log rows flipped to FAILED
        int errors;
        long totalRefunded;
        @Override public String toString() {
            return "scanned=" + scanned
                    + " refunded=" + refunded
                    + " already_refunded=" + alreadyRefunded
                    + " no_debit=" + noDebit
                    + " marked_failed=" + markedFailed
                    + " errors=" + errors
                    + " total_refunded=" + totalRefunded;
        }
    }

    /** One sweep pass; package-private for tests. */
    Outcome sweepOnce(int stuckThresholdSec) {
        Outcome out = new Outcome();
        List<StuckRow> rows = loadStuckRows(stuckThresholdSec, out);
        for (StuckRow row : rows) {
            out.scanned++;
            try {
                processRow(row, out);
            } catch (Throwable t) {
                out.errors++;
                logger.warn("GscStuckRowReconciler: row id=" + row.id
                        + " threw " + t.getMessage());
            }
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────
    // SQL helpers
    // ─────────────────────────────────────────────────────────────────

    /** A single eligible {@code gsc_event_log} row. */
    static final class StuckRow {
        long id;
        String memberAccount;
        String rawPayload;
        Timestamp receivedAt;
    }

    List<StuckRow> loadStuckRows(int stuckThresholdSec, Outcome out) {
        List<StuckRow> rows = new ArrayList<>();
        String sql =
                "SELECT id, member_account, raw_payload, received_at " +
                "FROM vinplay.gsc_event_log " +
                "WHERE gsc_endpoint = 'withdraw' " +
                "  AND processing_status = 'RECEIVED' " +
                "  AND received_at < NOW() - INTERVAL ? SECOND " +
                "  AND received_at > NOW() - INTERVAL ? DAY " +
                "ORDER BY received_at ASC " +
                "LIMIT ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stuckThresholdSec);
            ps.setInt(2, LOOKBACK_DAYS);
            ps.setInt(3, BATCH_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StuckRow r = new StuckRow();
                    r.id = rs.getLong("id");
                    r.memberAccount = rs.getString("member_account");
                    r.rawPayload = rs.getString("raw_payload");
                    r.receivedAt = rs.getTimestamp("received_at");
                    rows.add(r);
                }
            }
        } catch (Throwable t) {
            out.errors++;
            logger.warn("GscStuckRowReconciler.loadStuckRows failed: " + t.getMessage());
        }
        return rows;
    }

    /** Holds the data we need to process one transaction in a stuck row. */
    static final class TxnRef {
        String wagerId;            // GSC tx id (raw)
        String wagerCode;          // wager_code if present, for the alert
    }

    /** Parse {@code raw_payload} into the per-transaction list. */
    static List<TxnRef> extractTransactions(String rawPayload) {
        List<TxnRef> txns = new ArrayList<>();
        if (rawPayload == null || rawPayload.isEmpty()) return txns;
        JSONObject root;
        try {
            root = new JSONObject(rawPayload);
        } catch (org.json.JSONException t) {
            return txns;
        }
        // Top-level transactions array first, fall through to
        // batch_requests[*].transactions. Mirrors the parse order in
        // GscWithdrawAggregator.parseRequest.
        JSONArray topTx = root.optJSONArray("transactions");
        if (topTx != null && topTx.length() > 0) {
            collect(topTx, txns);
        }
        JSONArray batchArr = root.optJSONArray("batch_requests");
        if (batchArr != null) {
            for (int i = 0; i < batchArr.length(); i++) {
                JSONObject br = batchArr.optJSONObject(i);
                if (br == null) continue;
                JSONArray btx = br.optJSONArray("transactions");
                if (btx != null) collect(btx, txns);
            }
        }
        return txns;
    }

    private static void collect(JSONArray arr, List<TxnRef> out) {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.optJSONObject(i);
            if (t == null) continue;
            String id = t.optString("id", null);
            if (id == null || id.isEmpty()) continue;
            TxnRef r = new TxnRef();
            r.wagerId = id;
            r.wagerCode = t.optString("wager_code", null);
            out.add(r);
        }
    }

    /** Holds the (user_id, amount) pulled from the original GSC_DEBIT row. */
    static final class DebitInfo {
        long userId;
        long amount;            // signed; negative for debit (as written)
    }

    /**
     * Look up the original {@code GSC_DEBIT} audit row.
     * Returns null when no debit was ever written for this transaction
     * (in which case the wallet was never debited and we have nothing
     * to refund — caller skips).
     */
    DebitInfo findDebit(String wagerId, String memberAccount) {
        String txId = "withdraw_" + wagerId;
        String sql =
                "SELECT user_id, amount FROM vinplay.money_gateway_log " +
                "WHERE tx_id = ? AND source = ? AND nick_name = ? LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txId);
            ps.setString(2, MoneyGateway.SOURCE_GSC_DEBIT);
            ps.setString(3, memberAccount);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DebitInfo info = new DebitInfo();
                    info.userId = rs.getLong("user_id");
                    info.amount = rs.getLong("amount");
                    return info;
                }
            }
        } catch (Throwable t) {
            logger.warn("GscStuckRowReconciler.findDebit failed wager=" + wagerId
                    + " err=" + t.getMessage());
        }
        return null;
    }

    /**
     * Flip a stuck row to FAILED. Conditional on {@code RECEIVED} so a
     * concurrent recovery path that succeeded between our scan and our
     * UPDATE doesn't get clobbered.
     */
    int markFailed(long rowId, long stuckMinutes) {
        String last = "auto_reconciled_stuck_after_" + stuckMinutes + "_min";
        String sql =
                "UPDATE vinplay.gsc_event_log " +
                "SET processing_status='FAILED', " +
                "    processed_at=CURRENT_TIMESTAMP(3), " +
                "    response_at=CURRENT_TIMESTAMP(3), " +
                "    last_error=? " +
                "WHERE id = ? AND processing_status = 'RECEIVED'";
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, last);
            ps.setLong(2, rowId);
            return ps.executeUpdate();
        } catch (Throwable t) {
            logger.warn("GscStuckRowReconciler.markFailed failed id=" + rowId
                    + " err=" + t.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Per-row processing
    // ─────────────────────────────────────────────────────────────────

    private void processRow(StuckRow row, Outcome out) {
        long stuckMinutes = computeStuckMinutes(row.receivedAt);
        List<TxnRef> txns = extractTransactions(row.rawPayload);
        if (txns.isEmpty()) {
            // Nothing parseable. Still mark FAILED so we don't keep
            // re-scanning the same row on every tick — there's no
            // recovery action to take.
            int updated = markFailed(row.id, stuckMinutes);
            if (updated > 0) out.markedFailed++;
            logger.warn("GscStuckRowReconciler: row id=" + row.id
                    + " has no parseable transactions, marking FAILED");
            return;
        }

        for (TxnRef t : txns) {
            try {
                processTransaction(row, t, stuckMinutes, out);
            } catch (Throwable e) {
                out.errors++;
                logger.warn("GscStuckRowReconciler: row id=" + row.id
                        + " wager=" + t.wagerId
                        + " threw " + e.getMessage());
            }
        }

        int updated = markFailed(row.id, stuckMinutes);
        if (updated > 0) out.markedFailed++;
    }

    private void processTransaction(StuckRow row, TxnRef t, long stuckMinutes, Outcome out) {
        DebitInfo debit = findDebit(t.wagerId, row.memberAccount);
        if (debit == null) {
            // No debit was ever written. Nothing to refund. Don't touch
            // money_gateway_log — the GSC retry / next push may still
            // settle this transaction normally; we only need to clean
            // up the lingering RECEIVED row at the end.
            out.noDebit++;
            return;
        }

        if (debit.amount >= 0L) {
            // Defensive: if the audit row's amount is somehow positive
            // (a credit, not a debit), refunding would push the player
            // even higher. Skip + log.
            logger.warn("GscStuckRowReconciler: row id=" + row.id
                    + " wager=" + t.wagerId
                    + " has GSC_DEBIT row with non-negative amount=" + debit.amount
                    + " — skipping refund");
            out.errors++;
            return;
        }

        long refundAmount = Math.abs(debit.amount);
        String txId = "stuck_refund_" + t.wagerId;
        String description = "Auto-refund: gsc_event_log id=" + row.id
                + " stuck RECEIVED for " + stuckMinutes + " min";

        MoneyGateway.CreditResultWithCumulative cr =
                walletCreditClient.credit(
                        debit.userId,
                        row.memberAccount,
                        "vin",
                        refundAmount,
                        MoneyGateway.SOURCE_GSC_STUCK_REFUND,
                        txId,
                        description);

        if (!cr.success) {
            // Most likely cause: a concurrent reconciler instance won
            // the (tx_id, source) UNIQUE race, in which case
            // {@code creditUserWithCumulative} returns
            // {@code "Duplicate transaction"}. Treat as alreadyRefunded.
            String err = cr.error != null ? cr.error : "";
            String low = err.toLowerCase();
            if (low.contains("duplicate") || low.contains("already")) {
                out.alreadyRefunded++;
                return;
            }
            out.errors++;
            logger.warn("GscStuckRowReconciler: refund FAILED row id=" + row.id
                    + " wager=" + t.wagerId
                    + " amount=" + refundAmount
                    + " err=" + err);
            return;
        }

        out.refunded++;
        out.totalRefunded += refundAmount;
        logger.info("GscStuckRowReconciler: refunded row id=" + row.id
                + " user=" + row.memberAccount
                + " wager=" + t.wagerId
                + " amount=" + refundAmount
                + " stuck_min=" + stuckMinutes
                + " new_balance=" + cr.newBalance);

        // Best-effort Telegram alert. One coarse key collapses bursts of
        // refunds into the notifier's throttle window — a 50-row sweep
        // pages once, not 50 times. Body carries the per-event detail.
        String text = "⚠️ GSC stuck-row auto-refund\n"
                + "row_id: " + row.id + "\n"
                + "user: " + safe(row.memberAccount) + "\n"
                + "wager: " + safe(t.wagerCode != null ? t.wagerCode : t.wagerId) + "\n"
                + "amount: " + refundAmount + " vin\n"
                + "stuck_for: " + stuckMinutes + " min\n"
                + "refund_tx: " + txId;
        opsAlerter.alert("gsc_stuck_refund", text);
    }

    private static long computeStuckMinutes(Timestamp receivedAt) {
        if (receivedAt == null) return 0L;
        long ageMs = System.currentTimeMillis() - receivedAt.getTime();
        if (ageMs < 0L) return 0L;
        return ageMs / 60_000L;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
