package com.sunwinkr.minigame.engine.sicbo.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure-function evaluator for Sicbo winning bet types given three dice values.
 *
 * <p>Direct behavior-preserving port of
 * {@code MGRoomSicbo.getWinningStatuses(int[])} (SBR:1090-1144).
 * No state; thread-safe; idempotent for the same input (getWinningStatusesIsPure
 * invariant from spec §8.2).
 *
 * <h3>Algorithm summary (SBR:1090-1144)</h3>
 * <ol>
 *   <li>Build per-face count array {@code diceCounts[1..6]}.</li>
 *   <li>Check for triple (any count ≥ 3):
 *       <ul>
 *         <li>Add {@code TRIPLE_DICES_n} + {@code ANY_TRIPLE_DICES}.</li>
 *         <li>Set {@code storm=true} → suppress TAI/XIU/CHAN/LE/DOUBLE_DICES (INV-15).</li>
 *       </ul>
 *   </li>
 *   <li>If NOT storm: add TAI or XIU, CHAN or LE, and DOUBLE_DICES_x_y for
 *       any pair of distinct faces both present.</li>
 *   <li>Total == 3 → add {@code TRIPLE_DICES_1}; early return (no POINT/ONE_DICE).</li>
 *   <li>Total == 18 → add {@code TRIPLE_DICES_6}; early return (no POINT/ONE_DICE).</li>
 *   <li>Otherwise → add {@code POINT_<total>} + {@code ONE_DICE_n} for each face present.</li>
 * </ol>
 *
 * <h3>Edge-case notes</h3>
 * <ul>
 *   <li>Total 3 can only arise from dice (1,1,1) — that IS a triple, so {@code storm=true}
 *       before the {@code totalValue==3} check. The check adds {@code TRIPLE_DICES_1} (which
 *       is already present) and early-returns, skipping POINT/ONE_DICE. This matches SBR:1130.</li>
 *   <li>Total 18 likewise (6,6,6) — same reasoning for {@code TRIPLE_DICES_6}.</li>
 *   <li>DOUBLE_DICES_x_y requires BOTH faces present (diceCounts[i] ≥ 1 AND diceCounts[j] ≥ 1),
 *       not just a literal pair on two dice. With a triple, all three counts for the same face
 *       are ≥ 3, but storm suppresses the DOUBLE path anyway.</li>
 * </ul>
 */
public final class SicboWinningStatusEvaluator {

    private SicboWinningStatusEvaluator() {
        // static utility class — do not instantiate
    }

    /**
     * Returns the list of winning bet-type name strings for the given dice roll.
     *
     * @param d1 first die value  (1..6)
     * @param d2 second die value (1..6)
     * @param d3 third die value  (1..6)
     * @return non-null, non-empty list of bet-type names (e.g. "TAI", "POINT_11")
     * @throws IllegalArgumentException if any die is outside [1..6]
     */
    public static List<String> evaluate(int d1, int d2, int d3) {
        if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6 || d3 < 1 || d3 > 6) {
            throw new IllegalArgumentException(
                "Dice values must be in [1..6], got: " + d1 + "," + d2 + "," + d3);
        }

        // Port of SBR:1092-1098: build diceCounts array indexed 0..6 (index 0 unused)
        int[] diceCounts = new int[7];
        int[] diceValues = new int[]{d1, d2, d3};
        for (int value : diceValues) {
            diceCounts[value]++;
        }

        ArrayList<String> winningStatuses = new ArrayList<String>();
        int totalValue = d1 + d2 + d3;
        boolean storm = false;

        // SBR:1103-1108: check for triple — adds TRIPLE_DICES_n + ANY_TRIPLE_DICES
        for (int i = 1; i <= 6; i++) {
            if (diceCounts[i] < 3) {
                continue;
            }
            winningStatuses.add("TRIPLE_DICES_" + i);
            storm = true;
            winningStatuses.add("ANY_TRIPLE_DICES");
        }

        // SBR:1109-1128: non-triple branch — TAI/XIU, CHAN/LE, DOUBLE_DICES
        if (!storm) {
            // INV-14: total > 10 → TAI; total <= 10 → XIU
            // Note: totals 3 and 18 are triples so storm=true above; this branch
            // only sees totals 4..17 (minus 3 and 18 which can't be non-triple).
            if (totalValue >= 11 && totalValue <= 17) {
                winningStatuses.add("TAI");
            }
            if (totalValue >= 4 && totalValue <= 10) {
                winningStatuses.add("XIU");
            }

            // CHAN (even) or LE (odd)
            if (totalValue % 2 == 0) {
                winningStatuses.add("CHAN");
            } else {
                winningStatuses.add("LE");
            }

            // DOUBLE_DICES_x_y: both face x and face y present (INV-15: suppressed by storm)
            for (int i = 1; i <= 6; i++) {
                for (int j = i + 1; j <= 6; j++) {
                    if (diceCounts[i] < 1 || diceCounts[j] < 1) {
                        continue;
                    }
                    int smaller = Math.min(i, j);
                    int bigger = Math.max(i, j);
                    winningStatuses.add("DOUBLE_DICES_" + smaller + "_" + bigger);
                }
            }
        }

        // SBR:1130-1137: total==3 special case (always a triple (1,1,1))
        // TRIPLE_DICES_1 already added above; this adds it again — behavior-preserving.
        // Early return skips POINT_* and ONE_DICE_* (both suppressed at total 3 and 18).
        if (totalValue == 3) {
            winningStatuses.add("TRIPLE_DICES_1");
            return winningStatuses;
        }

        // SBR:1134-1137: total==18 special case (always a triple (6,6,6))
        if (totalValue == 18) {
            winningStatuses.add("TRIPLE_DICES_6");
            return winningStatuses;
        }

        // SBR:1138-1143: POINT_n + ONE_DICE_n per face present.
        // Guarded by !storm: triples like (2,2,2), (3,3,3), (4,4,4), (5,5,5)
        // have totals 6, 9, 12, 15 which don't hit the total==3/18 early-returns
        // above, so we must explicitly skip POINT/ONE_DICE when storm=true
        // (INV-15). Only non-triple totals 4..17 reach this block.
        if (!storm) {
            winningStatuses.add("POINT_" + totalValue);
            for (int i = 1; i <= 6; i++) {
                if (diceCounts[i] < 1) {
                    continue;
                }
                winningStatuses.add("ONE_DICE_" + i);
            }
        }

        return winningStatuses;
    }
}
