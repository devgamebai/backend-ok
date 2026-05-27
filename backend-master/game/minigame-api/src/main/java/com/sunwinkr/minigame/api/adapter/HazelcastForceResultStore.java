package com.sunwinkr.minigame.api.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter wrapping the {@code ketquataixiu} Hazelcast IMap (spec §4 +
 * INV-3). Atomic {@code remove} on the singleton key ensures single-use
 * consumption — concurrent consumers see at most one positive observation.
 *
 * <p>Plan §4.2 / D2.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when stale
 * compiled @Bean wrappers (forceResultStore, sicboForceResultStore) coexist
 * with the @Component adapters. This is the TaiXiu-canonical ForceResultStore.
 * Sicbo injections use @Qualifier("sicboForceResultStore") to bypass @Primary.
 */
@Primary
@Component
public class HazelcastForceResultStore implements ForceResultStore {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastForceResultStore.class);

    private static final String MAP_NAME = "ketquataixiu";
    private static final String KEY = "ketquataixiu";

    private final HazelcastInstance hazelcast;

    public HazelcastForceResultStore(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Optional<short[]> peekAndConsume() {
        try {
            IMap<String, short[]> map = hazelcast.getMap(MAP_NAME);
            short[] dice = map.remove(KEY);
            return Optional.ofNullable(dice);
        } catch (Throwable t) {
            LOG.warn("HazelcastForceResultStore.peekAndConsume failed", t);
            return Optional.empty();
        }
    }

    @Override
    public void set(short[] dice) {
        if (dice == null || dice.length < 3) {
            throw new IllegalArgumentException("dice must be length >= 3");
        }
        try {
            IMap<String, short[]> map = hazelcast.getMap(MAP_NAME);
            map.put(KEY, new short[] { dice[0], dice[1], dice[2] });
        } catch (Throwable t) {
            LOG.warn("HazelcastForceResultStore.set failed", t);
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        }
    }
}
