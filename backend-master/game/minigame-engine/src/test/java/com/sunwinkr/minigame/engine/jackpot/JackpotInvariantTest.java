package com.sunwinkr.minigame.engine.jackpot;

import com.sunwinkr.minigame.engine.bet.PotState;
import com.sunwinkr.minigame.engine.bet.TransactionTaiXiuDetail;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec INV-10 + INV-11: pool accumulates +0.6% and shares sum ≈ pool. */
class JackpotInvariantTest {

    @Test
    void poolAccumulates006Percent() {
        // INV-10: +0.6% of losing-pot accumulator (TXR:558). Floor 50M VIN.
        JackpotPool pool = new JackpotPool(/*seed*/ 100_000_000L); // 100M
        pool.accumulate(10_000_000L); // +60_000 expected
        assertThat(pool.value()).isEqualTo(100_060_000L);

        // Floor: never drops below 50M.
        JackpotPool floored = new JackpotPool(10L); // clamped to 50M
        assertThat(floored.value()).isEqualTo(JackpotPool.FLOOR_VIN);
    }

    @Test
    void distributionSumEqualsJpAtTrigger() {
        // INV-11: sum(jpAmount) over winners == jp, mod integer truncation
        // drift <= winner_count.
        PotState pot = new PotState();
        TransactionTaiXiuDetail t1 = makeTran("u1", 1, 100_000L);
        TransactionTaiXiuDetail t2 = makeTran("u2", 2, 200_000L);
        TransactionTaiXiuDetail t3 = makeTran("u3", 3, 300_000L);
        pot.addReal(t1);
        pot.addReal(t2);
        pot.addReal(t3);

        long jp = 50_000_000L;
        long tongTienHopLe = 1_000_000L;
        JackpotDistributor dist = new JackpotDistributor();
        List<JackpotDistributor.JackpotShare> shares =
            dist.distribute(pot, tongTienHopLe, jp, Collections.<String>emptySet());

        long sum = 0L;
        for (JackpotDistributor.JackpotShare s : shares) {
            sum += s.jpAmount;
        }
        // Drift <= winner_count (3).
        assertThat(jp - sum).isLessThanOrEqualTo(shares.size());
        assertThat(sum).isLessThanOrEqualTo(jp);
        // Proportional: u3 (3x bet) gets ~3x of u1's share.
        assertThat(shares.get(2).jpAmount).isGreaterThan(shares.get(0).jpAmount * 2);
    }

    @Test
    void botSkipMarkedInShares() {
        // INV-11 corollary: bot winners are flagged so the wallet/notify
        // adapter can skip them (TXR:813, 818). The distributor does NOT
        // skip the share computation — it only marks them.
        PotState pot = new PotState();
        TransactionTaiXiuDetail t1 = makeTran("realuser", 1, 100_000L);
        TransactionTaiXiuDetail t2 = makeTran("bot1", 0, 100_000L);
        pot.addReal(t1);
        pot.addBot(t2);

        HashSet<String> bots = new HashSet<>();
        bots.add("bot1");

        List<JackpotDistributor.JackpotShare> shares =
            new JackpotDistributor().distribute(pot, 200_000L, 50_000_000L, bots);
        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).isBot).isFalse();
        assertThat(shares.get(1).isBot).isTrue();
    }

    private static TransactionTaiXiuDetail makeTran(String user, int userId, long bet) {
        return new TransactionTaiXiuDetail(
            1L, userId, user, bet, 1, 10, 1, 1_000_000L,
            1_000_000L + userId);
    }
}
