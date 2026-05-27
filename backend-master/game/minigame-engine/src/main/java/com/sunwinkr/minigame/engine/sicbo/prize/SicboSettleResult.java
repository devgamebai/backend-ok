package com.sunwinkr.minigame.engine.sicbo.prize;

import java.util.Collections;
import java.util.List;

/**
 * Aggregate result of one Sicbo settlement pass.
 *
 * <p>Returned by {@link SicboPrizeCalculator#calculate(java.util.List, short[])}.
 * Encapsulates everything the adapter layer needs to write money + emit
 * client messages without re-walking the bets.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@link #perBet} — per-bet prize/fee/refund results in input order</li>
 *   <li>{@link #totalPayout} — sum of all {@code prize} amounts paid to users
 *       (drives fund decrement: {@code fund_new = fund_old + (totalBet - totalPayout)})</li>
 *   <li>{@link #totalFee} — sum of all house fees</li>
 *   <li>{@link #totalRefund} — sum of all refunded {@code betValue} when the
 *       exploit guard fired (Quochuy98 protection)</li>
 *   <li>{@link #exploitGuardFired} — {@code true} when the guard refunded
 *       all bets (null dice or empty winning-statuses)</li>
 * </ul>
 */
public final class SicboSettleResult {

    public final List<SicboPrizePerBet> perBet;
    public final long totalPayout;
    public final long totalFee;
    public final long totalRefund;
    public final boolean exploitGuardFired;

    public SicboSettleResult(List<SicboPrizePerBet> perBet,
                             long totalPayout,
                             long totalFee,
                             long totalRefund,
                             boolean exploitGuardFired) {
        this.perBet = Collections.unmodifiableList(perBet);
        this.totalPayout = totalPayout;
        this.totalFee = totalFee;
        this.totalRefund = totalRefund;
        this.exploitGuardFired = exploitGuardFired;
    }
}
