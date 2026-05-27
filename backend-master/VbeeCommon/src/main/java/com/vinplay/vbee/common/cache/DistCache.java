package com.vinplay.vbee.common.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Distributed cache facade — drop-in replacement for the subset of
 * {@link com.hazelcast.core.IMap} operations we actually use in
 * {@code backend-master/}. Two implementations live behind this interface:
 *
 * <ul>
 *   <li>{@link HazelcastCache} — delegates to the existing IMap. The
 *       baseline; flipping a map to this is a no-op.</li>
 *   <li>{@link RedisCache} — Lettuce-backed Redis 7 implementation.
 *       Lock-get-modify-put collapses to atomic Lua.</li>
 * </ul>
 *
 * <p>Selection per map is driven by {@link CacheFactory}, which reads
 * {@code cache.routing.properties} at boot. Default routing is
 * {@code hazelcast} until a map is explicitly flipped — see
 * {@code docs/architecture/HAZELCAST_TO_REDIS_PLAN.md} Phase 4 for the
 * cutover playbook.
 *
 * <p><b>Migration notes for callers:</b>
 * <ul>
 *   <li>Rename {@code HazelcastClientFactory.getInstance().getMap(name)}
 *       to {@code CacheFactory.get(name)}. Call shapes after that line
 *       are unchanged.</li>
 *   <li>The {@code lock(key)} / {@code tryLock(key, ttl, unit)} calls
 *       still work, but consider {@link #acquireLock(Object, long, TimeUnit)}
 *       with try-with-resources for new code — it auto-releases on
 *       exception and avoids the leak class.</li>
 *   <li>{@link #addEntryListener(EntryEventListener)} is intentionally
 *       narrow — it covers the {@code EntryAdapter} usage we have
 *       (3 sites as of the 2026-05-07 audit). Anything more exotic
 *       belongs in a follow-up.</li>
 * </ul>
 */
public interface DistCache<K, V> {

    /** Cache name (matches the IMap name for one-to-one rollback). */
    String getName();

    V get(K key);

    void put(K key, V value);

    /** Put with explicit TTL. {@code ttl <= 0} means no expiry. */
    void put(K key, V value, long ttl, TimeUnit unit);

    /** Returns {@code true} if the key was inserted (didn't already exist). */
    boolean putIfAbsent(K key, V value, long ttl, TimeUnit unit);

    V remove(K key);

    boolean containsKey(K key);

    /**
     * Bulk operations. Hazelcast IMap exposes them as O(1) cluster ops; the
     * Redis implementation translates them to SCAN over {@code cache:<map>:*}
     * which is O(N) on the map's contents. Acceptable for small enumerable
     * caches (e.g. {@code bannerCache} with ~50 entries); audit before
     * applying these patterns to large maps.
     */
    boolean isEmpty();
    void clear();
    Set<K> keySet();
    Set<Map.Entry<K, V>> entrySet();

    /** Mirrors {@code IMap.lock(key)}. Blocks until acquired. */
    void lock(K key);

    /**
     * Mirrors {@code IMap.tryLock(key, timeout, unit)}. Returns {@code true}
     * if acquired within the timeout, {@code false} otherwise.
     */
    boolean tryLock(K key, long timeout, TimeUnit unit);

    /** Mirrors {@code IMap.unlock(key)}. Idempotent on the calling thread. */
    void unlock(K key);

    /**
     * Try-with-resources lock acquisition. Returns {@code null} on timeout;
     * otherwise returns a handle whose {@link LockHandle#close()} releases
     * the lock. Prefer this over {@link #tryLock(Object, long, TimeUnit)}
     * for new code:
     *
     * <pre>{@code
     * try (LockHandle h = cache.acquireLock(key, 5, TimeUnit.SECONDS)) {
     *     if (h == null) {
     *         // timeout
     *         return;
     *     }
     *     // ... critical section ...
     * }
     * }</pre>
     */
    LockHandle acquireLock(K key, long timeout, TimeUnit unit);

    /**
     * Subscribe to add/update/remove events. Mirrors the subset of
     * {@code IMap.addEntryListener} used by {@code UserMissionServiceImpl},
     * {@code ManagerMission}, and {@code SessionKickListener} (3 sites).
     *
     * <p>For {@link RedisCache} this maps to a Redis pub/sub subscription
     * on {@code cache:<map>:events}; for {@link HazelcastCache} it
     * delegates to {@code IMap.addEntryListener}.
     */
    void addEntryListener(EntryEventListener<K, V> listener);

    /** Event types surfaced to {@link EntryEventListener}. */
    enum EventType { ADDED, UPDATED, REMOVED, EVICTED }

    /** Minimal entry-event surface. */
    interface EntryEventListener<K, V> {
        void onEvent(EventType type, K key, V value);
    }
}
