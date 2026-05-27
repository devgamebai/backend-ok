package com.sunwinkr.minigame.engine.sicbo;

import com.sunwinkr.minigame.engine.sicbo.eval.SicboWinningStatusEvaluator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies triple-roll behavior for SicboWinningStatusEvaluator (INV-15).
 *
 * <h3>Behavioral quirk — production SBR:1090-1144</h3>
 * The original {@code getWinningStatuses} only early-returns on total==3 (1,1,1)
 * and total==18 (6,6,6). For all other triples (2,2,2)..(5,5,5) the code falls
 * through to add {@code POINT_n} and {@code ONE_DICE_n} after setting storm=true.
 * This is behavior-preserving — the evaluator replicates this exactly.
 *
 * <p>Concretely:
 * <ul>
 *   <li>(1,1,1) total=3: [TRIPLE_DICES_1, ANY_TRIPLE_DICES, TRIPLE_DICES_1] — early-return,
 *       no POINT/TAI/XIU/CHAN/LE/DOUBLE. storm=true suppresses those.</li>
 *   <li>(2,2,2) total=6: [TRIPLE_DICES_2, ANY_TRIPLE_DICES, POINT_6, ONE_DICE_2] — falls
 *       through to POINT+ONE_DICE section. storm=true suppresses TAI/XIU/CHAN/LE/DOUBLE only.</li>
 *   <li>(6,6,6) total=18: [TRIPLE_DICES_6, ANY_TRIPLE_DICES, TRIPLE_DICES_6] — early-return,
 *       no POINT/TAI/XIU/CHAN/LE/DOUBLE.</li>
 * </ul>
 *
 * INV-15 as stated in the spec ("triple suppresses POINT/CHAN/LE/DOUBLE") is therefore
 * PARTIALLY correct: storm suppresses TAI/XIU/CHAN/LE/DOUBLE for ALL triples, but
 * POINT_* and ONE_DICE_* are only suppressed for (1,1,1) and (6,6,6) via early-return.
 * This is a known legacy quirk (TODO SUN-xxxx: fix POINT/ONE_DICE suppression for all triples).
 * The evaluator replicates the production behavior exactly.
 */
public class SicboTripleSuppressionTest {

    // -----------------------------------------------------------------------
    // TAI/XIU/CHAN/LE/DOUBLE suppressed for ALL triples (storm=true path)

    @Test
    public void tripleOneHasTaiXiuChanLeDoublesSuppressed() {
        assertTaiXiuChanLeDoubleSuppressed(1, 1, 1);
    }

    @Test
    public void tripleTwoHasTaiXiuChanLeDoublesSuppressed() {
        assertTaiXiuChanLeDoubleSuppressed(2, 2, 2);
    }

    @Test
    public void tripleThreeHasTaiXiuChanLeDoublesSuppressed() {
        assertTaiXiuChanLeDoubleSuppressed(3, 3, 3);
    }

    @Test
    public void tripleFourHasTaiXiuChanLeDoublesSuppressed() {
        assertTaiXiuChanLeDoubleSuppressed(4, 4, 4);
    }

    @Test
    public void tripleFiveHasTaiXiuChanLeDoublesSuppressed() {
        assertTaiXiuChanLeDoubleSuppressed(5, 5, 5);
    }

    @Test
    public void tripleSixHasTaiXiuChanLeDoublesSuppressed() {
        assertTaiXiuChanLeDoubleSuppressed(6, 6, 6);
    }

    // -----------------------------------------------------------------------
    // All triples must contain TRIPLE_DICES_n + ANY_TRIPLE_DICES

    @Test
    public void allTriplesContainTripleDicesNAndAnyTriple() {
        for (int n = 1; n <= 6; n++) {
            List<String> statuses = SicboWinningStatusEvaluator.evaluate(n, n, n);
            assertTrue("Must contain TRIPLE_DICES_" + n + " for triple " + n,
                    statuses.contains("TRIPLE_DICES_" + n));
            assertTrue("Must contain ANY_TRIPLE_DICES for triple " + n,
                    statuses.contains("ANY_TRIPLE_DICES"));
        }
    }

    // -----------------------------------------------------------------------
    // (1,1,1) total=3 — early-return path: no POINT, no ONE_DICE (INV-15 full suppression)

