package com.vinplay.drainer;

import org.apache.log4j.Logger;

/**
 * SUN-1112 (CQRS Phase 0) — drainer entry point, idle shell.
 *
 * <p>This Phase 0 build deploys but does no work. It exists to validate
 * the build pipeline, container packaging, compose entry, logging stack,
 * and operational placement before any commission-history logic is added.
 *
 * <p>Phase 1 will replace the idle loop with the actual drain logic:
 * read PENDING rows from {@code vinplay.commission_history_outbox},
 * upsert into MongoDB {@code vinplay.commission_history} keyed by
 * {@code (bet_event_id, agent_id)}, mark DONE, retry on failure with
 * exponential backoff. See SUN-1111 for the full design.
 *
 * <p>Operational toggles:
 * <ul>
 *   <li>{@code DRAINER_ENABLED} — when false (default), main() heartbeats
 *       and exits the loop only on SIGTERM. Lets the container stay up
 *       in compose without doing anything.</li>
 *   <li>{@code DRAINER_HEARTBEAT_SEC} — heartbeat cadence in idle mode.
 *       Default 60.</li>
 * </ul>
 */
public final class HistoryDrainerMain {

    private static final Logger logger = Logger.getLogger("drainer");

    private HistoryDrainerMain() {}

    public static void main(String[] args) {
        long startMs = System.currentTimeMillis();
        boolean enabled = parseBoolEnv("DRAINER_ENABLED", false);
        int heartbeatSec = parseIntEnv("DRAINER_HEARTBEAT_SEC", 60);

        logger.info("HistoryDrainer starting — enabled=" + enabled
                + " heartbeat=" + heartbeatSec + "s");
        System.out.println("[DRAINER] starting enabled=" + enabled
                + " heartbeat=" + heartbeatSec + "s");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                long upMs = System.currentTimeMillis() - startMs;
                logger.info("HistoryDrainer shutdown — uptime " + upMs + "ms");
                System.out.println("[DRAINER] shutdown uptime=" + upMs + "ms");
            }
        }, "drainer-shutdown"));

        if (!enabled) {
            idle(heartbeatSec);
            return;
        }

        // Phase 1+ replaces this with real drain loop. For Phase 0 a flipped
        // flag still drops to idle so a misconfigured prod doesn't silently
        // start dual-write behavior before the drain logic is in place.
        logger.warn("DRAINER_ENABLED=true but Phase 1 logic not yet shipped — staying idle");
        System.out.println("[DRAINER] enabled but no logic shipped — idling");
        idle(heartbeatSec);
    }

    private static void idle(int heartbeatSec) {
        long ticks = 0;
        while (true) {
            try {
                Thread.sleep(heartbeatSec * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.info("HistoryDrainer interrupted, exiting idle");
                return;
            }
            ticks++;
            logger.info("HistoryDrainer idle heartbeat tick=" + ticks);
        }
    }

    private static boolean parseBoolEnv(String key, boolean def) {
        String v = System.getenv(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    private static int parseIntEnv(String key, int def) {
        String v = System.getenv(key);
        if (v == null) return def;
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { return def; }
    }
}
