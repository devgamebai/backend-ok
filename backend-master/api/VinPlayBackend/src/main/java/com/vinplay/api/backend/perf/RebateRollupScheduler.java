package com.vinplay.api.backend.perf;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SUN-1108 Tier 3 — daily aggregation of rebate_logs into rebate_daily_rollup.
 *
 * <p>Lifecycle: {@link #start()} is called once from VinPlayBackendMain.
 * It schedules a single daily task at 00:30 UTC that runs the
 * {@code INSERT … ON DUPLICATE KEY UPDATE} aggregation for the
 * just-completed day. Multiple backend-api instances would each run the
 * same task, but the {@code ON DUPLICATE KEY UPDATE} clause makes the
 * second writer a no-op rewrite — no race condition, no double-counting.
 *
 * <p>Operational toggle: {@code REBATE_ROLLUP_ENABLED=false} disables the
 * scheduler at startup. Backfill from V8 migration covers historical
 * data; the scheduler only handles today→tomorrow boundary.
 *
 * <p>Why not Quartz / cron container: the existing codebase uses
 * {@code java.util.Timer} / {@code ScheduledExecutorService} elsewhere
 * (see {@code BackendUtils}, {@code AgentUtils}). Following the same
 * pattern keeps the deploy footprint identical — no new container, no
 * crontab entry on the host.
 */
public final class RebateRollupScheduler {

    private static final Logger logger = Logger.getLogger("backend");
    private static volatile ScheduledExecutorService executor;
    private static volatile ScheduledFuture<?> scheduledTask;

    private RebateRollupScheduler() {}

    private static boolean isEnabled() {
        String v = System.getenv("REBATE_ROLLUP_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    /** Run hour (UTC) — 00:30 by default, override via {@code REBATE_ROLLUP_HOUR_UTC}. */
    private static int runHour() {
        String v = System.getenv("REBATE_ROLLUP_HOUR_UTC");
        if (v == null) return 0;
        try { return Math.max(0, Math.min(23, Integer.parseInt(v))); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static int runMinute() {
        String v = System.getenv("REBATE_ROLLUP_MINUTE_UTC");
        if (v == null) return 30;
        try { return Math.max(0, Math.min(59, Integer.parseInt(v))); }
        catch (NumberFormatException ignored) { return 30; }
    }

    public static synchronized void start() {
        if (!isEnabled()) {
            logger.info("RebateRollupScheduler: disabled by env REBATE_ROLLUP_ENABLED=false");
            return;
        }
        if (executor != null) return; // already started

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rebate-rollup-scheduler");
            t.setDaemon(true);
            return t;
        });

        long initialDelaySec = secondsUntilNextRun();
        long periodSec = TimeUnit.DAYS.toSeconds(1);
        scheduledTask = executor.scheduleAtFixedRate(
                RebateRollupScheduler::runOnce,
                initialDelaySec,
                periodSec,
                TimeUnit.SECONDS);

        logger.info("RebateRollupScheduler: scheduled, first run in " + initialDelaySec + "s");
    }

    public static synchronized void stop() {
        if (scheduledTask != null) { scheduledTask.cancel(true); scheduledTask = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
    }

    private static long secondsUntilNextRun() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime next = now.with(LocalTime.of(runHour(), runMinute()));
        if (!next.isAfter(now)) next = next.plusDays(1);
        return Math.max(60, ChronoUnit.SECONDS.between(now, next));
    }

    /**
     * Idempotent rollup of rebate_logs for the just-completed day.
     * Uses ON DUPLICATE KEY UPDATE so concurrent runs from multiple
     * backend-api instances coalesce safely.
     */
    static void runOnce() {
        long t0 = System.currentTimeMillis();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
            String sql =
                "INSERT INTO rebate_daily_rollup " +
                "  (agent_user_id, rollup_date, rebate_type, sum_bet_amount, sum_commission, row_count) " +
                "SELECT agent_user_id, " +
                "       DATE(created_at)                  AS rollup_date, " +
                "       rebate_type, " +
                "       COALESCE(SUM(total_f1_volume), 0) AS sum_bet_amount, " +
                "       COALESCE(SUM(rebate_amount), 0)   AS sum_commission, " +
                "       COUNT(*)                          AS row_count " +
                "  FROM rebate_logs " +
                " WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY) " +
                "   AND status = 'PAID' " +
                " GROUP BY agent_user_id, DATE(created_at), rebate_type " +
                "ON DUPLICATE KEY UPDATE " +
                "  sum_bet_amount = VALUES(sum_bet_amount), " +
                "  sum_commission = VALUES(sum_commission), " +
                "  row_count      = VALUES(row_count), " +
                "  updated_at     = CURRENT_TIMESTAMP";
            ps = conn.prepareStatement(sql);
            int affected = ps.executeUpdate();
            long ms = System.currentTimeMillis() - t0;
            logger.info("RebateRollupScheduler: aggregated " + affected + " rollup row(s) in " + ms + "ms");
        } catch (Exception e) {
            logger.error("RebateRollupScheduler: run failed at " + LocalDateTime.now(ZoneOffset.UTC), e);
        } finally {
            if (ps != null) try { ps.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }
}
