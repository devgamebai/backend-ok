package com.sunwinkr.lottery.engine.ingest;

import com.sunwinkr.lottery.engine.model.LotteryResult;

/**
 * JSON → {@link LotteryResult} adapter shim.
 *
 * <p>The engine module is pure Java with NO Gson dependency on the
 * compile classpath. PR-3's {@code OkHttpScrapeClient} adapter will use
 * Gson directly. This class exists as a SPI — callers may wire any JSON
 * mapper they like (the only constraint is the {@code ĐB} Unicode field
 * name from {@link LotteryResult.Results}).
 *
 * <p>The {@link #parse} contract is the engine boundary — the JSON
 * binding is delegated to a {@link Provider} the host wires at startup.
 *
 * <p>See {@code docs/plans/lottery-extraction-plan.md §2.2 I2}.
 */
public final class DrawJsonParser {

    private final Provider provider;

    public DrawJsonParser(Provider provider) {
        this.provider = provider;
    }

    /**
     * Parse {@code rawJson} into a {@link LotteryResult}. Returns
     * {@code null} on malformed input — the {@link DrawIngest} treats
     * that as a transient scrape failure (matching legacy behaviour).
     */
    public LotteryResult parse(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) return null;
        try {
            return provider.from(rawJson);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * SPI for JSON deserialisation. The PR-3 adapter wires this with a
     * 2-line Gson impl: {@code (json) -> new Gson().fromJson(json, LotteryResult.class)}.
     */
    public interface Provider {
        LotteryResult from(String rawJson);
    }
}
