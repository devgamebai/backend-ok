package com.sunwinkr.minigame.engine.core;

import com.sunwinkr.minigame.engine.port.CachePort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plan §8.1: {@code RoundLifecycleInvariantTest}.
 *
 * <p>Covers:
 * <ul>
 *   <li>INV-17 — gameLoop tick monotonicity: count reaches 68 exactly
 *       once per round, and the phase sequence is the legal cycle
 *       {@code OPEN → LOCKED → GENERATING → REVEALED → SETTLED → CLEANUP → OPEN}.</li>
 *   <li>INV-1 — refId monotonicity: {@code startNewRound(refId)} advances
 *       the internal reference id strictly.</li>
 * </ul>
 */
class RoundLifecycleInvariantTest {

    /** Records every CachePort call so we can assert lifecycle side-effects later. */
    private static final class RecordingCachePort implements CachePort {
        final List<String> events = new ArrayList<>();

        @Override
        public void setAllowBetting(long refId, boolean v) {
            events.add("setAllowBetting(" + refId + "," + v + ")");
        }

        @Override
        public void removeAllowBetting(long refId) {
            events.add("removeAllowBetting(" + refId + ")");
        }

        @Override
        public void setCurrentReference(long refId) {
            events.add("setCurrentReference(" + refId + ")");
        }
    }

    @Test
    void advancesThroughAllPhases() {
        RecordingCachePort cache = new RecordingCachePort();
        TaiXiuRound round = new TaiXiuRound(new RevealClock.SimpleRevealClock(), cache);

        // Start at OPEN; explicitly seed the first round.
        round.startNewRound(2L);
        assertThat(round.phase()).isEqualTo(RevealPhase.OPEN);

        // Capture phase at every tick. We don't capture the post-NEW_ROUND
        // phase from the same tick - that's a separate transition that
        // happens via startNewRound() internally.
        List<RevealPhase> observedPhases = new ArrayList<>();
        observedPhases.add(round.phase());
        for (int t = 1; t <= 68; t++) {
            round.tick(t * 1000L);
            observedPhases.add(round.phase());
        }

        // Verify all expected phase transitions occurred in the right order.
        // Pre-lock: ticks 1..44 stay OPEN.
        for (int t = 1; t <= 44; t++) {
            assertThat(observedPhases.get(t))
                .as("tick %d should be OPEN", t)
                .isEqualTo(RevealPhase.OPEN);
        }
        // tick 45 → LOCKED; stays LOCKED through 50
        assertThat(observedPhases.get(45)).as("tick 45 LOCKED").isEqualTo(RevealPhase.LOCKED);
        assertThat(observedPhases.get(48)).as("tick 48 LOCKED").isEqualTo(RevealPhase.LOCKED);
        assertThat(observedPhases.get(50)).as("tick 50 LOCKED").isEqualTo(RevealPhase.LOCKED);
        // tick 51 → GENERATING; tick 52 → REVEALED
        assertThat(observedPhases.get(51)).as("tick 51 GENERATING").isEqualTo(RevealPhase.GENERATING);
        assertThat(observedPhases.get(52)).as("tick 52 REVEALED").isEqualTo(RevealPhase.REVEALED);
        // tick 56 → SETTLED
        assertThat(observedPhases.get(56)).as("tick 56 SETTLED").isEqualTo(RevealPhase.SETTLED);
        // tick 60 → CLEANUP
        assertThat(observedPhases.get(60)).as("tick 60 CLEANUP").isEqualTo(RevealPhase.CLEANUP);
        // tick 68 → NEW_ROUND drives startNewRound which transitions phase back to OPEN
        assertThat(observedPhases.get(68)).as("tick 68 OPEN (new round)").isEqualTo(RevealPhase.OPEN);

        // Confirm the unique-cycle sentinel: the legal forward sequence
        // (OPEN, LOCKED, GENERATING, REVEALED, SETTLED, CLEANUP, OPEN)
        // appears in order in the observed sequence.
        List<RevealPhase> expectedCycle = Arrays.asList(
            RevealPhase.OPEN,
            RevealPhase.LOCKED,
            RevealPhase.GENERATING,
            RevealPhase.REVEALED,
            RevealPhase.SETTLED,
            RevealPhase.CLEANUP,
            RevealPhase.OPEN);
        assertThat(extractTransitions(observedPhases))
            .as("phase transition sequence")
            .containsExactlyElementsOf(expectedCycle);

        // CachePort side-effects fired exactly: setCurrentReference + setAllowBetting(true)
        // at startNewRound; setAllowBetting(false) at lockBetting; removeAllowBetting
        // at finishRound; then setCurrentReference + setAllowBetting(true) again on the
        // wrap-around new round at tick 68.
        assertThat(cache.events).contains(
            "setCurrentReference(2)",
            "setAllowBetting(2,true)",
            "setAllowBetting(2,false)",
            "removeAllowBetting(2)",
            "setCurrentReference(3)",
            "setAllowBetting(3,true)");
    }

    @Test
    void refIdMonotonic() {
        // INV-1: refId must strictly increase across startNewRound() calls.
        TaiXiuRound round = new TaiXiuRound(new RevealClock.SimpleRevealClock(), new RecordingCachePort());

        // Constructor seeds refId=1L per plan §2.1 L8.
        assertThat(round.referenceId()).isEqualTo(1L);

        round.startNewRound(2L);
        assertThat(round.referenceId()).isEqualTo(2L);

        round.startNewRound(3L);
        assertThat(round.referenceId()).isEqualTo(3L);

        // Non-monotonic should throw.
        try {
            round.startNewRound(3L);
            org.junit.jupiter.api.Assertions.fail("expected IAE for non-monotonic refId");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage()).contains("strictly increasing");
        }

        try {
            round.startNewRound(1L);
            org.junit.jupiter.api.Assertions.fail("expected IAE for refId regression");
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage()).contains("strictly increasing");
        }
    }

    /** Squash consecutive duplicate phases into a transition list. */
    private static List<RevealPhase> extractTransitions(List<RevealPhase> raw) {
        List<RevealPhase> out = new ArrayList<>();
        RevealPhase prev = null;
        for (RevealPhase p : raw) {
            if (prev == null || p != prev) {
                out.add(p);
                prev = p;
            }
        }
        return out;
    }
}
