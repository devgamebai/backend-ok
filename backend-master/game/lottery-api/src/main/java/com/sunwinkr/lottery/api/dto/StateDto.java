package com.sunwinkr.lottery.api.dto;

import com.sunwinkr.lottery.engine.clock.LotteryPhase;

/**
 * Wire DTO for {@code GET /api/v2/lottery/xsmb/state}. Plan §5.2.
 *
 * <p>Lottery has no countdown — the state response carries the daily
 * phase ({@link LotteryPhase}) plus whether bets are currently accepted.
 * Lock time + scrape time are static constants surfaced for client UX.
 *
 * <p>SUN-1339 (Phase A4) — provider-style bet contract aliases:
 * <ul>
 *   <li>{@link #safeBetExpiresAt} mirrors {@link #lockTime} (GSC vocabulary)</li>
 *   <li>{@link #settleAt} mirrors {@link #scrapeTime} (GSC vocabulary)</li>
 *   <li>{@link #roundId} — Vietnamese-date numeric {@code yyyymmdd} for this draw day</li>
 * </ul>
 * {@link #lockTime} and {@link #scrapeTime} are kept unchanged for FE backward compat.
 */
public final class StateDto {

    /** {@link LotteryPhase#name()} — DRAW_PENDING / DRAW_LOCKED / SCRAPING / SETTLING / SETTLED. */
    public String phase;

    /** True iff bets accepted right now. */
    public boolean bettingOpen;

    /**
     * NEXT lock moment as Unix epoch milliseconds (UTC). Resolves to today's
     * 18:10 Hanoi if {@code now < 18:10 VN}, otherwise tomorrow's 18:10.
     * FE renders countdown via {@code lockTime - Date.now()}.
     * Kept for FE backward compat — same value as {@link #safeBetExpiresAt}.
     */
    public long lockTime;

    /**
     * Provider-vocab alias for {@link #lockTime}. Epoch-ms at which the
     * bet window hard-closes (18:10 Hanoi). Used by GSC-style integrations.
     * Always equal to {@link #lockTime}.
     */
    public long safeBetExpiresAt;

    /**
     * NEXT scrape moment as Unix epoch milliseconds (UTC). Same semantics as
     * {@link #lockTime} but anchored at 18:35 Hanoi.
     * Kept for FE backward compat — same value as {@link #settleAt}.
     */
    public long scrapeTime;

    /**
     * Provider-vocab alias for {@link #scrapeTime}. Epoch-ms at which the
     * result scrape (and settlement) is expected (18:35 Hanoi).
     * Always equal to {@link #scrapeTime}.
     */
    public long settleAt;

    /** Vietnam-wall date ISO (yyyy-MM-dd). */
    public String vnDate;

    /**
     * Round identifier — Vietnamese date as a {@code yyyymmdd} numeric
     * (e.g. {@code 20260515}). Matches the {@code round_id} column written
     * to {@code vinplay_minigame.lode} on every bet INSERT (SUN-1339 A1 migration).
     */
    public long roundId;

    public StateDto() {
    }
}
