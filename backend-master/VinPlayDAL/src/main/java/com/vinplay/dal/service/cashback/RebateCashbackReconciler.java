package com.vinplay.dal.service.cashback;

import com.vinplay.dal.service.MoneyGateway;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic compensating-credit sweep for the SELF-cashback claim path.
 *
 * <p>Pairs with {@code ClaimCashbackProcessor}, which performs:
 * <ol>
 *   <li>{@code UPDATE rebate_logs SET status='PAID'} — committed.</li>
 *   <li>{@link MoneyGateway#creditUser} with {@code SOURCE_REBATE_CLAIM}.</li>
 * </ol>
 * Step 2 is bounded-retried, but a process kill, JVM pause, or all retries
 * exhausted between (1) and (2) leaves rows PAID with no corresponding
 * {@code money_gateway_log} entry — i.e. the player is short their cashback.
 * Pre-SUN-1387 the same gap existed but on the Hazelcast cache-miss path;
 * 26 players / 9.2M KRW were lost before the rewrite. The retry guards the
 * routine case; this reconciler closes the residual.
 *
 * <h2>How it works</h2>
 * Every {@code DEFAULT_PERIOD_SECONDS} seconds, scan {@code rebate_logs}
 * for rows where:
 * <ul>
 *   <li>{@code status='PAID'}</li>
 *   <li>{@code paid_at} is older than {@code STUCK_THRESHOLD_SECONDS} (avoids
 *       races against ClaimCashbackProcessor's own in-flight retry loop)</li>
 *   <li>No {@code money_gateway_log} row exists for the matching user_id
 *       with {@code source IN (SOURCE_REBATE_CLAIM, SOURCE_REBATE_RECOVERY)}
 *       newer than the {@code paid_at} timestamp</li>
 * </ul>
 * For each stuck row, credit via {@link MoneyGateway#creditUser} using
 * {@code SOURCE_REBATE_RECOVERY} so the audit trail distinguishes the
 * recovered batches from normal claims. The recovery txId
 * ({@code "rebate_recovery_<row_id>"}) is stable per row, so a second
 * reconciler tick after a transient DB error short-circuits via the
 * {@code (tx_id, source)} UNIQUE on money_gateway_log.
 *
 * <h2>Safety properties</h2>
 * <ul>
 *   <li><b>Idempotent</b>. The unique constraint guarantees no double-credit
 *       even when two reconciler instances run side-by-side. The post-credit
 *       SELECT to re-check membership is intentionally absent — we rely on
 *       MoneyGateway to dedup, never on TOCTOU.</li>
 *   <li><b>Conservative</b>. Per-row failure logs and continues; no batch
 *       aborts. The next tick re-tries.</li>
 *   <li><b>Bounded</b>. {@code BATCH_LIMIT=50} caps per-tick work; the
 *       {@code LOOKBACK_DAYS=14} upper bound prevents a whole-table sweep
 *       on first run.</li>
 *   <li><b>Daemon-safe</b>. All exceptions caught at the run-loop boundary
 *       — {@link ScheduledExecutorService} silently cancels tasks that
 *       throw, which would leave the reconciler dead until restart.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * Singleton, started by {@code JettyServer} alongside
 * {@code GscStuckRowReconciler}. Feature-flagged via env
 * {@code REBATE_RECONCILER_ENABLED} — <b>default {@code false}</b>.
 * Operator opts in only after deciding what to do about pre-existing
 * residue: as of the 2026-05-18 MR !434 deploy, scanning the 14-day
 * window surfaces 29 users / ~9 M KRW of pre-SUN-1387 historical
 * stuck claims. The operator explicitly declined to back-credit those
 * (see SUN-1387 thread), so the reconciler stays OFF by default. Flip
 * to {@code true} only after a fresh decision on the lookback window.
 * Read once at {@link #start()}; toggling requires a restart.
 */
public final class RebateCashbackReconciler {

    private static final Logger logger = Logger.getLogger("dal");

    private static final String DB_POOL = "mysqlpoolname";

    /** Per-tick row cap. */
    private static final int BATCH_LIMIT = 50;

    /** Bounded lookback so the first run doesn't sweep the whole table. */
    private static final int LOOKBACK_DAYS = 14;

    /**
     * Minimum age before a PAID row is eligible. Smaller than the
     * 3-attempt retry latency inside ClaimCashbackProcessor — the in-flight
     * retry should win the race, and only the cases where the retry
     * exhausted / the process died fall through to here. 30 s is generous
     * even for slow MySQL contention spikes.
     */
    private static final int STUCK_THRESHOLD_SECONDS = 30;

    /** Default fixed-delay between ticks. */
    private static final int DEFAULT_PERIOD_SECONDS = 60;

    /** First tick delay — let the JVM warm up before any wallet writes. */
    private static final int DEFAULT_INITIAL_DELAY_SECONDS = 120;

    private static final RebateCashbackReconciler INSTANCE = new RebateCashbackReconciler();

    public static RebateCashbackReconciler getInstance() { return INSTANCE; }

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private RebateCashbackReconciler() {}

    /** Start the periodic sweep. Safe to call multiple times; subsequent calls are no-ops. */
    public synchronized void start() {
        if (running) return;
        // Default OFF — see class javadoc. Operator must explicitly opt
        // in via env after deciding what to do about pre-existing residue.
        String enabled = System.getenv("REBATE_RECONCILER_ENABLED");
        if (enabled == null || !("true".equalsIgnoreCase(enabled) || "1".equals(enabled))) {
            logger.info("RebateCashbackReconciler: disabled (REBATE_RECONCILER_ENABLED != true)");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rebate-cashback-reconciler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::runOneTick,
                DEFAULT_INITIAL_DELAY_SECONDS,
                DEFAULT_PERIOD_SECONDS,
                TimeUnit.SECONDS);
        running = true;
        logger.info("RebateCashbackReconciler: started (period=" + DEFAULT_PERIOD_SECONDS + "s, "
                + "stuckThreshold=" + STUCK_THRESHOLD_SECONDS + "s, batch=" + BATCH_LIMIT + ")");
    }

    private void runOneTick() {
        try {
            List<StuckClaim> stuck = scan();
            if (stuck.isEmpty()) return;
            int recovered = 0, failed = 0;
            for (StuckClaim s : stuck) {
                if (recoverOne(s)) recovered++; else failed++;
            }
            logger.info("RebateCashbackReconciler: tick complete — scanned=" + stuck.size()
                    + " recovered=" + recovered + " failed=" + failed);
        } catch (Throwable t) {
            // Never let an exception escape — silently cancels the schedule.
            logger.error("RebateCashbackReconciler tick crashed (next tick will retry): " + t.getMessage(), t);
        }
    }

    /**
     * Find SELF-claim PAID rebate_logs rows that lack a corresponding
     * money_gateway_log entry tagged SOURCE_REBATE_CLAIM or
     * SOURCE_REBATE_RECOVERY. Grouped by (agent_user_id, agent_nickname)
     * so a single recovery credit covers every PAID row for that user
     * in this tick — matches the per-claim batching ClaimCashbackProcessor
     * uses.
     *
     * <p>Column mapping (per rebate_logs schema 2026-05-18):
     * <ul>
     *   <li>{@code agent_user_id} → wallet user_id (the claimer; SELF rebates
     *       store the claimant as the "agent")</li>
     *   <li>{@code agent_nickname} → display name</li>
     *   <li>{@code rebate_amount} → amount to credit (vin)</li>
     *   <li>{@code approved_at} → moment the row was flipped PAID by
     *       ClaimCashbackProcessor (timestamp anchor)</li>
     * </ul>
     *
     * <p>The {@code rebate_type='SELF'} filter is load-bearing — DOWNLINE
     * rows are credited via a different flow (auto-allocate at settle) and
     * must NEVER be recovered here.
     */
    private List<StuckClaim> scan() {
        // LEFT JOIN ... IS NULL is the "no credit row exists" anti-join.
        // Window: approved_at older than STUCK_THRESHOLD_SECONDS so we
        // don't race the inline retry. LOOKBACK_DAYS caps the first-run
        // scan.
        String sql =
            "SELECT rl.agent_user_id, rl.agent_nickname, " +
            "       SUM(rl.rebate_amount) AS total_amount, " +
            "       MIN(UNIX_TIMESTAMP(rl.approved_at)) AS earliest_approved_sec " +
            "  FROM rebate_logs rl " +
            "  LEFT JOIN money_gateway_log mgl " +
            "    ON mgl.user_id = rl.agent_user_id " +
            "   AND mgl.source IN ('" + MoneyGateway.SOURCE_REBATE_CLAIM + "','"
                                       + MoneyGateway.SOURCE_REBATE_RECOVERY + "') " +
            "   AND mgl.created_at >= rl.approved_at " +
            " WHERE rl.status = 'PAID' " +
            "   AND rl.rebate_type = 'SELF' " +
            "   AND rl.rebate_amount > 0 " +
            "   AND rl.approved_at <= NOW() - INTERVAL " + STUCK_THRESHOLD_SECONDS + " SECOND " +
            "   AND rl.approved_at >= NOW() - INTERVAL " + LOOKBACK_DAYS + " DAY " +
            "   AND mgl.id IS NULL " +
            " GROUP BY rl.agent_user_id, rl.agent_nickname " +
            " LIMIT " + BATCH_LIMIT;

        List<StuckClaim> out = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection(DB_POOL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StuckClaim s = new StuckClaim();
                s.userId = rs.getLong("agent_user_id");
                s.nickname = rs.getString("agent_nickname");
                // rebate_amount is DECIMAL(20,4) — credit path uses long
                // vin; ClaimCashbackProcessor reads via rs.getLong, so
                // match its truncation semantics exactly. (rebate_amount
                // values are integral KRW in practice; the DECIMAL is
                // for downline share math.)
                s.totalPayout = rs.getLong("total_amount");
                s.earliestPaidEpochMs = rs.getLong("earliest_approved_sec") * 1000L;
                out.add(s);
            }
        } catch (Exception e) {
            logger.warn("RebateCashbackReconciler.scan failed: " + e.getMessage());
        }
        return out;
    }

    private boolean recoverOne(StuckClaim s) {
        if (s.totalPayout <= 0) {
            logger.warn("RebateCashbackReconciler: skip user=" + s.nickname
                    + " userId=" + s.userId + " — non-positive total payout " + s.totalPayout);
            return false;
        }
        // Stable per-(user, batch-window) so a second tick before MySQL
        // catches up is a no-op via the (tx_id, source) UNIQUE.
        String txId = "rebate_recovery_" + s.userId + "_" + s.earliestPaidEpochMs;
        String description = "Recovery hoàn cược (auto-reconciler)";
        try {
            MoneyGateway.CreditResult cr = MoneyGateway.creditUser(
                    s.userId, s.nickname, s.totalPayout,
                    MoneyGateway.SOURCE_REBATE_RECOVERY,
                    txId, description);
            if (!cr.success) {
                logger.warn("RebateCashbackReconciler: creditUser failed user=" + s.nickname
                        + " userId=" + s.userId + " amount=" + s.totalPayout
                        + " txId=" + txId + " err=" + cr.error);
                return false;
            }
            logger.warn("RebateCashbackReconciler: RECOVERED user=" + s.nickname
                    + " userId=" + s.userId + " amount=" + s.totalPayout
                    + " newBalance=" + cr.newBalance + " txId=" + txId);
            return true;
        } catch (Exception e) {
            logger.error("RebateCashbackReconciler.recoverOne crashed user=" + s.nickname
                    + " userId=" + s.userId + " txId=" + txId, e);
            return false;
        }
    }

    /** Test seam — drain pending and return how many rows were recovered. */
    int runOneTickForTest() {
        List<StuckClaim> stuck = scan();
        int recovered = 0;
        for (StuckClaim s : stuck) if (recoverOne(s)) recovered++;
        return recovered;
    }

    private static final class StuckClaim {
        long userId;
        String nickname;
        long totalPayout;
        long earliestPaidEpochMs;
    }
}
