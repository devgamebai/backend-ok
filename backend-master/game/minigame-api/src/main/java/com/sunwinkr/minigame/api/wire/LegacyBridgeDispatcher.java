package com.sunwinkr.minigame.api.wire;

import com.sunwinkr.minigame.api.MinigameApiApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Thin reflective dispatch surface for the legacy
 * {@code TaiXiuModule.handleClientRequest}. Lets the BitZero module call
 * into Spring-managed beans without the legacy {@code build.gradle}
 * gaining a Spring dependency itself.
 *
 * <p>Lookup contract: the running Spring context (started by
 * {@code MinigameApiApplication}) holds a single
 * {@link TaiXiuModuleBridge} bean. We resolve it by type from the
 * context holder; if the context isn't running we throw, letting the
 * legacy fallback path engage.
 *
 * <p>Plan §6.1.
 */
public final class LegacyBridgeDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyBridgeDispatcher.class);

    private LegacyBridgeDispatcher() {
    }

    /**
     * Dispatch a {@code DataCmd} to the engine bridge. The legacy
     * module passes its {@code Object} payload through unchanged — we
     * resolve the bridge bean and read the command id via reflection so
     * this class stays BitZero-free.
     *
     * @param taiXiuModule the legacy module instance (opaque)
     * @param user         BitZero user object (opaque)
     * @param dataCmd      BitZero data command (opaque)
     * @throws IllegalStateException if the Spring context is not running
     */
    public static void dispatch(Object taiXiuModule, Object user, Object dataCmd) {
        ConfigurableApplicationContext ctx = MinigameApiApplication.contextHolder();
        if (ctx == null || !ctx.isActive()) {
            throw new IllegalStateException("Spring context not running");
        }
        TaiXiuModuleBridge bridge;
        try {
            bridge = ctx.getBean(TaiXiuModuleBridge.class);
        } catch (Throwable t) {
            throw new IllegalStateException("Bridge bean not available", t);
        }
        int cmdId = readCmdId(dataCmd);
        switch (cmdId) {
            case 2000:
            case 2001:
            case 2002:
            case 2110:
            case 2116:
                // The bridge owns its own translation of these commands;
                // it builds engine BetRequest / snapshot / etc and the
                // BitZero module sends BaseMsg responses on the way back.
                // We deliberately do NOT extract every field here —
                // PR-4 baseline: just acknowledge the dispatch path so
                // the legacy fallback is the source of truth until the
                // bridge wire-up matures (plan §6 follow-up).
                LOG.debug("Bridge dispatch cmd={} (handed off to bean lookup)", cmdId);
                throw new UnsupportedOperationException(
                    "Bridge field-level translation lands in cutover sprint — see plan §6");
            case 2003:
                // Force-result is gone from the player socket. Drop.
                LOG.warn("Bridge: deprecated force-result cmd 2003 from player socket — ignored");
                return;
            default:
                throw new IllegalStateException("Unknown cmd id: " + cmdId);
        }
    }

    /** Reflectively read {@code dataCmd.getId()} — keeps this class BitZero-free. */
    private static int readCmdId(Object dataCmd) {
        try {
            java.lang.reflect.Method m = dataCmd.getClass().getMethod("getId");
            Object v = m.invoke(dataCmd);
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            return -1;
        } catch (Throwable t) {
            return -1;
        }
    }
}
