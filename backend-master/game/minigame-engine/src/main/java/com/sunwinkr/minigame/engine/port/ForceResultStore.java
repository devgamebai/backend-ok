package com.sunwinkr.minigame.engine.port;

import java.util.Optional;

/**
 * Engine-side port for the admin force-result Hazelcast {@code IMap}.
 * The Hazelcast map key is {@code "ketquataixiu"} on map
 * {@code "ketquataixiu"} (spec §4); the adapter (PR-4) does the atomic
 * {@code map.remove("ketquataixiu")} so the value is consumed exactly
 * once (INV-3).
 *
 * <p>{@link #set(short[])} mirrors the legacy
 * {@code TaiXiuModule.forceResultTaiXiu} cmd 2003 write path
 * (TXM:258-286); the engine itself is auth-agnostic — role checks live
 * in the API layer (plan §3.6).
 *
 * <p>Plan §2.3 row D2 / spec INV-3.
 */
public interface ForceResultStore {

    /**
     * Atomic read-and-remove. Returns the previously-stored
     * {@code short[3]} if a force-result was queued, else
     * {@link Optional#empty()}.
     *
     * <p>Adapter contract: implementations MUST be safe against
     * concurrent consumers — only one caller observes the value.
     */
    Optional<short[]> peekAndConsume();

    /**
     * Queue a force-result for the next round. Length-3 dice values in
     * {@code [1..6]} — no validation is performed by the engine; the
     * adapter / admin controller is responsible for sanitation.
     */
    void set(short[] dice);
}
