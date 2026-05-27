package com.sunwinkr.minigame.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.mongo.MongoMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the TaiXiu API + engine bridge.
 *
 * <p>Plan §1.3 / §1.4: this application is launched from the existing
 * BitZero {@code game-minigame} container's {@code ServerReadyTask} on a
 * background thread iff {@code MINIGAME_API_ENABLED=1}. It does NOT own
 * the JVM lifecycle — BitZero remains the primary owner. The shared JVM
 * affords zero-copy access to the same {@code HazelcastClientFactory.getInstance()}
 * and avoids a second Hazelcast client license slot.
 *
 * <p>The static {@link #contextHolder()} accessor exposes the running
 * {@link ConfigurableApplicationContext} so the BitZero adapter can pull
 * the {@code TaiXiuModuleBridge} bean by type without coupling to Spring
 * in the BitZero module classpath.
 *
 * <p>Feature flags:
 * <ul>
 *   <li>{@code MINIGAME_API_ENABLED} — start the Spring context at all.
 *   <li>{@code MINIGAME_ENGINE_ENABLED} — flip the BitZero bridge to
 *       delegate {@code handleClientRequest} to the engine (default OFF;
 *       legacy code path runs when OFF).
 *   <li>{@code MINIGAME_ENGINE_GHOST_MODE} — engine computes results in
 *       parallel and writes {@code shadow_*} Mongo collections without
 *       affecting wallet state (default OFF).
 * </ul>
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoMetricsAutoConfiguration.class})
@EnableScheduling
public class MinigameApiApplication {

    /**
     * Holds the running Spring context. Volatile because the BitZero
     * thread that booted us is not the same thread that reads the
     * reference — happens-before guarantee needed.
     */
    private static volatile ConfigurableApplicationContext CONTEXT;

    public static void main(String[] args) {
        CONTEXT = SpringApplication.run(MinigameApiApplication.class, args);
    }

    /**
     * Boot programmatically from the BitZero {@code ServerReadyTask}.
     * Safe to call multiple times — second call is a no-op.
     *
     * @return the running context (or the previously-booted one)
     */
    public static synchronized ConfigurableApplicationContext start(String[] args) {
        if (CONTEXT != null && CONTEXT.isActive()) {
            return CONTEXT;
        }
        CONTEXT = SpringApplication.run(MinigameApiApplication.class, args);
        return CONTEXT;
    }

    /**
     * Stop the Spring context. Called on JVM shutdown by the BitZero
     * container or in tests.
     */
    public static synchronized void stop() {
        if (CONTEXT != null) {
            try {
                CONTEXT.close();
            } catch (Throwable t) {
                // Shutdown best-effort; do not propagate.
            } finally {
                CONTEXT = null;
            }
        }
    }

    /**
     * Bean lookup seam for the BitZero adapter. Returns {@code null} when
     * the context isn't running — callers MUST tolerate this (legacy
     * fallback path).
     */
    public static ConfigurableApplicationContext contextHolder() {
        return CONTEXT;
    }
}
