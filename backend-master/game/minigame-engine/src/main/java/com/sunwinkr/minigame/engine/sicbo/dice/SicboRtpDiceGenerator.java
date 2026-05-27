package com.sunwinkr.minigame.engine.sicbo.dice;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 216-combo brute-force RTP balancer for Sicbo result generation.
 *
 * <p>Direct behavior-preserving port of
 * {@code MGRoomSicbo.generateResultWithHouseEdge()} (SBR:632-676).
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>If {@link FeatureFlagPort#isCanCuaRtpEnabled()} is {@code false} →
 *       delegate to {@link SicboRandomDiceGenerator} (INV-18 / SBR:633-635).</li>
 *   <li>Read {@code winRatePct = rtpResolver.effectivePct(0L, "sicbo")}
 *       (SBR:636).</li>
 *   <li>{@code targetEdgePct} = SBR:637 — when BOTH user-aware AND game-only
 *       baseline RTP are ≥ 92%, target edge is 0; otherwise {@code 100 - winRatePct}.</li>
 *   <li>If {@code targetEdgePct <= 0 || totalValueBetUser <= 0} → random
 *       (SBR:639-641).</li>
 *   <li>Tiny-pot guard: if {@code totalValueBetUser < 100_000} → random
 *       (SBR:643-646). Prevents whipsaw on negligible rounds.</li>
 *   <li>{@code targetProfit = totalValueBetUser * targetEdgePct / 100.0}
 *       (SBR:648).</li>
 *   <li>Brute-force all 6×6×6 = 216 ordered triples. For each triple:
 *       <ul>
 *         <li>{@code totalPayout = payout.calculatePotentialPayout(snapshot, dice)}</li>
 *         <li>{@code profit = totalValueBetUser - totalPayout}</li>
 *         <li>Track {@code bestCombinations} list minimizing
 *             {@code |profit - targetProfit|} (SBR:659-665).</li>
 *       </ul></li>
 *   <li>Uniform random pick from the tie-set (SBR:672-673).</li>
 * </ol>
 *
 * <h3>Empty-best fallback (SBR:670)</h3>
 * If the tie-set is empty (e.g. NaN comparisons or pathological inputs)
 * we return random dice — matches legacy behaviour.
 *
 * <h3>Errors</h3>
 * Any exception while reading the RTP resolver causes a graceful fallback
 * to random dice (kill-switch safety — RTP_ENGINE_DISABLED equivalent).
 *
 * <p>Thread-safe: holds only immutable refs to its collaborators.
 */
public final class SicboRtpDiceGenerator implements SicboDiceGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(SicboRtpDiceGenerator.class);

    /** RTP key for the Sicbo game (matches legacy SBR:636). */
    static final String GAME_CODE_SICBO = "sicbo";

    /** Threshold below which we fall back to random (SBR:644). */
    static final long TINY_POT_THRESHOLD = 100_000L;

    /** Both-≥92 high-RTP threshold for forcing zero edge (SBR:637). */
    static final double HIGH_RTP_THRESHOLD = 92.0;

    private final FeatureFlagPort flag;
    private final RtpResolverPort rtp;
    private final SicboPayoutCalculator payout;

    /** Optional instrumentation hook — counts {@code calculatePotentialPayout} calls. */
    private volatile EvaluationCounter counter;

    public SicboRtpDiceGenerator(FeatureFlagPort flag, RtpResolverPort rtp, SicboPayoutCalculator payout) {
        if (flag == null) {
            throw new NullPointerException("flag");
        }
        if (rtp == null) {
            throw new NullPointerException("rtp");
        }
        if (payout == null) {
            throw new NullPointerException("payout");
        }
        this.flag = flag;
        this.rtp = rtp;
        this.payout = payout;
    }

    @Override
    public short[] generate(SicboRound ctx, List<SicboPotEntry> snapshot, long totalValueBetUser) {
        // 1) Feature gate — INV-18 / SBR:633-635
        if (!flag.isCanCuaRtpEnabled()) {
            return SicboRandomDiceGenerator.INSTANCE.generate(ctx, snapshot, totalValueBetUser);
        }

        // 2) Resolve win-rate pct (user-aware override). Any failure → random.
        double winRatePct;
        try {
            winRatePct = rtp.effectivePct(0L, GAME_CODE_SICBO);
        } catch (Throwable t) {
            LOG.warn("SicboRtpDiceGenerator: rtp.effectivePct failed — falling back to random", t);
            return SicboRandomDiceGenerator.INSTANCE.generate(ctx, snapshot, totalValueBetUser);
        }

        // 3) Both-≥92 zero-edge guard (SBR:637)
        double targetEdgePct;
        try {
            double baseline = rtp.effectivePct(GAME_CODE_SICBO);
            targetEdgePct = (winRatePct >= HIGH_RTP_THRESHOLD && baseline >= HIGH_RTP_THRESHOLD)
                ? 0.0
                : 100.0 - winRatePct;
        } catch (Throwable t) {
            LOG.warn("SicboRtpDiceGenerator: rtp baseline read failed — falling back to random", t);
            return SicboRandomDiceGenerator.INSTANCE.generate(ctx, snapshot, totalValueBetUser);
        }

        // 4) Zero / negative edge OR no bets → random (SBR:639-641)
        if (targetEdgePct <= 0.0 || totalValueBetUser <= 0L) {
            return SicboRandomDiceGenerator.INSTANCE.generate(ctx, snapshot, totalValueBetUser);
        }

        // 5) Tiny-pot guard (SBR:643-646)
        if (totalValueBetUser < TINY_POT_THRESHOLD) {
            return SicboRandomDiceGenerator.INSTANCE.generate(ctx, snapshot, totalValueBetUser);
        }

        // 6) Target profit (SBR:648)
        double targetProfit = (double) totalValueBetUser * (targetEdgePct / 100.0);

        // 7) Brute force 216 combinations (SBR:650-668)
        double minDiff = Double.MAX_VALUE;
        List<short[]> best = new ArrayList<>();
        EvaluationCounter c = this.counter;
        for (short d1 = 1; d1 <= 6; d1++) {
            for (short d2 = 1; d2 <= 6; d2++) {
                for (short d3 = 1; d3 <= 6; d3++) {
                    short[] candidate = new short[] { d1, d2, d3 };
                    long totalPayout = payout.calculatePotentialPayout(snapshot, candidate);
                    if (c != null) {
                        c.increment();
                    }
                    double profit = (double) totalValueBetUser - (double) totalPayout;
                    double diff = Math.abs(profit - targetProfit);
                    if (diff < minDiff) {
                        minDiff = diff;
                        best.clear();
                        best.add(candidate);
                    } else if (diff == minDiff) {
                        best.add(candidate);
                    }
                }
            }
        }

        // 8) Empty fallback (SBR:670)
        if (best.isEmpty()) {
            return SicboRandomDiceGenerator.INSTANCE.generate(ctx, snapshot, totalValueBetUser);
        }

        // 9) Uniform random pick from tie-set (SBR:672-673)
        int idx = ThreadLocalRandom.current().nextInt(best.size());
        short[] picked = best.get(idx);
        LOG.debug("SicboRtpDiceGenerator: targetEdge={}% targetProfit={} minDiff={} bestSize={}",
                  targetEdgePct, targetProfit, minDiff, best.size());
        return picked;
    }

    // -----------------------------------------------------------------------
    // Test affordances
    // -----------------------------------------------------------------------

    /** Wire an instrumentation counter (test-only). */
    public void setEvaluationCounter(EvaluationCounter c) {
        this.counter = c;
    }

    /**
     * Simple tick-counter used by {@code brute216Coverage} test to assert the
     * algorithm evaluates exactly 216 payouts per call.
     */
    public static final class EvaluationCounter {
        private long count = 0L;
        public void increment() { count++; }
        public long get() { return count; }
        public void reset() { count = 0L; }
    }
}
