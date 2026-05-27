package com.sunwinkr.minigame.engine.sicbo;

import com.sunwinkr.minigame.engine.sicbo.eval.SicboWinningStatusEvaluator;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for SicboWinningStatusEvaluator.
 *
 * Uses jqwik to cover all 216 (6^3) ordered dice combinations.
 * Verifies:
 * - Non-null, non-empty result for any valid input.
 * - Pure function: same input always produces the same result list (getWinningStatusesIsPure).
 */
public class SicboWinningStatusPropertyTest {

    /** Arbitrary for a single die face value (1..6). */
    @Provide
    Arbitrary<Integer> dieValue() {
        return Arbitraries.integers().between(1, 6);
    }

    /**
     * For all 216 dice combos, evaluate() produces a non-null List with at least 1 entry.
     */
    @Property
    @Report(Reporting.GENERATED)
    void evaluateAlwaysReturnsNonNullNonEmptyList(
            @ForAll("dieValue") int d1,
            @ForAll("dieValue") int d2,
            @ForAll("dieValue") int d3) {
        List<String> result = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    /**
     * getWinningStatusesIsPure: same input always produces identical result.
     * Calls evaluate() twice with same dice and compares element-by-element.
     */
    @Property
    void evaluateIsPure(
            @ForAll("dieValue") int d1,
            @ForAll("dieValue") int d2,
            @ForAll("dieValue") int d3) {
        List<String> first  = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        List<String> second = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        assertThat(first).isEqualTo(second);
    }

    /**
     * All returned status strings are non-null and non-empty.
     */
    @Property
    void evaluateReturnsNoNullOrEmptyStrings(
            @ForAll("dieValue") int d1,
            @ForAll("dieValue") int d2,
            @ForAll("dieValue") int d3) {
        List<String> result = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
        for (String status : result) {
            assertThat(status).isNotNull().isNotEmpty();
        }
    }

    /**
     * Invalid dice values (0, 7, negative) must throw IllegalArgumentException.
     */
    @Example
    void invalidDiceThrows() {
        assertThrowsIllegalArg(() -> SicboWinningStatusEvaluator.evaluate(0, 1, 1));
        assertThrowsIllegalArg(() -> SicboWinningStatusEvaluator.evaluate(1, 7, 1));
        assertThrowsIllegalArg(() -> SicboWinningStatusEvaluator.evaluate(1, 1, -1));
    }

    private static void assertThrowsIllegalArg(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException but none was thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Exhaustive coverage: manually iterate all 216 combos and verify non-empty.
     * This is a deterministic complement to the property test.
     */
    @Example
    void allTwoHundredSixteenCombosNonEmpty() {
        int count = 0;
        for (int d1 = 1; d1 <= 6; d1++) {
            for (int d2 = 1; d2 <= 6; d2++) {
                for (int d3 = 1; d3 <= 6; d3++) {
                    List<String> result = SicboWinningStatusEvaluator.evaluate(d1, d2, d3);
                    assertThat(result)
                            .as("dice=(%d,%d,%d)", d1, d2, d3)
                            .isNotNull()
                            .isNotEmpty();
                    count++;
                }
            }
        }
        assertThat(count).isEqualTo(216);
    }
}
