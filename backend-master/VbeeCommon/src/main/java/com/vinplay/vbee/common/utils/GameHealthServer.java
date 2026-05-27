package com.vinplay.vbee.common.utils;

import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Liveness probe for game-server scheduled loops.
 *
 * <p>Usage:
 * <ol>
 *   <li>Call {@link #start(int)} once at server boot (typically after Hazelcast
 *       is connected and before game modules start scheduling).
 *   <li>Call {@link #tick()} from inside each game-loop scheduled task so it
 *       stamps the last-alive timestamp.
 *   <li>Docker healthcheck probes {@code http://localhost:PORT/health} — the
 *       endpoint returns HTTP 200 when the most recent tick is &lt; 90 s old,
 *       HTTP 503 otherwise. Compose auto-restarts after the configured retries.
 * </ol>
 *
 * <p>Rationale: {@code ScheduledExecutorService} silently cancels a scheduled
 * task when its {@code run()} escapes with a {@code Throwable} that's not an
 * {@code Exception}. P1 fix (catch Throwable everywhere) covers the common
 * case; this probe is the safety net for any residual deadlock, GC pause, or
 * other stall mode that doesn't raise an exception at all.
 */
public final class GameHealthServer {

    private static final Logger logger = Logger.getLogger("GameHealthServer");
    private static final long STALE_THRESHOLD_MS = 90_000L;
    private static final AtomicLong lastTickMs = new AtomicLong(System.currentTimeMillis());
    private static volatile HttpServer server;

    private GameHealthServer() {}

    // Auto-start when first loaded. Default port 8888 matches the compose
    // healthcheck fallback in docker-compose.games.yml. Set
    // GAME_HEALTH_PORT=disabled (or any non-numeric value) to turn the probe
    // off for a specific service (e.g. portal-api doesn't need it but the
    // class is still loaded transitively — harmless if port bind succeeds).
    static {
        String autoPort = System.getenv().getOrDefault("GAME_HEALTH_PORT", "8888");
        try { start(Integer.parseInt(autoPort.trim())); } catch (NumberFormatException disabled) {
            // opt-out: GAME_HEALTH_PORT explicitly set to non-numeric → skip
        } catch (Throwable t) {
            logger.error("GameHealthServer auto-start failed for port " + autoPort, t);
        }
    }

    public static synchronized void start(int port) {
        if (server != null) return;
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
            s.createContext("/health", exchange -> {
                long age = System.currentTimeMillis() - lastTickMs.get();
                boolean healthy = age < STALE_THRESHOLD_MS;
                String body = "{\"tick_age_ms\":" + age
                        + ",\"healthy\":" + healthy
                        + ",\"threshold_ms\":" + STALE_THRESHOLD_MS + "}";
                byte[] bytes = body.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(healthy ? 200 : 503, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            });
            // SUN-935: pool-stats endpoint — exposes HikariCP per-pool metrics
            s.createContext("/pool-stats", exchange -> {
                try {
                    java.util.Map<String, java.util.Map<String, Object>> stats =
                            com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getPoolStats();
                    StringBuilder sb = new StringBuilder("{");
                    boolean first = true;
                    for (java.util.Map.Entry<String, java.util.Map<String, Object>> e : stats.entrySet()) {
                        if (!first) sb.append(",");
                        sb.append("\"").append(e.getKey()).append("\":{");
                        boolean innerFirst = true;
                        for (java.util.Map.Entry<String, Object> v : e.getValue().entrySet()) {
                            if (!innerFirst) sb.append(",");
                            sb.append("\"").append(v.getKey()).append("\":");
                            if (v.getValue() instanceof String) sb.append("\"").append(v.getValue()).append("\"");
                            else sb.append(v.getValue());
                            innerFirst = false;
                        }
                        sb.append("}");
                        first = false;
                    }
                    sb.append("}");
                    byte[] bytes = sb.toString().getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                } catch (Throwable t) {
                    byte[] err = ("{\"error\":\"" + t.getMessage() + "\"}").getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, err.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(err); }
                }
            });
            s.setExecutor(null);
            s.start();
            server = s;
            logger.info("GameHealthServer listening on :" + port + "/health");
            startWatchdog();
        } catch (IOException e) {
            logger.error("GameHealthServer failed to bind on :" + port, e);
        }
    }

    // SUN-1248: services without a BitZero game loop (e.g. game-thirdparty,
    // pure HTTP integrations) never call tick(), so /health flipped to 503
    // 90s after boot and autoheal restarted them every ~3 minutes. The
    // watchdog auto-ticks every 30s as a floor — explicit tick() calls from
    // game loops still run and add their loop-stall detection on top.
    private static void startWatchdog() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-health-watchdog");
            t.setDaemon(true);
            return t;
        });
        exec.scheduleAtFixedRate(GameHealthServer::tick, 30L, 30L, TimeUnit.SECONDS);
    }

    public static void tick() {
        lastTickMs.set(System.currentTimeMillis());
    }

    public static long tickAgeMs() {
        return System.currentTimeMillis() - lastTickMs.get();
    }
}
