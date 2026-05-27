package com.sunwinkr.minigame.engine.port;

/**
 * Engine-side port for the persisted jackpot value. Wraps the Mongo
 * singleton document {@code jackpot_tx.{ jackpotTX: <string-num> }}
 * (TXR.updateJpValue:1234, spec §6).
 *
 * <p>Reads and writes are idempotent; the engine reads on round start to
 * seed {@link com.sunwinkr.minigame.engine.jackpot.JackpotPool} and
 * writes after pool accumulation / distribution.
 *
 * <p>Plan §2.5 row J4.
 */
public interface JackpotStatePort {

    /** Read the persisted jackpot value (VIN units). Returns 0 if missing. */
    long read();

    /** Persist the supplied jackpot value (VIN units). */
    void write(long jpAmount);
}
