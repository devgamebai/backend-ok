package com.sunwinkr.minigame.engine.prize;

import com.sunwinkr.minigame.engine.bet.PotState;

/**
 * Immutable input snapshot for {@link PrizeCalculator}. Captures only
 * the pot states + result + tax/balanceGate flags needed for prize math,
 * so the calculator stays pure (no coupling to
 * {@link com.sunwinkr.minigame.engine.core.TaiXiuRound}).
 *
 * <p>Plan §2.4 row P3.
 */
public final class RoundSnapshot {

    public final long referenceId;
    public final PotState potTai;
    public final PotState potXiu;
    /** {@code 0} = Xỉu wins, {@code 1} = Tài wins. */
    public final short result;
    /** Tax percent (e.g. {@code 5.0f}). */
    public final float taxPct;
    /**
     * {@code balanceGate} flag from {@code MGRoomTaiXiu.balanceGate} (TXR:861).
     * <b>AMBIGUOUS — semantics inverted vs intent (TODO(SUN-BAL-INV)).</b>
     * Preserved here: when {@code true}, the winning-side calc pays out
     * the full {@code tran.betValue} (no cross-pot trim).
     */
    public final boolean balanceGate;

    public RoundSnapshot(long referenceId,
                         PotState potTai,
                         PotState potXiu,
                         short result,
                         float taxPct,
                         boolean balanceGate) {
        if (potTai == null) {
            throw new NullPointerException("potTai");
        }
        if (potXiu == null) {
            throw new NullPointerException("potXiu");
        }
        this.referenceId = referenceId;
        this.potTai = potTai;
        this.potXiu = potXiu;
        this.result = result;
        this.taxPct = taxPct;
        this.balanceGate = balanceGate;
    }
}
