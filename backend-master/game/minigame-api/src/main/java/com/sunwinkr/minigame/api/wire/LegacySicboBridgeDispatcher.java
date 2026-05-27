package com.sunwinkr.minigame.api.wire;

import com.sunwinkr.minigame.api.MinigameApiApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Thin reflective dispatch surface for the legacy
 * {@code SicboModule.handleClientRequest}. Mirrors
 * {@link LegacyBridgeDispatcher} (TaiXiu) for Sicbo command ids.
 *
 * <p>Lookup contract: the running Spring context (started by
 * {@code MinigameApiApplication}) holds a single
 * {@link SicboModuleBridge} bean. We resolve it by type from the
 * context holder; if the context isn't running we throw, letting the
 * legacy fallback path engage.
 *
 * <h3>Command map</h3>
 * <ul>
 *   <li>28000 — subscribe (translated to bridge.subscribe)</li>
 *   <li>28001 — unsubscribe (translated to bridge.unsubscribe)</li>
 *   <li>28002 — change room (translated to bridge.changeRoom)</li>
 *   <li>28110 — bet (translated to bridge.bet)</li>
 *   <li>28003 — force-result — DEPRECATED on player socket; the
 *       Spring chain enforces admin via AdminSicboController. We drop
 *       it here (mirrors TaiXiu PR-4 dispatcher §6.1).</li>
 * </ul>
 *
 * <p>Plan §6 / 6.1.
 */
public final class LegacySicboBridgeDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(LegacySicboBridgeDispatcher.class);

    private LegacySicboBridgeDispatcher() {
    }

    /**
     * Dispatch a {@code DataCmd} to the Sicbo engine bridge.
     *
     * @param sicboModule the legacy module instance (opaque)
     * @param user        BitZero user object (opaque)
     * @param dataCmd     BitZero data command (opaque)
     * @throws IllegalStateException if the Spring context is not running
     */
    public static void dispatch(Object sicboModule, Object user, Object dataCmd) {
        ConfigurableApplicationContext ctx = MinigameApiApplication.contextHolder();
        if (ctx == null || !ctx.isActive()) {
            throw new IllegalStateException("Spring context not running");
        }
        SicboModuleBridge bridge;
        try {
            bridge = ctx.getBean(SicboModuleBridge.class);
        } catch (Throwable t) {
            throw new IllegalStateException("Sicbo bridge bean not available", t);
        }
        int cmdId = readCmdId(dataCmd);
        switch (cmdId) {
            case 28000:
            case 28001:
            case 28002:
            case 28110:
            case 28116:
                // The bridge owns its own translation of these commands;
                // it builds engine SicboBetRequest / snapshot / etc and
                // the BitZero module sends BaseMsg responses on the way
                // back. PR-4 baseline: just acknowledge the dispatch
                // path so the legacy fallback is the source of truth
                // until the bridge wire-up matures (plan §6 follow-up).
                LOG.debug("Sicbo bridge dispatch cmd={} (handed off to bean lookup)", cmdId);
                throw new UnsupportedOperationException(
                    "Bridge field-level translation lands in cutover sprint — see plan §6");
            case 28003:
                // Force-result is gone from the player socket. Drop.
                LOG.warn("Sicbo bridge: deprecated force-result cmd 28003 from player socket — ignored");
                return;
            default:
                throw new IllegalStateException("Unknown Sicbo cmd id: " + cmdId);
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
