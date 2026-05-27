package com.sunwinkr.minigame.engine.sicbo.dice;

/**
 * Engine-facing port that abstracts the legacy
 * {@code com.vinplay.vbee.common.rtp.RtpResolver} static utility.
 *
 * <p>The engine module MUST NOT import the legacy RTP class directly (see
 * {@code build.gradle}: ZERO Spring / BitZero / Hazelcast). Adapters live
 * in the integration layer and call {@code RtpResolver.effectivePct(...)}.
 *
 * <h3>Two-argument signatures</h3>
 * Both signatures of the legacy resolver are exposed:
 * <ul>
 *   <li>{@link #effectivePct(long, String)} — user-aware override (userId,
 *       game). Called with {@code 0L} in {@code SicboRtpDiceGenerator}
 *       (SBR:636).</li>
 *   <li>{@link #effectivePct(String)} — game-only baseline; used by the
 *       "100% RTP" guard at SBR:637.</li>
 * </ul>
 *
 * <p>Adapters MUST return a percentage value in {@code [0..100]} (NOT a
 * fraction). Non-finite values (NaN, infinity) and out-of-range values are
 * treated by the engine as "no RTP control" and fall back to random dice.
 */
public interface RtpResolverPort {

    /**
     * User-aware effective RTP pct.
     *
     * @param userId game-server user ID (or {@code 0L} for "no user context")
     * @param gameCode RTP key (e.g. {@code "sicbo"})
     * @return effective pct in {@code [0..100]}
     */
    double effectivePct(long userId, String gameCode);

    /**
     * Game-only baseline RTP pct.
     *
     * @param gameCode RTP key (e.g. {@code "sicbo"})
     * @return baseline pct in {@code [0..100]}
     */
    double effectivePct(String gameCode);
}
