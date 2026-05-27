package com.sunwinkr.minigame.engine.sicbo.dice;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;
import com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculator;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SicboFundProtector} covering AMBIGUOUS #4 (bounded retry).
 */
class SicboFundProtectorTest {

    private SicboPayoutCalculator payout;
    private SicboRound round;

    @BeforeEach
    void setUp() {
        payout = new SicboPayoutCalculator();
        round = new SicboRound(2001L);
    }

    @Property(tries = 100)
    void terminatesUnderBound(@ForAll @IntRange(min = 1, max = 6) int d1,
                              @ForAll @IntRange(min = 1, max = 6) int d2,
                              @ForAll @IntRange(min = 1, max = 6) int d3,
                              @ForAll @IntRange(min = 100_000, max = 10_000_000) int totalBet,
                              @ForAll @IntRange(min = -1_000_000_000, max = 1_000_000_000) int fund) {
        // jqwik property: ANY input must terminate in ≤ MAX_REROLL_ITERATIONS.
        // Note: jqwik does NOT call @BeforeEach — construct collaborators inline.
        SicboPayoutCalculator localPayout = new SicboPayoutCalculator();
        SicboRound localRound = new SicboRound(2001L);
        localRound.lockBetting(); // protect() requires LOCKED is not enforced, but round is valid
        SicboFundProtector p = new SicboFundProtector(SicboRandomDiceGenerator.INSTANCE, localPayout);
        List<SicboPotEntry> bets = Collections.singletonList(
            new SicboPotEntry("user1", 1, totalBet, SicboBetType.TAI.getId(),
                (short) 10, (short) 1, 1L, "txc", false, 0L));

        SicboFundProtector.Result result = p.protect(localRound, bets, totalBet, fund,
            new short[]{(short) d1, (short) d2, (short) d3});

        assertThat(result.iterations).isLessThanOrEqualTo(SicboFundProtector.MAX_REROLL_ITERATIONS);
        assertThat(result.dice).hasSize(3);
    }

    @Test
    void boundedAndReportsExhaustion() {
        // Pathological scenario: configure a generator that always returns
        // dice triggering huge payout > totalBet, with deep-negative fund.
        // Every iteration will fail the fund-safety check → exhaust.

        // Construct a bet payload where EVERY dice triple loses massively for
        // the house: huge ANY_TRIPLE_DICES bet (rotation=31). Triple is roughly
        // 6/216 ≈ 2.8% of all outcomes. Most rolls are non-triple → ANY_TRIPLE
        // does not win, so house GAINS, not loses. Need an alternative.
        //
        // Better: huge bet on each POINT_4..POINT_17 to maximize coverage.
        // Use a forcing generator that ALWAYS picks losing dice.

        // Simpler: stub the generator to return dice (3,3,3) — a triple.
        // Bets on ANY_TRIPLE_DICES win with rotation=31, paying 31x. With
        // bet=10000 and totalBet=10000, payout=310000 → tienloi=-300000.
        // With fund=-1000 → fund+tienloi = -301000 < 0 forever.
        SicboPayoutCalculator stubPayout = new SicboPayoutCalculator();

        SicboDiceGenerator alwaysTripleThree = new SicboDiceGenerator() {
            @Override
            public short[] generate(SicboRound ctx, List<SicboPotEntry> snap, long total) {
                return new short[]{3, 3, 3};
            }
        };

        SicboFundProtector p = new SicboFundProtector(
            (SicboRandomDiceGenerator) wrapAsRandom(alwaysTripleThree),
            stubPayout);

        long bet = 10_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            new SicboPotEntry("whale", 1, bet, SicboBetType.ANY_TRIPLE_DICES.getId(),
                (short) 10, (short) 1, 1L, "txc", false, 0L));

        long fund = -1_000_000L;
        SicboFundProtector.Result result = p.protect(round, bets, bet, fund, new short[]{3, 3, 3});

