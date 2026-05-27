package com.sunwinkr.minigame.engine.sicbo.dice;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SicboRtpDiceGenerator} covering:
 * <ul>
 *   <li>INV-18 — feature gate disabled returns random dice</li>
 *   <li>Tiny-pot fallback (totalValueBetUser &lt; 100_000)</li>
 *   <li>Deterministic tie-set picking when bets force a unique optimum</li>
 *   <li>brute216Coverage — exactly 216 payout evaluations per call</li>
 * </ul>
 */
class SicboRtpDiceGeneratorTest {

    private StubRtpResolver rtp;
    private SicboPayoutCalculator payout;
    private SicboRound round;

    @BeforeEach
    void setUp() {
        rtp = new StubRtpResolver(85.0, 85.0); // pct < 92 so targetEdge = 15
        payout = new SicboPayoutCalculator();
        round = new SicboRound(1001L);
    }

    @Test
    void featureGateOff_returnsRandom() {
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_OFF, rtp, payout);
        List<SicboPotEntry> bets = Collections.singletonList(bet("user1", 1, 1_000_000L, SicboBetType.TAI));

        // Run many times — every call must return valid random dice
        for (int i = 0; i < 50; i++) {
            short[] dice = gen.generate(round, bets, 1_000_000L);
            assertThat(dice).hasSize(3);
            for (short d : dice) {
                assertThat(d).isBetween((short) 1, (short) 6);
            }
        }
    }

    @Test
    void tinyPot_returnsRandom() {
        // totalValueBetUser < TINY_POT_THRESHOLD (100_000)
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_ON, rtp, payout);
        List<SicboPotEntry> bets = Collections.singletonList(bet("user1", 1, 50_000L, SicboBetType.TAI));

        // With tiny pot the result is the random path; we just verify
        // valid dice are returned and that the counter is NOT bumped.
        SicboRtpDiceGenerator.EvaluationCounter c = new SicboRtpDiceGenerator.EvaluationCounter();
        gen.setEvaluationCounter(c);
        short[] dice = gen.generate(round, bets, 50_000L);

        assertThat(dice).hasSize(3);
        assertThat(c.get()).as("no payout evaluations on tiny-pot path").isEqualTo(0L);
    }

    @Test
    void deterministicGivenSeed_oneBest() {
        // Construct a bet config where the optimum dice is unique so the
        // random tie-break doesn't matter — same seed/bets/target → same dice.
        //
        // Setup: a single huge POINT_4 bet. POINT_4 wins only on dice (1,1,2),
        // (1,2,1), (2,1,1). It is the ONLY combination where TAI/XIU don't win
        // (total=4 → XIU). targetEdge=15 → targetProfit = bet * 0.15.
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_ON, rtp, payout);
        List<SicboPotEntry> bets = Collections.singletonList(
            bet("user1", 1, 1_000_000L, SicboBetType.POINT_4));

        // Run several times and confirm the returned dice are stable membership
        // — the generator may pick any tie member, but the (sorted) payout must
        // match across runs (i.e. minimum |profit-target| is deterministic).
        long firstPayout = payout.calculatePotentialPayout(bets,
            gen.generate(round, bets, 1_000_000L));
        for (int i = 0; i < 20; i++) {
            short[] d = gen.generate(round, bets, 1_000_000L);
            long p = payout.calculatePotentialPayout(bets, d);
            assertThat(p).as("payout for chosen dice must equal first call's payout (tie-set has uniform payout)")
                         .isEqualTo(firstPayout);
        }
    }

    @Test
    void brute216Coverage() {
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_ON, rtp, payout);
        SicboRtpDiceGenerator.EvaluationCounter c = new SicboRtpDiceGenerator.EvaluationCounter();
        gen.setEvaluationCounter(c);

        List<SicboPotEntry> bets = Collections.singletonList(
            bet("user1", 1, 1_000_000L, SicboBetType.TAI));

        gen.generate(round, bets, 1_000_000L);
        assertThat(c.get()).as("brute-force must evaluate exactly 216 ordered triples").isEqualTo(216L);

        // And across multiple invocations the counter keeps cumulative count.
        c.reset();
        for (int i = 0; i < 5; i++) {
            gen.generate(round, bets, 1_000_000L);
        }
        assertThat(c.get()).as("5 calls × 216 = 1080 evaluations").isEqualTo(216L * 5L);
    }

    @Test
    void noBets_returnsRandom() {
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_ON, rtp, payout);
        SicboRtpDiceGenerator.EvaluationCounter c = new SicboRtpDiceGenerator.EvaluationCounter();
        gen.setEvaluationCounter(c);

        // No bets → totalValueBetUser=0 → random path (SBR:639-641)
        short[] dice = gen.generate(round, Collections.emptyList(), 0L);

        assertThat(dice).hasSize(3);
        assertThat(c.get()).isEqualTo(0L);
    }

    @Test
    void rtpResolverFailure_returnsRandom() {
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_ON,
            new RtpResolverPort() {
                @Override public double effectivePct(long u, String g) { throw new RuntimeException("rtp down"); }
                @Override public double effectivePct(String g) { return 85.0; }
            },
            payout);

        List<SicboPotEntry> bets = Collections.singletonList(
            bet("user1", 1, 1_000_000L, SicboBetType.TAI));

        short[] dice = gen.generate(round, bets, 1_000_000L);
        assertThat(dice).hasSize(3);
        for (short d : dice) {
            assertThat(d).isBetween((short) 1, (short) 6);
        }
    }

    @Test
    void rtpUnique216_returnsDiceFromBruteForceSet() {
        // Confirm chosen dice always lies within the tie-set (best |profit-target|)
        SicboRtpDiceGenerator gen = new SicboRtpDiceGenerator(
            DefaultFeatureFlagPort.ALWAYS_ON, rtp, payout);

        List<SicboPotEntry> bets = new ArrayList<>();
        bets.add(bet("user1", 1, 2_000_000L, SicboBetType.TAI));
        bets.add(bet("user2", 2, 1_500_000L, SicboBetType.XIU));

        long total = 3_500_000L;
        double targetProfit = total * 0.15;

        // Compute expected tie-set
        double minDiff = Double.MAX_VALUE;
        Set<String> tieSet = new HashSet<>();
        for (short d1 = 1; d1 <= 6; d1++) {
            for (short d2 = 1; d2 <= 6; d2++) {
                for (short d3 = 1; d3 <= 6; d3++) {
                    long p = payout.calculatePotentialPayout(bets, new short[]{d1, d2, d3});
                    double profit = total - p;
                    double diff = Math.abs(profit - targetProfit);
                    if (diff < minDiff) {
                        minDiff = diff;
                        tieSet.clear();
                        tieSet.add(d1 + "," + d2 + "," + d3);
                    } else if (diff == minDiff) {
                        tieSet.add(d1 + "," + d2 + "," + d3);
                    }
                }
            }
        }

        // Sample 40 times and check membership
        for (int i = 0; i < 40; i++) {
            short[] dice = gen.generate(round, bets, total);
            String key = dice[0] + "," + dice[1] + "," + dice[2];
            assertThat(tieSet).as("generator must pick from the tie-set").contains(key);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static SicboPotEntry bet(String nick, int userId, long value, SicboBetType type) {
        return new SicboPotEntry(
            nick, userId, value, type.getId(),
            (short) 10, (short) 1, 1L, "tx-" + nick, /* isBot= */ false, 0L);
    }

    /** RTP resolver stub returning fixed pcts for tests. */
    private static class StubRtpResolver implements RtpResolverPort {
        private final double userPct;
        private final double baselinePct;

        StubRtpResolver(double userPct, double baselinePct) {
            this.userPct = userPct;
            this.baselinePct = baselinePct;
        }

        @Override public double effectivePct(long u, String g) { return userPct; }
        @Override public double effectivePct(String g) { return baselinePct; }
    }
}
