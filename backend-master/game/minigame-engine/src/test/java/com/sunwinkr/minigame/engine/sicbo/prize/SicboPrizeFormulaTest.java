package com.sunwinkr.minigame.engine.sicbo.prize;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculatorTest.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SicboPrizeCalculator#calculate} verifying INV-8
 * (non-special prize = bet * rotation) and the fee formula.
 */
class SicboPrizeFormulaTest {

    private final SicboPayoutCalculator payout = new SicboPayoutCalculator();
    private final SicboPrizeCalculator prize = new SicboPrizeCalculator(5.0f, payout);

    @Test
    void winningNonSpecial() {
        // POINT_9 bet, dice (4,3,2) total=9 → POINT_9 wins
        // INV-8: prize = bet * rotation. POINT_9.rotation = 7.
        // Fee = tax * prize / (200 - tax) = 5 * 7000 / 195
        long bet = 1_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            entry("u1", 1, bet, SicboBetType.POINT_9));

        SicboSettleResult result = prize.calculate(bets, new short[]{4, 3, 2});

        assertThat(result.perBet).hasSize(1);
        SicboPrizePerBet pb = result.perBet.get(0);
        assertThat(pb.prize).isEqualTo(bet * 7L);
        long expectedFee = (long) (5.0f * (float) (bet * 7L) / 195.0f);
        assertThat(pb.fee).isEqualTo(expectedFee);
        assertThat(pb.refund).isFalse();
    }

    @Test
    void totalPayoutOnlyRealUsers() {
        long bet = 1_000L;
        List<SicboPotEntry> bets = new ArrayList<>();
        bets.add(entry("user1", 1, bet, SicboBetType.TAI));
        bets.add(entry("bot1",  0, bet * 50L, SicboBetType.TAI));

        // dice (6,5,1) → TAI wins; rotation=2
        SicboSettleResult result = prize.calculate(bets, new short[]{6, 5, 1});

        // Only real user contributes to totalPayout — matches sotienphaitra
        assertThat(result.totalPayout).isEqualTo(bet * 2L);
    }

    @Test
    void nonWinnerNoPrize() {
        long bet = 1_000L;
        List<SicboPotEntry> bets = Collections.singletonList(
            entry("u1", 1, bet, SicboBetType.TAI));

        // dice (1,1,1) triple → TAI suppressed (INV-15)
        SicboSettleResult result = prize.calculate(bets, new short[]{1, 1, 1});

        assertThat(result.perBet).hasSize(1);
        assertThat(result.perBet.get(0).prize).isEqualTo(0L);
        assertThat(result.perBet.get(0).fee).isEqualTo(0L);
        assertThat(result.totalPayout).isEqualTo(0L);
    }
}
