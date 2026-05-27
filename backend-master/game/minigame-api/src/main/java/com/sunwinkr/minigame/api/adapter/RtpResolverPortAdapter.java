package com.sunwinkr.minigame.api.adapter;

import com.sunwinkr.minigame.engine.dice.RtpResolverPort;
import com.vinplay.vbee.common.rtp.RtpResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter wrapping {@link RtpResolver} for the engine's house-edge dice
 * generator. Returns the unconfigured 92.0 default on any error so the
 * engine falls back to random — matches legacy GTX:51 behavior.
 *
 * <p>Plan §4 / D3.
 */
@Component
public class RtpResolverPortAdapter implements RtpResolverPort {

    private static final Logger LOG = LoggerFactory.getLogger(RtpResolverPortAdapter.class);

    private static final double UNCONFIGURED_DEFAULT = 92.0;

    @Override
    public double effectivePct(long userId, String gameId) {
        try {
            return RtpResolver.effectivePct(userId, gameId);
        } catch (Throwable t) {
            LOG.debug("RtpResolverPortAdapter.effectivePct(user) failed gameId=" + gameId, t);
            return UNCONFIGURED_DEFAULT;
        }
    }

    @Override
    public double effectivePct(String gameId) {
        try {
            return RtpResolver.effectivePct(gameId);
        } catch (Throwable t) {
            LOG.debug("RtpResolverPortAdapter.effectivePct failed gameId=" + gameId, t);
            return UNCONFIGURED_DEFAULT;
        }
    }
}
