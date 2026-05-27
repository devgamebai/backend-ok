package com.sunwinkr.lottery.engine.settle;

/**
 * Per-run settle telemetry. {@code settledCount} includes both winning
 * and losing tickets (anything we transitioned out of pending).
 * {@code failureCount} is wallet-credit OR DB-update failures — those
 * tickets remain pending and will be retried on the next ingest pass.
 */
public final class SettleSummary {

    public static final SettleSummary EMPTY = new SettleSummary(0, 0, 0L);

    private final int settledCount;
    private final int failureCount;
    private final long totalPrizeCredited;

    public SettleSummary(int settledCount, int failureCount, long totalPrizeCredited) {
        this.settledCount = settledCount;
        this.failureCount = failureCount;
        this.totalPrizeCredited = totalPrizeCredited;
    }

    public int getSettledCount() {
        return settledCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public long getTotalPrizeCredited() {
        return totalPrizeCredited;
    }
}
