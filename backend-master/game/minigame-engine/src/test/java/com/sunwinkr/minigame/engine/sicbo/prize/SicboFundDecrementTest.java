package com.sunwinkr.minigame.engine.sicbo.prize;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.sunwinkr.minigame.engine.sicbo.prize.SicboPayoutCalculatorTest.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-19: after reward,
 *   fundTaiXiu_new = fund_old + (totalValueBetUser - totalPayout)
 *
 * Verifies the engine's settle-result aligns with the SBR:619 fund-update
 * arithmetic in {@code MGRoomSicbo.getResult} / {@code reward()}.
 */
class SicboFundDecrementTest {

    private final SicboPayoutCalculator payout = new SicboPayoutCalculator();
    private final SicboPrizeCalculator prize = new SicboPrizeCalculator(5.0f, payout);

    @Test
    void fundUpdatedByDelta() {
        // 2 user bets:
        //   user1 bets 1000 on TAI
        //   user2 bets 2000 on POINT_9
        // Dice (4,3,2) total=9 → TAI suppressed (total<=10 means XIU not TAI),
        // POINT_9 wins (rotation=7).
        long u1Bet = 1_000L;
        long u2Bet = 2_000L;
        List<SicboPotEntry> bets = new ArrayList<>();
        bets.add(entry("user1", 1, u1Bet, SicboBetType.TAI));
        bets.add(entry("user2", 2, u2Bet, SicboBetType.POINT_9));

        long totalBet = u1Bet + u2Bet;
        long fundOld = 10_000_000L;

        // Run prize calc and payout calc — both must agree
        long calcPayout = payout.calculatePotentialPayout(bets, new short[]{4, 3, 2});
        SicboSettleResult result = prize.calculate(bets, new short[]{4, 3, 2});

        assertThat(calcPayout).as("payout calc and prize calc must agree").isEqualTo(result.totalPayout);

        long fundNew = fundOld + (totalBet - result.totalPayout);

        // POINT_9 pays 14000, TAI pays 0 → totalPayout=14000.
        // fundNew = 10000000 + (3000 - 14000) = 10000000 - 11000 = 9989000.
        assertThat(result.totalPayout).isEqualTo(u2Bet * 7L);
        assertThat(fundNew).isEqualTo(fundOld + (totalBet - u2Bet * 7L));
        assertThat(fundNew).isEqualTo(9_989_000L);
    }

    @Test
    void fundUpdatedByDeltaHouseWins() {
        // 1 user bet that loses → totalPayout=0 → fund grows by totalBet.
        long u1Bet = 5_000L;
        List<SicboPotEntry> bets = new ArrayList<>();
        bets.add(entry("user1", 1, u1Bet, SicboBetType.TAI));

        long fundOld = 1_000_000L;
        // Dice (1,1,1) — triple, TAI suppressed → no payout
        SicboSettleResult result = prize.calculate(bets, new short[]{1, 1, 1});

        long fundNew = fundOld + (u1Bet - result.totalPayout);
        assertThat(result.totalPayout).isEqualTo(0L);
        assertThat(fundNew).isEqualTo(fundOld + u1Bet);
    }
}
