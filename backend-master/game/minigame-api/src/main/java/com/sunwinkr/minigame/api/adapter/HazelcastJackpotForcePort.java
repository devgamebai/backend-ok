package com.sunwinkr.minigame.api.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.sunwinkr.minigame.engine.port.JackpotForcePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter wrapping the {@code jackpottaixiu} Hazelcast IMap (spec §4).
 *
 * <h3>Peek-without-remove note (TXM:399)</h3>
 * For bot pre-checks the legacy code does a non-destructive read via
 * {@code map.get} — when the production gameLoop later evaluates the
 * actual gate at TXR:594-615 it does the atomic remove. We expose the
 * {@link #peekJackpotSide()} as the destructive atomic remove because
 * the engine's {@link com.sunwinkr.minigame.engine.jackpot
 * .JackpotTriggerPolicy} is the gate, NOT the bot pre-check. The bot
 * pre-check path is handled by the BitZero bridge directly.
 *
 * <p>Plan §2.5 / J3.
 *
 * <p>{@code @Primary}: resolves NoUniqueBeanDefinitionException when the
 * stale compiled EngineConfig {@code jackpotForcePort} @Bean wrapper coexists
 * with this @Component. This is the canonical JackpotForcePort.
 */
@Primary
@Component
public class HazelcastJackpotForcePort implements JackpotForcePort {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastJackpotForcePort.class);

    private static final String MAP_NAME = "jackpottaixiu";
    private static final String KEY = "jackpottaixiu";

    private final HazelcastInstance hazelcast;

    public HazelcastJackpotForcePort(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public Optional<Short> peekJackpotSide() {
        try {
            IMap<String, Short> map = hazelcast.getMap(MAP_NAME);
            Short value = map.remove(KEY);
            return Optional.ofNullable(value);
        } catch (Throwable t) {
            LOG.warn("HazelcastJackpotForcePort.peekJackpotSide failed", t);
            return Optional.empty();
        }
    }

    /**
     * Bot-pre-check seam: non-destructive read used by the BitZero bridge
     * at TXM:399. NOT part of the engine port — the engine treats the
     * jackpot map as single-use.
     */
    public Optional<Short> peekWithoutConsume() {
        try {
            IMap<String, Short> map = hazelcast.getMap(MAP_NAME);
            return Optional.ofNullable(map.get(KEY));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}
