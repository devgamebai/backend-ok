package com.vinplay.dal.service;

import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Tracks the (product_code, game_code) a player most recently launched via
 * {@code c=3090 LaunchGSCGameProcessor}. Read at wager-push time
 * ({@code WithdrawProcess} / {@code DepositProcess}) when the vendor's
 * payload omits the per-bet game_code — Dream Gaming is the headline
 * example, but the same pattern applies to any "lobby-style" provider
 * whose seamless-wallet push only carries the product code.
 *
 * <p>Without this, the rebate pipeline can only resolve the synthetic
 * {@code gsc_<pc>_<gc>} game_key when the vendor itself sends the
 * game_code, so providers that don't forward it fall through to the
 * legacy provider-level alias ({@code "ag"}, {@code "wm"}, …) and miss
 * the per-category rates ops configured on
 * {@code game_commission_rate} (e.g. {@code live_cat_Baccarat}).
 *
 * <p>Backed by a Hazelcast IMap shared across portal-api replicas so
 * launch on one node and bet-arrival on another both see the same
 * attribution. TTL of 2 hours mirrors the GSC launch-token TTL — past
 * that the player's session is invalid anyway and a stale attribution
 * would be wrong.
 *
 * <p>Limitation: a player who keeps two Dream tables open in two browser
 * tabs gets last-launch-wins attribution. Single-tab is the norm and
 * the worst-case effect (commission attributed to category A while bet
 * was placed at category B) is bounded by ops's per-category rates
 * which are usually equal across live-casino categories.
 */
public final class PlayerGameSessionService {

    private static final Logger logger = Logger.getLogger("dal");

    /** Hazelcast IMap name; cluster-shared. */
    public static final String MAP_NAME = "gsc_player_session";

    /** TTL on session entries — matches GSC launch-token TTL. */
    private static final long TTL_SECONDS = 7200L;

    /** Snapshot of a player's last-launched live game. Field types kept
     *  primitive-compatible for easy Hazelcast serialization across
     *  Java 8 / Hazelcast 3.12. */
    public static final class Session implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final int productCode;
        public final String gameCode;
        public final long launchedAt;

        public Session(int productCode, String gameCode, long launchedAt) {
            this.productCode = productCode;
            this.gameCode = gameCode != null ? gameCode : "";
            this.launchedAt = launchedAt;
        }
    }

    private PlayerGameSessionService() {}

    /**
     * Record the game the player just launched. Called by
     * {@code LaunchGSCGameProcessor} on every successful launch.
     * Best-effort: a Hazelcast hiccup is logged but never blocks the
     * launch response — the player still gets their URL even if we
     * lose the attribution for this round.
     */
    public static void record(String memberAccount, int productCode, String gameCode) {
        if (memberAccount == null || memberAccount.isEmpty() || productCode <= 0) return;
        try {
            IMap<String, Session> map = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            Session s = new Session(productCode, gameCode, System.currentTimeMillis());
            map.put(memberAccount, s, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("PlayerGameSessionService.record failed member=" + memberAccount
                    + " pc=" + productCode + " gc=" + gameCode + " err=" + e.getMessage());
        }
    }

    /**
     * Read the player's last-launched session. Returns {@code null} if
     * the player has not launched any game in the last {@link #TTL_SECONDS},
     * if the IMap is unreachable, or if the entry has been evicted.
     *
     * <p>Callers must treat {@code null} as "no attribution available"
     * and fall through to the next layer of the resolver chain.
     */
    public static Session lookup(String memberAccount) {
        if (memberAccount == null || memberAccount.isEmpty()) return null;
        try {
            IMap<String, Session> map = HazelcastClientFactory.getInstance().getMap(MAP_NAME);
            return map.get(memberAccount);
        } catch (Exception e) {
            logger.warn("PlayerGameSessionService.lookup failed member=" + memberAccount
                    + " err=" + e.getMessage());
            return null;
        }
    }
}
