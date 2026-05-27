package com.vinplay.dal.audit;

import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SUN-1108/1110 follow-up — ops alerts for finance / data-integrity events.
 *
 * <p>Posts a Telegram message to the ops channel when a rare-but-serious
 * condition fires. Currently used to surface {@code log_gsc_bets} write
 * failures (the SUN-1108/1110 incident pattern) within seconds instead
 * of waiting for a customer complaint days later.
 *
 * <p>Configuration via env (in this priority order — first match wins):
 * <ul>
 *   <li>{@code TELEGRAM_OPS_BOT_TOKEN} + {@code TELEGRAM_OPS_CHAT_ID}
 *       — dedicated ops channel, recommended</li>
 *   <li>{@code TELEGRAM_BOT_TOKEN} + {@code TELEGRAM_DEPOSIT_CHAT_ID}
 *       — fallback, reuses the deposit-bot infrastructure</li>
 * </ul>
 *
 * <p>Throttling: same {@code alertKey} won't fire more than once per
 * {@link #THROTTLE_MS}. Prevents alert storms when the underlying issue
 * affects many requests in quick succession (the 30-min Mongo blip
 * scenario from SUN-1108/1110 would have produced 3 alerts at most,
 * one per 5-min window).
 *
 * <p>Defensive contract: every public method swallows all exceptions
 * and only logs at WARN level. The caller's GSC API path is never
 * affected by alerting failures.
 *
 * <p>Toggle: env {@code OPS_ALERTS_ENABLED=false} disables all sends.
 */
public final class TelegramOpsNotifier {

    private static final Logger logger = Logger.getLogger("ops-alerts");
    private static final long THROTTLE_MS = 5L * 60L * 1000L;          // 5 minutes
    private static final ConcurrentMap<String, Long> LAST_SENT = new ConcurrentHashMap<>();

    private static final String botToken;
    private static final String chatId;
    static {
        String t = System.getenv("TELEGRAM_OPS_BOT_TOKEN");
        String c = System.getenv("TELEGRAM_OPS_CHAT_ID");
        if (t == null || t.isEmpty()) t = System.getenv("TELEGRAM_BOT_TOKEN");
        if (c == null || c.isEmpty()) c = System.getenv("TELEGRAM_DEPOSIT_CHAT_ID");
        botToken = t;
        chatId = c;
    }

    private TelegramOpsNotifier() {}

    private static boolean isEnabled() {
        String v = System.getenv("OPS_ALERTS_ENABLED");
        if (v == null) return true;
        return !"false".equalsIgnoreCase(v) && !"0".equals(v);
    }

    private static boolean isConfigured() {
        return botToken != null && !botToken.isEmpty() && chatId != null && !chatId.isEmpty();
    }

    /**
     * Specific alert for {@code log_gsc_bets} write failures
     * (SUN-1108/1110 incident pattern). Fires only after MongoRetry
     * exhaustion — not on individual retry attempts.
     */
    public static void alertGscBetWriteFailure(String op, String wagerCode,
                                               String memberAccount, long bet,
                                               String errorMessage) {
        try {
            String text = "🚨 GSC bet log write failed (no retry left)\n"
                    + "━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "op: " + safe(op) + "\n"
                    + "wager: " + safe(wagerCode) + "\n"
                    + "member: " + safe(memberAccount) + "\n"
                    + "bet: " + bet + "\n"
                    + "err: " + safe(errorMessage) + "\n"
                    + "\n"
                    + "Wallet was credited via the RMQ path — this is an audit gap, not money loss. "
                    + "Ops: run bin/gsc-audit.py for the affected window to confirm with GSC.";
            // Throttle by op kind, NOT by wager — many wagers fail together
            // during a Mongo blip and one alert is enough to wake ops.
            sendThrottled("gsc_bet_write_failure:" + op, text);
        } catch (Throwable t) {
            // Never let alerting break the caller path.
            logger.warn("alertGscBetWriteFailure dropped: " + t.getMessage());
        }
    }

    /**
     * Phase 5 prep gate 5p4 — fires when {@code mysqlpoolname} has been
     * above the utilization threshold for 2+ consecutive 30s samples.
     * Throttled per pool name so a sustained-pressure window produces at
     * most one alert per {@link #THROTTLE_MS}.
     */
    public static void alertPoolPressure(String poolName, double utilizationPct,
                                         int active, int maxPoolSize) {
        try {
            String text = "⚠️ ConnectionPool pressure: " + safe(poolName)
                    + " at " + String.format("%.1f", utilizationPct) + "% ("
                    + active + "/" + maxPoolSize + " active) "
                    + "for 2+ consecutive 30s samples. "
                    + "Investigate slow queries / Mongo backoff chain.";
            // Throttle by pool — multiple consecutive alerts on the same pool
            // collapse into one per 5-min window.
            sendThrottled("pool_pressure:" + poolName, text);
        } catch (Throwable t) {
            // Never let alerting break the caller path.
            logger.warn("alertPoolPressure dropped: " + t.getMessage());
        }
    }

    /**
     * Phase 5 prep gate 5p3 — fires when a {@code SeamlessWalletAggregator}
     * handler's rolling 1-minute p99 exceeds 100ms. The companion
     * {@code AggregatorP99Scheduler} polls {@code AggregatorMetrics} every
     * 60s and calls this when any aggregator breaches the threshold.
     * Throttled per aggregator so sustained pressure produces at most one
     * alert per {@link #THROTTLE_MS} (5 min) — avoids paging ops every
     * minute when Mongo is under sustained backoff.
     *
     * @param aggregatorName e.g. {@code "GscBalance"}, {@code "GscPushBet"}
     * @param p99Ms          observed p99 in milliseconds
     * @param count          sample count over the 1-min window
     */
    public static void alertHandlerSlowP99(String aggregatorName, long p99Ms, int count) {
        try {
            String text = "⚠️ SeamlessWalletAggregator p99 latency high: "
                    + safe(aggregatorName)
                    + " p99=" + p99Ms + "ms over last minute"
                    + " (count=" + count + "). "
                    + "Check Hazelcast / Mongo pool pressure (env GSC_P99_THRESHOLD_MS to tune).";
            // Throttle by aggregator name — many handlers may breach during
            // a Mongo blip; one alert per handler per 5-min window is enough.
            sendThrottled("aggregator_slow_p99:" + safe(aggregatorName), text);
        } catch (Throwable t) {
            // Never let alerting break the caller path.
            logger.warn("alertHandlerSlowP99 dropped: " + t.getMessage());
        }
    }

    /** Generic escape hatch for ad-hoc alerts. {@code alertKey} controls throttling. */
    public static void alert(String alertKey, String text) {
        try {
            sendThrottled(alertKey, text);
        } catch (Throwable t) {
            logger.warn("alert dropped: " + t.getMessage());
        }
    }

    private static void sendThrottled(String alertKey, String text) {
        if (!isEnabled() || !isConfigured()) return;
        long now = System.currentTimeMillis();
        Long last = LAST_SENT.get(alertKey);
        if (last != null && (now - last) < THROTTLE_MS) {
            // Suppressed — but still WARN-log so the rate isn't completely
            // hidden during sustained issues.
            logger.warn("ops alert THROTTLED key=" + alertKey
                    + " (last sent " + ((now - last) / 1000) + "s ago)");
            return;
        }
        LAST_SENT.put(alertKey, now);
        sendBlocking(text);
    }

    private static void sendBlocking(String text) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendMessage");
            String body = "chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                    + "&text=" + URLEncoder.encode(text, "UTF-8");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warn("Telegram ops alert HTTP " + code);
            }
        } catch (Throwable t) {
            logger.warn("Telegram ops alert send failed: " + t.getMessage());
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
