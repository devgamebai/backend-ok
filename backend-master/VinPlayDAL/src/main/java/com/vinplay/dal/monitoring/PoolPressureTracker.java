package com.vinplay.dal.monitoring;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 5 prep gate 5p4 — stateful consecutive-sample counter for pool
 * utilization alerting.
 *
 * <p>{@link #recordSample(double)} returns {@code true} exactly when the
 * consecutive-above-threshold sample count first reaches
 * {@code consecutiveThreshold}; the counter is then reset to 0 so the
 * next alert requires another full streak. This avoids per-tick spam
 * during sustained pressure — the Telegram-side throttle in
 * {@code TelegramOpsNotifier.sendThrottled} is the second line of
 * defense.
 *
 * <p>Lives in {@code VinPlayDAL} (not in {@code VinPlayBackend} next to
 * the scheduler) so the consecutive-counter logic can be unit-tested
 * without pulling the API server's classpath into the test fixture.
 */
public final class PoolPressureTracker {

    private final double thresholdPct;
    private final int consecutiveThreshold;
    private final AtomicInteger consecutive = new AtomicInteger(0);

    public PoolPressureTracker(double thresholdPct, int consecutiveThreshold) {
        this.thresholdPct = thresholdPct;
        this.consecutiveThreshold = consecutiveThreshold;
    }

    /**
     * @return {@code true} if this sample completed a streak of
     *         {@code consecutiveThreshold} above-threshold samples
     *         (caller should fire the alert); {@code false} otherwise.
     */
    public boolean recordSample(double utilizationPct) {
        if (utilizationPct > thresholdPct) {
            int n = consecutive.incrementAndGet();
            if (n >= consecutiveThreshold) {
                consecutive.set(0);
                return true;
            }
            return false;
        }
        consecutive.set(0);
        return false;
    }

    public int currentStreak() {
        return consecutive.get();
    }

    public void reset() {
        consecutive.set(0);
    }
}
