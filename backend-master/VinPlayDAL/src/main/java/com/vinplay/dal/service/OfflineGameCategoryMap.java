package com.vinplay.dal.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Offline-game → category static map. Used by commission-rate resolution
 * when an agent has a {@code offline_cat_<CategoryName>} row instead of a
 * per-game row. Matches the category buckets surfaced by c=9894
 * ({@code &type=OFFLINE}) so admin CMS UX and rebate runtime agree.
 *
 * <p>Offline games change rarely (adding a new slot requires a code change
 * elsewhere anyway), so a static map avoids a DB round-trip on every bet.
 * When a new offline game ships, add its game_key + label here.
 */
public final class OfflineGameCategoryMap {

    private static final Map<String, String> GAME_TO_CATEGORY = new LinkedHashMap<>();
    private static final Map<String, String[]> CATEGORY_TO_GAMES = new LinkedHashMap<>();

    static {
        put("TaiXiu",    "taixiu", "taixiu_st", "taixiu_live", "taixiu_md5", "taixiu_sicbo");
        put("Slot",      "slot_pokemon", "slot_chiemtinh", "slot_bikini", "slot_galaxy",
                         "slot_thanbai", "slot_bitcoin", "slot_taydu", "slot_angrybird",
                         "slot_thantai", "slot_thethao");
        put("CardGame",  "tlmn", "bacay");
        put("BauCua",    "baucua");
        put("XocDia",    "xocdia");
        put("CaoThap",   "caothap");
        put("MiniPoker", "minipoker");
        put("Fish",      "fish");
    }

    private static void put(String category, String... games) {
        CATEGORY_TO_GAMES.put(category, games);
        for (String g : games) GAME_TO_CATEGORY.put(g, category);
    }

    private OfflineGameCategoryMap() {}

    /** Look up the category bucket for a given offline game_key. Returns {@code null} if not mapped. */
    public static String categoryOf(String gameKey) {
        if (gameKey == null) return null;
        return GAME_TO_CATEGORY.get(gameKey);
    }

    /** Ordered list of all offline categories (for c=9894&type=OFFLINE response). */
    public static Map<String, String[]> allCategories() {
        return Collections.unmodifiableMap(CATEGORY_TO_GAMES);
    }

    /** Whole map of game_key → category (read-only view). */
    public static Map<String, String> allGames() {
        return Collections.unmodifiableMap(GAME_TO_CATEGORY);
    }
}
