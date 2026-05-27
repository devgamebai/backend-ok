/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.xocdia.conf.XocDiaConfig
 */
package game.xocdia.entities;

import game.xocdia.conf.XocDiaConfig;

public enum PotType {
    EVEN((byte)0, 2.0, XocDiaConfig.maxBetChanLe, "Ch\u1eb5n"),
    ODD((byte)1, 2.0, XocDiaConfig.maxBetChanLe, "L\u1ebb"),
    FOUR_WHITE((byte)2, XocDiaConfig.ratio4, XocDiaConfig.maxBet4, "4 Tr\u1eafng"),
    FOUR_BLACK((byte)3, XocDiaConfig.ratio4, XocDiaConfig.maxBet4, "4 \u0110en"),
    ONE_WHITE((byte)4, XocDiaConfig.ratio1, XocDiaConfig.maxBet1, "1 Tr\u1eafng"),
    ONE_BLACK((byte)5, XocDiaConfig.ratio1, XocDiaConfig.maxBet1, "1 \u0110en");
    
    private byte id;
    private double ratio;
    private double maxRatioBet;
    private String name;

    private PotType(byte id, double ratio, double maxRatioBet, String name) {
        this.id = id;
        this.ratio = ratio;
        this.maxRatioBet = maxRatioBet;
        this.name = name;
    }

    public byte getId() {
        return this.id;
    }

    public double getRatio() {
        return this.ratio;
    }

    public String getName() {
        return this.name;
    }

    public double getMaxRatioBet() {
        return this.maxRatioBet;
    }

    public static PotType findPotType(int id) {
        for (PotType entry : PotType.values()) {
            if (entry.getId() != id) continue;
            return entry;
        }
        return null;
    }
}

