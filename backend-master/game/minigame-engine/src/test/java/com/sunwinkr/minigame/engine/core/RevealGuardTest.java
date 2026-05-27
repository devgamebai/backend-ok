package com.sunwinkr.minigame.engine.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plan §8.1: {@code RevealGuardTest}.
 *
 * <p>Asserts that {@link RevealGuard#traceDice(RevealPhase, short[], String)}
 * throws {@link IllegalStateException} in every phase except REVEALED and
 * SETTLED (the two visible phases). This is the safety net replacing the
 * three legacy {@code System.out.println} sites (plan §3.4) that print
 * dice values.
 */
class RevealGuardTest {

    private final short[] dice = new short[]{3, 5, 4};

    @Test
    void throwsBeforeReveal_open() {
        assertThatThrownBy(() -> RevealGuard.traceDice(RevealPhase.OPEN, dice, "x"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPEN");
    }

    @Test
    void throwsBeforeReveal_locked() {
        assertThatThrownBy(() -> RevealGuard.traceDice(RevealPhase.LOCKED, dice, "x"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("LOCKED");
    }

    @Test
    void throwsBeforeReveal_generating() {
        // GENERATING is the most important guard — pendingDice exists here.
        assertThatThrownBy(() -> RevealGuard.traceDice(RevealPhase.GENERATING, dice, "x"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GENERATING");
    }

    @Test
    void okAfterReveal() {
        assertThatCode(() -> RevealGuard.traceDice(RevealPhase.REVEALED, dice, "Result End"))
            .doesNotThrowAnyException();
        assertThatCode(() -> RevealGuard.traceDice(RevealPhase.SETTLED, dice, "Settled"))
            .doesNotThrowAnyException();
    }

    @Test
    void throwsBeforeReveal_cleanup() {
        // CLEANUP comes after SETTLED so dice technically remain visible
        // in our model, but for safety, only REVEALED and SETTLED are
        // explicitly diceVisible(). CLEANUP guard MUST throw.
        assertThatThrownBy(() -> RevealGuard.traceDice(RevealPhase.CLEANUP, dice, "x"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CLEANUP");
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> RevealGuard.traceDice(null, dice, "x"))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RevealGuard.traceDice(RevealPhase.REVEALED, null, "x"))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RevealGuard.traceDice(RevealPhase.REVEALED, dice, null))
            .isInstanceOf(NullPointerException.class);
    }
}
