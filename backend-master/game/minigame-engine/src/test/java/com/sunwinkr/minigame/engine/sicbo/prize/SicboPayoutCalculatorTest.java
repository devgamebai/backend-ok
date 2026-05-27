package com.sunwinkr.minigame.engine.sicbo.prize;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SicboPayoutCalculator} covering INV-9 (ONE_DICE_* occurrence
 * multipliers) and INV-15 (triple suppression).
 */
class SicboPayoutCalculatorTest {

    private final SicboPayoutCalculator calc = new SicboPayoutCalculator();

    // -----------------------------------------------------------------------
    // INV-9 — ONE_DICE_* occurrence-count special payout
    // -----------------------------------------------------------------------

    @Test
    void oneDiceSpecialOccurrence1() {
        // ONE_DICE_3 bet, dice (3, 4, 5) → face 3 occurs once → bet * 2
        long bet = 10_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            entry("u1", 1, bet, SicboBetType.ONE_DICE_3));

        long payout = calc.calculatePotentialPayout(bets, new short[]{3, 4, 5});

        assertThat(payout).as("INV-9 / 1 occurrence → bet * 2").isEqualTo(bet * 2L);
    }

    @Test
    void oneDiceSpecialOccurrence2() {
        // ONE_DICE_3 bet, dice (3, 3, 5) → face 3 occurs twice → bet * 3
        long bet = 10_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            entry("u1", 1, bet, SicboBetType.ONE_DICE_3));

        long payout = calc.calculatePotentialPayout(bets, new short[]{3, 3, 5});

        assertThat(payout).as("INV-9 / 2 occurrences → bet * 3").isEqualTo(bet * 3L);
    }

    @Test
    void oneDiceSpecialOccurrence3() {
        // ONE_DICE_* occurrence=3 requires all three dice equal the same face,
        // which is a triple (INV-15). Triples suppress ONE_DICE_* — the evaluator
        // only returns TRIPLE_DICES_n + ANY_TRIPLE_DICES. So a ONE_DICE_3 bet on
        // dice (3,3,3) pays 0 (suppressed), not bet*4. The occurrence=3 branch in
        // the payout code is unreachable via the normal prize path; the behavior-
        // preserving port retains the branch for source fidelity.
        long bet = 10_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            entry("u1", 1, bet, SicboBetType.ONE_DICE_3));

        long payout = calc.calculatePotentialPayout(bets, new short[]{3, 3, 3});

        // INV-15: triple suppresses ONE_DICE_3 → pays 0
        assertThat(payout).as("INV-15 suppresses ONE_DICE_3 on triple (3,3,3)").isEqualTo(0L);
    }

    // -----------------------------------------------------------------------
    // INV-15 — triple suppression
    // -----------------------------------------------------------------------

    @Test
    void tripleSuppression() {
        // Multi-bet pot: TAI, XIU, CHAN, LE, DOUBLE_DICES_3_3, ANY_TRIPLE_DICES,
        // TRIPLE_DICES_3, POINT_9. With dice (3,3,3) triple:
        //   TAI/XIU/CHAN/LE/DOUBLE/POINT all suppressed → 0
        //   ANY_TRIPLE_DICES wins → bet * 31
        //   TRIPLE_DICES_3 wins → bet * 31
        long bet = 1_000L;
        List<SicboPotEntry> bets = new ArrayList<>();
        bets.add(entry("u1", 1, bet, SicboBetType.TAI));
        bets.add(entry("u2", 2, bet, SicboBetType.XIU));
        bets.add(entry("u3", 3, bet, SicboBetType.CHAN));
        bets.add(entry("u4", 4, bet, SicboBetType.LE));
        bets.add(entry("u5", 5, bet, SicboBetType.DOUBLE_DICES_3_3));
        bets.add(entry("u6", 6, bet, SicboBetType.POINT_9));
        bets.add(entry("u7", 7, bet, SicboBetType.ANY_TRIPLE_DICES));
        bets.add(entry("u8", 8, bet, SicboBetType.TRIPLE_DICES_3));

        long payout = calc.calculatePotentialPayout(bets, new short[]{3, 3, 3});

        // Only ANY_TRIPLE_DICES + TRIPLE_DICES_3 pay. rotation=31 each.
        long expected = bet * 31L + bet * 31L;
        assertThat(payout).as("INV-15 / only TRIPLE_DICES_3 + ANY_TRIPLE_DICES pay on (3,3,3)")
                          .isEqualTo(expected);
    }

    @Test
    void botBetsExcluded() {
        // Bot bets (userId <= 0) MUST NOT contribute to payout (SBR:1046).
        long bet = 1_000L;
        List<SicboPotEntry> bets = new ArrayList<>();
        bets.add(entry("user", 1, bet, SicboBetType.TAI));         // real user
        bets.add(entry("bot",  0, bet * 100L, SicboBetType.TAI));  // bot

        long payout = calc.calculatePotentialPayout(bets, new short[]{5, 5, 5});

        // Dice (5,5,5) is a triple → TAI suppressed → payout=0
        assertThat(payout).isEqualTo(0L);

        // Non-triple win path: dice (6,5,1) total=12 → TAI wins (rotation=2)
        payout = calc.calculatePotentialPayout(bets, new short[]{6, 5, 1});
        // Only real user (bet=1000) contributes → 1000*2 = 2000.
        assertThat(payout).as("bot bets excluded from payout sum").isEqualTo(bet * 2L);
    }

    @Test
    void nonTripleStandardPayout() {
        // POINT_9 bet, dice (4,3,2) total=9 → POINT_9 wins → bet * 7 (rotation 7)
        long bet = 1_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            entry("u1", 1, bet, SicboBetType.POINT_9));

        long payout = calc.calculatePotentialPayout(bets, new short[]{4, 3, 2});

        assertThat(payout).isEqualTo(bet * 7L);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    static SicboPotEntry entry(String nick, int userId, long value, SicboBetType type) {
        return new SicboPotEntry(
            nick, userId, value, type.getId(),
            (short) 10, (short) 1, 1L, "tx-" + nick, /* isBot= */ userId <= 0, 0L);
    }
}
