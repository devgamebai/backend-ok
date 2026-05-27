package com.sunwinkr.minigame.engine.dice;

import com.sunwinkr.minigame.engine.jackpot.JackpotTriggerPolicy;
import com.sunwinkr.minigame.engine.port.ForceResultStore;

import java.util.Optional;

/**
 * Orchestrates dice generation per the legacy {@code getResult(id)}
 * (TXR:550-621) ordering:
 *
 * <ol>
 *   <li>{@link ForceResultStore#peekAndConsume()} — admin force-result
 *       always wins (TXR:579-582).</li>
 *   <li>{@link HouseEdgeDiceGenerator} — RTP balancer (TXR:587-592).</li>
 *   <li>{@link JackpotTriggerPolicy#apply} — jackpot side override iff the
 *       per-side {@code %5} gate passes (TXR:594-615).</li>
 * </ol>
 *
 * Per plan §3.4: the returned dice are written by the caller into
 * {@code TaiXiuRound.pendingDice} during the {@code GENERATING} phase
 * and only published into {@code revealedDice} on entry to
 * {@code REVEALED}. {@link ResultPipeline} does NOT broadcast.
 *
 * <p>Plan §2.3 row D1.
 */
public final class ResultPipeline {

    private final ForceResultStore forceStore;
    private final DiceGenerator base;
    private final JackpotTriggerPolicy jackpotPolicy;

    /**
     * @param forceStore     adapter for the {@code ketquataixiu} HZ map; may
     *                       be {@code null} (treated as always-empty)
     * @param base           main generator — typically {@link HouseEdgeDiceGenerator}
     * @param jackpotPolicy  jackpot override policy; may be {@code null}
     *                       (no jackpot)
     */
    public ResultPipeline(ForceResultStore forceStore,
                          DiceGenerator base,
                          JackpotTriggerPolicy jackpotPolicy) {
        if (base == null) {
            throw new NullPointerException("base");
        }
        this.forceStore = forceStore;
        this.base = base;
        this.jackpotPolicy = jackpotPolicy;
    }

    /**
     * Generate dice for the supplied round context. Returns a fresh
     * 3-element {@code short[]} — callers should treat the result as
     * immutable.
     *
     * @return new {@code short[3]}; never {@code null}
     */
    public short[] generate(RoundContext ctx,
                            long potTaiNumBet,
                            long potXiuNumBet) {
        // Step 1: admin force.
        if (forceStore != null) {
            Optional<short[]> forced = forceStore.peekAndConsume();
            if (forced.isPresent() && forced.get() != null && forced.get().length >= 3) {
                short[] f = forced.get();
                short[] dice = new short[] { f[0], f[1], f[2] };
                return maybeApplyJackpot(dice, potTaiNumBet, potXiuNumBet);
            }
        }
        // Step 2: house edge / random.
        short[] dice = base.generate(ctx);
        if (dice == null || dice.length < 3) {
            throw new IllegalStateException(
                "DiceGenerator returned invalid dice: "
                    + (dice == null ? "null" : "length=" + dice.length));
        }
        // Step 3: jackpot override.
        return maybeApplyJackpot(dice, potTaiNumBet, potXiuNumBet);
    }

    private short[] maybeApplyJackpot(short[] dice,
                                       long potTaiNumBet,
                                       long potXiuNumBet) {
        if (jackpotPolicy == null) {
            return dice;
        }
        return jackpotPolicy.apply(dice, potTaiNumBet, potXiuNumBet);
    }
}
