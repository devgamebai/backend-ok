package com.sunwinkr.lottery.engine.ingest;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.port.ResultStore;
import com.sunwinkr.lottery.engine.port.ScrapeClient;
import com.sunwinkr.lottery.engine.port.SettledFlagStore;
import com.sunwinkr.lottery.engine.settle.LotterySettleService;
import com.sunwinkr.lottery.engine.settle.SettleSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Daily draw ingest orchestrator. Ports
 * {@code LotteryModule.getResultLottery} (JLM:100-142) to the engine
 * boundary.
 *
 * <p>Order (each step idempotent on resume):
 * <ol>
 *   <li>{@link ResultStore#findByDate} — dedupe; short-circuit on hit
 *       UNLESS the day is not yet settled (so a crash mid-settle resumes)</li>
 *   <li>{@link ScrapeClient#fetch} — pull JSON via HTTP (PR-3 adapter)</li>
 *   <li>{@link ResultStore#save} — persist with {@code settled_at=NULL}</li>
 *   <li>{@link LotterySettleService#settleAll} — credit each winning ticket</li>
 *   <li>{@link SettledFlagStore#markSettled} — LAST write, closes the gate</li>
 * </ol>
 *
 * <p>Crash resilience: any failure between (3) and (5) leaves
 * {@code result_lottery.settled_at IS NULL} — a re-run finds the row in
 * (1) but {@link SettledFlagStore#isSettled} reports false, so the
 * settle loop runs again with the same payload. The settle loop itself
 * is per-ticket idempotent via {@code lode.settled_at}.
 */
public final class DrawIngest {

    private static final Logger log = LoggerFactory.getLogger(DrawIngest.class);

    private final ScrapeClient scrapeClient;
    private final ResultStore resultStore;
    private final SettledFlagStore settledFlagStore;
    private final LotterySettleService settleService;
    private final JsonRenderer jsonRenderer;

    public DrawIngest(ScrapeClient scrapeClient,
                      ResultStore resultStore,
                      SettledFlagStore settledFlagStore,
                      LotterySettleService settleService,
                      JsonRenderer jsonRenderer) {
        this.scrapeClient = scrapeClient;
        this.resultStore = resultStore;
        this.settledFlagStore = settledFlagStore;
        this.settleService = settleService;
        this.jsonRenderer = jsonRenderer;
    }

    /**
     * Single-pass ingest for {@code vnDate}. Safe to call repeatedly —
     * a fully-settled day is a no-op; a partially-settled day picks up
     * from the unmarked tickets.
     *
     * @param vnDate Vietnam-wall draw date (typically {@code LocalDate.now(LotteryClock.VN)})
     * @return summary of what changed (counts for telemetry)
     */
    public IngestSummary runOnce(LocalDate vnDate) {
        Optional<LotteryResult> existing = resultStore.findByDate(vnDate);
        boolean alreadySettled = settledFlagStore.isSettled(vnDate);
        if (existing.isPresent() && alreadySettled) {
            log.debug("DrawIngest no-op for {} — already settled", vnDate);
            return IngestSummary.noop();
        }

        LotteryResult draw;
        boolean newlyScraped = false;
        if (existing.isPresent()) {
            log.info("DrawIngest resuming partial settle for {}", vnDate);
            draw = existing.get();
        } else {
            try {
                draw = scrapeClient.fetch();
            } catch (ScrapeClient.ScrapeException e) {
                log.warn("DrawIngest scrape failed for {}: {}", vnDate, e.getMessage());
                return IngestSummary.scrapeFailed();
            }
            if (draw == null) {
                log.warn("DrawIngest scrape returned null for {}", vnDate);
                return IngestSummary.scrapeFailed();
            }
            String rendered = jsonRenderer.render(draw);
            resultStore.save(rendered, vnDate);
            newlyScraped = true;
        }

        SettleSummary settle = settleService.settleAll(vnDate, draw);
        // Last write — gates REST visibility + tomorrow's bet window.
        settledFlagStore.markSettled(vnDate);
        return new IngestSummary(newlyScraped, settle.getSettledCount(), settle.getFailureCount());
    }

    /**
     * SPI for re-serialising the parsed draw back to JSON for persistence.
     * The adapter wires this with the same Gson instance the parser uses.
     */
    public interface JsonRenderer {
        String render(LotteryResult draw);
    }

    /** Per-call telemetry — adapter logs / metrics this. */
    public static final class IngestSummary {
        public final boolean newScrapePersisted;
        public final int settledCount;
        public final int failureCount;
        public final boolean scrapeFailed;

        private IngestSummary(boolean newScrapePersisted, int settledCount, int failureCount) {
            this.newScrapePersisted = newScrapePersisted;
            this.settledCount = settledCount;
            this.failureCount = failureCount;
            this.scrapeFailed = false;
        }

        private IngestSummary(boolean scrapeFailed) {
            this.newScrapePersisted = false;
            this.settledCount = 0;
            this.failureCount = 0;
            this.scrapeFailed = scrapeFailed;
        }

        public static IngestSummary noop() {
            return new IngestSummary(false, 0, 0);
        }

        public static IngestSummary scrapeFailed() {
            return new IngestSummary(true);
        }
    }
}
