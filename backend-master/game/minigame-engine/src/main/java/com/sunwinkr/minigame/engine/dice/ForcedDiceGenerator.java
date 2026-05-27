package com.sunwinkr.minigame.engine.dice;

/**
 * Force-loop dice generator. Spins {@link RandomDiceGenerator} until
 * the resulting total resolves to the supplied {@code forceSide}.
 *
 * <p>Mirrors {@code GenerationTaiXiu.generateResult(short forceBetSide)}
 * (GTX:71-81). The legacy code rerolls until
 * {@code (total>10 ? 1 : 0) == forceSide}.
 *
 * <p>Loop termination: at uniform random the loop terminates in
 * {@code O(1)} expected iterations because each side has &gt;= 50%
 * probability per roll. No max-iter cap — preserved from legacy.
 *
 * <p>Plan §2.3 row D4.
 */
public final class ForcedDiceGenerator implements DiceGenerator {

    private final RandomDiceGenerator random;
    private final short forceSide; // 0 = Xỉu, 1 = Tài

    /**
     * @param forceSide 0 (Xỉu) or 1 (Tài); other values are accepted but
     *                  will loop forever since {@code total>10 ? 1 : 0}
     *                  can only be 0 or 1
     */
    public ForcedDiceGenerator(short forceSide) {
        this(forceSide, new RandomDiceGenerator());
    }

    /** Test seam: inject a deterministic random source. */
    ForcedDiceGenerator(short forceSide, RandomDiceGenerator random) {
        if (random == null) {
            throw new NullPointerException("random");
        }
        this.forceSide = forceSide;
        this.random = random;
    }

    @Override
    public short[] generate(RoundContext ctx) {
        return forceUntilSide(forceSide);
    }

    /**
     * Roll until the resulting total matches {@code side}. Package-private
     * for direct use by {@link ResultPipeline}.
     */
    short[] forceUntilSide(short side) {
        short[] dice;
        int total;
        int outcome;
        do {
            dice = random.generate(null);
            total = dice[0] + dice[1] + dice[2];
            outcome = total > 10 ? 1 : 0;
        } while (outcome != side);
        return dice;
    }
}
