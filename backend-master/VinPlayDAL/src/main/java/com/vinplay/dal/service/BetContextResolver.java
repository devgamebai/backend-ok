package com.vinplay.dal.service;

import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Resolves {@code (productCode, gameCode)} for a wager push, regardless of
 * what the vendor included in the payload. Centralizes the fallback chain
 * used by every seamless-wallet hook (Withdraw, Deposit, RollIn, RollOut)
 * so each provider integration only sets the parts it knows and leaves
 * the rest to this resolver.
 *
 * <p>Fallback chain — first non-null wins:
 * <ol>
 *   <li><b>VENDOR</b> — vendor included both productCode and a non-empty
 *       gameCode in the wager push. Trust it; this is the canonical
 *       per-bet attribution.</li>
 *   <li><b>SESSION</b> — vendor sent productCode but empty gameCode.
 *       Look up the player's most recent {@code c=3090} launch via
 *       {@link PlayerGameSessionService}; if the productCode matches,
 *       use that gameCode. Handles "lobby-style" providers (Dream
 *       Gaming) whose wager push doesn't carry the per-bet table.</li>
 *   <li><b>DEFAULT</b> — same productCode, but no session entry (e.g. a
 *       wager arrives before the launch was recorded, or the session
 *       expired). Read {@code gsc_provider_defaults.default_game_code}
 *       — ops-configured per provider so commission still resolves
 *       through the per-category rates rather than 0%.</li>
 *   <li><b>FALLBACK</b> — no resolution possible. Return the raw
 *       (productCode, "") tuple; downstream code falls back to the
 *       legacy provider alias path ({@code "ag"}, {@code "wm"}, …).</li>
 * </ol>
 *
 * <p>The resolver is stateless — instance methods only call into
 * {@link PlayerGameSessionService} (Hazelcast) and the
 * {@code gsc_provider_defaults} table. Safe to invoke from any seamless
 * hook without coordination.
 */
public final class BetContextResolver {

    private static final Logger logger = Logger.getLogger("dal");

    /** Where the resolved game_code came from. Stamped on the result so
     *  downstream code can log telemetry / debug attribution drift. */
    public enum Source {
        /** Vendor's wager push contained the game_code. */
        VENDOR,
        /** Recovered from the player's most recent launch session. */
        SESSION,
        /** Provider's ops-configured default in {@code gsc_provider_defaults}. */
        DEFAULT,
        /** No source — game_code stays empty; callers fall back to legacy alias. */
        FALLBACK
    }

    public static final class Context {
        public final int productCode;
        public final String gameCode;
        public final Source source;

        Context(int pc, String gc, Source src) {
            this.productCode = pc;
            this.gameCode = gc != null ? gc : "";
            this.source = src;
        }

        /** True when {@code gameCode} is non-empty — i.e. callers can
         *  build a {@code gsc_<pc>_<gc>} synthetic key for commission
         *  lookup. */
        public boolean hasGameCode() {
            return gameCode != null && !gameCode.isEmpty();
        }
    }

    private BetContextResolver() {}

    /**
     * Run the fallback chain. {@code memberAccount} may be null for
     * back-office calls; the SESSION layer is skipped in that case.
     */
    public static Context resolve(String memberAccount, int txProductCode, String txGameCode) {
        // Layer 1 — vendor sent it.
        if (txProductCode > 0 && txGameCode != null && !txGameCode.isEmpty()) {
            return new Context(txProductCode, txGameCode, Source.VENDOR);
        }

        // Layer 2 — recover from launch session.
        if (memberAccount != null && !memberAccount.isEmpty() && txProductCode > 0) {
            PlayerGameSessionService.Session session =
                    PlayerGameSessionService.lookup(memberAccount);
            if (session != null
                    && session.productCode == txProductCode
                    && session.gameCode != null
                    && !session.gameCode.isEmpty()) {
                return new Context(txProductCode, session.gameCode, Source.SESSION);
            }
        }

        // Layer 3 — provider-configured default.
        if (txProductCode > 0) {
            String def = readProviderDefault(txProductCode);
            if (def != null && !def.isEmpty()) {
                return new Context(txProductCode, def, Source.DEFAULT);
            }
        }

        // Layer 4 — give up. Caller falls through to legacy alias path.
        return new Context(txProductCode, "", Source.FALLBACK);
    }

    /**
     * Read {@code gsc_provider_defaults.default_game_code} for the given
     * product. Returns {@code null} on miss / DB error. Best-effort:
     * never throws, so the resolver chain stays clean.
     */
    private static String readProviderDefault(int productCode) {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT default_game_code FROM gsc_provider_defaults " +
                     "WHERE product_code = ? LIMIT 1")) {
            ps.setInt(1, productCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String gc = rs.getString(1);
                    if (gc != null && !gc.isEmpty()) return gc;
                }
            }
        } catch (Exception e) {
            // gsc_provider_defaults may not exist yet on older DBs;
            // log at warn once-per-product and move on.
            logger.warn("BetContextResolver.readProviderDefault failed pc="
                    + productCode + " err=" + e.getMessage());
        }
        return null;
    }
}
