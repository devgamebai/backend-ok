package com.sunwinkr.minigame.engine.sicbo.dice;

/**
 * Reference implementations of {@link FeatureFlagPort} for tests and adapters
 * that don't need env-var lookup.
 *
 * <p>Production servers wire a real adapter that reads
 * {@code System.getenv("CANCUA_USE_DYNAMIC_RTP")} — that adapter lives
 * outside the engine module.
 */
public final class DefaultFeatureFlagPort {

    private DefaultFeatureFlagPort() {}

    /** Always-on flag — useful when wiring the RTP path unconditionally. */
    public static final FeatureFlagPort ALWAYS_ON = new FeatureFlagPort() {
        @Override
        public boolean isCanCuaRtpEnabled() {
            return true;
        }
    };

    /** Always-off flag — engine falls back to random dice (legacy path). */
    public static final FeatureFlagPort ALWAYS_OFF = new FeatureFlagPort() {
        @Override
        public boolean isCanCuaRtpEnabled() {
            return false;
        }
    };

    /** Mutable test gate — flip {@link #enabled} from tests. */
    public static final class Toggle implements FeatureFlagPort {
        public volatile boolean enabled;

        public Toggle(boolean initial) {
            this.enabled = initial;
        }

        @Override
        public boolean isCanCuaRtpEnabled() {
            return enabled;
        }
    }
}
