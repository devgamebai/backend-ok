package com.sunwinkr.lottery.engine.port;

import java.time.LocalDate;

/**
 * Persistence port for the daily settle-complete flag.
 *
 * <p>Implements the audit fix for finding L-1 (pre-settle result reveal,
 * see {@code docs/specs/lottery-anticheat-audit.md §2.2}). The settle
 * loop writes the row, then calls {@link #markSettled} as its LAST step
 * — REST result queries are gated on this flag so today's payload is
 * never visible until the settle is structurally complete.
 *
 * <p>Concrete adapter (JDBC over
 * {@code vinplay_minigame.result_lottery.settled_at}) ships in PR-2.
 *
 * <p>Date keys are always Vietnam wall date — see
 * {@link com.sunwinkr.lottery.engine.clock.LotteryClock#VN}.
 */
public interface SettledFlagStore {

    /**
     * @param vnDate Vietnam-wall date to query
     * @return {@code true} iff {@code result_lottery.settled_at IS NOT NULL}
     *         for {@code vnDate}
     */
    boolean isSettled(LocalDate vnDate);

    /**
     * Mark today's settle complete. MUST be the last write of the
     * settle pipeline — failure ordering matters for audit invariants.
     *
     * <p>Idempotent: calling on an already-settled date is a no-op.
     *
     * @param vnDate Vietnam-wall date to mark
     */
    void markSettled(LocalDate vnDate);
}