    @Test
    public void tripleOneNoPointOrOneDice() {
        // (1,1,1) total=3 → early return at SBR:1132, no POINT_3 or ONE_DICE_1
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(1, 1, 1);
        for (String s : statuses) {
            assertFalse("POINT_* must not appear on (1,1,1): " + s, s.startsWith("POINT_"));
            assertFalse("ONE_DICE_* must not appear on (1,1,1): " + s, s.startsWith("ONE_DICE_"));
        }
        // The early-return path adds TRIPLE_DICES_1 a second time — list length is 3
        assertEquals("(1,1,1) must have exactly 3 entries [TRIPLE_DICES_1, ANY_TRIPLE_DICES, TRIPLE_DICES_1]",
                3, statuses.size());
    }

    // -----------------------------------------------------------------------
    // (6,6,6) total=18 — early-return path: no POINT, no ONE_DICE (INV-15 full suppression)

    @Test
    public void tripleSixNoPointOrOneDice() {
        // (6,6,6) total=18 → early return at SBR:1136, no POINT_18 or ONE_DICE_6
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(6, 6, 6);
        for (String s : statuses) {
            assertFalse("POINT_* must not appear on (6,6,6): " + s, s.startsWith("POINT_"));
            assertFalse("ONE_DICE_* must not appear on (6,6,6): " + s, s.startsWith("ONE_DICE_"));
        }
        // The early-return path adds TRIPLE_DICES_6 a second time — list length is 3
        assertEquals("(6,6,6) must have exactly 3 entries [TRIPLE_DICES_6, ANY_TRIPLE_DICES, TRIPLE_DICES_6]",
                3, statuses.size());
    }

    // -----------------------------------------------------------------------
    // (2,2,2)..(5,5,5) — storm=true but no early-return: POINT_n + ONE_DICE_n present
    // TODO(SUN-xxxx): legacy quirk — POINT/ONE_DICE not suppressed for mid-range triples.
    // Fix in dedicated hardening MR after extraction lands.

    @Test
    public void tripleTwoHasPointAndOneDice() {
        // (2,2,2) total=6 → storm=true (no TAI/XIU/CHAN/LE/DOUBLE) but falls through to POINT+ONE_DICE
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(2, 2, 2);
        assertTrue("(2,2,2) must contain POINT_6 (legacy quirk SBR:1138)",
                statuses.contains("POINT_6"));
        assertTrue("(2,2,2) must contain ONE_DICE_2 (legacy quirk SBR:1141)",
                statuses.contains("ONE_DICE_2"));
    }

    @Test
    public void tripleThreeHasPointAndOneDice() {
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(3, 3, 3);
        assertTrue("(3,3,3) must contain POINT_9", statuses.contains("POINT_9"));
        assertTrue("(3,3,3) must contain ONE_DICE_3", statuses.contains("ONE_DICE_3"));
    }

    @Test
    public void tripleFourHasPointAndOneDice() {
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(4, 4, 4);
        assertTrue("(4,4,4) must contain POINT_12", statuses.contains("POINT_12"));
        assertTrue("(4,4,4) must contain ONE_DICE_4", statuses.contains("ONE_DICE_4"));
    }

    @Test
    public void tripleFiveHasPointAndOneDice() {
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(5, 5, 5);
        assertTrue("(5,5,5) must contain POINT_15", statuses.contains("POINT_15"));
        assertTrue("(5,5,5) must contain ONE_DICE_5", statuses.contains("ONE_DICE_5"));
    }

    // -----------------------------------------------------------------------

    /**
     * Asserts TAI, XIU, CHAN, LE, and DOUBLE_DICES_* do NOT appear in triple result.
     * These are suppressed by storm=true for ALL triples (1,1,1)..(6,6,6).
     */
    private static void assertTaiXiuChanLeDoubleSuppressed(int d1, int d2, int d3) {
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        assertNotNull("Result must not be null", statuses);
        assertFalse("Result must not be empty", statuses.isEmpty());

        for (String s : statuses) {
            assertFalse("TAI must not appear on triple (" + d1 + "," + d2 + "," + d3 + ")",
                    s.equals("TAI"));
            assertFalse("XIU must not appear on triple (" + d1 + "," + d2 + "," + d3 + ")",
                    s.equals("XIU"));
            assertFalse("CHAN must not appear on triple (" + d1 + "," + d2 + "," + d3 + ")",
                    s.equals("CHAN"));
            assertFalse("LE must not appear on triple (" + d1 + "," + d2 + "," + d3 + ")",
                    s.equals("LE"));
            assertFalse("DOUBLE_DICES_* must not appear on triple (" + d1 + "," + d2 + "," + d3 + "): " + s,
                    s.startsWith("DOUBLE_DICES_"));
        }
    }
}
