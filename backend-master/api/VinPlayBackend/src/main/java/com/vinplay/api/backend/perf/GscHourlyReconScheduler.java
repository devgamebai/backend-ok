package com.vinplay.api.backend.perf;

import com.vinplay.dal.service.GscHourlyRecon;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Hourly cross-check scheduler — pairs the existing 5-minute
 * {@link com.vinplay.dal.service.GscWagerReconciler} (which walks our
 * records) with a complementary scan that walks GSC's authoritative
 * wager-list (3.2 API) so bets we never recorded show up too.
 *
 * <p>Lifecycle: {@link #start()} called once from
 * {@code VinPlayBackendMain}. Schedules
 * {@link GscHourlyRecon#reconcileOnce()} at a fixed rate (default
 * every 60 minutes, override via {@code GSC_HOURLY_RECON_PERIOD_SEC}).
 *
 * <p>Multi-instance safe: every wallet movement flows through
 * {@code MoneyGateway} with a {@code (tx_id=wager_code, source=...)}
 * UNIQUE — concurrent runs across instances dedup naturally.
 *
 * <p>Operational toggle: {@code GSC_HOURLY_RECON_ENABLED=false}
 * disables at startup. Useful when investigating an upstream GSC
 * outage that would cause the recon to spam logs.
 */
public final class GscHourlyReconScheduler {

    private static final Logger logger = Logger.getLogger("backend");
    private static volatile ScheduledExecutorService executor;
    private static volatile ScheduledFuture<?> scheduledTask;

    private GscHourlyReconScheduler() {}

    private static boolean isEnabled() {
        String v = System.getenv("GSC_HOURLY_RECON_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    private static long periodSeconds() {
        String v = System.getenv("GSC_HOURLY_RECON_PERIOD_SEC");
        if (v == null) return 3600L;          // 60 min default
        try { return Math.max(300L, Long.parseLong(v)); }
        catch (NumberFormatException ignored) { return 3600L; }
    }

    private static long initialDelaySeconds() {
        String v = System.getenv("GSC_HOURLY_RECON_INITIAL_DELAY_SEC");
        if (v == null) return 600L;           // 10 min after startup so 5-min recon settles first
        try { return Math.max(0L, Long.parseLong(v)); }
        catch (NumberFormatException ignored) { return 600L; }
    }

    public static synchronized void start() {
        if (!isEnabled()) {
            logger.info("GscHourlyReconScheduler: disabled by env GSC_HOURLY_RECON_ENABLED=false");
            return;
        }
        if (executor != null) return;

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gsc-hourly-recon");
            t.setDaemon(true);
            return t;
        });

        long initialDelay = initialDelaySeconds();
        long period = periodSeconds();
        scheduledTask = executor.scheduleAtFixedRate(
                GscHourlyReconScheduler::runOnce,
                initialDelay,
                period,
                TimeUnit.SECONDS);

        logger.info("GscHourlyReconScheduler: scheduled — first run in "
                + initialDelay + "s, period " + period + "s");
    }

    public static synchronized void stop() {
        if (scheduledTask != null) { scheduledTask.cancel(true); scheduledTask = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
    }

    private static void runOnce() {
        long t0 = System.currentTimeMillis();
        try {
            GscHourlyRecon.Outcome o = GscHourlyRecon.reconcileOnce();
            long ms = System.currentTimeMillis() - t0;
            // Always log the tick — operators want to see "0 discrepancies"
            // mornings as a positive signal that the cross-check ran.
            logger.info("GscHourlyRecon tick: " + o + " in " + ms + "ms");
        } catch (Throwable t) {
            // Never let the scheduled task die on an exception — that
            // would silently kill the recon until the next process
            // restart.
            logger.error("GscHourlyRecon tick threw — recon paused until next period", t);
        }
    }
}
