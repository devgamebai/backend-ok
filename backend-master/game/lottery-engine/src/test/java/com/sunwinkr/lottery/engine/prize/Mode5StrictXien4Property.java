package com.sunwinkr.lottery.engine.prize;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-LOTTERY-07 — Mode 5 (Lô Xiên 4) canonical 4/4.
 *
 * <p>Exhaustive 2^4 = 16-pattern property — for every possible
 * (match,miss) combination of the 4 picks against the 27-line draw
 * pool, only the all-4-match case wins. Anything ≤3 returns zero
 * with the canonical flag.
 *
 * <p>Per binding decision D2 in
 * {@code docs/plans/lottery-extraction-plan.md} — Mode 5 now matches
 * C# behaviour. Legacy 3/4 rule lives behind
 * {@code LOTTERY_MODE5_LEGACY_3OF4=1} for rollback.
 */
class Mode5StrictXien4Property {

    private static final long STORED_BET = 1_000_000L;
    private static final int PRIZE_MUL = 160;

    @Property
    void canonical4of4_onlyAllMatchWins(@ForAll @IntRange(min = 0, max = 15) int bits) {
        // bits is the 4-bit mask of which picks should match the draw.
        List<String> picks = Arrays.asList("11", "22", "33", "44");
        List<String> pool = new ArrayList<>();
        int trueMatches = 0;
        for (int i = 0; i < 4; i++) {
            if ((bits & (1 << i)) != 0) {
                pool.add("X" + picks.get(i));
                trueMatches++;
            } else {
                // Random non-matching filler — must not end with any pick.
                pool.add("Y9" + i);
            }
        }

        long prize = PrizeCalculator.mode5(pool, String.join(",", picks), STORED_BET, PRIZE_MUL, false).getPrize();

        if (trueMatches == 4) {
            assertThat(prize).isEqualTo(STORED_BET * (long) PRIZE_MUL);
        } else {
            assertThat(prize).isZero();
        }
    }

    @Property
    void legacy3of4_threeOrFourWins(@ForAll @IntRange(min = 0, max = 15) int bits) {
        List<String> picks = Arrays.asList("11", "22", "33", "44");
        List<String> pool = new ArrayList<>();
        int trueMatches = 0;
        for (int i = 0; i < 4; i++) {
            if ((bits & (1 << i)) != 0) {
                pool.add("X" + picks.get(i));
                trueMatches++;
            } else {
                pool.add("Y9" + i);
            }
        }

        long prize = PrizeCalculator.mode5(pool, String.join(",", picks), STORED_BET, PRIZE_MUL, true).getPrize();

        if (trueMatches >= 3) {
            assertThat(prize).isEqualTo(STORED_BET * (long) PRIZE_MUL);
        } else {
            assertThat(prize).isZero();
        }
    }
}
