package com.sunwinkr.minigame.engine.sicbo.prize;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import com.sunwinkr.minigame.engine.sicbo.bet.SicboPotEntry;
import com.sunwinkr.minigame.engine.sicbo.eval.SicboWinningStatusEvaluator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-function calculator for the total payout that a given dice triple would
 * cost the house against a given snapshot of real-user bets.
 *
 * <p>Direct behavior-preserving port of {@code MGRoomSicbo.sotienphaitra(int,
 * int, int)} (SBR:1040-1062). Used by:
 * <ul>
 *   <li>{@code SicboRtpDiceGenerator} (216-combo brute-force loop, SBR:655)</li>
 *   <li>{@code SicboFundProtector} (retry loop, SBR:614-617)</li>
 *   <li>{@code SicboPrizeCalculator} (final settlement payout sum)</li>
 * </ul>
 *
 * <h3>Real-user filter (SBR:1046)</h3>
 * Iterates the snapshot and skips any entry with {@code userId <= 0} —
 * matches the legacy {@code if (tx.userId <= 0 || ...) continue;} check.
 * Bot bets do NOT contribute to the payout total because bots are not
 * paid out in cash (their balances are virtual within the room).
 *
 * <h3>ONE_DICE_* special payout (INV-9, SBR:1047-1057)</h3>
 * For bet sides {@code ONE_DICE_1..ONE_DICE_6} (IDs 15..20), the payout
 * is occurrence-count based on the rolled dice — NOT the stored rotation:
 * <ul>
 *   <li>1 occurrence  → {@code bet * 2}</li>
 *   <li>2 occurrences → {@code bet * 3}</li>
 *   <li>3 occurrences → {@code bet * 4}</li>
 * </ul>
 * The "matching face" is derived as {@code betSideId - 14} (so ONE_DICE_1
 * → face 1, ONE_DICE_6 → face 6), exactly as the legacy code does.
 *
 * <h3>Triple suppression (INV-15)</h3>
 * Handled by {@link SicboWinningStatusEvaluator}: on any triple, only the
 * matching {@code TRIPLE_DICES_n} and {@code ANY_TRIPLE_DICES} appear in
 * the winning-statuses set. All other bet types skip the {@code contains}
 * check and pay 0 — see {@link SicboWinningStatusEvaluator} for details.
 */
public class SicboPayoutCalculator {

    /** Lowest ONE_DICE_* id (ONE_DICE_1 = 15). */
    private static final int ONE_DICE_MIN_ID = 15;
    /** Highest ONE_DICE_* id (ONE_DICE_6 = 20). */
    private static final int ONE_DICE_MAX_ID = 20;
    /** Offset applied to ONE_DICE_n id to recover the matching face value n. */
    private static final int ONE_DICE_FACE_OFFSET = 14;

    /**
     * Compute the total amount the house would pay out across all real-user
     * bets in {@code bets} if the dice rolled is {@code dice}.
     *
     * @param bets immutable snapshot of accepted bets for the round
     * @param dice three-element array {@code [d1, d2, d3]} with each in [1..6]
     * @return non-negative total payout in money units; 0 if no real users won
     * @throws NullPointerException if {@code bets} or {@code dice} is null
     * @throws IllegalArgumentException if {@code dice} length != 3 or out of range
     */
    public long calculatePotentialPayout(List<SicboPotEntry> bets, short[] dice) {
        if (bets == null) {
            throw new NullPointerException("bets");
        }
        if (dice == null) {
            throw new NullPointerException("dice");
        }
        if (dice.length != 3) {
            throw new IllegalArgumentException("dice must be length 3, got " + dice.length);
        }

        // Pre-compute the winning-statuses set once and the dice values once,
        // then loop over bets. SicboWinningStatusEvaluator returns a List but
        // membership tests are O(1) via a HashSet.
        List<String> winners = SicboWinningStatusEvaluator.evaluate(dice[0], dice[1], dice[2]);
        Set<String> winnersSet = new HashSet<>(winners);
        int[] diceValues = new int[] { dice[0], dice[1], dice[2] };

        long total = 0L;
        for (SicboPotEntry tx : bets) {
            // SBR:1046 — skip bot/virtual rows (userId <= 0) before the contains check.
            if (tx.userId <= 0) {
                continue;
            }
            SicboBetType betTx;
            try {
                betTx = SicboBetType.byId(tx.betSideId);
            } catch (IllegalArgumentException e) {
                // Unknown bet side — defensive skip (legacy throws NPE here, but
                // the engine refuses to crash a whole round on one bad row).
                continue;
            }
            if (!winnersSet.contains(betTx.getName())) {
                continue;
            }
            // SBR:1047-1057 — ONE_DICE_* occurrence-count special.
            if (tx.betSideId >= ONE_DICE_MIN_ID && tx.betSideId <= ONE_DICE_MAX_ID) {
                int face = tx.betSideId - ONE_DICE_FACE_OFFSET;
                int occurrences = countOccurrences(diceValues, face);
                if (occurrences == 2) {
                    total += tx.betValue * 3L;
                } else if (occurrences == 3) {
                    total += tx.betValue * 4L;
                } else {
                    total += tx.betValue * 2L;
                }
            } else {
                // SBR:1059 — non-ONE_DICE_* path uses the stored rotation.
                total += tx.betValue * (long) betTx.getRotation();
            }
        }
        return total;
    }

    /**
     * Convenience for tests / callers without a snapshot — returns
     * an empty {@code winningStatuses} when no real users bet.
     * Mirrors SBR:1042 which builds the set unconditionally.
     */
    public Set<String> winningStatusesFor(short[] dice) {
        if (dice == null || dice.length != 3) {
            return Collections.emptySet();
        }
        return new HashSet<>(SicboWinningStatusEvaluator.evaluate(dice[0], dice[1], dice[2]));
    }

    /** Count occurrences of {@code target} in {@code values} (SBR:1121-1128). */
    static int countOccurrences(int[] values, int target) {
        int count = 0;
        for (int v : values) {
            if (v == target) {
                count++;
            }
        }
        return count;
    }
}
