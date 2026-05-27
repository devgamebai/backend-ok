package com.sunwinkr.lottery.engine.port;

import com.sunwinkr.lottery.engine.model.LotteryResult;

/**
 * Outbound HTTP boundary — the engine's view of the XSMB scraper
 * ({@code api-xsmb-today-main} container on port 49111).
 *
 * <p>PR-3 ships the {@code OkHttpScrapeClient} adapter — see
 * {@code docs/plans/lottery-extraction-plan.md §2.2 I1}. Default URL
 * {@code http://lottery-api:49111/api/v1} overridable via
 * {@code LOTTERY_API_URL} env. Connect/read timeout 10s, one retry.
 */
public interface ScrapeClient {

    /**
     * Pull the latest draw payload. Returns the deserialised
     * {@link LotteryResult} (Gson via
     * {@link com.sunwinkr.lottery.engine.ingest.DrawJsonParser}).
     *
     * @throws ScrapeException on HTTP failure, parse failure, or timeout
     */
    LotteryResult fetch() throws ScrapeException;

    /** Wrapped scrape failure — the {@link DrawIngest} loop swallows + alerts. */
    final class ScrapeException extends Exception {
        public ScrapeException(String msg) {
            super(msg);
        }

        public ScrapeException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
