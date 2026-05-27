package com.vinplay.api.backend.perf;

import com.vinplay.dal.audit.TelegramOpsNotifier;
import com.vinplay.dal.service.seamless.AggregatorMetrics;
import com.vinplay.dal.service.seamless.AggregatorMetrics.Snapshot;
import org.apache.log4j.Logger;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Phase 5p3 — periodic scan of {@link AggregatorMetrics} for slow-handler
 * conditions. Polls every 60s; for each registered aggregator name takes
 * a snapshot, fires {@link TelegramOpsNotifier#alertHandlerSlowP99} when
 * p99 exceeds {@value #P99_ALERT_THRESHOLD_MS}ms, and INFO-logs every
 * snapshot for the GSC handlers (skipping empty-window cases).
 *
 * <p><b>Why a separate scheduler from the per-call WARN.</b> An
 * individual 60ms blip is normal during Mongo retry; firing Telegram on
 * every spike would page ops dozens of times an hour. The per-call WARN
 * (in {@link com.vinplay.dal.service.seamless.SeamlessWalletAggregator})
 * gives ops a per-request log signal; this scheduler's p99-over-window
 * gate is what actually pages.
 *
 * <p>Operational toggle: {@code AGGREGATOR_P99_SCHEDULER_ENABLED=false}
 * disables at startup.
 *
 * <p>Single-threaded; daemon thread; uses {@code scheduleWithFixedDelay}
 * (NOT {@code scheduleAtFixedRate}) so a long-running snapshot pass never
 * pile-ups behind itself.
 */
public final class AggregatorP99Scheduler {

    private static final Logger logger = Logger.getLogger("backend");

    /**
     * P99 threshold over the rolling window before we page ops. Default 100ms;
     * override per environment via env var {@code GSC_P99_THRESHOLD_MS}.
     * Staging traffic is bursty (low concurrent users → single slow call inflates
     * p99) and the single-node Hazelcast adds 30-80ms to every map op, so
     * staging typically wants 250-500ms. Production keeps the 100ms default.
     */
    static final long P99_ALERT_THRESHOLD_MS;
    static {
        long t = 100L;
        try {
            String v = System.getenv("GSC_P99_THRESHOLD_MS");
            if (v != null && !v.isEmpty()) t = Long.parseLong(v.trim());
        } catch (Throwable ignored) { /* default */ }
        P99_ALERT_THRESHOLD_MS = t;
    }

    /** Poll period — once per minute lines up with the metric window. */
    static final long PERIOD_SECONDS = 60L;

    private static volatile ScheduledExecutorService executor;
    private static volatile ScheduledFuture<?> scheduledTask;

    private AggregatorP99Scheduler() {}

    private static boolean isEnabled() {
        String v = System.getenv("AGGREGATOR_P99_SCHEDULER_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    public static synchronized void start() {
        if (!isEnabled()) {
            logger.info("AggregatorP99Scheduler: disabled by env "
                    + "AGGREGATOR_P99_SCHEDULER_ENABLED=false");
            return;
        }
        if (executor != null) return; // already started

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "aggregator-p99-scheduler");
            t.setDaemon(true);
            return t;
        });

        // Start one period in to give the system a chance to record some
        // samples first; otherwise the first run logs only zeros.
        scheduledTask = executor.scheduleWithFixedDelay(
                AggregatorP99Scheduler::runOnce,
                PERIOD_SECONDS,
                PERIOD_SECONDS,
                TimeUnit.SECONDS);

        logger.info("AggregatorP99Scheduler: scheduled, period=" + PERIOD_SECONDS + "s, "
                + "p99 threshold=" + P99_ALERT_THRESHOLD_MS + "ms");
    }

    public static synchronized void stop() {
        if (scheduledTask != null) { scheduledTask.cancel(true); scheduledTask = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
    }

    /**
     * One pass over all registered aggregator names. Catches Throwable so a
     * bug here can't kill the daemon thread.
     */
    static void runOnce() {
        try {
            Set<String> names = AggregatorMetrics.registeredNames();
            if (names.isEmpty()) return;

            for (String name : names) {
                Snapshot snap;
                try {
                    snap = AggregatorMetrics.snapshot(name);
                } catch (Throwable t) {
                    logger.warn("AggregatorP99Scheduler: snapshot(" + name + ") failed: "
                            + t.getMessage());
                    continue;
                }
                if (snap.count == 0) continue;

                if (snap.p99Ms > P99_ALERT_THRESHOLD_MS) {
                    logger.warn("AggregatorP99Scheduler: SLOW handler "
                            + name + " " + snap);
                    try {
                        TelegramOpsNotifier.alertHandlerSlowP99(
                                name, snap.p99Ms, snap.count);
                    } catch (Throwable t) {
                        logger.warn("AggregatorP99Scheduler: alert send failed for "
                                + name + ": " + t.getMessage());
                    }
                } else {
                    logger.info("AggregatorP99Scheduler: " + snap);
                }
            }
        } catch (Throwable t) {
            // Never let the periodic task die.
            logger.error("AggregatorP99Scheduler: runOnce error", t);
        }
    }
}
