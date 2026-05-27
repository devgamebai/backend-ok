/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.entities;

public enum PotSicbo {
    POINT_4("POINT_4", 61, 1),
    POINT_5("POINT_5", 31, 2),
    POINT_6("POINT_6", 18, 3),
    POINT_7("POINT_7", 13, 4),
    POINT_8("POINT_8", 9, 5),
    POINT_9("POINT_9", 7, 6),
    POINT_10("POINT_10", 7, 7),
    POINT_11("POINT_11", 7, 8),
    POINT_12("POINT_12", 7, 9),
    POINT_13("POINT_13", 9, 10),
    POINT_14("POINT_14", 13, 11),
    POINT_15("POINT_15", 18, 12),
    POINT_16("POINT_16", 31, 13),
    POINT_17("POINT_17", 61, 14),
    ONE_DICE_1("ONE_DICE_1", 1, 15),
    ONE_DICE_2("ONE_DICE_2", 1, 16),
    ONE_DICE_3("ONE_DICE_3", 1, 17),
    ONE_DICE_4("ONE_DICE_4", 1, 18),
    ONE_DICE_5("ONE_DICE_5", 1, 19),
    ONE_DICE_6("ONE_DICE_6", 1, 20),
    DOUBLE_DICES_1_1("DOUBLE_DICES_1_1", 6, 21),
    DOUBLE_DICES_1_2("DOUBLE_DICES_1_2", 6, 22),
    DOUBLE_DICES_1_3("DOUBLE_DICES_1_3", 6, 23),
    DOUBLE_DICES_1_4("DOUBLE_DICES_1_4", 6, 24),
    DOUBLE_DICES_1_5("DOUBLE_DICES_1_5", 6, 25),
    DOUBLE_DICES_1_6("DOUBLE_DICES_1_6", 6, 26),
    DOUBLE_DICES_2_2("DOUBLE_DICES_2_2", 6, 27),
    DOUBLE_DICES_2_3("DOUBLE_DICES_2_3", 6, 28),
    DOUBLE_DICES_2_4("DOUBLE_DICES_2_4", 6, 29),
    DOUBLE_DICES_2_5("DOUBLE_DICES_2_5", 6, 30),
    DOUBLE_DICES_2_6("DOUBLE_DICES_2_6", 6, 31),
    DOUBLE_DICES_3_3("DOUBLE_DICES_3_3", 6, 32),
    DOUBLE_DICES_3_4("DOUBLE_DICES_3_4", 6, 33),
    DOUBLE_DICES_3_5("DOUBLE_DICES_3_5", 6, 34),
    DOUBLE_DICES_3_6("DOUBLE_DICES_3_6", 6, 35),
    DOUBLE_DICES_4_4("DOUBLE_DICES_4_4", 6, 36),
    DOUBLE_DICES_4_5("DOUBLE_DICES_4_5", 6, 37),
    DOUBLE_DICES_4_6("DOUBLE_DICES_4_6", 6, 38),
    DOUBLE_DICES_5_5("DOUBLE_DICES_5_5", 6, 39),
    DOUBLE_DICES_5_6("DOUBLE_DICES_5_6", 6, 40),
    DOUBLE_DICES_6_6("DOUBLE_DICES_6_6", 6, 41),
    TRIPLE_DICES_1("TRIPLE_DICES_1", 31, 42),
    TRIPLE_DICES_2("TRIPLE_DICES_2", 31, 43),
    TRIPLE_DICES_3("TRIPLE_DICES_3", 31, 44),
    TRIPLE_DICES_4("TRIPLE_DICES_4", 31, 45),
    TRIPLE_DICES_5("TRIPLE_DICES_5", 31, 46),
    TRIPLE_DICES_6("TRIPLE_DICES_6", 31, 47),
    TAI("TAI", 2, 48),
    XIU("XIU", 2, 49),
    CHAN("CHAN", 2, 50),
    LE("LE", 2, 51),
    ANY_TRIPLE_DICES("ANY_TRIPLE_DICES", 31, 52);

    private String name;
    private int rotation;
    private int id;

    private PotSicbo(String name, int rotation, int id) {
        this.name = name;
        this.rotation = rotation;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public int getRotation() {
        return this.rotation;
    }

    public int getId() {
        return this.id;
    }

    public static PotSicbo getEnumByName(String name) {
        for (PotSicbo potSicbo : PotSicbo.values()) {
            if (!potSicbo.name.equals(name)) continue;
            return potSicbo;
        }
        throw new IllegalArgumentException("No enum constant with name " + name);
    }

    public static PotSicbo getEnumById(int id) {
        for (PotSicbo potSicbo : PotSicbo.values()) {
            if (potSicbo.id != id) continue;
            return potSicbo;
        }
        throw new IllegalArgumentException("No enum constant with id " + id);
    }
}

