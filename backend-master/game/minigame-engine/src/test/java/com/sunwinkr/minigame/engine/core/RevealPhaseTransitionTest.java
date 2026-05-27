package com.sunwinkr.minigame.engine.core;

import com.sunwinkr.minigame.engine.port.CachePort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plan §8.1: {@code RevealPhaseTransitionTest}.
 *
 * <p>Asserts that illegal phase transitions throw
 * {@link IllegalStateException}, e.g. {@code OPEN → GENERATING} (skipping
 * {@code LOCKED}). The state machine MUST reject any transition that is
 * not in the canonical cycle.
 */
class RevealPhaseTransitionTest {

    private static final CachePort NO_OP_CACHE = new CachePort() {
        @Override public void setAllowBetting(long refId, boolean v) { }
        @Override public void removeAllowBetting(long refId) { }
        @Override public void setCurrentReference(long refId) { }
    };

    @Test
    void illegalTransitionThrows_openToGenerating() {
        TaiXiuRound r = new TaiXiuRound(new RevealClock.SimpleRevealClock(), NO_OP_CACHE);
        assertThatThrownBy(() -> r.advance(RevealPhase.GENERATING))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPEN")
            .hasMessageContaining("GENERATING");
    }

    @Test
    void illegalTransitionThrows_openToSettled() {
        TaiXiuRound r = new TaiXiuRound(new RevealClock.SimpleRevealClock(), NO_OP_CACHE);
        assertThatThrownBy(() -> r.advance(RevealPhase.SETTLED))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void illegalTransitionThrows_lockedToReveal_skipsGenerating() {
        TaiXiuRound r = new TaiXiuRound(new RevealClock.SimpleRevealClock(), NO_OP_CACHE);
        r.advance(RevealPhase.LOCKED);
        assertThatThrownBy(() -> r.advance(RevealPhase.REVEALED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("LOCKED")
            .hasMessageContaining("REVEALED");
    }

    @Test
    void illegalTransition_backwards() {
        // REVEALED → GENERATING is backwards and must be rejected.
        RevealPhase.requireLegalTransition(RevealPhase.OPEN, RevealPhase.LOCKED);
        RevealPhase.requireLegalTransition(RevealPhase.LOCKED, RevealPhase.GENERATING);
        RevealPhase.requireLegalTransition(RevealPhase.GENERATING, RevealPhase.REVEALED);
        assertThatThrownBy(() ->
            RevealPhase.requireLegalTransition(RevealPhase.REVEALED, RevealPhase.GENERATING))
            .isInstanceOf(IllegalStateException.class);
    }
}
