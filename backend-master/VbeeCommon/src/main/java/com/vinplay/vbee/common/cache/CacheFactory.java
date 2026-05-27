package com.vinplay.vbee.common.cache;

import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Entry point for distributed-cache access. Replaces
 * {@code HazelcastClientFactory.getInstance().getMap(name)} at every call site.
 *
 * <pre>{@code
 * // Before:
 * IMap<String, UserCacheModel> map = HazelcastClientFactory.getInstance().getMap("users");
 *
 * // After:
 * DistCache<String, UserCacheModel> map = CacheFactory.get("users", UserCacheModel.class);
 * }</pre>
 *
 * <p>The {@code valueClass} argument is required for the Redis backend
 * (Jackson needs the target type to deserialize); Hazelcast ignores it.
 *
 * <p>Routing per map is decided by {@link CacheRouter} at first access and
 * cached in the per-name {@link DistCache} instance. Restart to flip a map.
 *
 * <p>Untyped overload {@link #get(String)} is kept for migration legibility
 * — call sites that pass {@code Object} or use raw types can flip without
 * a Class.forName() ceremony, at the cost of giving up Jackson type info on
 * Redis (deserializes to {@code LinkedHashMap}). Audit and tighten in Phase 4.
 */
public final class CacheFactory {

    private static final Logger logger = Logger.getLogger("cache");

    private static final ConcurrentMap<String, DistCache<?, ?>> instances =
            new ConcurrentHashMap<>();

    public static <K, V> DistCache<K, V> get(String name, Class<V> valueClass) {
        @SuppressWarnings("unchecked")
        DistCache<K, V> existing = (DistCache<K, V>) instances.get(name);
        if (existing != null) return existing;
        return create(name, valueClass);
    }

    public static <K, V> DistCache<K, V> get(String name) {
        return get(name, null);
    }

    private static synchronized <K, V> DistCache<K, V> create(String name, Class<V> valueClass) {
        @SuppressWarnings("unchecked")
        DistCache<K, V> existing = (DistCache<K, V>) instances.get(name);
        if (existing != null) return existing;

        CacheRouter.Backend backend = CacheRouter.backendFor(name);
        DistCache<K, V> impl;
        switch (backend) {
            case REDIS:
                impl = new RedisCache<>(name, valueClass);
                logger.info("CacheFactory: cache:" + name + " → REDIS");
                break;
            case HAZELCAST:
            default:
                impl = new HazelcastCache<>(name);
                logger.info("CacheFactory: cache:" + name + " → HAZELCAST");
                break;
        }
        instances.put(name, impl);
        return impl;
    }

    private CacheFactory() {}
}
