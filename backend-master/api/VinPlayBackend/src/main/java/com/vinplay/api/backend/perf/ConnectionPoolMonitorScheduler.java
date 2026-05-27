package com.vinplay.api.backend.perf;

import com.vinplay.dal.audit.TelegramOpsNotifier;
import com.vinplay.dal.monitoring.ConnectionPoolMonitor;
import com.vinplay.dal.monitoring.PoolPressureTracker;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Phase 5 prep gate 5p4 — periodic pool-utilization monitor.
 *
 * <p>Lifecycle: {@link #start()} is called once from
 * {@code VinPlayBackendMain} alongside the other schedulers. Samples
 * {@code mysqlpoolname}'s active-connection count every
 * {@link #PERIOD_SECONDS} via {@link ConnectionPoolMonitor#sampleOnce()}
 * and fires {@link TelegramOpsNotifier#alertPoolPressure} when
 * utilization exceeds {@link #UTIL_THRESHOLD_PCT} for
 * {@link #CONSECUTIVE_THRESHOLD} samples in a row.
 *
 * <p>Lives next to {@code GscWagerReconcilerScheduler} so ops sees
 * pool pressure with the same alerting plumbing as ledger-write
 * failures. The 80% / 2-sample policy is a tunable: tighten if false
 * negatives appear in the 200 TPS smoke test (5p5).
 *
 * <p>Operational toggle: {@code POOL_MONITOR_ENABLED=false} disables
 * the scheduler at startup.
 */
public final class ConnectionPoolMonitorScheduler {

    private static final Logger logger = Logger.getLogger("backend");

    /** Sample period — every 30s. */
    static final long PERIOD_SECONDS = 30L;
    /** ~30s after startup so the pool finishes its first connection probe. */
    static final long INITIAL_DELAY_SECONDS = 30L;
    /** Utilization% threshold above which a sample counts as "high". */
    static final double UTIL_THRESHOLD_PCT = 80.0;
    /** Number of consecutive high-utilization samples that triggers an alert. */
    static final int CONSECUTIVE_THRESHOLD = 2;

    private static volatile ScheduledExecutorService executor;
    private static volatile ScheduledFuture<?> scheduledTask;

    /** Tracks consecutive high-utilization samples across ticks. */
    private static final PoolPressureTracker TRACKER =
            new PoolPressureTracker(UTIL_THRESHOLD_PCT, CONSECUTIVE_THRESHOLD);

    private ConnectionPoolMonitorScheduler() {}

    private static boolean isEnabled() {
        String v = System.getenv("POOL_MONITOR_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    public static synchronized void start() {
        if (!isEnabled()) {
            logger.info("ConnectionPoolMonitorScheduler: disabled by env POOL_MONITOR_ENABLED=false");
            return;
        }
        if (executor != null) return;

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pool-utilization-monitor");
            t.setDaemon(true);
            return t;
        });

        scheduledTask = executor.scheduleWithFixedDelay(
                ConnectionPoolMonitorScheduler::runOnce,
                INITIAL_DELAY_SECONDS,
                PERIOD_SECONDS,
                TimeUnit.SECONDS);

        logger.info("ConnectionPoolMonitorScheduler: scheduled — first run in "
                + INITIAL_DELAY_SECONDS + "s, period " + PERIOD_SECONDS + "s, "
                + "threshold " + UTIL_THRESHOLD_PCT + "% × "
                + CONSECUTIVE_THRESHOLD + " consecutive samples");
    }

    public static synchronized void stop() {
        if (scheduledTask != null) { scheduledTask.cancel(true); scheduledTask = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
        TRACKER.reset();
    }

    private static void runOnce() {
        try {
            ConnectionPoolMonitor.SampleResult s = ConnectionPoolMonitor.sampleOnce();
            if (!s.isAvailable()) {
                // ConnectionPoolMonitor already WARN-logged the cause —
                // skip the alerting tick but keep the schedule alive.
                return;
            }
            logger.info("pool-monitor tick: " + s);
            if (TRACKER.recordSample(s.utilizationPct)) {
                try {
                    TelegramOpsNotifier.alertPoolPressure(
                            s.poolName, s.utilizationPct, s.active, s.maxPoolSize);
                } catch (Throwable t) {
                    logger.warn("pool-monitor: alert send failed (ignored): " + t.getMessage());
                }
            }
        } catch (Throwable t) {
            // Catch Throwable: a scheduled task that throws is silently
            // cancelled by ScheduledExecutorService, which would leave the
            // monitor dead until backend-api restart.
            logger.error("pool-monitor tick threw — schedule continues", t);
        }
    }

}
