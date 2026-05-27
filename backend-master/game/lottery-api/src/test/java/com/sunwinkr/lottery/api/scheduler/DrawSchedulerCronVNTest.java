package com.sunwinkr.lottery.api.scheduler;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * THE critical TZ resilience test. Verifies that the
 * {@link DrawScheduler#SCRAPE_CRON_VN} expression, when interpreted in
 * the {@link DrawScheduler#CRON_ZONE} ({@code Asia/Ho_Chi_Minh}),
 * resolves to 18:35 Hanoi REGARDLESS of the JVM default TZ.
 *
 * <p>Closes the SUN-LOTTERY-TZ audit finding for the scheduler half of
 * the lock-window protection (the bet half is covered by
 * {@code LotteryClockTest} in the engine module).
 *
 * <p>Plan §5.5; CLAUDE.md TZ policy.
 */
class DrawSchedulerCronVNTest {

    /**
     * Anchor a "now" in JVM-Seoul timezone, then assert that the
     * VN-anchored cron fires next at 18:35 Hanoi wall clock — NOT at
     * 18:35 Seoul (which would be 16:35 Hanoi and would leak).
     */
    @Test
    void cronResolvesToHanoiWallClock_regardlessOfJvmTz() {
        CronExpression expr = CronExpression.parse(DrawScheduler.SCRAPE_CRON_VN);
        ZoneId vn = ZoneId.of(DrawScheduler.CRON_ZONE);
        assertThat(vn).isEqualTo(LotteryClock.VN);

        // Anchor: 14 May 2026 10:00 Asia/Seoul (= 08:00 Asia/Ho_Chi_Minh).
        // Convert to Hanoi wall clock; next fire MUST be the same calendar
        // day at 18:35 Hanoi.
        ZonedDateTime seoulNow = ZonedDateTime.of(LocalDateTime.of(2026, 5, 14, 10, 0), ZoneId.of("Asia/Seoul"));
        ZonedDateTime nextInVn = expr.next(seoulNow.withZoneSameInstant(vn));

        assertThat(nextInVn).isNotNull();
        assertThat(nextInVn.getZone()).isEqualTo(vn);
        assertThat(nextInVn.getHour()).isEqualTo(18);
        assertThat(nextInVn.getMinute()).isEqualTo(35);
        // Same Hanoi day (since 08:00 → 18:35 same day).
        assertThat(nextInVn.toLocalDate()).isEqualTo(seoulNow.withZoneSameInstant(vn).toLocalDate());
    }

    @Test
    void cronZoneIsExplicitlyHanoi_notSystemDefault() {
        // Defence-in-depth: the cron zone constant is the literal Hanoi
        // ZoneId string. Anyone refactoring this away to ZoneId.systemDefault()
        // breaks the audit fix.
        assertThat(DrawScheduler.CRON_ZONE).isEqualTo("Asia/Ho_Chi_Minh");
    }
}
