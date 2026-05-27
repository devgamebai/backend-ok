package com.sunwinkr.lottery.api.scheduler;

import com.sunwinkr.lottery.api.push.SettleAnnouncePublisher;
import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.ingest.DrawIngest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Daily 18:35 Hanoi scrape — replaces the legacy
 * {@code LotteryModule.init} {@code ScheduledExecutorService} with a
 * Spring-managed cron job.
 *
 * <h3>CRITICAL TZ POLICY</h3>
 * Cron expression carries {@code zone="Asia/Ho_Chi_Minh"} EXPLICITLY.
 * The JVM default zone remains {@code Asia/Seoul} (intentional — other
 * games anchor there). This is the central enforcement point of
 * {@link LotteryClock#VN} — every lottery time decision routes through
 * Vietnam wall clock regardless of JVM TZ.
 *
 * <p>Plan §5.5, §6.
 */
@Component
public class DrawScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(DrawScheduler.class);

    /** Cron format Spring 6-field: sec min hour day month dow. */
    public static final String SCRAPE_CRON_VN = "0 35 18 * * *";

    /** Pinned VN zone for the scrape cron. */
    public static final String CRON_ZONE = "Asia/Ho_Chi_Minh";

    private final DrawIngest ingest;
    private final SettleAnnouncePublisher announcer;

    public DrawScheduler(DrawIngest ingest, SettleAnnouncePublisher announcer) {
        this.ingest = ingest;
        this.announcer = announcer;
    }

    /**
     * Fire at 18:35 Hanoi every day. The scrape is idempotent — a
     * re-run on a fully-settled day is a no-op. Failures are logged but
     * do not halt the cron — the next 18:35 attempt retries.
     *
     * <p>On a successful scrape+settle the announcer publishes the
     * one-shot {@code settled} event on
     * {@code /topic/lottery/xsmb/announce}. Result fields are NEVER
     * included — clients pull the payload via REST.
     */
    @Scheduled(cron = SCRAPE_CRON_VN, zone = CRON_ZONE)
    public void runDailyScrape() {
        LocalDate vnToday = LocalDate.now(LotteryClock.VN);
        try {
            DrawIngest.IngestSummary summary = ingest.runOnce(vnToday);
            LOG.info("DrawScheduler runDailyScrape vnDate={} newScrape={} settled={} failures={} scrapeFailed={}",
                vnToday, summary.newScrapePersisted, summary.settledCount,
                summary.failureCount, summary.scrapeFailed);
            if (!summary.scrapeFailed) {
                announcer.announceSettled(vnToday);
            }
        } catch (Throwable t) {
            LOG.error("DrawScheduler runDailyScrape failed for vnDate={}", vnToday, t);
        }
    }

    /**
     * Fire at 18:10 Hanoi every day — one-shot announce that bets are
     * locked. Lets STOMP clients flip their UI to read-only without
     * polling.
     */
    @Scheduled(cron = "0 10 18 * * *", zone = CRON_ZONE)
    public void runLockAnnounce() {
        try {
            announcer.announceLocked(LotteryClock.LOCK_TIME.toString());
        } catch (Throwable t) {
            LOG.warn("DrawScheduler runLockAnnounce failed", t);
        }
    }
}
