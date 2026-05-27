package com.vinplay.api.backend.perf;

import com.vinplay.dal.service.GscWagerReconciler;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SUN-1204 — periodic reconciler for GSC live-casino wagers whose
 * deposit/settle event was never delivered (Dream Gaming, Evolution,
 * Pragmatic, AI Live, etc.). See
 * {@link com.vinplay.dal.service.GscWagerReconciler} for the data
 * pipeline.
 *
 * <p>Lifecycle: {@link #start()} is called once from
 * {@code VinPlayBackendMain}. Schedules
 * {@link GscWagerReconciler#reconcileOnce()} at a fixed rate (default
 * every 5 minutes, override via {@code GSC_RECONCILER_PERIOD_SEC}).
 *
 * <p>Multiple backend-api instances are safe to run the scheduler
 * concurrently: {@code MoneyGateway.creditUser} dedup-keys on
 * {@code (tx_id=wager_code, source=GSC_RECONCILE)} so the second
 * writer's credit is silently ignored. Mongo {@code updateOne}'s
 * {@code $set} is also idempotent.
 *
 * <p>Operational toggle: {@code GSC_RECONCILER_ENABLED=false} disables
 * at startup. Useful when investigating an upstream GSC outage that
 * would cause the reconciler to spam logs.
 */
public final class GscWagerReconcilerScheduler {

    private static final Logger logger = Logger.getLogger("backend");
    private static volatile ScheduledExecutorService executor;
    private static volatile ScheduledFuture<?> scheduledTask;

    private GscWagerReconcilerScheduler() {}

    private static boolean isEnabled() {
        String v = System.getenv("GSC_RECONCILER_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    private static long periodSeconds() {
        String v = System.getenv("GSC_RECONCILER_PERIOD_SEC");
        if (v == null) return 300L;          // 5 min default
        try { return Math.max(60L, Long.parseLong(v)); }
        catch (NumberFormatException ignored) { return 300L; }
    }

    private static long initialDelaySeconds() {
        String v = System.getenv("GSC_RECONCILER_INITIAL_DELAY_SEC");
        if (v == null) return 90L;           // ~90s after startup so DAL connections are warm
        try { return Math.max(0L, Long.parseLong(v)); }
        catch (NumberFormatException ignored) { return 90L; }
    }

    public static synchronized void start() {
        if (!isEnabled()) {
            logger.info("GscWagerReconcilerScheduler: disabled by env GSC_RECONCILER_ENABLED=false");
            return;
        }
        if (executor != null) return;

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gsc-wager-reconciler");
            t.setDaemon(true);
            return t;
        });

        long initialDelay = initialDelaySeconds();
        long period = periodSeconds();
        scheduledTask = executor.scheduleAtFixedRate(
                GscWagerReconcilerScheduler::runOnce,
                initialDelay,
                period,
                TimeUnit.SECONDS);

        logger.info("GscWagerReconcilerScheduler: scheduled — first run in "
                + initialDelay + "s, period " + period + "s");
    }

    public static synchronized void stop() {
        if (scheduledTask != null) { scheduledTask.cancel(true); scheduledTask = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
    }

    private static void runOnce() {
        long t0 = System.currentTimeMillis();
        try {
            GscWagerReconciler.Outcome o = GscWagerReconciler.reconcileOnce();
            long ms = System.currentTimeMillis() - t0;
            // Only log when there's actual activity OR an error — otherwise
            // a quiet 0/0/0/0 run every 5 min would clutter the log.
            if (o.scanned > 0) {
                logger.info("GscWagerReconciler tick: " + o + " in " + ms + "ms");
            }
        } catch (Throwable t) {
            // Catch Throwable, not Exception — a scheduled task that throws
            // ANY exception is silently cancelled by ScheduledExecutorService,
            // which would leave the reconciler dead until next backend-api
            // restart. Logging + swallowing keeps the schedule alive.
            logger.error("GscWagerReconciler tick threw — schedule continues", t);
        }

        // SUN-1205/1206 — rebate drift correction for hedge-prone live-casino
        // providers. Dream Gaming (1052) ships valid_bet_amount=bet_amount in
        // the seamless BET push (placeholder); the post-hedge truth lives in
        // GSC's wager-detail PULL API. Run a separate pass per provider so a
        // single bad provider can't starve the others; today's only enrolled
        // provider is Dream but the loop is set up to scale.
        for (int productCode : DRIFT_PROVIDERS) {
            long dt0 = System.currentTimeMillis();
            try {
                GscWagerReconciler.DriftOutcome d =
                        GscWagerReconciler.reconcileRebateDriftOnce(productCode);
                long dms = System.currentTimeMillis() - dt0;
                if (d.scanned > 0) {
                    logger.info("GscWagerReconciler drift pc=" + productCode
                            + ": " + d + " in " + dms + "ms");
                }
            } catch (Throwable t) {
                logger.error("GscWagerReconciler drift pc=" + productCode
                        + " threw — schedule continues", t);
            }
        }
    }

    // Providers whose seamless BET push carries a placeholder valid_bet_amount
    // and therefore need a wager-detail pull pass to correct hedge drift.
    // Add new product_codes here as additional live-casino integrations land.
    private static final int[] DRIFT_PROVIDERS = { 1052 };
}
