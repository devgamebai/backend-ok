package com.sunwinkr.lottery.engine.prize;

import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-LOTTERY-06 — Mode 1 closed-form property.
 *
 * <p>For any {@code userBet} and any number of {@code matches},
 * {@code prize = matches * userBet * 80}. Holds across the full
 * 27-line draw pool because mode 1 stores {@code finalBetValue =
 * userBet * 22} on the row and the legacy formula
 * {@code matches * finalBetValue * 80 / 22} cancels {@code /22}.
 *
 * <p>Spec source: {@code docs/specs/lottery-rules-spec.md §9
 * INV-LOTTERY-06}.
 */
class Mode1ClosedFormProperty {

    @Property
    void closedFormHoldsForArbitraryBetAndMatchCount(
            @ForAll @LongRange(min = 1_000L, max = 1_000_000_000L) long userBet,
            @ForAll @IntRange(min = 0, max = 27) int matches) {

        LotteryResult rs = drawWithMatchingLines(matches, "42");
        // SUN-1295 snapshot: rate=22, prizeMul=80.
        LotteryTicket t = new LotteryTicket(
                1L, 10L, "u", userBet * 22L, 1, "42",
                null, LocalDateTime.now(), null,
                userBet, 22, 80);

        PrizeBreakdown pb = PrizeCalculator.calculate(rs, t);

        long expected = (long) matches * userBet * 80L;
        assertThat(pb.getPrize()).isEqualTo(expected);
        assertThat(pb.getMatches()).isEqualTo(matches);
    }

    private static LotteryResult drawWithMatchingLines(int matchCount, String suffix) {
        LotteryResult rs = new LotteryResult();
        LotteryResult.Results r = new LotteryResult.Results();
        r.setĐB(Collections.singletonList("11111")); // does not end in "42"
        List<String> matching = new ArrayList<>();
        for (int i = 0; i < matchCount; i++) {
            matching.add("X" + i + suffix);
        }
        r.setG1(matching);
        r.setG2(Collections.<String>emptyList());
        r.setG3(Collections.<String>emptyList());
        r.setG4(Collections.<String>emptyList());
        r.setG5(Collections.<String>emptyList());
        r.setG6(Collections.<String>emptyList());
        r.setG7(Collections.<String>emptyList());
        rs.setResults(r);
        rs.setTime("14-05-2026");
        return rs;
    }
}
