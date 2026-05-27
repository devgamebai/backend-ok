package com.sunwinkr.lottery.engine.clock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LotteryPhaseTest {

    @Test
    void diceVisibleOnlyInSettled() {
        // Phase transition matrix per audit §5.9 — today's draw payload
        // (the "dice" by analogy with TaiXiu) is visible only after the
        // settle loop has completed. Pre-SETTLED phases MUST hide it.
        assertThat(LotteryPhase.DRAW_PENDING.resultVisible()).isFalse();
        assertThat(LotteryPhase.DRAW_LOCKED.resultVisible()).isFalse();
        assertThat(LotteryPhase.SCRAPING.resultVisible()).isFalse();
        assertThat(LotteryPhase.SETTLING.resultVisible()).isFalse();
        assertThat(LotteryPhase.SETTLED.resultVisible()).isTrue();

        // Bet acceptance: open only in DRAW_PENDING (today) and SETTLED (next day).
        assertThat(LotteryPhase.DRAW_PENDING.acceptsBets()).isTrue();
        assertThat(LotteryPhase.DRAW_LOCKED.acceptsBets()).isFalse();
        assertThat(LotteryPhase.SCRAPING.acceptsBets()).isFalse();
        assertThat(LotteryPhase.SETTLING.acceptsBets()).isFalse();
        assertThat(LotteryPhase.SETTLED.acceptsBets()).isTrue();
    }
}
