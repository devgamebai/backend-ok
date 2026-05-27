package com.sunwinkr.minigame.engine.sicbo.dice;

/**
 * Engine-facing port that abstracts {@code CanCuaRtpBalancer.isEnabled()}
 * (legacy env-var feature gate {@code CANCUA_USE_DYNAMIC_RTP}).
 *
 * <p>Keeps the engine module free of {@code System.getenv} reads so tests
 * can flip the flag without touching process env.
 *
 * <p>Default singletons live in {@link DefaultFeatureFlagPort}.
 */
public interface FeatureFlagPort {

    /**
     * @return {@code true} when the cân-cửa RTP balancer should be active
     *         for Sicbo result generation (INV-18).
     */
    boolean isCanCuaRtpEnabled();
}
