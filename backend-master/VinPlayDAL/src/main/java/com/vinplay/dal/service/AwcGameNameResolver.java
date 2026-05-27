package com.vinplay.dal.service;

import com.vinplay.dal.service.seamless.awc.AwcPlatformAdapter;
import com.vinplay.dal.service.seamless.awc.AwcPlatformRegistry;
import com.vinplay.vbee.common.pools.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves AWC (platform, gameCode) → display name.
 *
 * <p>SUN-GAME-FK Phase 4 cleanup: backed by the unified
 * {@code vinplay.games} catalog (provider='AWC'). Falls back to the
 * platform stub row ({@code game_code='*'}) when no per-game row
 * exists, then to a synthesized "PLATFORM gameCode" string.
 *
 * <p>Pre-Phase-4 versions read {@code awc_game_catalog} +
 * {@code awc_platform_map}. Those tables stay populated for the
 * admin sync flow but are no longer consulted on the hot path —
 * `games` is the source of truth.
 */
public final class AwcGameNameResolver {

    /** Cache entry: name + epoch-ms expiry. */
    private static final class Cached {
        final String name; final long expiresAt;
        Cached(String name, long expiresAt) { this.name = name; this.expiresAt = expiresAt; }
    }
    private static final ConcurrentHashMap<String, Cached> CACHE = new ConcurrentHashMap<>();

    /**
     * 60-second TTL on the per-key cache. Lets the catalog be re-seeded
     * (e.g. ops adds a Mexico-09 row mid-day) without requiring a container
     * restart to flush stale "miss" entries that previously fell through to
     * the generic platform stub. Mirrors {@code GscBetWhitelist}'s posture.
     */
    private static final long CACHE_TTL_MS = 60_000L;

    private AwcGameNameResolver() {}

    public static String displayName(String platform, String gameCode) {
        return displayName(platform, gameCode, null);
    }

    /** Internal — looks up a row keyed by (platform, gameCode, tableTag). */
    private static String lookupName(String platform, String gameCode, String tableTag) {
        if (platform == null || platform.isEmpty()) return "AWC";
        String tag = tableTag == null ? "" : tableTag;
        String key = platform + "|" + (gameCode != null ? gameCode : "") + "|" + tag;
        long now = System.currentTimeMillis();

        Cached c = CACHE.get(key);
        if (c != null && c.expiresAt > now) return c.name;

        // Per-table row (platform, gameCode, tableTag).
        String name = querySingleName(platform, gameCode != null ? gameCode : "", tag);
        // Per-game row, no table specified.
        if (name == null && !tag.isEmpty()) {
            name = querySingleName(platform, gameCode != null ? gameCode : "", "");
        }
        // Platform stub.
        if (name == null) {
            name = querySingleName(platform, "*", "");
        }
        if (name == null) {
            name = platform + (gameCode != null && !gameCode.isEmpty() ? " " + gameCode : "");
            if (!tag.isEmpty()) name = name + " " + tag;
        }
        CACHE.put(key, new Cached(name, now + CACHE_TTL_MS));
        return name;
    }

    /** Test-only — drop the cache. */
    static void resetCacheForTests() { CACHE.clear(); }

    public static String providerName(String platform) {
        return displayName(platform, "*");
    }

    /**
     * SUN-1252 / SUN-1258 / SUN-1259: Sexy Live tables share a single AWC
     * {@code game_code} per game type (e.g. all baccarat tables = MX-LIVE-001).
     * The actual table identity lives in the AWC {@code roundId} prefix
     * (e.g. {@code Mexico-31-GA500610086} → table "Mexico-31"). Augment the
     * curated game name with the parsed table id so the agency views can
     * differentiate "Sexy Baccarat #M01" vs "Sexy Baccarat #M31".
     *
     * <p>Falls through to {@link #displayName(String, String)} when the
     * round id doesn't carry a parseable table prefix (e.g. R-1777881704
     * is a synthetic timestamped id, no table info). Pass {@code null} or
     * {@code ""} for {@code roundId} to skip parsing entirely.
     */
    public static String displayName(String platform, String gameCode, String roundId) {
        // SUN-1252 follow-up: parse table tag and look up the
        // catalog row keyed by (platform, gameCode, tableTag). The catalog
        // is now the authoritative source for table-level names — LS Cược,
        // LS Rolling, and the admin Game Catalog admin UI all read the
        // same row, so a per-table rename in the DB propagates to every
        // view immediately. Fallback chain: per-table → per-game →
        // platform stub → synthetic. Cached in-process per (platform,
        // gameCode, tag).
        //
        // 2026-05-18 — table-suffix parsing dispatches via
        // AwcPlatformRegistry so each platform owns its own round_id
        // shape (Sexy: Mexico-C07-GA…, JILI/CQ9/PG: no embedded table).
        // Pre-2026-05-18 the static regex applied to every platform's
        // round_id, risking spurious matches on non-Sexy ids.
        AwcPlatformAdapter adapter = AwcPlatformRegistry.forPlatform(platform);
        String suffix = adapter.parseTableSuffix(roundId);
        return lookupName(platform, gameCode, suffix);
    }

    /**
     * Platform-aware table-tag parser. Dispatches via
     * {@link AwcPlatformRegistry} so each platform's round_id shape lives
     * in its own adapter (SexyBcrtAdapter today; JILI/CQ9/PG inherit the
     * null-returning default).
     *
     * <p>Call this overload when the platform is known. The
     * {@link #parseTableSuffix(String)} no-platform overload is kept for
     * backward compatibility with one in-tree caller and assumes Sexy
     * shapes; remove after that caller is updated.
     */
    public static String parseTableSuffix(String platform, String roundId) {
        AwcPlatformAdapter adapter = AwcPlatformRegistry.forPlatform(platform);
        return adapter.parseTableSuffix(roundId);
    }

    /**
     * Backward-compatible no-platform overload. Assumes Sexy shapes —
     * correct for the legacy caller in AwcCallbackProcessor but will
     * misclassify non-Sexy round_ids if other callers appear. Prefer the
     * two-arg form.
     *
     * @deprecated use {@link #parseTableSuffix(String, String)} with the
     *             platform code so cross-platform round_ids don't
     *             accidentally match the Sexy regex.
     */
    @Deprecated
    public static String parseTableSuffix(String roundId) {
        return parseTableSuffix("SEXYBCRT", roundId);
    }

    private static String querySingleName(String platform, String gameCode, String tableTag) {
        String sql = "SELECT game_name FROM vinplay.games "
                   + "WHERE provider = 'AWC' "
                   + "  AND vendor_platform COLLATE utf8mb4_unicode_ci = ? "
                   + "  AND game_code COLLATE utf8mb4_unicode_ci = ? "
                   + "  AND table_tag COLLATE utf8mb4_unicode_ci = ? "
                   + "  AND is_active = 1 "
                   + "LIMIT 1";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, platform);
            ps.setString(2, gameCode);
            ps.setString(3, tableTag == null ? "" : tableTag);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("game_name");
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}
