package com.sunwinkr.minigame.engine.dice;

import com.sunwinkr.minigame.engine.jackpot.JackpotTriggerPolicy;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec INV-3: force-result is consumed exactly once. */
class ForceResultInvariantTest {

    @Test
    void consumedOnce() {
        // INV-3: the IMap.remove("ketquataixiu") is atomic — once
        // consumed, the next peek must return empty.
        InMemoryForceResultStore store = new InMemoryForceResultStore();
        store.set(new short[] { 1, 2, 3 });

        ResultPipeline pipeline = new ResultPipeline(
            store,
            new RandomDiceGenerator(),
            new JackpotTriggerPolicy(new InMemoryJackpotForcePort()));

        short[] first = pipeline.generate(RoundContext.of(0L, 0L), 0L, 0L);
        assertThat(first).containsExactly((short) 1, (short) 2, (short) 3);

        // Second call: no admin force queued anymore → random fallback.
        assertThat(store.hasValue()).isFalse();
    }

    @Test
    void peekRemovesKey() {
        // Port-level invariant: peekAndConsume() returns Empty after one
        // call without a re-set.
        InMemoryForceResultStore store = new InMemoryForceResultStore();
        store.set(new short[] { 6, 6, 6 });
        Optional<short[]> first = store.peekAndConsume();
        Optional<short[]> second = store.peekAndConsume();
        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(store.peekCount()).isEqualTo(2);
    }
}
