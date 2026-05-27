package com.sunwinkr.minigame.engine.sicbo.dice;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure-random Sicbo dice generator.
 *
 * <p>Mirror of {@code TaiXiuUtil.genarateRandomResult()} — three independent
 * uniform-random face values in [1..6]. Used as:
 * <ul>
 *   <li>Default path when {@code CANCUA_USE_DYNAMIC_RTP} is disabled
 *       (SBR:633-635 / INV-18).</li>
 *   <li>Tiny-pot fallback ({@code totalValueBetUser < 100_000}, SBR:643-646).</li>
 *   <li>Empty bet-snapshot fallback ({@code totalValueBetUser <= 0}, SBR:639-641).</li>
 *   <li>{@code SicboFundProtector}'s retry source on pathological pot states.</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * Uses {@link ThreadLocalRandom} — no shared state, safe to invoke from any
 * thread. Stateless singleton.
 */
public class SicboRandomDiceGenerator implements SicboDiceGenerator {

    /** Shared stateless instance. */
    public static final SicboRandomDiceGenerator INSTANCE = new SicboRandomDiceGenerator();

    public SicboRandomDiceGenerator() {
        // public to allow direct instantiation in tests with a seeded variant
    }

    @Override
    public short[] generate(SicboRound ctx, List<SicboPotEntry> snapshot, long totalValueBetUser) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new short[] {
            (short) (r.nextInt(6) + 1),
            (short) (r.nextInt(6) + 1),
            (short) (r.nextInt(6) + 1)
        };
    }
}
