/*
 * Decompiled with CFR 0.144.
 */
package game.xocdia.entities;

public class BettingModel {
    public String nickname;
    public long money;
    public boolean isBot;

    public BettingModel(String nickname, long money, boolean isBot) {
        this.nickname = nickname;
        this.money = money;
        this.isBot = isBot;
    }
}

