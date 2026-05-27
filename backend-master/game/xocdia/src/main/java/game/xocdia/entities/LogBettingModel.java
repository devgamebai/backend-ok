/*
 * Decompiled with CFR 0.144.
 */
package game.xocdia.entities;

public class LogBettingModel {
    private String nickname;
    private int potId;
    private long money;
    private int type;

    public LogBettingModel(String nickname, int potId, long money, int type) {
        this.nickname = nickname;
        this.potId = potId;
        this.money = money;
        this.type = type;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPotId() {
        return this.potId;
    }

    public void setPotId(int potId) {
        this.potId = potId;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