        assertThat(result.exhausted).as("pathological scenario must exhaust").isTrue();
        assertThat(result.iterations).isEqualTo(SicboFundProtector.MAX_REROLL_ITERATIONS);
        assertThat(p.exhaustedCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void bestEffortOnExhaust() {
        // When exhausted, the returned dice must be the least-bad seen
        // (maximizing tienloi). We use a deterministic generator that cycles
        // through three dice combos with known, different payouts — all of which
        // keep the fund negative — to verify the protector tracks the best.
        //
        // Setup: large TAI bet (rotation=2). Dice that give TAI wins all have
        // total ≥ 11 (non-triple). Three candidates and their tienloi:
        //   (6,6,1) total=13 → TAI wins → payout=bet*2=20000 → tienloi=bet-20000=-10000
        //   (6,5,1) total=12 → TAI wins → payout=20000          → tienloi=-10000  (same)
        //   (6,4,1) total=11 → TAI wins → payout=20000          → tienloi=-10000  (same)
        // All three are equally bad. Use POINT_4 (rotation=61) instead for variety:
        //   POINT_4 wins on non-triple total 4: (1,1,2),(1,2,1),(2,1,1)
        //   POINT_5 wins on total 5: (1,2,2),(2,1,2),(2,2,1)
        //
        // Use two bets: POINT_4 and POINT_5, both large. Cycle dice:
        //   (1,1,2) total=4 → POINT_4 wins, POINT_5 loses → payout = 1000*61 = 61000
        //   (1,2,2) total=5 → POINT_4 loses, POINT_5 wins → payout = 500*31  = 15500
        //   (2,2,2) total=6 → triple → both suppressed     → payout = 0
        // totalBet = 1000+500 = 1500, fund = -1_000_000
        // tienloi: (1,1,2)=-59500  (1,2,2)=-14000  (2,2,2)=+1500 (house wins!)
        //
        // (2,2,2) exits immediately — not useful for exhaustion. Use only losing dice.
        // Replace with all-TAI bets; cycle dice that all make TAI win at rotation=2:
        //   Dice A (6,5,1) total=12 → TAI → payout=big*2 → tienloi big negative
        //   Dice B (5,5,2) total=12 → TAI → same payout → same tienloi
        //   Dice C (5,4,3) total=12 → TAI → same → same
        // All three produce identical tienloi; best-effort returns any of them.
        //
        // For a meaningful "best differs" test, mix two bet types and vary dice:
        //   Bet: POINT_17 (rotation=61) only.
        //   (5,6,6) total=17 → POINT_17 wins → payout=bet*61 (worst)
        //   (4,6,6) total=16 → POINT_17 loses → payout=0 → tienloi=+bet (best!) exits early
        //
        // Simplest exhaustion test with differing best-effort: use a custom
        // payout calculator that returns fixed values per-dice.

        SicboPayoutCalculator realPayout = new SicboPayoutCalculator();

        // Use POINT_9 (rotation=7) + POINT_12 (rotation=7) both have rotation 7.
        // Dice (3,3,3) triple → both suppressed → payout=0 → tienloi=+totalBet (exits immediately).
        // We must avoid triples. Use only non-triple dice in the cycle.
        //
        // Cycle: all TAI bets (rotation=2), dice all have total >= 11 (TAI wins):
        //   (6,3,2) total=11 → TAI wins → payout=bet*2 → tienloi=bet-2*bet=-bet  (-1000)
        //   (5,3,2) total=10 → TAI DOES NOT WIN (total<=10 → XIU) → payout=0 → tienloi=+bet (exits)
        // Still exits early on dice B.
        //
        // Definitive approach: use a stub payout calculator with wired returns.

        final long betAmt = 10_000L;
        final long totalBet = betAmt;
        final long fund = -1_000_000_000L; // so deep-negative that no retry recovers

        // Stub payout: returns 60000, 50000, 40000 in sequence (all > totalBet → tienloi < 0,
        // all cause fund + tienloi < 0 since fund is -1B).
        final long[] payouts = {60_000L, 50_000L, 40_000L};
        final int[] idx = {0};
        SicboPayoutCalculator stubCalc = new SicboPayoutCalculator() {
            @Override
            public long calculatePotentialPayout(
                    java.util.List<SicboPotEntry> b, short[] d) {
                return payouts[(idx[0]++) % payouts.length];
            }
        };

        // Cycling dice generator (values don't matter since stubCalc ignores them)
        final short[][] diceCycle = {
            new short[]{1, 2, 3},
            new short[]{1, 3, 2},
            new short[]{2, 1, 3}
        };
        final int[] diceIdx = {0};
        SicboFundProtector p = new SicboFundProtector(
            wrapAsRandom((ctx, s, t) -> diceCycle[(diceIdx[0]++) % diceCycle.length]),
            stubCalc);

        List<SicboPotEntry> bets = Collections.singletonList(
            new SicboPotEntry("whale", 1, betAmt, SicboBetType.TAI.getId(),
                (short) 10, (short) 1, 1L, "txc", false, 0L));

        // candidate dice also uses stub: first call to stubCalc is for candidate itself
        // Reset idx so candidate is payout[0]=60000, then retries get 50000, 40000, ...
        idx[0] = 0;
        diceIdx[0] = 0;
        SicboFundProtector.Result result = p.protect(round, bets, totalBet, fund,
            new short[]{1, 1, 1});

        assertThat(result.exhausted).isTrue();
        // Best tienloi = totalBet - min(payouts) = 10000 - 40000 = -30000
        assertThat(result.tienloi).isEqualTo(totalBet - 40_000L);
    }

    @Test
    void noProtectionNeeded_returnsCandidateAsIs() {
        // Candidate already safe (positive tienloi) — no retries.
        SicboFundProtector p = new SicboFundProtector(SicboRandomDiceGenerator.INSTANCE, payout);

        // Bet=10000 on TAI, dice=(1,1,1) → triple, TAI is suppressed → payout=0.
        // tienloi = 10000-0 = +10000. Fund of 0 still works.
        long bet = 10_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            new SicboPotEntry("user1", 1, bet, SicboBetType.TAI.getId(),
                (short) 10, (short) 1, 1L, "txc", false, 0L));

        SicboFundProtector.Result r = p.protect(round, bets, bet, 0L, new short[]{1, 1, 1});
        assertThat(r.iterations).isEqualTo(0);
        assertThat(r.exhausted).isFalse();
        assertThat(r.tienloi).isEqualTo(bet); // house wins all
    }

    // -----------------------------------------------------------------------
    // Helpers — adapter for the protector's constructor type.
    // -----------------------------------------------------------------------

    /**
     * The protector constructor demands a {@code SicboRandomDiceGenerator}.
     * Tests using a custom dice source wrap their generator in a subclass.
     */
    private static SicboRandomDiceGenerator wrapAsRandom(SicboDiceGenerator inner) {
        return new SicboRandomDiceGenerator() {
            @Override
            public short[] generate(SicboRound ctx, List<SicboPotEntry> snap, long total) {
                return inner.generate(ctx, snap, total);
            }
        };
    }
}
