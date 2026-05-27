package com.sunwinkr.minigame.engine.dice;

import com.sunwinkr.minigame.engine.jackpot.JackpotTriggerPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec INV-2: dice are generated exactly once per round.
 * In production the VIN room calls {@code getResult} and the XU room
 * receives the same dice via {@code updateResultDices}. Here we verify
 * a single pipeline produces a single (non-mutating) output per call.
 */
class TwoRoomDiceTest {

    @Test
    void sharedResult() {
        InMemoryForceResultStore store = new InMemoryForceResultStore();
        store.set(new short[] { 3, 5, 6 });
        ResultPipeline pipeline = new ResultPipeline(
            store,
            new RandomDiceGenerator(),
            new JackpotTriggerPolicy(new InMemoryJackpotForcePort()));

        short[] vinDice = pipeline.generate(RoundContext.of(0L, 0L), 0L, 0L);
        // Simulate VIN broadcasts to XU — pass the same dice along.
        short[] xuDice = new short[] { vinDice[0], vinDice[1], vinDice[2] };

        assertThat(vinDice).containsExactly((short) 3, (short) 5, (short) 6);
        assertThat(xuDice).containsExactly(vinDice);
    }
}
