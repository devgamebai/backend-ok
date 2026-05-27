package com.sunwinkr.lottery.engine.prize;

import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.model.LotteryTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-mode prize calculation tests. Exercises the 10 modes against
 * synthetic {@link LotteryResult} fixtures plus the SUN-1295 snapshot
 * (INV-LOTTERY-05/06) and Mode 5 canonical 4/4 vs legacy 3/4
 * (INV-LOTTERY-07 reconciled to 4/4 per binding decision D2 in the
 * extraction plan).
 */
class PrizeCalculatorTest {

    /** Compose a synthetic draw with the given suffix-bearing lines. */
    private static LotteryResult drawWith(String db, List<String> g1ThroughG7) {
        LotteryResult rs = new LotteryResult();
        LotteryResult.Results r = new LotteryResult.Results();
        r.setĐB(Collections.singletonList(db));
        // Place all the "other" lines into G1 for simplicity — the engine
        // only cares about end-of-line suffix membership across the
        // 27/24 unions.
        r.setG1(g1ThroughG7);
        r.setG2(Collections.<String>emptyList());
        r.setG3(Collections.<String>emptyList());
        r.setG4(Collections.<String>emptyList());
        r.setG5(Collections.<String>emptyList());
        r.setG6(Collections.<String>emptyList());
        r.setG7(Collections.<String>emptyList());
        rs.setResults(r);
        rs.setCountNumbers(0);
        rs.setTime("14-05-2026");
        return rs;
    }

