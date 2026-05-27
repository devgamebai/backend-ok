package com.sunwinkr.minigame.engine.prize;

/**
 * Settlement fee calculator. Direct port of TXR:1318 / spec §5:
 * <pre>fee = (long)(tax * totalPrize / (200 - tax))</pre>
 *
 * <p>Tax is in percent (e.g. {@code 5.0f} for TaiXiu). The {@code 200 -
 * tax} denominator gives the back-out fee from a prize that already
 * netted the tax (legacy convention).
 *
 * <p>Plan §2.6 row S3.
 */
public final class FeeCalc {

    private FeeCalc() {
        // utility
    }

    /**
     * @param totalPrize total prize amount (bet + winnings)
     * @param tax        tax percent — e.g. {@code 5.0f}
     * @return fee in {@code long} units (truncated, matches legacy cast)
     */
    public static long fee(long totalPrize, float tax) {
        if (totalPrize <= 0L) {
            return 0L;
        }
        double denom = 200.0 - (double) tax;
        if (denom <= 0.0) {
            return 0L;
        }
        return (long) ((double) tax * (double) totalPrize / denom);
    }
}
