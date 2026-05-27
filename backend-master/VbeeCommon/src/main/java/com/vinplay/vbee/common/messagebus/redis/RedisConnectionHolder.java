package com.vinplay.vbee.common.messagebus.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide singleton holder for the Lettuce {@link StatefulRedisConnection}
 * used by the Redis Streams transport.
 *
 * <h2>Why one connection</h2>
 * Lettuce is built around a single multiplexed Netty connection that is
 * thread-safe by design &mdash; the synchronous, asynchronous, and reactive
 * command interfaces all share it and a single connection sustains
 * &gt;50 000 commands/sec on a properly tuned host. Opening one connection per
 * publish (or per producer instance) would gain nothing and cost a TCP+RESP
 * handshake on every send. Sharing a single connection across the JVM
 * matches Lettuce's documented usage pattern.
 *
 * <h2>Lazy init</h2>
 * The connection is opened on the first call to {@link #get()}. Synchronous
 * locking on the class is fine because (a) the cost only hits the first
 * caller, and (b) once the volatile reference is non-null subsequent calls
 * read it without re-entering the synchronized block. We don't use a
 * double-checked-locking idiom because the synchronization cost on this code
 * path is dominated by the network call that follows; a coarse lock is
 * simpler and equally correct.
 *
 * <h2>Codec choice &mdash; ByteArrayCodec</h2>
 * The transport carries {@link com.vinplay.vbee.common.messages.BaseMessage#toBytes()}
 * raw Java-serialization bytes. Those bytes are <em>not</em> valid UTF-8 and
 * the default Lettuce codec ({@code StringCodec.UTF8}) would mangle them on
 * the wire by replacing invalid sequences with U+FFFD on decode. Pinning
 * {@link ByteArrayCodec#INSTANCE} keeps the payload bit-exact end-to-end.
 *
 * <h2>Configuration</h2>
 * Reads four environment variables on first use; defaults match the
 * docker-compose layout from P3 ({@code .env.example}):
 * <table>
 *   <caption>Environment variables read at first {@link #get()} call</caption>
 *   <tr><th>Variable</th><th>Default</th><th>Notes</th></tr>
 *   <tr><td>{@code REDIS_STREAMS_HOST}</td><td>{@code redis}</td>
 *       <td>compose service name on the {@code messaging} network.</td></tr>
 *   <tr><td>{@code REDIS_STREAMS_PORT}</td><td>{@code 6379}</td>
 *       <td>standard Redis port; non-numeric values fall back to default.</td></tr>
 *   <tr><td>{@code REDIS_STREAMS_PASSWORD}</td><td>(unset)</td>
 *       <td>resolved upstream from {@code ${REDIS_PASSWORD}} via the
 *       compose env file. Empty/null &rArr; no AUTH sent.</td></tr>
 *   <tr><td>{@code REDIS_STREAMS_DB}</td><td>{@code 1}</td>
 *       <td>kept off DB 0 (cache traffic). DB 1 is reserved for streams.</td></tr>
 * </table>
 *
 * <h2>Lifecycle</h2>
 * {@link #shutdown()} is idempotent. It closes the connection and the client
 * in that order &mdash; closing the client first would race with the
 * connection's own close. Subsequent calls to {@link #get()} after shutdown
 * will reopen a fresh connection.
 */
public final class RedisConnectionHolder {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionHolder.class);

    private static final String DEFAULT_HOST = "redis";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DB = 1;

    private static RedisClient client;
    private static StatefulRedisConnection<byte[], byte[]> connection;

    /**
     * Returns the shared {@link StatefulRedisConnection}, opening it on first
     * call.
     *
     * <p>The returned connection is thread-safe and intended to be reused
     * indefinitely. Callers should NOT call {@code close()} on it &mdash; use
     * {@link #shutdown()} instead, which is driven by the JVM shutdown hook.
     *
     * @return a live, open Lettuce connection backed by
     *         {@link ByteArrayCodec#INSTANCE}; never null.
     * @throws io.lettuce.core.RedisConnectionException if the initial
     *         connect fails. Callers in the message-bus adapters catch this
     *         and swallow per the {@code MessageBus#publish} contract.
     */
    public static synchronized StatefulRedisConnection<byte[], byte[]> get() {
        if (connection != null && connection.isOpen()) {
            return connection;
        }
        // Either first call, or previous connection was closed (shutdown
        // followed by a stray late publisher). Build fresh.
        RedisURI uri = buildUri();
        client = RedisClient.create(uri);
        connection = client.connect(ByteArrayCodec.INSTANCE);
        logger.info("RedisConnectionHolder opened connection host={} port={} db={}",
                uri.getHost(), uri.getPort(), uri.getDatabase());
        return connection;
    }

    /**
     * Close the shared connection and release the client's Netty resources.
     *
     * <p>Idempotent: a second invocation is a no-op. Safe to call from the
     * JVM shutdown hook.
     */
    public static synchronized void shutdown() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Throwable t) {
            logger.warn("RedisConnectionHolder.shutdown: connection close failed: {}", t.getMessage());
        } finally {
            connection = null;
        }
        try {
            if (client != null) {
                client.shutdown();
            }
        } catch (Throwable t) {
            logger.warn("RedisConnectionHolder.shutdown: client shutdown failed: {}", t.getMessage());
        } finally {
            client = null;
        }
    }

    private static RedisURI buildUri() {
        String host = envOr("REDIS_STREAMS_HOST", DEFAULT_HOST);
        int port = parseIntEnv("REDIS_STREAMS_PORT", DEFAULT_PORT);
        int db = parseIntEnv("REDIS_STREAMS_DB", DEFAULT_DB);
        String password = System.getenv("REDIS_STREAMS_PASSWORD");

        RedisURI.Builder b = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(db);
        if (password != null && !password.isEmpty()) {
            // char[] overload — Lettuce zeros the array internally after use.
            b.withPassword(password.toCharArray());
        }
        return b.build();
    }

    private static String envOr(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private static int parseIntEnv(String name, int fallback) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            logger.warn("RedisConnectionHolder: {}='{}' is not an int; falling back to {}",
                    name, v, fallback);
            return fallback;
        }
    }

    private RedisConnectionHolder() {
        // singleton holder — no instances
    }
}
