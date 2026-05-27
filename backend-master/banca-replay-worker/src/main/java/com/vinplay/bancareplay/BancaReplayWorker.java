package com.vinplay.bancareplay;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SUN-1054 follow-up — BanCa failed-settle replay worker.
 *
 * <p>BanCa's C# {@code MoneyGatewayClient} (see
 * {@code banca/Core/Libs/UnifiedWallet/MoneyGatewayClient.cs}) pushes
 * exhausted settle payloads to the Redis list
 * {@code banca:failed_settle} after 3 HTTP retries fail. With no drain
 * worker, that list grows forever. This sidecar walks the list every
 * {@code BANCA_REPLAY_INTERVAL_SEC} seconds and re-posts each payload to
 * the Java backend at {@code c=9998} ({@code BanCaSettleProcessor}).
 *
 * <p>Idempotency is delegated to the server: every payload carries an
 * {@code external_ref} of the form
 * {@code banca:settle:{userId}:{sessionId}:{checkpointMs}}; the server's
 * {@code (tx_id, source)} UNIQUE on {@code money_gateway_log} dedupes a
 * replay of an already-applied entry, so the worker can safely retry
 * any item it isn't sure about.
 *
 * <h2>Failure handling</h2>
 * <ul>
 *   <li><b>HTTP 2xx</b> — drop the item.</li>
 *   <li><b>HTTP 4xx</b> or {@code success:false} with a terminal error
 *       code — move to dead list {@code banca:failed_settle_dead} for
 *       ops investigation (validation failure, missing user, etc.).</li>
 *   <li><b>HTTP 5xx, IO timeout, or {@code success:false} with
 *       errorCode=9999</b> — transient. Re-push with exponential delay
 *       and an {@code attempt} counter. After {@code BANCA_REPLAY_MAX_ATTEMPTS}
 *       (default 8) the entry also moves to the dead list.</li>
 * </ul>
 *
 * <p>Delayed entries are stored on the auxiliary list
 * {@code banca:failed_settle:retry_at:{ts}} (one list per second-bucket).
 * Every tick the worker scans buckets whose timestamp has elapsed and
 * moves them back to the main list. This pattern avoids depending on
 * a Redis SortedSet schema and keeps the keyspace bounded to a small
 * number of short-lived keys.
 *
 * <h2>Stats</h2>
 * Each tick emits a one-line stats log + writes a row to
 * {@code vinplay.money_anomaly} on any dead-list growth so the existing
 * Telegram alert pipeline (see SUN-1141 audit-bot) surfaces the event.
 *
 * <h2>Env vars</h2>
 * <table border="1">
 *   <tr><td>{@code BANCA_REPLAY_ENABLED}</td><td>{@code 0}|{@code 1}, default {@code 0}.</td></tr>
 *   <tr><td>{@code BANCA_REPLAY_INTERVAL_SEC}</td><td>tick cadence, default {@code 30}.</td></tr>
 *   <tr><td>{@code BANCA_REPLAY_BATCH_SIZE}</td><td>max items per tick, default {@code 200}.</td></tr>
 *   <tr><td>{@code BANCA_REPLAY_MAX_ATTEMPTS}</td><td>max retries before dead-list, default {@code 8}.</td></tr>
 *   <tr><td>{@code BANCA_MONEYGATEWAY_URL}</td><td>full URL of {@code c=9998}, e.g. {@code http://backend-api:19082/api_backend?c=9998}.</td></tr>
 *   <tr><td>{@code BANCA_SERVICE_TOKEN}</td><td>{@code X-Service-Token} header (shared with BanCa C# + LogBetCommission).</td></tr>
 *   <tr><td>{@code BANCA_REPLAY_HTTP_TIMEOUT_MS}</td><td>per-call timeout, default {@code 5000}.</td></tr>
 * </table>
 */
public final class BancaReplayWorker {

    private static final Logger logger = Logger.getLogger("banca-replay");

    static final String LIST_PENDING = "banca:failed_settle";
    static final String LIST_DEAD    = "banca:failed_settle_dead";
    static final String RETRY_BUCKET_PREFIX = "banca:failed_settle:retry_at:";

    private final RedisOps redis;
    private final HttpPoster http;
    private final AnomalySink anomalySink;
    private final int batchSize;
    private final int maxAttempts;
    private final int intervalSec;
    private final long initialBackoffMs;

    // Stats counters (reset on each tick log line)
    private final AtomicLong totalReplayed = new AtomicLong();
    private final AtomicLong totalSucceeded = new AtomicLong();
    private final AtomicLong totalToDead = new AtomicLong();
    private final AtomicLong totalRescheduled = new AtomicLong();

    BancaReplayWorker(RedisOps redis,
                      HttpPoster http,
                      AnomalySink anomalySink,
                      int batchSize,
                      int maxAttempts,
                      int intervalSec,
                      long initialBackoffMs) {
        this.redis = redis;
        this.http = http;
        this.anomalySink = anomalySink;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.intervalSec = intervalSec;
        this.initialBackoffMs = initialBackoffMs;
    }

    // ---------------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------------

    public static void main(String[] args) {
        boolean enabled = boolEnv("BANCA_REPLAY_ENABLED", false);
        int interval   = intEnv("BANCA_REPLAY_INTERVAL_SEC", 30);
        int batchSize  = intEnv("BANCA_REPLAY_BATCH_SIZE", 200);
        int maxAttempts = intEnv("BANCA_REPLAY_MAX_ATTEMPTS", 8);
        int httpTimeoutMs = intEnv("BANCA_REPLAY_HTTP_TIMEOUT_MS", 5000);

        logger.info("BancaReplayWorker boot — enabled=" + enabled
                + " interval=" + interval + "s batch=" + batchSize
                + " maxAttempts=" + maxAttempts);
        System.out.println("[banca-replay] boot enabled=" + enabled
                + " interval=" + interval + "s batch=" + batchSize);

        if (!enabled) {
            // Idle-shell mode — same pattern as HistoryDrainerMain. Lets the
            // container land in compose with the flag off until ops flips it.
            idle(interval);
            return;
        }

        String url = System.getenv("BANCA_MONEYGATEWAY_URL");
        String token = System.getenv("BANCA_SERVICE_TOKEN");
        if (url == null || url.isEmpty() || token == null || token.isEmpty()) {
            logger.error("BANCA_MONEYGATEWAY_URL / BANCA_SERVICE_TOKEN not set — refusing to start");
            System.err.println("[banca-replay] config missing, exiting");
            System.exit(2);
        }

        LettuceRedisOps redis = new LettuceRedisOps();
        UrlConnectionPoster http = new UrlConnectionPoster(url, token, httpTimeoutMs);
        MysqlAnomalySink sink = new MysqlAnomalySink();

        BancaReplayWorker worker = new BancaReplayWorker(
                redis, http, sink, batchSize, maxAttempts, interval, 1000L);

        Runtime.getRuntime().addShutdownHook(new Thread(redis::close, "banca-replay-shutdown"));

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "banca-replay-tick");
            t.setDaemon(false);
            return t;
        });
        ses.scheduleWithFixedDelay(worker::safeTick, 5, interval, TimeUnit.SECONDS);

        try { Thread.currentThread().join(); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private static void idle(int heartbeatSec) {
        long ticks = 0;
        while (true) {
            try { Thread.sleep(heartbeatSec * 1000L); }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            ticks++;
            logger.info("BancaReplayWorker idle heartbeat tick=" + ticks);
        }
    }

    // ---------------------------------------------------------------------
    // Tick loop (testable)
    // ---------------------------------------------------------------------

    void safeTick() {
        try {
            tick();
        } catch (Throwable t) {
            logger.error("BancaReplayWorker tick crashed: " + t.getMessage(), t);
        }
    }

    /**
     * One pass: (1) promote due retry buckets back to the pending list,
     * (2) LPOP up to batchSize items and replay each.
     */
    void tick() {
        promoteDueBuckets(System.currentTimeMillis());

        int replayed = 0;
        int succeeded = 0;
        int dead = 0;
        int rescheduled = 0;

        for (int i = 0; i < batchSize; i++) {
            String item = redis.lpop(LIST_PENDING);
            if (item == null) break;
            replayed++;

            ReplayOutcome out = replayOne(item);
            switch (out) {
                case OK:           succeeded++;    break;
                case DEAD:         dead++;         break;
                case RESCHEDULED:  rescheduled++;  break;
            }
        }

        totalReplayed.addAndGet(replayed);
        totalSucceeded.addAndGet(succeeded);
        totalToDead.addAndGet(dead);
        totalRescheduled.addAndGet(rescheduled);

        if (replayed > 0) {
            logger.info("BancaReplayWorker tick replayed=" + replayed
                    + " ok=" + succeeded + " dead=" + dead
                    + " rescheduled=" + rescheduled
                    + " | total ok=" + totalSucceeded.get()
                    + " dead=" + totalToDead.get());
            if (dead > 0) {
                anomalySink.write("WARN", "BANCA_FAILED_SETTLE_DEAD",
                        "{\"count\":" + dead + ",\"tick_ts\":" + System.currentTimeMillis() + "}");
            }
        }
    }

    enum ReplayOutcome { OK, DEAD, RESCHEDULED }

    ReplayOutcome replayOne(String rawJson) {
        // Parse the payload + read/bump attempt counter. We carry the
        // attempt count INSIDE the JSON itself so it survives a re-push
        // through the retry bucket without needing a side table.
        JSONObject payload;
        try {
            payload = new JSONObject(rawJson);
        } catch (Exception e) {
            logger.error("Unparseable item moved to dead list: " + truncate(rawJson, 200));
            redis.rpush(LIST_DEAD, rawJson);
            return ReplayOutcome.DEAD;
        }

        int attempt = payload.optInt("_replay_attempt", 0) + 1;
        payload.put("_replay_attempt", attempt);

        HttpResponse resp;
        try {
            resp = http.post(payload.toString());
        } catch (IOException e) {
            // IO failure == transient. Re-push with backoff.
            logger.warn("HTTP IO failure ref=" + payload.optString("external_ref")
                    + " attempt=" + attempt + " err=" + e.getMessage());
            return reschedule(payload, attempt);
        }

        if (resp.status >= 200 && resp.status < 300) {
            // success:false with errorCode 9999 == internal error == transient
            // success:false with errorCode 4xxx / 1xxx == terminal
            boolean success = parseJsonSuccess(resp.body);
            if (success) {
                return ReplayOutcome.OK;
            }
            String code = parseErrorCode(resp.body);
            if ("9999".equals(code)) {
                return reschedule(payload, attempt);
            }
            // 4xxx / 1xxx — terminal, dead list
            logger.warn("Terminal failure code=" + code
                    + " ref=" + payload.optString("external_ref")
                    + " -> dead");
            redis.rpush(LIST_DEAD, payload.toString());
            return ReplayOutcome.DEAD;
        }

        if (resp.status >= 400 && resp.status < 500) {
            logger.warn("HTTP 4xx status=" + resp.status
                    + " ref=" + payload.optString("external_ref") + " -> dead");
            redis.rpush(LIST_DEAD, payload.toString());
            return ReplayOutcome.DEAD;
        }

        // 5xx — transient
        logger.warn("HTTP 5xx status=" + resp.status
                + " ref=" + payload.optString("external_ref")
                + " attempt=" + attempt);
        return reschedule(payload, attempt);
    }

    private ReplayOutcome reschedule(JSONObject payload, int attempt) {
        if (attempt >= maxAttempts) {
            logger.warn("Max attempts hit ref=" + payload.optString("external_ref")
                    + " attempt=" + attempt + " -> dead");
            redis.rpush(LIST_DEAD, payload.toString());
            return ReplayOutcome.DEAD;
        }
        // Exponential backoff: initialBackoffMs * 2^(attempt-1).
        // attempt=1 -> 1s, 2 -> 2s, 3 -> 4s, ... 8 -> 128s.
        long delayMs = initialBackoffMs * (1L << Math.min(attempt - 1, 16));
        long retryAt = (System.currentTimeMillis() + delayMs) / 1000L;
        String bucket = RETRY_BUCKET_PREFIX + retryAt;
        redis.rpush(bucket, payload.toString());
        return ReplayOutcome.RESCHEDULED;
    }

    /**
     * Walk retry buckets keyed by second-timestamp; for any bucket whose
     * timestamp has elapsed, move all its entries back onto the pending
     * list, then delete the bucket. Bounded by batchSize per pass so an
     * accidental backlog can't starve the main loop.
     */
    void promoteDueBuckets(long nowMs) {
        long nowSec = nowMs / 1000L;
        java.util.List<String> due = redis.scanKeys(RETRY_BUCKET_PREFIX + "*", batchSize);
        int promoted = 0;
        for (String key : due) {
            // Key format: banca:failed_settle:retry_at:{epoch_sec}
            int idx = key.lastIndexOf(':');
            if (idx < 0) continue;
            long ts;
            try { ts = Long.parseLong(key.substring(idx + 1)); }
            catch (NumberFormatException nfe) { continue; }
            if (ts > nowSec) continue;
            // Drain and re-queue
            String item;
            while ((item = redis.lpop(key)) != null) {
                redis.rpush(LIST_PENDING, item);
                promoted++;
            }
            redis.del(key);
        }
        if (promoted > 0) {
            logger.info("BancaReplayWorker promoted " + promoted + " items from retry buckets");
        }
    }

    // ---------------------------------------------------------------------
    // Stats accessors for tests / introspection
    // ---------------------------------------------------------------------

    long getTotalReplayed()    { return totalReplayed.get(); }
    long getTotalSucceeded()   { return totalSucceeded.get(); }
    long getTotalToDead()      { return totalToDead.get(); }
    long getTotalRescheduled() { return totalRescheduled.get(); }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static boolean parseJsonSuccess(String body) {
        if (body == null || body.isEmpty()) return false;
        try {
            JSONObject o = new JSONObject(body);
            return o.optBoolean("success", false);
        } catch (Exception e) {
            return false;
        }
    }

    private static String parseErrorCode(String body) {
        if (body == null || body.isEmpty()) return "";
        try {
            JSONObject o = new JSONObject(body);
            return o.optString("errorCode", "");
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean boolEnv(String k, boolean d) {
        String v = System.getenv(k);
        if (v == null) return d;
        return v.equals("1") || v.equalsIgnoreCase("true");
    }

    private static int intEnv(String k, int d) {
        String v = System.getenv(k);
        if (v == null) return d;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return d; }
    }

    private static String truncate(String s, int n) {
        return s == null ? "" : (s.length() <= n ? s : s.substring(0, n) + "...");
    }

    // ---------------------------------------------------------------------
    // Interfaces (mocked in tests)
    // ---------------------------------------------------------------------

    interface RedisOps {
        String lpop(String key);
        void rpush(String key, String value);
        void del(String key);
        java.util.List<String> scanKeys(String pattern, int limit);
        default void close() {}
    }

    interface HttpPoster {
        HttpResponse post(String jsonBody) throws IOException;
    }

    interface AnomalySink {
        void write(String severity, String invariant, String detailsJson);
    }

    static final class HttpResponse {
        final int status;
        final String body;
        HttpResponse(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }
    }

    // ---------------------------------------------------------------------
    // Default impls (production)
    // ---------------------------------------------------------------------

    /**
     * Lettuce-backed Redis ops. Reads connection config from env vars
     * matching the rest of the stack: {@code REDIS_STREAMS_HOST},
     * {@code REDIS_STREAMS_PORT}, {@code REDIS_PASSWORD}, and DB 0
     * (BanCa C# pushes onto DB 0, not the cache DB 1).
     */
    static final class LettuceRedisOps implements RedisOps {
        private final io.lettuce.core.RedisClient client;
        private final io.lettuce.core.api.StatefulRedisConnection<String, String> conn;

        LettuceRedisOps() {
            String host = envOr("REDIS_STREAMS_HOST", "redis");
            int port = Integer.parseInt(envOr("REDIS_STREAMS_PORT", "6379"));
            int db = Integer.parseInt(envOr("BANCA_REPLAY_REDIS_DB", "0"));
            String pwd = System.getenv("REDIS_PASSWORD");
            io.lettuce.core.RedisURI.Builder b = io.lettuce.core.RedisURI.builder()
                    .withHost(host).withPort(port).withDatabase(db);
            if (pwd != null && !pwd.isEmpty()) b.withPassword(pwd.toCharArray());
            this.client = io.lettuce.core.RedisClient.create(b.build());
            this.conn = client.connect();
            logger.info("LettuceRedisOps connected " + host + ":" + port + "/db" + db);
        }

        @Override public String lpop(String key) {
            return conn.sync().lpop(key);
        }

        @Override public void rpush(String key, String value) {
            conn.sync().rpush(key, value);
        }

        @Override public void del(String key) {
            conn.sync().del(key);
        }

        @Override public java.util.List<String> scanKeys(String pattern, int limit) {
            io.lettuce.core.ScanArgs args = io.lettuce.core.ScanArgs.Builder.matches(pattern).limit(limit);
            io.lettuce.core.KeyScanCursor<String> cur = conn.sync().scan(args);
            return new java.util.ArrayList<>(cur.getKeys());
        }

        @Override public void close() {
            try { conn.close(); } catch (Exception ignore) {}
            try { client.shutdown(); } catch (Exception ignore) {}
        }

        private static String envOr(String k, String d) {
            String v = System.getenv(k);
            return v == null || v.isEmpty() ? d : v;
        }
    }

    /**
     * Tiny HttpURLConnection-based POSTer — keeps deps off the
     * httpclient module (already in runtime-libs but the worker jar
     * stays slim by reusing only what's in VbeeCommon's transitive cone).
     */
    static final class UrlConnectionPoster implements HttpPoster {
        private final URL url;
        private final String token;
        private final int timeoutMs;

        UrlConnectionPoster(String url, String token, int timeoutMs) {
            try { this.url = new URL(url); }
            catch (Exception e) { throw new IllegalArgumentException("bad url: " + url, e); }
            this.token = token;
            this.timeoutMs = timeoutMs;
        }

        @Override public HttpResponse post(String jsonBody) throws IOException {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("X-Service-Token", token);
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = c.getOutputStream()) {
                os.write(bytes);
            }
            int status = c.getResponseCode();
            InputStream is = status >= 400 ? c.getErrorStream() : c.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (is != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            return new HttpResponse(status, sb.toString());
        }
    }

    /**
     * Writes a row to {@code vinplay.money_anomaly} so the existing
     * SUN-1141 audit-bot Telegram pipeline surfaces dead-list growth.
     * Best-effort — never throws back to the tick loop.
     */
    static final class MysqlAnomalySink implements AnomalySink {
        @Override public void write(String severity, String invariant, String detailsJson) {
            try (Connection conn = com.vinplay.vbee.common.pools.ConnectionPool
                    .getInstance().getConnection("mysqlpoolname");
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO vinplay.money_anomaly " +
                         "(severity, invariant, details) VALUES (?, ?, ?)")) {
                ps.setString(1, severity);
                ps.setString(2, invariant);
                ps.setString(3, detailsJson);
                ps.executeUpdate();
            } catch (Throwable t) {
                logger.warn("MysqlAnomalySink write failed: " + t.getMessage());
            }
        }
    }
}
