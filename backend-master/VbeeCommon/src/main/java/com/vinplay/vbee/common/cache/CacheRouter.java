package com.vinplay.vbee.common.cache;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Reads {@code cache.routing.properties} from the classpath and answers
 * "which backend serves cache:<name>?". Mirrors the per-queue routing
 * pattern used by {@code MessageBusFactory} — flip one line, restart, done.
 *
 * <p>File format (root of {@code /app/config}):
 * <pre>
 * cache.default=hazelcast
 * cache.bannerCache=redis        # already cut over
 * cache.jackpottaixiu=redis
 * cache.users=hazelcast          # last to flip — see HAZELCAST_TO_REDIS_PLAN.md Wave 4
 * </pre>
 *
 * <p>Default behavior when the file is missing or a key is absent:
 * {@link Backend#HAZELCAST}. Default-deny on the migration — a typo or a
 * lost config file never accidentally cuts a map over to Redis.
 *
 * <p>Hot-reload is intentionally NOT supported. Routing changes happen
 * via container restart so the rollback playbook stays simple
 * (flag → restart → done, both during cutover and during incident response).
 */
final class CacheRouter {

    private static final Logger logger = Logger.getLogger("cache");

    enum Backend { HAZELCAST, REDIS }

    private static final Properties props = new Properties();
    private static final Backend defaultBackend;

    static {
        // System property override allows local dev / tests to force a
        // backend without editing the file: -Dcache.routing.default=redis
        try (InputStream in = CacheRouter.class.getClassLoader()
                .getResourceAsStream("cache.routing.properties")) {
            if (in != null) {
                props.load(in);
                logger.info("CacheRouter: loaded cache.routing.properties (" + props.size() + " entries)");
            } else {
                logger.info("CacheRouter: cache.routing.properties not found on classpath; "
                        + "all maps default to HAZELCAST");
            }
        } catch (Exception e) {
            logger.warn("CacheRouter: failed to read cache.routing.properties — "
                    + "all maps default to HAZELCAST: " + e.getMessage());
        }

        String def = System.getProperty("cache.routing.default",
                props.getProperty("cache.default", "hazelcast"));
        defaultBackend = parse(def, Backend.HAZELCAST);
    }

    static Backend backendFor(String mapName) {
        if (mapName == null || mapName.isEmpty()) return defaultBackend;
        // System property per-map override: -Dcache.users=redis
        String sys = System.getProperty("cache." + mapName);
        if (sys != null) return parse(sys, defaultBackend);
        String val = props.getProperty("cache." + mapName);
        if (val == null) return defaultBackend;
        return parse(val, defaultBackend);
    }

    private static Backend parse(String s, Backend fallback) {
        if (s == null) return fallback;
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "redis":     return Backend.REDIS;
            case "hazelcast": return Backend.HAZELCAST;
            default:
                logger.warn("CacheRouter: unknown backend '" + s + "', using " + fallback);
                return fallback;
        }
    }

    private CacheRouter() {}
}
