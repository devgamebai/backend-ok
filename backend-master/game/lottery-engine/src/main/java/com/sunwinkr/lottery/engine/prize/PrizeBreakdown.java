package com.sunwinkr.lottery.engine.prize;

/**
 * Per-ticket settle outcome — what {@link PrizeCalculator#calculate}
 * returns to the settle loop.
 *
 * <p>{@code prize} is the credit amount; {@code matches} is for audit /
 * telemetry only (per-match modes 1, 2 expose how many lines hit);
 * {@code refund} is reserved for the future "refund-on-scrape-failure"
 * mode (AMBIGUOUS #8) — today always 0.
 */
public final class PrizeBreakdown {

    public static final PrizeBreakdown ZERO = new PrizeBreakdown(0L, 0, 0L);

    private final long prize;
    private final int matches;
    private final long refund;

    public PrizeBreakdown(long prize, int matches, long refund) {
        this.prize = prize;
        this.matches = matches;
        this.refund = refund;
    }

    public long getPrize() {
        return prize;
    }

    public int getMatches() {
        return matches;
    }

    public long getRefund() {
        return refund;
    }
}
