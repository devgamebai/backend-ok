package com.sunwinkr.lottery.engine.bet;

import com.sunwinkr.lottery.engine.model.LotteryMode;

/**
 * SUN-1295 snapshot — captures {@link LotteryMode#getRate()} and
 * {@link LotteryMode#getPrizeMultiplier()} at PURCHASE TIME so a future
 * rate change cannot retroactively rewrite a pending bet's payout.
 *
 * <p>Stamped onto the {@code lode} row at insert time
 * ({@code rate_at_purchase} + {@code prize_multiplier}). Settle reads
 * these values back from the row, NEVER from the live enum.
 *
 * <p>Closes the time-of-check / time-of-use bug documented in
 * {@code LotteryModule.computePrize} (legacy JLM:284-369) and per the
 * test {@code Sun1295RateSnapshotTest}.
 */
public final class BetSnapshot {

    private final int rateAtPurchase;
    private final int prizeMultiplierAtPurchase;

    private BetSnapshot(int rateAtPurchase, int prizeMultiplierAtPurchase) {
        this.rateAtPurchase = rateAtPurchase;
        this.prizeMultiplierAtPurchase = prizeMultiplierAtPurchase;
    }

    /**
     * Snapshot the current state of {@code mode}. Returned value is
     * immutable — even if {@code mode} is mutated later (or replaced —
     * see {@code docs/specs/lottery-rules-spec.md §9 INV-LOTTERY-05}), the
     * captured snapshot stays stable.
     */
    public static BetSnapshot of(LotteryMode mode) {
        return new BetSnapshot(mode.getRate(), mode.getPrizeMultiplier());
    }

    public int getRateAtPurchase() {
        return rateAtPurchase;
    }

    public int getPrizeMultiplierAtPurchase() {
        return prizeMultiplierAtPurchase;
    }
}
