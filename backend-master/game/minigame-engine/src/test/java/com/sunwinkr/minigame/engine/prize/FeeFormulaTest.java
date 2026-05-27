package com.sunwinkr.minigame.engine.prize;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec §5 / S3: fee = tax * prize / (200 - tax). */
class FeeFormulaTest {

    @Test
    void fivePctTaxOn100k() {
        // tax=5%, prize=100_000 → fee = 5 * 100_000 / 195 = 2564.
        assertThat(FeeCalc.fee(100_000L, 5.0f)).isEqualTo(2_564L);
    }

    @Test
    void zeroPrizeYieldsZeroFee() {
        assertThat(FeeCalc.fee(0L, 5.0f)).isEqualTo(0L);
        assertThat(FeeCalc.fee(-1L, 5.0f)).isEqualTo(0L);
    }

    @Test
    void zeroTaxYieldsZeroFee() {
        assertThat(FeeCalc.fee(1_000_000L, 0.0f)).isEqualTo(0L);
    }
}
