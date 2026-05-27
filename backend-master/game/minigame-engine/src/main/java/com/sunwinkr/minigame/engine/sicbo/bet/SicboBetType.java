package com.sunwinkr.minigame.engine.sicbo.bet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum mirror of {@code PotSicbo} (PS:7-58) — 52 entries, id 1..52.
 *
 * <p>Extraction: Sicbo PR-1 (SUN-xxxx). Behavior-preserving port of
 * {@code game.modules.minigame.entities.PotSicbo}.
 *
 * <h3>ONE_DICE_* special payout (INV-9)</h3>
 * Entries with {@link #isOneDiceSpecial()} == true (IDs 15-20) use an
 * occurrence-count multiplier instead of {@link #getRotation()}:
 * <ul>
 *   <li>1 occurrence → {@code bet * 2}</li>
 *   <li>2 occurrences → {@code bet * 3}</li>
 *   <li>3 occurrences → {@code bet * 4}</li>
 * </ul>
 * The stored {@code rotation=1} for these entries is intentionally unused in
 * the prize calculation path (SBR:1153-1163, 1207). The field is kept for
 * wire-compat; callers MUST check {@link #isOneDiceSpecial()} before applying
 * the rotation multiplier.
 *
 * <h3>TRIPLE_DICES_* and ANY_TRIPLE_DICES suppression (INV-15)</h3>
 * When any triple is rolled, the evaluator ({@code SicboWinningStatusEvaluator})
 * returns ONLY the matching {@code TRIPLE_DICES_n} + {@code ANY_TRIPLE_DICES}.
 * All other bet types (TAI, XIU, CHAN, LE, DOUBLE_DICES_*, POINT_*, ONE_DICE_*)
 * are suppressed and pay zero. This is enforced in the evaluator, not here.
 * See §2.6 of the Sicbo extraction plan and INV-15 in the spec.
 *
 * <h3>DOUBLE_DICES ordering</h3>
 * IDs 21-41 follow the pattern DOUBLE_DICES_x_y where x ≤ y, ordered
 * lexicographically: 1_1, 1_2, 1_3, 1_4, 1_5, 1_6, 2_2, 2_3, ... 6_6.
 * This matches the original PotSicbo declaration order exactly.
 */
public enum SicboBetType {

    // --- Point bets (IDs 1-14): total of three dice ---
    POINT_4("POINT_4",   61, 1,  false),
    POINT_5("POINT_5",   31, 2,  false),
    POINT_6("POINT_6",   18, 3,  false),
    POINT_7("POINT_7",   13, 4,  false),
    POINT_8("POINT_8",    9, 5,  false),
    POINT_9("POINT_9",    7, 6,  false),
    POINT_10("POINT_10",  7, 7,  false),
    POINT_11("POINT_11",  7, 8,  false),
    POINT_12("POINT_12",  7, 9,  false),
    POINT_13("POINT_13",  9, 10, false),
    POINT_14("POINT_14", 13, 11, false),
    POINT_15("POINT_15", 18, 12, false),
    POINT_16("POINT_16", 31, 13, false),
    POINT_17("POINT_17", 61, 14, false),

    // --- One-die bets (IDs 15-20): payout by occurrence count, NOT rotation ---
    // isOneDiceSpecial=true. rotation=1 stored for wire-compat only; unused in payout.
    ONE_DICE_1("ONE_DICE_1", 1, 15, true),
    ONE_DICE_2("ONE_DICE_2", 1, 16, true),
    ONE_DICE_3("ONE_DICE_3", 1, 17, true),
    ONE_DICE_4("ONE_DICE_4", 1, 18, true),
    ONE_DICE_5("ONE_DICE_5", 1, 19, true),
    ONE_DICE_6("ONE_DICE_6", 1, 20, true),

    // --- Double-dice bets (IDs 21-41): exactly two of the three dice match x and y ---
    // TRIPLE suppresses these (INV-15). rotation=6 for all.
    DOUBLE_DICES_1_1("DOUBLE_DICES_1_1", 6, 21, false),
    DOUBLE_DICES_1_2("DOUBLE_DICES_1_2", 6, 22, false),
    DOUBLE_DICES_1_3("DOUBLE_DICES_1_3", 6, 23, false),
    DOUBLE_DICES_1_4("DOUBLE_DICES_1_4", 6, 24, false),
    DOUBLE_DICES_1_5("DOUBLE_DICES_1_5", 6, 25, false),
    DOUBLE_DICES_1_6("DOUBLE_DICES_1_6", 6, 26, false),
    DOUBLE_DICES_2_2("DOUBLE_DICES_2_2", 6, 27, false),
    DOUBLE_DICES_2_3("DOUBLE_DICES_2_3", 6, 28, false),
    DOUBLE_DICES_2_4("DOUBLE_DICES_2_4", 6, 29, false),
    DOUBLE_DICES_2_5("DOUBLE_DICES_2_5", 6, 30, false),
    DOUBLE_DICES_2_6("DOUBLE_DICES_2_6", 6, 31, false),
    DOUBLE_DICES_3_3("DOUBLE_DICES_3_3", 6, 32, false),
    DOUBLE_DICES_3_4("DOUBLE_DICES_3_4", 6, 33, false),
    DOUBLE_DICES_3_5("DOUBLE_DICES_3_5", 6, 34, false),
    DOUBLE_DICES_3_6("DOUBLE_DICES_3_6", 6, 35, false),
    DOUBLE_DICES_4_4("DOUBLE_DICES_4_4", 6, 36, false),
    DOUBLE_DICES_4_5("DOUBLE_DICES_4_5", 6, 37, false),
    DOUBLE_DICES_4_6("DOUBLE_DICES_4_6", 6, 38, false),
    DOUBLE_DICES_5_5("DOUBLE_DICES_5_5", 6, 39, false),
    DOUBLE_DICES_5_6("DOUBLE_DICES_5_6", 6, 40, false),
    DOUBLE_DICES_6_6("DOUBLE_DICES_6_6", 6, 41, false),

    // --- Specific triple bets (IDs 42-47): all three dice show value n ---
    // When any triple is rolled, ONLY the matching TRIPLE_DICES_n + ANY_TRIPLE_DICES win.
    TRIPLE_DICES_1("TRIPLE_DICES_1", 31, 42, false),
    TRIPLE_DICES_2("TRIPLE_DICES_2", 31, 43, false),
    TRIPLE_DICES_3("TRIPLE_DICES_3", 31, 44, false),
    TRIPLE_DICES_4("TRIPLE_DICES_4", 31, 45, false),
    TRIPLE_DICES_5("TRIPLE_DICES_5", 31, 46, false),
    TRIPLE_DICES_6("TRIPLE_DICES_6", 31, 47, false),

    // --- TAI/XIU (IDs 48-49): total > 10 = TAI, total <= 10 = XIU ---
    // Suppressed by any triple (INV-14, INV-15).
    TAI("TAI",   2, 48, false),
    XIU("XIU",   2, 49, false),

    // --- CHAN/LE (IDs 50-51): even/odd total ---
    // Suppressed by any triple (INV-15).
    CHAN("CHAN",  2, 50, false),
    LE("LE",     2, 51, false),

    // --- Any triple (ID 52): all three dice show the same value ---
    ANY_TRIPLE_DICES("ANY_TRIPLE_DICES", 31, 52, false);

    // -----------------------------------------------------------------------

    private final String name;
    private final int rotation;
    private final int id;
    private final boolean isOneDiceSpecial;

    SicboBetType(String name, int rotation, int id, boolean isOneDiceSpecial) {
        this.name = name;
        this.rotation = rotation;
        this.id = id;
        this.isOneDiceSpecial = isOneDiceSpecial;
    }

    public String getName() {
        return name;
    }

    /**
     * Payout multiplier for non-ONE_DICE_* types.
     * For ONE_DICE_* ({@link #isOneDiceSpecial()} == true), this returns 1
     * but the actual prize is {@code bet * (occurrences + 1)} — callers
     * MUST NOT use this value for ONE_DICE_* payout.
     */
    public int getRotation() {
        return rotation;
    }

    public int getId() {
        return id;
    }

    /**
     * Returns true for ONE_DICE_1 through ONE_DICE_6 (IDs 15-20).
     * Prize for these is occurrence-count based (×2/×3/×4), not rotation.
     */
    public boolean isOneDiceSpecial() {
        return isOneDiceSpecial;
    }

    // -----------------------------------------------------------------------
    // Static lookup maps — built once at class load time

    /** Lookup by numeric id (1..52). Returns null for unknown ids. */
    public static final Map<Integer, SicboBetType> BY_ID;

    /** Lookup by wire name string (e.g. "POINT_4"). Returns null for unknown names. */
    public static final Map<String, SicboBetType> BY_NAME;

    static {
        Map<Integer, SicboBetType> byId = new HashMap<Integer, SicboBetType>();
        Map<String, SicboBetType> byName = new HashMap<String, SicboBetType>();
        for (SicboBetType t : values()) {
            byId.put(t.id, t);
            byName.put(t.name, t);
        }
        BY_ID = Collections.unmodifiableMap(byId);
        BY_NAME = Collections.unmodifiableMap(byName);
    }

    /**
     * Look up by numeric id. Mirrors {@code PotSicbo.getEnumById(int)}.
     *
     * @throws IllegalArgumentException if no entry has this id
     */
    public static SicboBetType byId(int id) {
        SicboBetType t = BY_ID.get(id);
        if (t == null) {
            throw new IllegalArgumentException("No SicboBetType with id " + id);
        }
        return t;
    }

    /**
     * Look up by wire name string. Mirrors {@code PotSicbo.getEnumByName(String)}.
     *
     * @throws IllegalArgumentException if no entry has this name
     */
    public static SicboBetType byName(String name) {
        SicboBetType t = BY_NAME.get(name);
        if (t == null) {
            throw new IllegalArgumentException("No SicboBetType with name " + name);
        }
        return t;
    }
}
