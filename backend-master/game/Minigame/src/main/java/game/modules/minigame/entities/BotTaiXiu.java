/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities;

public class BotTaiXiu {
    private String nickname;
    private short timeBetting;
    private long betValue;
    private short betSide;

    public BotTaiXiu(String nickname, short timeBetting, long betValue, short betSide) {
        this.nickname = nickname;
        this.timeBetting = timeBetting;
        this.betValue = betValue;
        this.betSide = betSide;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setTimeBetting(short timeBetting) {
        this.timeBetting = timeBetting;
    }

    public short getTimeBetting() {
        return this.timeBetting;
    }

    public void setBetValue(long betValue) {
        this.betValue = betValue;
    }

    public long getBetValue() {
        return this.betValue;
    }

    public void setBetSide(short betSide) {
        this.betSide = betSide;
    }

    public short getBetSide() {
        return this.betSide;
    }
}

