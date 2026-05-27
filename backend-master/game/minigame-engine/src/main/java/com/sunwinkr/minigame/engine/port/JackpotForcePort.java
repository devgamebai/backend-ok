package com.sunwinkr.minigame.engine.port;

import java.util.Optional;

/**
 * Engine-side port for the {@code jackpottaixiu} Hazelcast {@code IMap}.
 * Admin writes a {@code short} ({@code 1} = Xỉu, {@code 6} = Tài) to
 * the map; the engine consumes it on the next dice-generation pass
 * (TXR:594).
 *
 * <p>Read is atomic remove (single-use, INV-3 mirror). Per spec §4 the
 * jackpot side is gated by per-side {@code numBet % 5 == 0} —
 * gating lives in {@link com.sunwinkr.minigame.engine.jackpot
 * .JackpotTriggerPolicy}, NOT here.
 *
 * <p>Plan §2.5 row J3 (peek side).
 */
public interface JackpotForcePort {

    /**
     * Atomic read-and-remove on the {@code jackpottaixiu} key. Returns
     * the queued side value if present (typically 1 or 6) — engine code
     * treats any other value as "no override".
     */
    Optional<Short> peekJackpotSide();
}
