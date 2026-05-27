package com.sunwinkr.minigame.engine.bet;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plan §2.2 row B8 — {@link PotState} thread-safety.
 * Stress-drives concurrent {@code addContributor} from 8 threads and
 * asserts the final {@code totalValue} / contributors size are exact.
 */
class PotStateTest {

    @Test
    void threadSafeAdd() throws Exception {
        PotState pot = new PotState();
        int threads = 8;
        int perThread = 1_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger seq = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        int n = seq.getAndIncrement();
                        TransactionTaiXiuDetail trans = new TransactionTaiXiuDetail(
                            1L, tid, "u" + n, 1L,
                            /*betSide*/ 1, /*inputTime*/ 30, /*moneyType*/ 1,
                            /*currentMoney*/ 0L, /*txId*/ n);
                        pot.addReal(trans);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        int total = threads * perThread;
        assertThat(pot.totalValue()).isEqualTo(total);
        assertThat(pot.contributors()).hasSize(total);
        // Every user is unique → numBet equals total (capped at short MAX,
        // but 8000 fits).
        assertThat(pot.numBet()).isEqualTo((short) total);
        assertThat(pot.realNumBet()).isEqualTo((short) total);
    }

    @Test
    void botContributorsIsolatedFromRealTotals() {
        PotState pot = new PotState();
        pot.addReal(detail("alice", 1000L));
        pot.addBot(detail("botX", 500L));
        pot.addReal(detail("alice", 200L));

        assertThat(pot.totalValue()).isEqualTo(1700L);
        assertThat(pot.botStats().numBot).isEqualTo(1);
        assertThat(pot.botStats().totalBotBet).isEqualTo(500L);
        assertThat(pot.realTotal()).isEqualTo(1200L);
        assertThat(pot.realNumBet()).isEqualTo((short) 1);  // alice only
        assertThat(pot.numBet()).isEqualTo((short) 2);      // alice + botX
        assertThat(pot.totalByUser("alice")).isEqualTo(1200L);
        assertThat(pot.totalByUser("botX")).isEqualTo(500L);
        assertThat(pot.totalByUser("ghost")).isZero();
        assertThat(pot.hasBet("alice")).isTrue();
        assertThat(pot.hasBet("botX")).isFalse();
    }

    @Test
    void renewClearsAllState() {
        PotState pot = new PotState();
        pot.addReal(detail("alice", 1000L));
        pot.addBot(detail("botX", 500L));
        pot.renew();
        assertThat(pot.totalValue()).isZero();
        assertThat(pot.contributors()).isEmpty();
        assertThat(pot.botStats().numBot).isZero();
        assertThat(pot.botStats().totalBotBet).isZero();
    }

    private static TransactionTaiXiuDetail detail(String user, long value) {
        return new TransactionTaiXiuDetail(1L, 0, user, value, 1, 30, 1, 0L, 0L);
    }
}
