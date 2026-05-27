package com.sunwinkr.minigame.engine.dice;

/**
 * Engine-side dice generator port. Implementations produce a length-3
 * {@code short[]} of values in {@code [1..6]}.
 *
 * <p>Mirrors the legacy {@code GenerationTaiXiu.generateDices} /
 * {@code TaiXiuUtil.genarateResult} entry points without coupling to
 * BitZero or static helpers.
 *
 * <p>Plan §2.3 row D4.
 */
public interface DiceGenerator {

    /**
     * Produce three dice values. Total over the three dice determines
     * the round result (sum &gt; 10 → Tài, else → Xỉu — TXM:479).
     *
     * @param ctx generation context (pot totals, tax, userId). May be
     *            {@code null} for stateless generators (e.g.
     *            {@link RandomDiceGenerator}).
     * @return three dice values in {@code [1..6]}; never {@code null}
     */
    short[] generate(RoundContext ctx);
}
