package com.sunwinkr.minigame.engine.sicbo.prize;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;

/**
 * Per-bet prize result computed by {@link SicboPrizeCalculator}.
 *
 * <p>Immutable record paralleling the legacy {@code TransactionTaiXiuDetail}
 * mutations at SBR:1071-1072:
 * <pre>{@code
 * tx2.prize = ... bet * rotation ... or occurrence-special ...;
 * long fee = (long)(tax * (float)tx2.prize / (200.0f - tax));
 * }</pre>
 *
 * <h3>Refund flag (exploit guard)</h3>
 * When {@link #refund} is {@code true} (exploit guard fired — null dice or
 * empty winning-statuses), {@link #prize} and {@link #fee} are both 0 and
 * the adapter MUST route the bet through {@code TransType.END_TRANS} for
 * a full refund of {@link SicboPotEntry#betValue}. See
 * {@code SicboPrizeCalculator} javadoc for the Quochuy98 incident.
 */
public final class SicboPrizePerBet {

    /** The bet this prize result corresponds to (back-pointer for adapters). */
    public final SicboPotEntry bet;

    /** Gross prize amount paid out to the user (0 if not a winner / refund). */
    public final long prize;

    /** House fee deducted from {@link #prize} (0 when prize is 0). */
    public final long fee;

    /**
     * When {@code true}, the exploit guard fired and this entry should be
     * refunded in full (regardless of {@link SicboPotEntry#betSideId}).
     * {@link #prize} and {@link #fee} are both 0 in this case.
     */
    public final boolean refund;

    public SicboPrizePerBet(SicboPotEntry bet, long prize, long fee, boolean refund) {
        this.bet = bet;
        this.prize = prize;
        this.fee = fee;
        this.refund = refund;
    }

    /** Convenience factory: a non-winning bet with no payout and no refund. */
    public static SicboPrizePerBet none(SicboPotEntry bet) {
        return new SicboPrizePerBet(bet, 0L, 0L, false);
    }

    /** Convenience factory: a winning bet with prize + fee. */
    public static SicboPrizePerBet win(SicboPotEntry bet, long prize, long fee) {
        return new SicboPrizePerBet(bet, prize, fee, false);
    }

    /** Convenience factory: exploit-guard refund (full betValue). */
    public static SicboPrizePerBet refund(SicboPotEntry bet) {
        return new SicboPrizePerBet(bet, 0L, 0L, true);
    }
}
