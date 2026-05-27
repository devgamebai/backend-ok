package com.sunwinkr.minigame.engine.dice;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stateless uniform-random dice generator. Mirrors
 * {@code GenerationTaiXiu.generateDices} (GTX:175-184): three calls to
 * {@code ThreadLocalRandom.nextInt(6) + 1}.
 *
 * <p>No {@code SecureRandom}, no seeding — matches the legacy behavior
 * exactly. Tests inject a deterministic generator when needed.
 *
 * <p>Plan §2.3 row D4. Spec §4 random source.
 */
public final class RandomDiceGenerator implements DiceGenerator {

    @Override
    public short[] generate(RoundContext ctx) {
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        short[] dice = new short[3];
        dice[0] = (short) (rd.nextInt(6) + 1);
        dice[1] = (short) (rd.nextInt(6) + 1);
        dice[2] = (short) (rd.nextInt(6) + 1);
        return dice;
    }
}
