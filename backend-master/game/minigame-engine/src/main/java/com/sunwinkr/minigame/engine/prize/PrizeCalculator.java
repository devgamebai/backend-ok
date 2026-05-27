package com.sunwinkr.minigame.engine.prize;

import com.sunwinkr.minigame.engine.bet.PotState;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;

import java.util.List;

/**
 * Pure-function TaiXiu prize calculator. Direct port of
 * {@code MGRoomTaiXiu.calculatePrize} (TXR:825-984).
 *
 * <h3>Two branches</h3>
 * <ul>
 *   <li>{@code result == 0} (Xỉu wins) → Xỉu side gets prize; Tài
 *       contributors get refund only.</li>
 *   <li>{@code result == 1} (Tài wins) → Tài side gets prize; Xỉu
 *       contributors get refund only.</li>
 * </ul>
 *
 * <h3>Winning formula (spec INV-7)</h3>
 * <pre>
 *   tienDuocTinh   = bet (or trimmed by cross-pot if balanceGate=false)
 *   prize          = (long)(tienDuocTinh * (100 - tax) / 100) + tienDuocTinh
 *   refund         = bet - tienDuocTinh
 * </pre>
 *
 * <h3>balanceGate quirk</h3>
 * <b>AMBIGUOUS (TODO(SUN-BAL-INV)):</b> when {@code balanceGate == true}
 * the cross-pot trim is bypassed and {@code tienDuocTinh = tran.betValue}.
 * Semantics look inverted vs intent. Preserved here for behavior match.
 *
 * <p>Plan §2.4 rows P3, P4. Spec INV-7.
 */
public final class PrizeCalculator {

    private PrizeCalculator() {
        // utility
    }

    /**
     * Compute settlement aggregates for the round.
     *
     * @param snap round snapshot — pots + result + tax + balanceGate
     * @return immutable {@link SettleResult}
     */
    public static SettleResult calculate(RoundSnapshot snap) {
        if (snap == null) {
            throw new NullPointerException("snap");
        }
        PotState potTai = snap.potTai;
        PotState potXiu = snap.potXiu;
        long tongTienHopLe = CrossPotBalancer.legalAmount(potTai, potXiu);

        SettleResult.Builder b = new SettleResult.Builder();
        b.result = snap.result;

        switch (snap.result) {
            case (short) 0:
                // Xỉu wins (TXR:853-899). Xỉu gets prize; Tài refund only.
                allocateWinning(potXiu, tongTienHopLe, snap, b.sumXiu, b.sumAll, b, /*onTaiSide*/ false);
                allocateRefundOnly(potTai, tongTienHopLe, snap, b.sumTai, b.sumAll, b, /*onTaiSide*/ true);
                break;
            case (short) 1:
                // Tài wins (TXR:901-947). Tài gets prize; Xỉu refund only.
                allocateWinning(potTai, tongTienHopLe, snap, b.sumTai, b.sumAll, b, /*onTaiSide*/ true);
                allocateRefundOnly(potXiu, tongTienHopLe, snap, b.sumXiu, b.sumAll, b, /*onTaiSide*/ false);
                break;
            default:
                // Spec doesn't define result -1; mirror legacy default Debug.trace.
                break;
        }
        // Totals — exclude bots per TXR:956-959.
        b.totalTai = potTai.realTotal();
        b.totalXiu = potXiu.realTotal();
        b.numBetTai = potTai.realNumBet();
        b.numBetXiu = potXiu.realNumBet();
        return b.build();
    }

    /**
     * Allocate winning side: full prize formula. Mirrors TXR:854-899
     * (result=0/Xỉu wins) and TXR:901-925 (result=1/Tài wins).
     */
    private static void allocateWinning(PotState pot,
                                        long tongTienHopLe,
                                        RoundSnapshot snap,
                                        java.util.Map<String, UserAggregator.Aggregate> sumSide,
                                        java.util.Map<String, UserAggregator.Aggregate> sumAll,
                                        SettleResult.Builder b,
                                        boolean onTaiSide) {
        List<CrossPotBalancer.Allocation> allocs = CrossPotBalancer.allocate(pot, tongTienHopLe);
        for (CrossPotBalancer.Allocation a : allocs) {
            TransactionTaiXiuDetail tran = a.tran;
            // balanceGate quirk: bypass cross-pot trim (TXR:861, 885, 909, 933).
            long tienDuocTinh = snap.balanceGate ? tran.betValue : a.tienDuocTinh;
            long prize = (long) ((float) tienDuocTinh * (100.0f - snap.taxPct) / 100.0f) + tienDuocTinh;
            long refund = tran.betValue - tienDuocTinh;
            b.totalPrize += prize;
            if (onTaiSide) {
                b.totalRefundTai += refund;
            } else {
                b.totalRefundXiu += refund;
            }
            UserAggregator.merge(sumSide, tran, prize, refund, /*jp*/ 0L);
            UserAggregator.merge(sumAll, tran, prize, refund, /*jp*/ 0L);
        }
    }

    /**
     * Allocate losing side: prize=0, refund only. Mirrors TXR:879-898
     * (result=0/Xỉu wins, Tài losing) and TXR:927-946 (result=1/Tài wins,
     * Xỉu losing).
     */
    private static void allocateRefundOnly(PotState pot,
                                            long tongTienHopLe,
                                            RoundSnapshot snap,
                                            java.util.Map<String, UserAggregator.Aggregate> sumSide,
                                            java.util.Map<String, UserAggregator.Aggregate> sumAll,
                                            SettleResult.Builder b,
                                            boolean onTaiSide) {
        List<CrossPotBalancer.Allocation> allocs = CrossPotBalancer.allocate(pot, tongTienHopLe);
        for (CrossPotBalancer.Allocation a : allocs) {
            TransactionTaiXiuDetail tran = a.tran;
            long tienDuocTinh = snap.balanceGate ? tran.betValue : a.tienDuocTinh;
            long refund = tran.betValue - tienDuocTinh;
            if (onTaiSide) {
                b.totalRefundTai += refund;
            } else {
                b.totalRefundXiu += refund;
            }
            UserAggregator.merge(sumSide, tran, /*prize*/ 0L, refund, /*jp*/ 0L);
            UserAggregator.merge(sumAll, tran, /*prize*/ 0L, refund, /*jp*/ 0L);
        }
    }

    /**
     * Convenience — compute the winning prize for a single trimmed
     * {@code tienDuocTinh}. Mirrors TXR:865 / 913. Exposed for INV-7 jqwik
     * property tests.
     */
    public static long winningPrize(long tienDuocTinh, float taxPct) {
        return (long) ((float) tienDuocTinh * (100.0f - taxPct) / 100.0f) + tienDuocTinh;
    }
}
