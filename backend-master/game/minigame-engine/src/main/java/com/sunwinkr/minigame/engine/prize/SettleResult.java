package com.sunwinkr.minigame.engine.prize;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Output of {@link PrizeCalculator#calculate}. Mirrors the trio of
 * {@code sumTai} / {@code sumXiu} / {@code sumTXTMap} that the legacy
 * {@code calculatePrize} (TXR:825-984) builds, plus per-side totals for
 * the {@code ResultTaiXiu} broadcast.
 *
 * <p>Bots are excluded from the totals via {@code potX/potT.realTotal()}
 * per TXR:956-959 (plan §2.4 row P6).
 *
 * <p>Plan §2.4 row P3, P5, P6.
 */
public final class SettleResult {

    /** Per-user aggregate on the Tài side (multi-bet merged, INV row P5). */
    public final Map<String, UserAggregator.Aggregate> sumTai;
    /** Per-user aggregate on the Xỉu side. */
    public final Map<String, UserAggregator.Aggregate> sumXiu;
    /** Per-user aggregate across BOTH sides (for {@code transaction_tai_xiu}). */
    public final Map<String, UserAggregator.Aggregate> sumAll;

    public final long totalTai;
    public final long totalXiu;
    public final short numBetTai;
    public final short numBetXiu;
    /** {@code totalPrize} across winning side (gross, including stake). */
    public final long totalPrize;
    public final long totalRefundTai;
    public final long totalRefundXiu;
    /** Round result (0 = Xỉu wins, 1 = Tài wins). */
    public final short result;

    SettleResult(Builder b) {
        this.sumTai = Collections.unmodifiableMap(new HashMap<>(b.sumTai));
        this.sumXiu = Collections.unmodifiableMap(new HashMap<>(b.sumXiu));
        this.sumAll = Collections.unmodifiableMap(new HashMap<>(b.sumAll));
        this.totalTai = b.totalTai;
        this.totalXiu = b.totalXiu;
        this.numBetTai = b.numBetTai;
        this.numBetXiu = b.numBetXiu;
        this.totalPrize = b.totalPrize;
        this.totalRefundTai = b.totalRefundTai;
        this.totalRefundXiu = b.totalRefundXiu;
        this.result = b.result;
    }

    static final class Builder {
        final Map<String, UserAggregator.Aggregate> sumTai = new HashMap<>();
        final Map<String, UserAggregator.Aggregate> sumXiu = new HashMap<>();
        final Map<String, UserAggregator.Aggregate> sumAll = new HashMap<>();
        long totalTai;
        long totalXiu;
        short numBetTai;
        short numBetXiu;
        long totalPrize;
        long totalRefundTai;
        long totalRefundXiu;
        short result;

        SettleResult build() {
            return new SettleResult(this);
        }
    }
}
