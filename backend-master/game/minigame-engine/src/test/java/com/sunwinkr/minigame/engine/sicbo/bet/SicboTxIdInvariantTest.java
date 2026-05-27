package com.sunwinkr.minigame.engine.sicbo.bet;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-12: SicboTxIdGenerator uniqueness within a round.
 *
 * Generates 1_000_000 IDs from a single generator and asserts zero collisions.
 * Also verifies the formula: {@code referenceId * 1_000_000L + sequence}.
 */
class SicboTxIdInvariantTest {

    /** 1M IDs from the same generator must all be distinct. */
    @Test
    void uniqueWithinRound() {
        long refId = 999L;
        SicboTxIdGenerator gen = new SicboTxIdGenerator(refId);

        int count = 1_000_000;
        Set<Long> seen = new HashSet<>(count * 2);
        for (int i = 0; i < count; i++) {
            long id = gen.next();
            assertThat(seen.add(id)).as("collision at i=" + i + " id=" + id).isTrue();
        }
        assertThat(seen).hasSize(count);
    }

    /** Formula check: first ID = refId * 1_000_000 + 1. */
    @Test
    void formulaIsCorrect() {
        long refId = 42L;
        SicboTxIdGenerator gen = new SicboTxIdGenerator(refId);

        assertThat(gen.next()).isEqualTo(42L * 1_000_000L + 1L);
        assertThat(gen.next()).isEqualTo(42L * 1_000_000L + 2L);
        assertThat(gen.next()).isEqualTo(42L * 1_000_000L + 3L);
    }

    /** Concurrent callers from two threads produce no collisions. */
    @Test
    void uniqueUnderConcurrency() throws InterruptedException {
        long refId = 7L;
        SicboTxIdGenerator gen = new SicboTxIdGenerator(refId);

        int perThread = 50_000;
        int threads = 4;
        Set<Long> seen = ConcurrentHashMap.newKeySet(perThread * threads * 2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        seen.add(gen.next());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(seen).hasSize(perThread * threads);
    }
}
