package com.sunwinkr.minigame.engine.sicbo;

import com.sunwinkr.minigame.engine.sicbo.eval.SicboWinningStatusEvaluator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * INV-14: For non-triple dice:
 * - total > 10 → result contains "TAI"
 * - total <= 10 → result contains "XIU"
 * (Also verifies mutual exclusivity — cannot have both TAI and XIU.)
 */
public class SicboTaiXiuMappingTest {

    @Test
    public void total11IsTai() {
        // 1+4+6=11 — non-triple, TAI
        assertTai(1, 4, 6);
    }

    @Test
    public void total12IsTai() {
        assertTai(2, 4, 6);
    }

    @Test
    public void total13IsTai() {
        assertTai(3, 4, 6);
    }

    @Test
    public void total14IsTai() {
        assertTai(2, 6, 6);
    }

    @Test
    public void total15IsTai() {
        assertTai(3, 6, 6);
    }

    @Test
    public void total16IsTai() {
        assertTai(4, 6, 6);
    }

    @Test
    public void total17IsTai() {
        assertTai(5, 6, 6);
    }

    @Test
    public void total10IsXiu() {
        // 1+3+6=10 — non-triple, XIU
        assertXiu(1, 3, 6);
    }

    @Test
    public void total9IsXiu() {
        assertXiu(1, 2, 6);
    }

    @Test
    public void total8IsXiu() {
        assertXiu(1, 1, 6);
    }

    @Test
    public void total7IsXiu() {
        assertXiu(1, 2, 4);
    }

    @Test
    public void total6IsXiu() {
        assertXiu(1, 2, 3);
    }

    @Test
    public void total5IsXiu() {
        assertXiu(1, 1, 3);
    }

    @Test
    public void total4IsXiu() {
        // 1+1+2=4 — non-triple, XIU
        assertXiu(1, 1, 2);
    }

    @Test
    public void taiAndXiuMutuallyExclusive() {
        // Test a broad range of non-triple combos
        for (int d1 = 1; d1 <= 6; d1++) {
            for (int d2 = 1; d2 <= 6; d2++) {
                for (int d3 = 1; d3 <= 6; d3++) {
                    if (isTriple(d1, d2, d3)) continue;
                    List<String> statuses = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
                    boolean hasTai = statuses.contains("TAI");
                    boolean hasXiu = statuses.contains("XIU");
                    assertFalse("Cannot have both TAI and XIU for (" + d1 + "," + d2 + "," + d3 + ")",
                            hasTai && hasXiu);
                    assertTrue("Must have exactly one of TAI or XIU for (" + d1 + "," + d2 + "," + d3 + ")",
                            hasTai || hasXiu);
                }
            }
        }
    }

    @Test
    public void tripleHasNeitherTaiNorXiu() {
        for (int n = 1; n <= 6; n++) {
            List<String> statuses = SicboWinningStatusEvaluator.evaluate(n, n, n);
            assertFalse("Triple must not contain TAI", statuses.contains("TAI"));
            assertFalse("Triple must not contain XIU", statuses.contains("XIU"));
        }
    }

    // -----------------------------------------------------------------------

    private static void assertTai(int d1, int d2, int d3) {
        int total = d1 + d2 + d3;
        assertTrue("Expected total > 10 for assertTai, got " + total, total > 10);
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        assertTrue("Expected TAI for total=" + total + " dice=(" + d1 + "," + d2 + "," + d3 + ")",
                statuses.contains("TAI"));
        assertFalse("Must not contain XIU when TAI expected", statuses.contains("XIU"));
    }

    private static void assertXiu(int d1, int d2, int d3) {
        int total = d1 + d2 + d3;
        assertTrue("Expected total <= 10 for assertXiu, got " + total, total <= 10);
        List<String> statuses = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        assertTrue("Expected XIU for total=" + total + " dice=(" + d1 + "," + d2 + "," + d3 + ")",
                statuses.contains("XIU"));
        assertFalse("Must not contain TAI when XIU expected", statuses.contains("TAI"));
    }

    private static boolean isTriple(int d1, int d2, int d3) {
        return d1 == d2 && d2 == d3;
    }
}
