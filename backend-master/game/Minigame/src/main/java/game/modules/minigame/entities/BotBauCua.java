/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities;

public class BotBauCua {
    private String nickname;
    private short timeBetting;
    private String betStr;

    public BotBauCua(String nickname, short timeBetting, String betStr) {
        this.nickname = nickname;
        this.timeBetting = timeBetting;
        this.betStr = betStr;
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

    public void setBetStr(String betStr) {
        this.betStr = betStr;
    }

    public String getBetStr() {
        return this.betStr;
    }
}

