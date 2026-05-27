package com.sunwinkr.lottery.engine.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Authoritative clock for XSMB (Lô Đề) lottery operations.
 *
 * <p><b>CRITICAL TZ POLICY.</b> The rest of the sunwinkr stack runs
 * {@code TZ=Asia/Seoul} intentionally (TaiXiu / Sicbo / realtime games are
 * Korean-anchored). The XSMB product is the single exception — it is a
 * Vietnamese draw and MUST follow Hanoi wall clock. This class pins
 * {@link #VN} = {@code Asia/Ho_Chi_Minh} and every lottery decision MUST
 * route through it. Do NOT touch the global {@code .env:62 TZ} value.
 *
 * <p>See {@code docs/specs/lottery-anticheat-audit.md §5.1} and
 * {@code docs/plans/lottery-extraction-plan.md §3} for the reasoning and
 * the audit findings this class fixes (CRITICAL — bet-after-result via
 * {@code LocalTime.now()} JVM-TZ leak).
 *
 * <p>Lock window:
 * <ul>
 *   <li>Bets accepted 00:00 → 18:10 Hanoi</li>
 *   <li>Hard lock at {@link #LOCK_TIME} = 18:10 Hanoi</li>
 *   <li>Post-lock hold of {@link #POST_LOCK_HOLD} = 45 min (until 18:55)</li>
 *   <li>After 18:55 the gate stays locked until today's settlement loop
 *       has marked the day complete ({@code todaySettleComplete=true})</li>
 *   <li>Once today's settle is complete, the gate re-opens for the next
 *       day's bets immediately</li>
 * </ul>
 *
 * <p>{@link #SCRAPE_TIME} = 18:35 Hanoi is reference data for the
 * scheduler — this class does not enforce it directly.
 */
public final class LotteryClock {

    /** Vietnam time zone. Pinned at code level — never read JVM default. */
    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Hard lock at 18:10 Hanoi — 5 min before the public TV draw at ~18:15. */
    public static final LocalTime LOCK_TIME = LocalTime.of(18, 10);

    /** Scheduler fires at 18:35 Hanoi to pull the published result. */
    public static final LocalTime SCRAPE_TIME = LocalTime.of(18, 35);

    /** Unconditional hold after lock — 45 min, ending at 18:55 Hanoi. */
    public static final Duration POST_LOCK_HOLD = Duration.ofMinutes(45);

    private LotteryClock() {
        // utility
    }

    /**
     * @return the configured hard-lock wall time (18:10 Hanoi)
     */
    public static LocalTime lockTime() {
        return LOCK_TIME;
    }

    /**
     * @return the scheduled scrape wall time (18:35 Hanoi)
     */
    public static LocalTime scrapeTime() {
        return SCRAPE_TIME;
    }

    /**
     * Server-authoritative bet-window check. Always reads
     * {@link #VN} regardless of the supplied {@link Clock}'s zone or the
     * JVM default zone — this is the line that defeats the SUN-LOTTERY-TZ
     * audit finding.
     *
     * @param clock                source of "now" — production passes
     *                             {@code Clock.systemUTC()}; tests pass
     *                             {@code Clock.fixed(...)} for determinism
     * @param todaySettleComplete  whether today's settlement loop has run
     *                             to completion (i.e.
     *                             {@code result_lottery.settled_at IS NOT NULL}
     *                             for today's Hanoi date)
     * @return {@code true} iff bets may be accepted right now
     */
    public static boolean isBettingOpen(Clock clock, boolean todaySettleComplete) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(VN));
        LocalTime t = now.toLocalTime();
        LocalTime postLockEnd = LOCK_TIME.plus(POST_LOCK_HOLD);

        // Before 18:10 Hanoi — always open.
        if (t.isBefore(LOCK_TIME)) {
            return true;
        }
        // 18:10 (inclusive) → 18:55 Hanoi — unconditional hold.
        if (t.isBefore(postLockEnd)) {
            return false;
        }
        // After 18:55 Hanoi — gated on settle completion for today.
        return todaySettleComplete;
    }
}
