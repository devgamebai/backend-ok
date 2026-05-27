package com.sunwinkr.minigame.engine.prize;

import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;

import java.util.Map;

/**
 * Multi-bet same-side aggregator. Direct port of
 * {@code MGRoomTaiXiu.updateSumTran} (TXR:986-1009).
 *
 * <h3>Behavior</h3>
 * For a {@code (username, betSide)} pair already present in the map,
 * sums {@code betValue + prize + refund + jp} into the existing entry.
 * If the existing entry has a different side, it is left untouched and
 * the new detail is silently dropped (legacy behavior — the legacy code
 * only writes when {@code txt.betSide == tranDetail.betSide}).
 *
 * <p>Plan §2.4 row P5.
 */
public final class UserAggregator {

    private UserAggregator() {
        // utility
    }

    /** Per-user same-side aggregate. */
    public static final class Aggregate {
        public long referenceId;
        public int userId;
        public String username;
        public int moneyType;
        public int betSide;
        public long betValue;
        public long totalPrize;
        public long totalRefund;
        public long totalJp;
    }

    /**
     * Merge {@code tranDetail} (plus accompanying prize/refund/jp) into
     * the supplied map. Mirrors {@code updateSumTran} (TXR:986-1009).
     *
     * @param map       per-user aggregate map (keyed by username)
     * @param tran      bet contributor record
     * @param prize     prize amount for THIS bet (0 if losing)
     * @param refund    refund amount for THIS bet
     * @param jpAmount  jackpot share for THIS bet (0 if no jp)
     */
    public static void merge(Map<String, Aggregate> map,
                             TransactionTaiXiuDetail tran,
                             long prize,
                             long refund,
                             long jpAmount) {
        if (map == null) {
            throw new NullPointerException("map");
        }
        if (tran == null) {
            throw new NullPointerException("tran");
        }
        Aggregate existing = map.get(tran.username);
        if (existing != null) {
            // Legacy: only merge when betSide matches (TXR:989).
            if (existing.betSide == tran.betSide) {
                existing.betValue += tran.betValue;
                existing.totalPrize += prize;
                existing.totalRefund += refund;
                existing.totalJp += jpAmount;
            }
            return;
        }
        Aggregate agg = new Aggregate();
        agg.referenceId = tran.referenceId;
        agg.userId = tran.userId;
        agg.username = tran.username;
        agg.moneyType = tran.moneyType;
        agg.betSide = tran.betSide;
        agg.betValue = tran.betValue;
        agg.totalPrize = prize;
        agg.totalRefund = refund;
        agg.totalJp = jpAmount;
        map.put(tran.username, agg);
    }
}
