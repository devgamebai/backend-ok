package com.sunwinkr.lottery.engine.clock;

/**
 * Lottery-day phase machine. Derived from
 * {@code docs/specs/lottery-anticheat-audit.md §5.9}.
 *
 * <p>Single daily cycle (no realtime round abstraction):
 * <pre>
 *   DRAW_PENDING → DRAW_LOCKED → SCRAPING → SETTLING → SETTLED → (next day) DRAW_PENDING
 * </pre>
 *
 * <p>Bet acceptance and result visibility are derived from the phase via
 * {@link #acceptsBets()} and {@link #resultVisible()} — the dice (raw
 * draw payload) MUST NOT be visible until {@link #SETTLED}.
 */
public enum LotteryPhase {

    /** 00:00 → 18:10 Hanoi. Bets open. Yesterday's settled result visible. */
    DRAW_PENDING,

    /** 18:10 → 18:35 Hanoi. Bets closed. Yesterday's result still visible. */
    DRAW_LOCKED,

    /** Scrape in flight after 18:35 Hanoi. Bets closed. Today censored. */
    SCRAPING,

    /** Result row written ({@code settled_at IS NULL}); settle loop running. */
    SETTLING,

    /** Settle loop complete ({@code settled_at IS NOT NULL}). Today visible. */
    SETTLED;

    /**
     * @return {@code true} iff bets may be accepted in this phase
     */
    public boolean acceptsBets() {
        return this == DRAW_PENDING || this == SETTLED;
    }

    /**
     * @return {@code true} iff today's raw draw payload may be shipped to
     *         clients via REST. False during DRAW_LOCKED, SCRAPING, and
     *         SETTLING — that window is the pre-reveal censorship gate.
     */
    public boolean resultVisible() {
        return this == SETTLED;
    }
}
