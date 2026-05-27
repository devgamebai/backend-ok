package com.sunwinkr.minigame.engine.jackpot;

/**
 * Engine-side jackpot accumulator. Mirrors the legacy
 * {@code MGRoomTaiXiu.jackpotAccumulate} field with the +0.6% growth
 * rule from TXR:558 and the 50M VIN floor from TXR:166-168.
 *
 * <p>Accumulation rule (TXR:558):
 * <pre>jp_new = (long)(jp_prev + losingPot * 0.006)</pre>
 *
 * Reset rule: when a jackpot triggers (per
 * {@link JackpotTriggerPolicy}), {@code resetJp = true} is set; the
 * caller is expected to follow up with {@link #resetToFloor()} on the
 * next round start.
 *
 * <p>Plan §2.5 row J1 / spec INV-10.
 */
public final class JackpotPool {

    /** Floor jackpot — 50M VIN (TXR:166-168). */
    public static final long FLOOR_VIN = 50_000_000L;

    private volatile long value;

    /** Initialize at the floor (50M VIN). */
    public JackpotPool() {
        this.value = FLOOR_VIN;
    }

    /** Initialize at a supplied value (used on bootstrap from persisted state). */
    public JackpotPool(long initial) {
        this.value = Math.max(FLOOR_VIN, initial);
    }

    /** Current accumulated value in VIN units. */
    public long value() {
        return value;
    }

    /**
     * Accumulate +0.6% of the losing-side pot.
     *
     * <p>Sign-preserving — caller passes the losing pot's total (positive).
     * Negative inputs are clamped to zero (legacy {@code (long)(0 + neg *
     * 0.006)} would underflow incorrectly).
     */
    public void accumulate(long losingPotTotal) {
        if (losingPotTotal <= 0L) {
            return;
        }
        this.value = (long) ((double) this.value + (double) losingPotTotal * 0.006);
    }

    /** Reset to the floor — after a jackpot trigger. */
    public void resetToFloor() {
        this.value = FLOOR_VIN;
    }

    /** Test/admin seam — overwrite directly (clamped at floor). */
    public void setValue(long v) {
        this.value = Math.max(FLOOR_VIN, v);
    }
}
