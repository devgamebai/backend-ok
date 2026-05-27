/*
 * Decompiled with CFR 0.144.
 */
package game.xocdia.entities;

public class SubBanker {
    public byte potId;
    public String nickname;
    public long money;
    public long moneyNoFee;
    public long currentMoney;

    public SubBanker(byte potId, String nickname, long money, long moneyNoFee, long currentMoney) {
        this.potId = potId;
        this.nickname = nickname;
        this.money = money;
        this.moneyNoFee = moneyNoFee;
        this.currentMoney = currentMoney;
    }

    public SubBanker() {
    }
}

