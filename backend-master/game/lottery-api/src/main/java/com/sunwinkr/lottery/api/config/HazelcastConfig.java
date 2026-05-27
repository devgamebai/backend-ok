package com.sunwinkr.lottery.api.config;

import com.hazelcast.core.HazelcastInstance;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Hazelcast bean wiring.
 *
 * <p>Per plan §9.3, we MUST NOT pull {@code spring-boot-starter-hazelcast}
 * — that starter wants Hazelcast 5.x while the repo is pinned to 3.12.13
 * for Cocos Creator wire compatibility. We expose the singleton
 * {@link HazelcastInstance} sourced from
 * {@link HazelcastClientFactory#getInstance()} so adapters can autowire it.
 *
 * <p>When running as a standalone Spring Boot container (not embedded in the
 * BitZero JVM), {@link HazelcastClientFactory#init} has not been called yet
 * — {@code instance} is null and {@code getInstance()} would NPE. We call
 * {@code init()} here using env vars ({@code HAZELCAST_HOST},
 * {@code HAZELCAST_GROUP}, {@code HAZELCAST_PASSWORD}) before delegating to
 * {@code getInstance()}.
 */
@Configuration
public class HazelcastConfig {

    @Bean
    public HazelcastInstance hazelcastInstance() {
        // Standalone container: factory not yet initialised — call init() with
        // env-var coordinates before getInstance() dereferences instance.
        String host     = envOrDefault("HAZELCAST_HOST",     "hazelcast-1");
        String group    = envOrDefault("HAZELCAST_GROUP",    "vinplay");
        String password = envOrDefault("HAZELCAST_PASSWORD", "vinplay@123");
        HazelcastClientFactory.init(
                Collections.singletonList(host),
                group,
                password);
        return HazelcastClientFactory.getInstance();
    }

    private static String envOrDefault(String name, String def) {
        String v = System.getenv(name);
        return (v != null && !v.isEmpty()) ? v : def;
    }
}
