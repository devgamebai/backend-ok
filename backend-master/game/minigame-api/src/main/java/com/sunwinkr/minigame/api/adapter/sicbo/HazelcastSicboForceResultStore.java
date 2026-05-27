package com.sunwinkr.minigame.api.adapter.sicbo;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.sunwinkr.minigame.engine.port.ForceResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Sicbo force-result adapter wrapping the {@code ketquataixiusicbo}
 * Hazelcast IMap. Sicbo and TaiXiu live in distinct maps so admin force
 * pushes don't cross-contaminate.
 *
 * <p>AMBIGUOUS #2 (Sicbo extraction plan §2.2): The legacy module uses
 * a single map name {@code "ketquataixiusicbo"} for the Sicbo force
 * result; both the admin write path ({@code SicboCheatHandler}) and the
 * game-loop read path ({@code MGRoomSicbo.getResult}) agree on this key.
 *
 * <p>Atomic {@code remove} on the singleton key ensures single-use
 * consumption — concurrent consumers see at most one positive observation.
 *
 * <p>Plan §6 / D2 (Sicbo analog).
 */
@Component("sicboForceResultStore")
public class HazelcastSicboForceResultStore implements ForceResultStore {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastSicboForceResultStore.class);

    private static final String MAP_NAME = "ketquataixiusicbo";
    private static final String KEY = "ketquataixiusicbo";

    private final HazelcastInstance hazelcast;

    public HazelcastSicboForceResultStore(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Optional<short[]> peekAndConsume() {
        try {
            IMap<String, short[]> map = hazelcast.getMap(MAP_NAME);
            short[] dice = map.remove(KEY);
            return Optional.ofNullable(dice);
        } catch (Throwable t) {
            LOG.warn("HazelcastSicboForceResultStore.peekAndConsume failed", t);
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
            LOG.warn("HazelcastSicboForceResultStore.set failed", t);
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        }
    }
}
