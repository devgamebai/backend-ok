package com.sunwinkr.minigame.engine.dice;

/**
 * Engine-side port for the RTP / house-edge resolver. Adapter (PR-4)
 * wraps {@code com.vinplay.vbee.common.rtp.RtpResolver} from VbeeCommon;
 * the engine remains free of any vinplay-* coupling.
 *
 * <p>Contract: {@link #effectivePct(long, String)} returns the user's
 * effective payback percent. {@code &gt;= 92} means "not configured" —
 * the engine treats this as the default-no-config branch.
 *
 * <p>Plan §2.3 row D3.
 */
public interface RtpResolverPort {

    /** Per-user effective win-rate percent ({@code [0..100]}). */
    double effectivePct(long userId, String gameId);

    /** Game-default effective win-rate percent ({@code [0..100]}). */
    double effectivePct(String gameId);

    /** No-op resolver: always returns the unconfigured default (92). */
    RtpResolverPort DEFAULT_92 = new RtpResolverPort() {
        @Override
        public double effectivePct(long userId, String gameId) {
            return 92.0;
        }
        @Override
        public double effectivePct(String gameId) {
            return 92.0;
        }
    };
}
