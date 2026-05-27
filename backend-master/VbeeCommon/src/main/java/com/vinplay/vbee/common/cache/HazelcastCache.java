package com.vinplay.vbee.common.cache;

import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * No-op delegate to the existing {@link IMap}. Switching a map to this is
 * the baseline and the rollback target — call shapes match Hazelcast 1:1.
 */
final class HazelcastCache<K, V> implements DistCache<K, V> {

    private final String name;
    private final IMap<K, V> map;

    HazelcastCache(String name) {
        this.name = name;
        this.map = HazelcastClientFactory.getInstance().getMap(name);
    }

    @Override public String getName() { return name; }

    @Override public V get(K key) { return map.get(key); }

    @Override public void put(K key, V value) { map.put(key, value); }

    @Override
    public void put(K key, V value, long ttl, TimeUnit unit) {
        if (ttl <= 0) {
            map.put(key, value);
        } else {
            map.put(key, value, ttl, unit);
        }
    }

    @Override
    public boolean putIfAbsent(K key, V value, long ttl, TimeUnit unit) {
        // IMap.putIfAbsent returns the previous value (or null if inserted).
        return map.putIfAbsent(key, value, ttl <= 0 ? 0 : ttl, unit == null ? TimeUnit.SECONDS : unit) == null;
    }

    @Override public V remove(K key) { return map.remove(key); }

    @Override public boolean containsKey(K key) { return map.containsKey(key); }

    @Override public boolean isEmpty() { return map.isEmpty(); }

    @Override public void clear() { map.clear(); }

    @Override public Set<K> keySet() { return new LinkedHashSet<>(map.keySet()); }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        // Defensive copy: IMap.entrySet() returns a snapshot, but typing it
        // through Map.Entry<K,V> avoids leaking IMap-specific Entry classes
        // to callers that the Redis impl can't produce.
        Set<Map.Entry<K, V>> out = new LinkedHashSet<>();
        for (Map.Entry<K, V> e : map.entrySet()) {
            out.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }
        return out;
    }

    @Override public void lock(K key) { map.lock(key); }

    @Override
    public boolean tryLock(K key, long timeout, TimeUnit unit) {
        try {
            return map.tryLock(key, timeout, unit);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override public void unlock(K key) { map.unlock(key); }

    @Override
    public LockHandle acquireLock(K key, long timeout, TimeUnit unit) {
        if (!tryLock(key, timeout, unit)) return null;
        return () -> {
            try { map.unlock(key); } catch (Exception ignore) {}
        };
    }

    @Override
    public void addEntryListener(EntryEventListener<K, V> listener) {
        map.addEntryListener(new EntryAdapter<K, V>() {
            @Override public void entryAdded(EntryEvent<K, V> e) {
                listener.onEvent(EventType.ADDED, e.getKey(), e.getValue());
            }
            @Override public void entryUpdated(EntryEvent<K, V> e) {
                listener.onEvent(EventType.UPDATED, e.getKey(), e.getValue());
            }
            @Override public void entryRemoved(EntryEvent<K, V> e) {
                listener.onEvent(EventType.REMOVED, e.getKey(), e.getOldValue());
            }
            @Override public void entryEvicted(EntryEvent<K, V> e) {
                listener.onEvent(EventType.EVICTED, e.getKey(), e.getOldValue());
            }
        }, true);
    }
}
