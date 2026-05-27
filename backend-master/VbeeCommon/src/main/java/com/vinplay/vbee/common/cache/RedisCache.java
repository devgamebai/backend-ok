package com.vinplay.vbee.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.apache.log4j.Logger;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Lettuce-backed {@link DistCache} implementation. JSON serialization via
 * Jackson keyed under {@code cache:<map>:<key>}. Locks use atomic
 * {@code SET NX PX} with a per-acquisition UUID token; release runs a Lua
 * script that only deletes if the token still matches (prevents
 * unlock-by-other when a TTL expires mid-critical-section).
 *
 * <p>Per-thread lock ownership tracking lets the {@code lock(key)} /
 * {@code tryLock(...)} / {@code unlock(key)} call shape match Hazelcast 1:1
 * for non-AutoCloseable callers. New code should prefer
 * {@link #acquireLock(Object, long, TimeUnit)}.
 *
 * <p>Cluster events (for {@link EntryEventListener}) ride a Redis pub/sub
 * channel {@code cache:<map>:events}. Writers publish a small JSON envelope
 * after the SET; subscribers translate back to {@link EventType}.
 */
final class RedisCache<K, V> implements DistCache<K, V> {

    private static final Logger logger = Logger.getLogger("cache");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Default lock TTL — long enough to outlast any reasonable critical section. */
    private static final long DEFAULT_LOCK_TTL_MS = 30_000L;
    /** Lua: atomic GET-and-DELETE; returns the prior value (or nil if absent).
     *  Ensures `remove(K)` is single-round-trip atomic — required by callers
     *  that rely on remove-returns-prev as a serialization point (e.g. the
     *  cacheApiOtp single-use OTP consume in RechargeServiceImpl). Without
     *  this script, two concurrent removers could both observe a non-null
     *  prior value before either DEL runs, causing double-spend. */
    private static final String GET_AND_DEL_LUA =
            "local v = redis.call('GET', KEYS[1]) "
          + "if v then redis.call('DEL', KEYS[1]) end "
          + "return v";
    /** Lua: delete only if value matches our token. */
    private static final String RELEASE_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
          + "  return redis.call('del', KEYS[1]) "
          + "else "
          + "  return 0 "
          + "end";

    private final String name;
    private final Class<V> valueClass;
    /** Tracks "this thread holds the lock for <key> with <token>" for the
     *  Hazelcast-shaped {@code lock/unlock} API. {@link #acquireLock} bypasses
     *  this map and stores the token directly on the returned handle. */
    private final ThreadLocal<Map<K, String>> ownedLocks =
            ThreadLocal.withInitial(HashMap::new);

    RedisCache(String name, Class<V> valueClass) {
        this.name = name;
        this.valueClass = valueClass;
    }

    private String dataKey(K key) {
        return "cache:" + name + ":" + String.valueOf(key);
    }
    private String lockKey(K key) {
        return "cache:" + name + ":lock:" + String.valueOf(key);
    }
    private String eventChannel() {
        return "cache:" + name + ":events";
    }

    @Override public String getName() { return name; }

    @Override
    public V get(K key) {
        try {
            String raw = sync().get(dataKey(key));
            if (raw == null) return null;
            return decode(raw);
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].get error: " + e.getMessage());
            return null;
        }
    }

    @Override public void put(K key, V value) { put(key, value, 0L, null); }

    @Override
    public void put(K key, V value, long ttl, TimeUnit unit) {
        try {
            String raw = encode(value);
            if (ttl <= 0) {
                sync().set(dataKey(key), raw);
            } else {
                long ms = (unit == null ? TimeUnit.MILLISECONDS : unit).toMillis(ttl);
                sync().psetex(dataKey(key), ms, raw);
            }
            publish(EventType.ADDED, key);
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].put error: " + e.getMessage());
        }
    }

    @Override
    public boolean putIfAbsent(K key, V value, long ttl, TimeUnit unit) {
        try {
            String raw = encode(value);
            SetArgs args = new SetArgs().nx();
            if (ttl > 0) {
                args.px((unit == null ? TimeUnit.MILLISECONDS : unit).toMillis(ttl));
            }
            String result = sync().set(dataKey(key), raw, args);
            boolean inserted = "OK".equals(result);
            if (inserted) publish(EventType.ADDED, key);
            return inserted;
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].putIfAbsent error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public V remove(K key) {
        try {
            // SUN-1248 Task H: atomic GET+DEL via Lua. Single round-trip;
            // returns prior value or null. Required for callers that use
            // remove-returns-prev as a one-shot consume primitive (e.g.
            // RechargeServiceImpl cacheApiOtp single-use OTP consume).
            // Replaces the prior get-then-del 2-call sequence which was
            // racy under concurrent removers.
            String raw = sync().eval(GET_AND_DEL_LUA,
                    io.lettuce.core.ScriptOutputType.VALUE,
                    new String[]{ dataKey(key) });
            V prev = raw != null ? decode(raw) : null;
            if (prev != null) publish(EventType.REMOVED, key);
            return prev;
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].remove error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean containsKey(K key) {
        try {
            return sync().exists(dataKey(key)) > 0;
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].containsKey error: " + e.getMessage());
            return false;
        }
    }

    /** Match pattern for SCAN over this cache's data keys (skips the lock keyspace). */
    private String dataScanPattern() { return "cache:" + name + ":*"; }
    private String lockKeyPrefix() { return "cache:" + name + ":lock:"; }

    @Override
    public boolean isEmpty() {
        try {
            ScanArgs args = ScanArgs.Builder.matches(dataScanPattern()).limit(64);
            KeyScanCursor<String> cursor = sync().scan(args);
            // First batch decides — Redis SCAN may return empty pages even when
            // matches exist later, so iterate until we see a key OR finish.
            while (true) {
                for (String k : cursor.getKeys()) {
                    // Skip locks — they share the namespace prefix.
                    if (!k.startsWith(lockKeyPrefix())) return false;
                }
                if (cursor.isFinished()) return true;
                cursor = sync().scan(cursor, args);
            }
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].isEmpty error: " + e.getMessage());
            return true;  // fail-safe to "empty" so callers refill from DB
        }
    }

    @Override
    public void clear() {
        try {
            ScanArgs args = ScanArgs.Builder.matches(dataScanPattern()).limit(256);
            KeyScanCursor<String> cursor = sync().scan(args);
            while (true) {
                List<String> keys = cursor.getKeys();
                // Strip locks so a clear() on the data set never wipes
                // in-flight critical-section locks.
                String[] toDelete = keys.stream()
                        .filter(k -> !k.startsWith(lockKeyPrefix()))
                        .toArray(String[]::new);
                if (toDelete.length > 0) sync().del(toDelete);
                if (cursor.isFinished()) break;
                cursor = sync().scan(cursor, args);
            }
            publish(EventType.REMOVED, null);
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].clear error: " + e.getMessage());
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> out = new LinkedHashSet<>();
        try {
            ScanArgs args = ScanArgs.Builder.matches(dataScanPattern()).limit(256);
            KeyScanCursor<String> cursor = sync().scan(args);
            String prefix = "cache:" + name + ":";
            while (true) {
                for (String fullKey : cursor.getKeys()) {
                    if (fullKey.startsWith(lockKeyPrefix())) continue;
                    @SuppressWarnings("unchecked")
                    K typedKey = (K) fullKey.substring(prefix.length());
                    out.add(typedKey);
                }
                if (cursor.isFinished()) break;
                cursor = sync().scan(cursor, args);
            }
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].keySet error: " + e.getMessage());
        }
        return out;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> out = new LinkedHashSet<>();
        try {
            ScanArgs args = ScanArgs.Builder.matches(dataScanPattern()).limit(256);
            KeyScanCursor<String> cursor = sync().scan(args);
            String prefix = "cache:" + name + ":";
            while (true) {
                for (String fullKey : cursor.getKeys()) {
                    if (fullKey.startsWith(lockKeyPrefix())) continue;
                    String suffix = fullKey.substring(prefix.length());
                    String raw = sync().get(fullKey);
                    if (raw == null) continue;
                    try {
                        @SuppressWarnings("unchecked")
                        K typedKey = (K) suffix;  // String-keyed maps are the
                        // common case in our codebase. Non-String keys would
                        // need a serializer — flag in audit when it shows up.
                        V value = decode(raw);
                        out.add(new AbstractMap.SimpleImmutableEntry<>(typedKey, value));
                    } catch (Exception decodeErr) {
                        logger.warn("RedisCache[" + name + "].entrySet decode error for "
                                + fullKey + ": " + decodeErr.getMessage());
                    }
                }
                if (cursor.isFinished()) break;
                cursor = sync().scan(cursor, args);
            }
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].entrySet error: " + e.getMessage());
        }
        return out;
    }

    @Override
    public void lock(K key) {
        // Block until acquired. Uses 30s TTL as a safety net so a crashed
        // holder can't deadlock the cluster forever — pick a value larger
        // than any expected critical section but smaller than infinity.
        long sleepMs = 5;
        while (!tryAcquire(key, DEFAULT_LOCK_TTL_MS)) {
            try { Thread.sleep(sleepMs); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            sleepMs = Math.min(sleepMs * 2, 200);
        }
    }

    @Override
    public boolean tryLock(K key, long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis()
                + (unit == null ? TimeUnit.SECONDS : unit).toMillis(timeout);
        long sleepMs = 5;
        while (true) {
            if (tryAcquire(key, DEFAULT_LOCK_TTL_MS)) return true;
            if (System.currentTimeMillis() >= deadline) return false;
            try { Thread.sleep(sleepMs); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            sleepMs = Math.min(sleepMs * 2, 200);
        }
    }

    @Override
    public void unlock(K key) {
        Map<K, String> owned = ownedLocks.get();
        String token = owned.remove(key);
        if (token == null) {
            // Not owned by this thread — Hazelcast would throw IllegalMonitorStateException;
            // we log and move on for graceful migration. Catch this in tests.
            logger.warn("RedisCache[" + name + "].unlock: thread does not own key " + key);
            return;
        }
        releaseToken(key, token);
    }

    @Override
    public LockHandle acquireLock(K key, long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis()
                + (unit == null ? TimeUnit.SECONDS : unit).toMillis(timeout);
        String token = UUID.randomUUID().toString();
        long sleepMs = 5;
        while (true) {
            if (tryAcquireWithToken(key, token, DEFAULT_LOCK_TTL_MS)) {
                return () -> releaseToken(key, token);
            }
            if (System.currentTimeMillis() >= deadline) return null;
            try { Thread.sleep(sleepMs); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
            sleepMs = Math.min(sleepMs * 2, 200);
        }
    }

    private boolean tryAcquire(K key, long ttlMs) {
        String token = UUID.randomUUID().toString();
        if (tryAcquireWithToken(key, token, ttlMs)) {
            ownedLocks.get().put(key, token);
            return true;
        }
        return false;
    }

    private boolean tryAcquireWithToken(K key, String token, long ttlMs) {
        try {
            String result = sync().set(lockKey(key), token, new SetArgs().nx().px(ttlMs));
            return "OK".equals(result);
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].tryAcquire error: " + e.getMessage());
            return false;
        }
    }

    private void releaseToken(K key, String token) {
        try {
            sync().eval(RELEASE_LUA,
                    io.lettuce.core.ScriptOutputType.INTEGER,
                    new String[]{ lockKey(key) },
                    token);
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].releaseToken error: " + e.getMessage());
        }
    }

    @Override
    public void addEntryListener(EntryEventListener<K, V> listener) {
        StatefulRedisPubSubConnection<String, String> sub =
                RedisConnectionHolder.getInstance().pubSubConnection();
        sub.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                if (!eventChannel().equals(channel)) return;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> env = MAPPER.readValue(message, Map.class);
                    EventType type = EventType.valueOf((String) env.get("type"));
                    @SuppressWarnings("unchecked")
                    K key = (K) env.get("key");
                    V value = key == null ? null : get(key);
                    listener.onEvent(type, key, value);
                } catch (Exception e) {
                    logger.warn("RedisCache[" + name + "].onMessage decode error: " + e.getMessage());
                }
            }
        });
        sub.sync().subscribe(eventChannel());
    }

    private void publish(EventType type, K key) {
        try {
            Map<String, Object> env = new HashMap<>();
            env.put("type", type.name());
            env.put("key", key);
            String payload = MAPPER.writeValueAsString(env);
            asyncPublish(eventChannel(), payload);
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].publish error: " + e.getMessage());
        }
    }

    private void asyncPublish(String channel, String payload) {
        try {
            RedisAsyncCommands<String, String> async =
                    RedisConnectionHolder.getInstance().connection().async();
            RedisFuture<Long> future = async.publish(channel, payload);
            // Fire-and-forget; failures are logged via the future's listener.
            future.exceptionally(t -> {
                logger.warn("RedisCache[" + name + "].publish async error: " + t.getMessage());
                return null;
            });
        } catch (Exception e) {
            logger.warn("RedisCache[" + name + "].asyncPublish error: " + e.getMessage());
        }
    }

    private RedisCommands<String, String> sync() {
        return RedisConnectionHolder.getInstance().connection().sync();
    }

    private String encode(V value) throws Exception {
        return MAPPER.writeValueAsString(value);
    }

    @SuppressWarnings("unchecked")
    private V decode(String raw) throws Exception {
        if (valueClass == null) return (V) MAPPER.readValue(raw, Object.class);
        return MAPPER.readValue(raw, valueClass);
    }
}
