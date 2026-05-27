package com.sunwinkr.minigame.engine.bet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plan §2.2 row B10 — {@link FakePlayerPad}.
 *
 * <p>Pins:
 * <ul>
 *   <li>Per-round caching — same refId returns the same pair.</li>
 *   <li>Equal counts get a ±1 jitter so the two sides are never equal
 *       (TXR:524).</li>
 *   <li>Default-bounds disabled config returns (0, 0).</li>
 * </ul>
 */
class FakePlayerPadTest {

    @Test
    void cachedPerRoundCollisionsJittered() {
        FakePlayerPad pad = new FakePlayerPad();
        FakePlayerPad.Counts a = pad.countsFor(1L);
        FakePlayerPad.Counts b = pad.countsFor(1L);

        // Caching: identical refId → identical counts.
        assertThat(a.numBetTai).isEqualTo(b.numBetTai);
        assertThat(a.numBetXiu).isEqualTo(b.numBetXiu);

        // Bounds within [30..60].
        assertThat(a.numBetTai).isBetween(FakePlayerPad.DEFAULT_MIN, FakePlayerPad.DEFAULT_MAX);
        assertThat(a.numBetXiu).isBetween(FakePlayerPad.DEFAULT_MIN, FakePlayerPad.DEFAULT_MAX);

        // Jitter invariant: two sides MUST differ.
        assertThat(a.numBetTai).isNotEqualTo(a.numBetXiu);

        // New refId draws a fresh pair (could happen to match — only
        // assert independence by asserting the call returns within bounds).
        FakePlayerPad.Counts c = pad.countsFor(2L);
        assertThat(c.numBetTai).isBetween(FakePlayerPad.DEFAULT_MIN, FakePlayerPad.DEFAULT_MAX);
        assertThat(c.numBetXiu).isBetween(FakePlayerPad.DEFAULT_MIN, FakePlayerPad.DEFAULT_MAX);
        assertThat(c.numBetTai).isNotEqualTo(c.numBetXiu);
    }

    @Test
    void disabledBoundsReturnsZero() {
        // max <= 0 → pad disabled per TXR:518.
        FakePlayerPad pad = new FakePlayerPad(30, 0);
        FakePlayerPad.Counts c = pad.countsFor(5L);
        assertThat(c.numBetTai).isZero();
        assertThat(c.numBetXiu).isZero();
    }

    @Test
    void jitterAcrossManyDraws() {
        // Repeated draws never produce equal pairs.
        FakePlayerPad pad = new FakePlayerPad();
        for (long ref = 100L; ref < 1_000L; ref++) {
            FakePlayerPad.Counts c = pad.countsFor(ref);
            assertThat(c.numBetTai)
                .as("ref=%d should not have equal pad counts", ref)
                .isNotEqualTo(c.numBetXiu);
        }
    }
}