    /** Place {@code count} lines that all end with {@code suffix}, plus filler. */
    private static List<String> linesEndingWith(int count, String suffix) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add("9" + i + suffix);
        }
        return out;
    }

    /**
     * Build a ticket carrying the SUN-1295 snapshot — settle reads back
     * from these stamped fields, never the live enum.
     */
    private static LotteryTicket ticket(int modeId, String num, long userBet,
                                        int rate, int prizeMul) {
        long finalBetValue = userBet * (long) rate;
        return new LotteryTicket(
                1L, 10L, "u", finalBetValue, modeId, num,
                null, LocalDateTime.now(), null,
                userBet, rate, prizeMul);
    }

    // ---------- Mode 1 — INV-LOTTERY-06 closed form ----------

    /**
     * Closed form: {@code prize = matches * userBet * 80}.
     * Mode 1 stores {@code finalBetValue = userBet * 22} on the row.
     */
    @ParameterizedTest
    @CsvSource({
            // userBet, matches
            "1000, 1",
            "1000, 5",
            "10000, 3",
            "50000, 11"
    })
    void mode1ClosedForm(long userBet, int matches) {
        LotteryResult rs = drawWith("12345",
                PrizeCalculator.concat(linesEndingWith(matches, "42"),
                        PrizeCalculator.asList("11", "22", "33")));
        LotteryTicket t = ticket(1, "42", userBet, 22, 80);

        PrizeBreakdown pb = PrizeCalculator.calculate(rs, t);

        long expected = (long) matches * userBet * 80L;
        assertThat(pb.getPrize()).isEqualTo(expected);
        assertThat(pb.getMatches()).isEqualTo(matches);
    }

    // ---------- Mode 5 — canonical 4/4 + legacy 3/4 ----------

    /**
     * Canonical Mode 5: only 4/4 wins. 3/4 returns zero with default flag.
     */
    @Test
    void mode5Strict4of4() {
        // 4 picks; only 3 match in the pool.
        List<String> pool = PrizeCalculator.asList("AA11", "BB22", "CC33", "ZZ99");
        long zero = PrizeCalculator.mode5(pool, "11,22,33,44", 1_000_000L, 160, false)
                .getPrize();
        assertThat(zero).isZero();

        // 4 picks, all 4 match.
        List<String> pool2 = PrizeCalculator.asList("AA11", "BB22", "CC33", "DD44");
        long win = PrizeCalculator.mode5(pool2, "11,22,33,44", 1_000_000L, 160, false)
                .getPrize();
        assertThat(win).isEqualTo(160_000_000L);
    }

    /**
     * Legacy 3/4 flag — restores pre-2026-05-14 Java behaviour. 3/4 wins.
     */
    @Test
    void mode5Legacy3of4() {
        List<String> pool = PrizeCalculator.asList("AA11", "BB22", "CC33", "ZZ99");
        long win = PrizeCalculator.mode5(pool, "11,22,33,44", 1_000_000L, 160, true)
                .getPrize();
        assertThat(win).isEqualTo(160_000_000L);
    }

    /** Below the legacy threshold even with the flag — still zero. */
    @Test
    void mode5LegacyStillZeroAtTwoOfFour() {
        List<String> pool = PrizeCalculator.asList("AA11", "BB22", "XX99", "ZZ88");
        long prize = PrizeCalculator.mode5(pool, "11,22,33,44", 1_000_000L, 160, true)
                .getPrize();
        assertThat(prize).isZero();
    }

    // ---------- Mode 9 — SUN-1295: 85 ----------

    @Test
    void mode9_85Multiplier() {
        // DB suffix "42" == ticket "42" → win at the snapshot's 85.
        LotteryResult rs = drawWith("12342", Collections.<String>emptyList());
        LotteryTicket t = ticket(9, "42", 100_000L, 1, 85);

        PrizeBreakdown pb = PrizeCalculator.calculate(rs, t);
        assertThat(pb.getPrize()).isEqualTo(100_000L * 85L);
    }

    /** Legacy snapshot (pre-1295) settles at 95 even today. */
    @Test
    void mode9LegacySnapshotSettlesAtStoredMultiplier() {
        LotteryResult rs = drawWith("12342", Collections.<String>emptyList());
        // Ticket stamped at the OLD multiplier (95).
        LotteryTicket t = ticket(9, "42", 100_000L, 1, 95);

        PrizeBreakdown pb = PrizeCalculator.calculate(rs, t);
        assertThat(pb.getPrize()).isEqualTo(100_000L * 95L);
    }

    // ---------- Mode 11 — SUN-1295: 450 ----------

    @Test
    void mode11_450Multiplier() {
        // DB ends with 3-digit "342" → match.
        LotteryResult rs = drawWith("12342", Collections.<String>emptyList());
        LotteryTicket t = ticket(11, "342", 10_000L, 1, 450);

        PrizeBreakdown pb = PrizeCalculator.calculate(rs, t);
        assertThat(pb.getPrize()).isEqualTo(10_000L * 450L);
    }

    // ---------- Mode 8 — 3 Càng Đặc Biệt (SUN-1366) ----------

    @Test
    void mode8_BaCangDacBiet_Last3OfDB() {
        // ĐB = "12342" → last 3 = "342". Pick "342" → win, "999" → lose.
        LotteryResult rs = drawWith("12342", Collections.<String>emptyList());
        LotteryTicket winning = ticket(8, "342", 50_000L, 1, 450);
        LotteryTicket losing  = ticket(8, "999", 50_000L, 1, 450);

        // 50_000 * 450 = 22_500_000
        assertThat(PrizeCalculator.calculate(rs, winning).getPrize()).isEqualTo(22_500_000L);
        assertThat(PrizeCalculator.calculate(rs, losing).getPrize()).isZero();
    }

    // ---------- Modes 6 / 7 — Đề (SUN-1366) ----------

    @Test
    void mode6_DeGiaiNhat_Last2OfG1() {
        // G1[0] = "00111" → last 2 = "11". Pick "11" → win, "22" → lose.
        LotteryResult rs = drawWith("12342", java.util.Arrays.asList("00111"));
        LotteryTicket winning = ticket(6, "11", 10_000L, 1, 85);
        LotteryTicket losing  = ticket(6, "22", 10_000L, 1, 85);

        assertThat(PrizeCalculator.calculate(rs, winning).getPrize()).isEqualTo(850_000L);
        assertThat(PrizeCalculator.calculate(rs, losing).getPrize()).isZero();
    }

    @Test
    void mode7_DeDacBiet_Last2OfDB() {
        // ĐB = "12342" → last 2 = "42". Pick "42" → win, "99" → lose.
        LotteryResult rs = drawWith("12342", Collections.<String>emptyList());
        LotteryTicket winning = ticket(7, "42", 10_000L, 1, 85);
        LotteryTicket losing  = ticket(7, "99", 10_000L, 1, 85);

        assertThat(PrizeCalculator.calculate(rs, winning).getPrize()).isEqualTo(850_000L);
        assertThat(PrizeCalculator.calculate(rs, losing).getPrize()).isZero();
    }

    // ---------- Mode 2 — 24-line pool (excl G7) ----------

    @Test
    void mode2_PerMatchOver24Pool() {
        LotteryResult rs = new LotteryResult();
        LotteryResult.Results r = new LotteryResult.Results();
        r.setĐB(Collections.singletonList("12345"));
        r.setG1(Arrays.asList("00111"));
        r.setG2(Collections.<String>emptyList());
        r.setG3(Collections.<String>emptyList());
        r.setG4(Collections.<String>emptyList());
        r.setG5(Collections.<String>emptyList());
        r.setG6(Collections.<String>emptyList());
        // G7 lines end in 111 too — but mode 2 ignores G7, so they should NOT count.
        r.setG7(Arrays.asList("9111", "8111"));
        rs.setResults(r);
        rs.setTime("14-05-2026");

        LotteryTicket t = ticket(2, "111", 1000L, 23, 600);

        PrizeBreakdown pb = PrizeCalculator.calculate(rs, t);
        // Only G1 match should count → 1 * 1000 * 600 = 600_000
        assertThat(pb.getMatches()).isEqualTo(1);
        assertThat(pb.getPrize()).isEqualTo(600_000L);
    }

    // ---------- Mode 3 / 4 flat payouts ----------

    @Test
    void mode3_FlatIfBothMatch() {
        List<String> pool = PrizeCalculator.asList("AA11", "BB22", "ZZ99");
        // storedBet = userBet * rate; mode 3 has rate=1 so storedBet=userBet.
        long prize = PrizeCalculator.mode3(pool, "11,22", 50_000L, 12).getPrize();
        assertThat(prize).isEqualTo(50_000L * 12L);

        long miss = PrizeCalculator.mode3(pool, "11,33", 50_000L, 12).getPrize();
        assertThat(miss).isZero();
    }

    @Test
    void mode4_FlatIfAllThreeMatch() {
        List<String> pool = PrizeCalculator.asList("AA11", "BB22", "CC33", "ZZ99");
        long prize = PrizeCalculator.mode4(pool, "11,22,33", 10_000L, 48).getPrize();
        assertThat(prize).isEqualTo(10_000L * 48L);

        long miss = PrizeCalculator.mode4(pool, "11,22,44", 10_000L, 48).getPrize();
        assertThat(miss).isZero();
    }
}
