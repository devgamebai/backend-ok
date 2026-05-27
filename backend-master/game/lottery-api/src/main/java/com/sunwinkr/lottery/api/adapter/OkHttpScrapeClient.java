package com.sunwinkr.lottery.api.adapter;

import com.google.gson.Gson;
import com.sunwinkr.lottery.engine.model.LotteryResult;
import com.sunwinkr.lottery.engine.port.ScrapeClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp adapter for {@link ScrapeClient}. Replaces the in-line
 * {@code OkHttpClient + Request} usage at
 * {@code LotteryModule.getResultLottery} (JLM:106-108) with a managed
 * adapter — single shared client, configurable URL, explicit timeouts.
 *
 * <h3>Config</h3>
 * <ul>
 *   <li>URL: {@code LOTTERY_API_URL} env, default
 *       {@code http://lottery-api:49111/api/v1}</li>
 *   <li>Connect timeout: 10s</li>
 *   <li>Read timeout: 10s</li>
 *   <li>No automatic retry — {@link com.sunwinkr.lottery.engine.ingest.DrawIngest}
 *       handles transient failures by re-running on the next ingest tick.</li>
 * </ul>
 *
 * <p>Plan §2.2 I1, §6.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * EngineConfig {@code scrapeClient} @Bean wrapper coexists with this
 * @Component. The @Component registration is canonical.
 */
@Primary
@Component
public class OkHttpScrapeClient implements ScrapeClient {

    private static final Logger LOG = LoggerFactory.getLogger(OkHttpScrapeClient.class);

    private static final String DEFAULT_URL = "http://lottery-api:49111/api/v1";

    private final OkHttpClient client;
    private final Gson gson;
    private final String url;

    public OkHttpScrapeClient() {
        this(new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build(),
            new Gson(),
            resolveUrl());
    }

    /** Test seam. */
    public OkHttpScrapeClient(OkHttpClient client, Gson gson, String url) {
        this.client = client;
        this.gson = gson;
        this.url = url;
    }

    private static String resolveUrl() {
        String v = System.getenv("LOTTERY_API_URL");
        return (v != null && !v.isEmpty()) ? v : DEFAULT_URL;
    }

    @Override
    public LotteryResult fetch() throws ScrapeException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ScrapeException("scrape http " + response.code() + " from " + url);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ScrapeException("scrape empty body from " + url);
            }
            String json = body.string();
            if (json == null || json.isEmpty()) {
                throw new ScrapeException("scrape empty payload from " + url);
            }
            LotteryResult parsed = gson.fromJson(json, LotteryResult.class);
            if (parsed == null || parsed.getResults() == null) {
                throw new ScrapeException("scrape parse returned null payload from " + url);
            }
            return parsed;
        } catch (IOException e) {
            throw new ScrapeException("scrape io failure from " + url, e);
        } catch (RuntimeException e) {
            throw new ScrapeException("scrape parse failure from " + url, e);
        }
    }
}
