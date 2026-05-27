package com.sunwinkr.minigame.engine.jackpot;

import com.sunwinkr.minigame.engine.dice.InMemoryJackpotForcePort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec INV-10: jackpot side override gated by %5 of matching side. */
class JackpotTriggerTest {

    @Test
    void gatedBy5Modulo() {
        // INV-10: jp side override fires iff potTai.numBet%5==0 (for jp=6)
        // or potXiu.numBet%5==0 (for jp=1). Otherwise the dice are passed
        // through unchanged.
        InMemoryJackpotForcePort port = new InMemoryJackpotForcePort();
        JackpotTriggerPolicy policy = new JackpotTriggerPolicy(port);
        short[] in = new short[] { 2, 3, 4 };

        // jp=6, numBet=3 → suppressed (3 % 5 != 0).
        port.set((short) 6);
        short[] out1 = policy.apply(in, /*potTaiNumBet*/ 3, /*potXiuNumBet*/ 0);
        assertThat(out1).containsExactly((short) 2, (short) 3, (short) 4);
        assertThat(policy.isJpTai()).isFalse();
        assertThat(policy.isResetJp()).isFalse();

        // jp=6, numBet=5 → triggered → triple-6.
        port.set((short) 6);
        short[] out2 = policy.apply(in, /*potTaiNumBet*/ 5, /*potXiuNumBet*/ 0);
        assertThat(out2).containsExactly((short) 6, (short) 6, (short) 6);
        assertThat(policy.isJpTai()).isTrue();
        assertThat(policy.isJpXiu()).isFalse();
        assertThat(policy.isResetJp()).isTrue();

        // jp=1, potXiu.numBet=10 → triggered → triple-1.
        port.set((short) 1);
        short[] out3 = policy.apply(in, /*potTaiNumBet*/ 0, /*potXiuNumBet*/ 10);
        assertThat(out3).containsExactly((short) 1, (short) 1, (short) 1);
        assertThat(policy.isJpXiu()).isTrue();
        assertThat(policy.isJpTai()).isFalse();
        assertThat(policy.isResetJp()).isTrue();

        // jp=99 (junk) → no override.
        port.set((short) 99);
        short[] out4 = policy.apply(in, /*potTaiNumBet*/ 5, /*potXiuNumBet*/ 5);
        assertThat(out4).containsExactly((short) 2, (short) 3, (short) 4);
        assertThat(policy.isResetJp()).isFalse();
    }
}
