package com.sunwinkr.lottery.engine.clock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lock-window + TZ-resilience tests for {@link LotteryClock}.
 *
 * <p>The {@code timezoneAlwaysVietnam*} cases are the load-bearing audit
 * fix — production runs {@code TZ=Asia/Seoul} and the legacy
 * {@code LotteryModule.handleClientRequest} used {@code LocalTime.now()}
 * which silently picked up that JVM default and fired the lock window at
 * the wrong wall hour. These tests force the JVM default TZ to a wrong
 * value at test time and prove {@code LotteryClock} still computes
 * decisions against Hanoi wall clock.
 */
class LotteryClockTest {

    private String originalTz;

    @BeforeEach
    void captureTz() {
        originalTz = System.getProperty("user.timezone");
    }

    @AfterEach
    void restoreTz() {
        if (originalTz == null) {
            System.clearProperty("user.timezone");
        } else {
            System.setProperty("user.timezone", originalTz);
        }
    }

    // Helper: build a fixed-instant Clock for a given Hanoi wall time.
    private static Clock vnClock(int hour, int min) {
        Instant instant = LocalDateTime.of(LocalDate.of(2026, 5, 14), java.time.LocalTime.of(hour, min))
                .atZone(LotteryClock.VN)
                .toInstant();
        // Keep clock zone neutral — LotteryClock.isBettingOpen re-zones to VN itself.
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    void bettingOpenBeforeLockTime() {
        // 17:00 Hanoi — well before 18:10 lock.
        assertThat(LotteryClock.isBettingOpen(vnClock(17, 0), false)).isTrue();
        assertThat(LotteryClock.isBettingOpen(vnClock(17, 0), true)).isTrue();
    }

    @Test
    void bettingLockedAtLockTime() {
        // 18:10 Hanoi — boundary, locked.
        assertThat(LotteryClock.isBettingOpen(vnClock(18, 10), false)).isFalse();
        assertThat(LotteryClock.isBettingOpen(vnClock(18, 10), true)).isFalse();
    }

    @Test
    void bettingLockedDuringPostHold() {
        // 18:30 Hanoi — inside the 45min post-lock hold. Settle flag MUST be
        // ignored during the hold; gate stays closed unconditionally.
        assertThat(LotteryClock.isBettingOpen(vnClock(18, 30), false)).isFalse();
        assertThat(LotteryClock.isBettingOpen(vnClock(18, 30), true)).isFalse();
    }

    @Test
    void bettingOpensAfterSettleComplete() {
        // 19:00 Hanoi — past the 18:55 hold expiry. Today's settle complete →
        // gate re-opens for next day's bets.
        assertThat(LotteryClock.isBettingOpen(vnClock(19, 0), true)).isTrue();
    }

    @Test
    void bettingLockedAfter1855IfSettleStillIncomplete() {
        // 19:00 Hanoi — past the 18:55 hold expiry. Settle NOT complete →
        // gate stays closed. Closes audit finding L-1 (bet-after-result).
        assertThat(LotteryClock.isBettingOpen(vnClock(19, 0), false)).isFalse();
    }

    @Test
    void timezoneAlwaysVietnamRegardlessOfJvmDefault() {
        // Simulate production: JVM default TZ = Asia/Seoul (the global .env
        // policy). LotteryClock MUST still pin to Hanoi.
        System.setProperty("user.timezone", "Asia/Seoul");

        // Instant that is 18:30 Hanoi (= 20:30 Seoul). If LotteryClock
        // accidentally picked up the JVM default, it would see 20:30,
        // think we are well past the hold, and open the gate. The test
        // proves it stays closed because we re-zone to VN.
        Clock c = vnClock(18, 30);
        assertThat(LotteryClock.isBettingOpen(c, true)).isFalse();
        assertThat(LotteryClock.isBettingOpen(c, false)).isFalse();
    }

    @Test
    void timezoneIgnoresUTC() {
        // Same audit fix, JVM default = UTC. 18:30 Hanoi = 11:30 UTC.
        // A naive LocalTime.now() implementation in UTC would think we
        // are at 11:30 — well before the 18:10 lock — and open the gate.
        // LotteryClock must instead see Hanoi wall and lock.
        System.setProperty("user.timezone", "UTC");

        Clock c = vnClock(18, 30);
        assertThat(LotteryClock.isBettingOpen(c, true)).isFalse();
        assertThat(LotteryClock.isBettingOpen(c, false)).isFalse();
    }

    @Test
    void lockAndScrapeConstantsExposed() {
        // Belt-and-suspenders: the public constants used by adapters /
        // schedulers (DrawScheduler in PR-3) must remain stable.
        assertThat(LotteryClock.lockTime()).isEqualTo(java.time.LocalTime.of(18, 10));
        assertThat(LotteryClock.scrapeTime()).isEqualTo(java.time.LocalTime.of(18, 35));
        assertThat(LotteryClock.POST_LOCK_HOLD).isEqualTo(java.time.Duration.ofMinutes(45));
        assertThat(LotteryClock.VN).isEqualTo(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
