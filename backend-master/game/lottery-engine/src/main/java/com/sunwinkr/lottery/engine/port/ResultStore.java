package com.sunwinkr.lottery.engine.port;

import com.sunwinkr.lottery.engine.model.LotteryResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@code vinplay_minigame.result_lottery} (per-day
 * XSMB draw payload).
 *
 * <p>{@link #findByDate} is the dedupe entry point — the
 * {@link com.sunwinkr.lottery.engine.ingest.DrawIngest} loop calls it
 * before scraping a second time on the same Vietnam-wall date and
 * short-circuits on hit (matches legacy
 * {@code LoDeServiceImpl.getLatestResult} behaviour).
 *
 * <p>{@link #save} writes a fresh draw with {@code settled_at=NULL} —
 * the settle loop flips {@code settled_at} via
 * {@link SettledFlagStore#markSettled} as its LAST step. Closes audit
 * finding L-1 (pre-settle result reveal).
 *
 * <p>{@link #listSettled} is the REST history backing — it MUST filter
 * {@code WHERE settled_at IS NOT NULL} so today's payload is invisible
 * until settle is structurally complete. Closes
 * {@code docs/plans/lottery-extraction-plan.md §2.6 H2}.
 */
public interface ResultStore {

    /**
     * Look up an existing draw for {@code vnDate}. Returns
     * {@link Optional#empty()} on miss — DrawIngest treats that as
     * "scrape needed". Used by both the ingest dedupe and the settle
     * loop's draw read-back.
     *
     * @param vnDate Vietnam-wall draw date
     * @return parsed draw if persisted, else empty
     */
    Optional<LotteryResult> findByDate(LocalDate vnDate);

    /**
     * Persist a freshly scraped draw with {@code settled_at=NULL}.
     * Idempotent at the row level — {@link DrawIngest} guards via
     * {@link #findByDate} first.
     *
     * @param rawJson Gson-serialised payload (preserves the literal
     *                {@code ĐB} Unicode field name)
     * @param vnDate  Vietnam-wall draw date
     */
    void save(String rawJson, LocalDate vnDate);

    /**
     * History endpoint backing — return all draws in the date range
     * whose {@code settled_at IS NOT NULL}. Pre-settle rows are
     * invisible to REST consumers per audit L-1.
     */
    List<LotteryResult> listSettled(LocalDate from, LocalDate to);
}
