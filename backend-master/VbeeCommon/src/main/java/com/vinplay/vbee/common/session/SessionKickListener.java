package com.vinplay.vbee.common.session;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.MapEvent;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

/**
 * Distributed session-kick listener on the Hazelcast {@code cacheToken} map
 * (SUN-816 / SUN-767).
 *
 * <p>Every game server registers one of these at init time. When a user logs
 * in from a second device, {@code PortalUtils.loginSuccess} calls
 * {@code cacheToken.remove(oldAccessToken)}. Hazelcast fires an
 * {@code entryRemoved} event to every connected client — including every
 * Java game server — giving us a single point where the old bitzero session
 * can be disconnected, regardless of which game the user was playing.
 *
 * <p>This listener lives in {@code VbeeCommon} so it can be shared by all
 * services. It has NO bitzero dependency; each game module plugs in its own
 * {@link KickHandler} that knows how to look up the bitzero {@code User} by
 * nickname, send the wire force-logout message (cmd 20200), and disconnect
 * the socket.
 *
 * <p>Why this replaces the SUN-767 RMQ fanout:
 * <ul>
 *   <li>Hazelcast is already connected on every Java process — no extra
 *       queue/exchange infra.
 *   <li>Event fan-out is implicit — every client with a listener gets the
 *       event, so adding a new game server is automatic.
 *   <li>Any invalidation path (login, admin kick, ban, TTL) triggers the
 *       same code path. No new message type per reason.
 * </ul>
 *
 * <p>We intentionally do NOT fire on {@code entryExpired} / {@code entryEvicted}:
 * TTL-driven expiry (180 minutes of idleness) should let the user keep their
 * active socket until they attempt their next API call; firing on eviction
 * would kick players mid-game whenever their token aged out naturally.
 * {@code entryRemoved} is triggered only by explicit {@code IMap.remove(...)}
 * calls — today only by {@code PortalUtils.loginSuccess} on new-login, which
 * is exactly the semantic we want ("a new device replaced this one").
 */
public final class SessionKickListener {

    private static final Logger logger = Logger.getLogger("vbee");
    private static final String MAP_NAME = "cacheToken";

    /**
     * Pluggable handler — each game module implements this to run the actual
     * bitzero disconnect. Called on a Hazelcast event thread; implementations
     * must not block.
     */
    public interface KickHandler {
        /**
         * @param accessToken the removed token (the map KEY). Portal-side
         *                    handlers use this to close the specific
         *                    {@code /ws/balance} socket tied to it. Game
         *                    handlers typically ignore it and rely on
         *                    {@code nickname} to find the bitzero session.
         * @param nickname    login name of the user whose session just got
         *                    invalidated (the old map VALUE)
         * @param reason      always {@code "DUPLICATE_LOGIN"} today; reserved
         *                    for future invalidation reasons
         */
        void kick(String accessToken, String nickname, String reason);
    }

    private SessionKickListener() {}

    /**
     * Register a kick handler on the {@code cacheToken} map. Idempotent per
     * JVM — repeated calls register multiple listeners, so call once from
     * the extension's {@code init()} after {@code HazelcastLoader.start()}.
     *
     * <p>If Hazelcast is not yet available, this logs and returns without
     * registering — init code should tolerate that (game server still runs,
     * kick is simply degraded to the existing RMQ path if any).
     *
     * @return the registration id, or {@code null} if registration failed
     */
    public static String register(final KickHandler handler) {
        if (handler == null) {
            logger.warn("SessionKickListener.register: handler is null, skipping");
            return null;
        }
        try {
            final HazelcastInstance hz = HazelcastClientFactory.getInstance();
            if (hz == null) {
                logger.warn("SessionKickListener.register: Hazelcast not initialized");
                return null;
            }

            // Track the current server-side registration id so we can remove
            // stale registrations across reconnects (see LifecycleListener below).
            final AtomicReference<String> regIdRef = new AtomicReference<>();
            String id = registerEntryListener(hz, handler, regIdRef);

            // SUN-816 follow-up: Hazelcast 3.12 client can issue a NEW client
            // UUID on reconnect after a heartbeat timeout. Server-side listener
            // registrations are keyed by client UUID, so after such a reconnect
            // the listener is effectively dead (still present in JVM memory,
            // but no events delivered). Observed in staging: minigame kept its
            // UUID across reconnect and worked, poker+others got new UUIDs and
            // went silent. Fix: re-register on every CLIENT_CONNECTED event.
            hz.getLifecycleService().addLifecycleListener(new LifecycleListener() {
                @Override
                public void stateChanged(LifecycleEvent event) {
                    if (event.getState() == LifecycleEvent.LifecycleState.CLIENT_CONNECTED) {
                        // Hazelcast 3.12 LifecycleListener doesn't replay past
                        // events. The listener is added AFTER the initial
                        // registerEntryListener in step 2 above, so the FIRST
                        // CLIENT_CONNECTED this handler sees is the reconnect
                        // after a heartbeat timeout — NOT the initial startup.
                        // We re-register unconditionally; the remove-before-add
                        // handles the edge case where the server-side registration
                        // survived (rare — usually new UUID = stale on server).
                        try {
                            String stale = regIdRef.get();
                            if (stale != null) {
                                try { hz.getMap(MAP_NAME).removeEntryListener(stale); }
                                catch (Exception ignored) {}
                            }
                            registerEntryListener(hz, handler, regIdRef);
                            logger.info("SessionKickListener: re-registered on CLIENT_CONNECTED (reconnect)");
                        } catch (Throwable t) {
                            logger.warn("SessionKickListener: re-register after reconnect failed: "
                                    + t.getMessage());
                        }
                    }
                }
            });
            return id;
        } catch (Exception e) {
            logger.warn("SessionKickListener.register failed: " + e.getMessage());
            return null;
        }
    }

    private static String registerEntryListener(final HazelcastInstance hz,
                                                final KickHandler handler,
                                                final AtomicReference<String> regIdRef) {
        IMap<String, String> tokenMap = hz.getMap(MAP_NAME);
        // includeValue=true so entryRemoved events carry the old value
        // (the nickname) — needed to find the bitzero session.
        String id = tokenMap.addEntryListener(new EntryListener<String, String>() {
            @Override
            public void entryAdded(EntryEvent<String, String> event) { /* no-op */ }

            @Override
            public void entryUpdated(EntryEvent<String, String> event) { /* no-op */ }

            @Override
            public void entryRemoved(EntryEvent<String, String> event) {
                // Fired by explicit IMap.remove(oldAccessToken) in
                // PortalUtils.loginSuccess when a new device logs in.
                String accessToken = event.getKey();
                String nickname = event.getOldValue();
                if (nickname == null || nickname.isEmpty()) {
                    return;
                }
                try {
                    handler.kick(accessToken, nickname, "DUPLICATE_LOGIN");
                } catch (Throwable t) {
                    // Never let a handler failure escape the listener —
                    // would stop Hazelcast from delivering future events.
                    logger.warn("SessionKickListener: handler threw for nickname="
                            + nickname + " err=" + t.getMessage());
                }
            }

            @Override
            public void entryEvicted(EntryEvent<String, String> event) {
                // TTL / size-based eviction — do NOT kick. User should
                // keep their active socket until their next API call.
            }

            @Override
            public void mapEvicted(MapEvent event) { /* no-op */ }

            @Override
            public void mapCleared(MapEvent event) { /* no-op */ }
        }, /* includeValue */ true);

        regIdRef.set(id);
        logger.info("SessionKickListener: registered on map '" + MAP_NAME
                + "' regId=" + id);
        return id;
    }
}
