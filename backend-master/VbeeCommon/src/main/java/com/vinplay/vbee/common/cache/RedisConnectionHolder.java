package com.vinplay.vbee.common.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Single Lettuce {@link RedisClient} + reused {@link StatefulRedisConnection}
 * shared by all {@link RedisCache} instances. Lettuce connections are
 * thread-safe and multiplex commands over one socket — one connection
 * suffices for the entire JVM.
 *
 * <p>Configuration sources, in priority order:
 * <ol>
 *   <li>System property {@code redis.cache.url} — full {@code redis://…} URI</li>
 *   <li>{@code redis.properties} on the classpath
 *       (keys: {@code redis.host}, {@code redis.port}, {@code redis.password},
 *       {@code redis.db})</li>
 *   <li>Defaults: {@code redis://redis:6379/1} — same host as the Streams
 *       work, DB 1 to keep cache keyspace separate from streams</li>
 * </ol>
 *
 * <p>The pub/sub connection is lazily created on first {@code addEntryListener}
 * call and reused thereafter (Lettuce supports many channel subscriptions on
 * a single pub/sub connection).
 */
final class RedisConnectionHolder {

    private static final Logger logger = Logger.getLogger("cache");

    private static volatile RedisConnectionHolder INSTANCE;

    static RedisConnectionHolder getInstance() {
        RedisConnectionHolder local = INSTANCE;
        if (local == null) {
            synchronized (RedisConnectionHolder.class) {
                local = INSTANCE;
                if (local == null) {
                    INSTANCE = local = new RedisConnectionHolder();
                }
            }
        }
        return local;
    }

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private volatile StatefulRedisPubSubConnection<String, String> pubSub;

    private RedisConnectionHolder() {
        RedisURI uri = resolveUri();
        this.client = RedisClient.create(uri);
        this.connection = client.connect();
        logger.info("RedisConnectionHolder: connected to " + safeUri(uri));
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "redis-cache-shutdown"));
    }

    private RedisURI resolveUri() {
        // Source priority: -Dredis.cache.url > redis.properties > env vars > defaults
        String sys = System.getProperty("redis.cache.url");
        if (sys != null && !sys.isEmpty()) {
            return RedisURI.create(sys);
        }
        Properties p = new Properties();
        try (java.io.InputStream in = RedisConnectionHolder.class.getClassLoader()
                .getResourceAsStream("redis.properties")) {
            if (in != null) p.load(in);
        } catch (Exception e) {
            logger.warn("RedisConnectionHolder: redis.properties read error — using defaults: "
                    + e.getMessage());
        }
        // Env vars match the rest of the stack (.env → docker-compose):
        //   REDIS_STREAMS_HOST, REDIS_STREAMS_PORT, REDIS_PASSWORD
        // The cache uses a different DB index than streams (configurable
        // via redis.cache.db / REDIS_CACHE_DB) so keyspace stays clean.
        String host = pick(System.getenv("REDIS_STREAMS_HOST"),
                p.getProperty("redis.host"), "redis");
        int port = Integer.parseInt(pick(System.getenv("REDIS_STREAMS_PORT"),
                p.getProperty("redis.port"), "6379"));
        int db = Integer.parseInt(pick(System.getenv("REDIS_CACHE_DB"),
                p.getProperty("redis.cache.db"),
                p.getProperty("redis.db"), "1"));
        String pwd = pick(System.getenv("REDIS_PASSWORD"),
                p.getProperty("redis.password"), "");
        RedisURI.Builder b = RedisURI.builder().withHost(host).withPort(port).withDatabase(db);
        if (!pwd.isEmpty()) b.withPassword(pwd.toCharArray());
        return b.build();
    }

    /** Returns the first non-empty value, or empty string. */
    private static String pick(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isEmpty()) return c;
        }
        return "";
    }

    StatefulRedisConnection<String, String> connection() {
        return connection;
    }

    StatefulRedisPubSubConnection<String, String> pubSubConnection() {
        StatefulRedisPubSubConnection<String, String> local = pubSub;
        if (local == null) {
            synchronized (this) {
                local = pubSub;
                if (local == null) {
                    pubSub = local = client.connectPubSub();
                    logger.info("RedisConnectionHolder: pub/sub connection opened");
                }
            }
        }
        return local;
    }

    private void shutdown() {
        try { if (pubSub != null) pubSub.close(); } catch (Exception ignore) {}
        try { connection.close(); } catch (Exception ignore) {}
        try { client.shutdown(); } catch (Exception ignore) {}
    }

    private static String safeUri(RedisURI uri) {
        return uri.getHost() + ":" + uri.getPort() + "/db" + uri.getDatabase();
    }
}
