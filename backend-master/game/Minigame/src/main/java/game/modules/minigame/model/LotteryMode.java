/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.model;

public enum LotteryMode {
    // SUN-1295: rates (bet-cost multipliers) and prize multipliers per LodeRatio CSV
    // 2026-05-10. Both fields are also stamped onto each `lode` row at purchase
    // time (bet_unit / rate_at_purchase / prize_multiplier columns) so a future
    // rate change cannot retroactively alter pending bets \u2014 settle reads from
    // the row, not from this enum. The enum stays the source of truth at
    // PURCHASE time only. See LotteryModule.buyTicket + getPrize.
    //
    // Constructor args: (id, name, description, rate, prizeMultiplier)
    LO_2_SO   (1,  "L\u00d4 2 S\u1ed0",   "Ch\u1ecdn 2 s\u1ed1", 22, 80),
    LO_3_SO   (2,  "L\u00d4 3 S\u1ed0",   "Ch\u1ecdn 3 s\u1ed1", 23, 600),
    LO_XIEN_2 (3,  "L\u00d4 XI\u00caN 2", "Ch\u1ecdn 2 s\u1ed1", 1,  12),
    LO_XIEN_3 (4,  "L\u00d4 XI\u00caN 3", "Ch\u1ecdn 3 s\u1ed1", 1,  48),
    LO_XIEN_4 (5,  "L\u00d4 XI\u00caN 4", "Ch\u1ecdn 4 s\u1ed1", 1,  160),
    DAU       (6,  "\u0110\u1ea6U",       "Ch\u1ecdn 1 s\u1ed1", 1,  8),
    DUOI      (7,  "\u0110U\u00d4I",      "Ch\u1ecdn 1 s\u1ed1", 1,  8),
    DE_DAU    (8,  "\u0110\u1ec0",        "Ch\u1ecdn 2 s\u1ed1", 1,  80),  // legacy: getPrize divides by DUOI.getRate(); kept for back-compat
    DE        (9,  "\u0110\u1ec0",        "Ch\u1ecdn 2 s\u1ed1", 1,  85),
    BA_CANG   (11, "BA C\u00c0NG",   "Ch\u1ecdn 3 s\u1ed1", 1,  450);

    private int id;
    private String name;
    private String description;
    private int rate;
    private int prizeMultiplier;

    private LotteryMode(int id, String name, String description, int rate, int prizeMultiplier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rate = rate;
        this.prizeMultiplier = prizeMultiplier;
    }

    public int getPrizeMultiplier() {
        return this.prizeMultiplier;
    }

    public void setPrizeMultiplier(int prizeMultiplier) {
        this.prizeMultiplier = prizeMultiplier;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRate() {
        return this.rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public static LotteryMode findLotteryModeById(int id) {
        for (LotteryMode entry : LotteryMode.values()) {
            if (entry.getId() != id) continue;
            return entry;
        }
        return null;
    }
}

