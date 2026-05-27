package com.sunwinkr.minigame.engine.bet;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-round virtual-player pad. Mirrors {@code MGRoomTaiXiu
 * .refreshPadIfNeeded()} (TXR:513-527) — for SUN-807 each side draws
 * an independent count in {@code [tx_fake_player_min, tx_fake_player_max]}
 * once per round (keyed on {@code referenceId}); collisions get a
 * ±1 jitter so the two sides are never equal.
 *
 * <p>Defaults match legacy: min=30, max=60. The class is thread-safe
 * for concurrent readers because every {@code countsFor(refId)} call
 * latches the result behind a volatile read of {@link #padReferenceId}.
 *
 * <p>Plan §2.2 row B10.
 */
public final class FakePlayerPad {

    /** Default min count per side (TXR:516 — {@code tx_fake_player_min}). */
    public static final int DEFAULT_MIN = 30;

    /** Default max count per side (TXR:517 — {@code tx_fake_player_max}). */
    public static final int DEFAULT_MAX = 60;

    private final int fakeMin;
    private final int fakeMax;

    private volatile long padReferenceId = -1L;
    private volatile int cachedPadTai;
    private volatile int cachedPadXiu;

    /** Default-bounds constructor [30..60]. */
    public FakePlayerPad() {
        this(DEFAULT_MIN, DEFAULT_MAX);
    }

    /**
     * @param fakeMin lower bound inclusive
     * @param fakeMax upper bound inclusive; must be {@code >= fakeMin}
     *                and {@code > 0} per TXR:518 — otherwise pad
     *                disabled (returns 0/0).
     */
    public FakePlayerPad(int fakeMin, int fakeMax) {
        this.fakeMin = fakeMin;
        this.fakeMax = fakeMax;
    }

    /**
     * Return cached (or freshly drawn) pad counts for the given refId.
     * Synchronized to make the draw atomic — multiple snapshot threads
     * racing on a new round must not produce different pad values.
     *
     * @return immutable {@link Counts} pair
     */
    public synchronized Counts countsFor(long refId) {
        if (padReferenceId == refId) {
            return new Counts(cachedPadTai, cachedPadXiu);
        }
        padReferenceId = refId;
        if (fakeMax <= 0 || fakeMax < fakeMin) {
            cachedPadTai = 0;
            cachedPadXiu = 0;
            return new Counts(0, 0);
        }
        int span = fakeMax - fakeMin + 1;
        int a = fakeMin + ThreadLocalRandom.current().nextInt(span);
        int b = fakeMin + ThreadLocalRandom.current().nextInt(span);
        if (a == b) {
            // Same legacy jitter (TXR:524): bump b away from the floor.
            b += (b > fakeMin) ? -1 : 1;
        }
        cachedPadTai = a;
        cachedPadXiu = b;
        return new Counts(a, b);
    }

    /** Result pair returned by {@link #countsFor(long)}. */
    public static final class Counts {
        public final int numBetTai;
        public final int numBetXiu;

        public Counts(int numBetTai, int numBetXiu) {
            this.numBetTai = numBetTai;
            this.numBetXiu = numBetXiu;
        }
    }
}
