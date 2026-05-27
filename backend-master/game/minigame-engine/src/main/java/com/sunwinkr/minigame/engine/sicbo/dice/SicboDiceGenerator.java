package com.sunwinkr.minigame.engine.sicbo.dice;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.core.SicboRound;

import java.util.List;

/**
 * Engine-facing strategy for producing a Sicbo dice triple at result-generation
 * time (SBR:598-630 — {@code MGRoomSicbo.getResult}).
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link SicboRandomDiceGenerator} — pure random via {@code ThreadLocalRandom}
 *       (legacy {@code TaiXiuUtil.genarateRandomResult}).</li>
 *   <li>{@link SicboRtpDiceGenerator} — full port of
 *       {@code MGRoomSicbo.generateResultWithHouseEdge} (SBR:632-676). Brute
 *       forces all 6×6×6=216 ordered dice triples to pick the combination
 *       whose house profit is closest to the RTP target.</li>
 * </ul>
 *
 * <h3>Contract</h3>
 * Returns a non-null {@code short[]} of length 3 with each element in [1..6].
 * Pure-functional: same inputs MUST produce the same probability distribution
 * (deterministic up to the RNG source).
 */
public interface SicboDiceGenerator {

    /**
     * Produce three dice values (each 1..6) for the given round and bet snapshot.
     *
     * @param ctx                 current round (for refId / phase context)
     * @param snapshot            immutable bet snapshot captured at result-time
     *                            (see {@code SicboPotState.listUserBet()})
     * @param totalValueBetUser   sum of real-user (non-bot) wagered value
     *                            (drives the RTP balancer's target-edge calc)
     * @return three-element array of dice values, each in [1..6]
     */
    short[] generate(SicboRound ctx, List<SicboPotEntry> snapshot, long totalValueBetUser);
}
