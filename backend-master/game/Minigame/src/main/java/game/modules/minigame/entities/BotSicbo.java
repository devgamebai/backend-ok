/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.entities;

/**
 * SICBO — handler 28000.
 *
 * Renamed 2026-04-12 from TaiXiuSicbo* -> Sicbo* so stack traces, log greps,
 * and bug reports unambiguously distinguish Sicbo from TaiXiu (handler 2000).
 * See docs/TAIXIU-SICBO-GAME-ARCHITECTURE.md for the architecture rules,
 * especially the hardcoded-vs-config-driven round timer pitfall that caused
 * the Apr 8-12 instability window.
 */
public class BotSicbo
implements Comparable<BotSicbo> {
    private String nickname;
    private short timeBetting;
    private long betValue;
    private short betSide;
    private long money;
    private int avatar;

    public BotSicbo(String nickname, short timeBetting, long betValue, short betSide, long money, int avatar) {
        this.nickname = nickname;
        this.timeBetting = timeBetting;
        this.betValue = betValue;
        this.betSide = betSide;
        this.money = money;
        this.avatar = avatar;
    }

    public synchronized void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public synchronized String getNickname() {
        return this.nickname;
    }

    public synchronized void setTimeBetting(short timeBetting) {
        this.timeBetting = timeBetting;
    }

    public synchronized short getTimeBetting() {
        return this.timeBetting;
    }

    public synchronized void setBetValue(long betValue) {
        this.betValue = betValue;
    }

    public synchronized long getBetValue() {
        return this.betValue;
    }

    public synchronized void setBetSide(short betSide) {
        this.betSide = betSide;
    }

    public synchronized short getBetSide() {
        return this.betSide;
    }

    public synchronized long getMoney() {
        return this.money;
    }

    public synchronized void setMoney(long money) {
        this.money = money;
    }

    @Override
    public synchronized int compareTo(BotSicbo otherUser) {
        return Long.compare(otherUser.money, this.money);
    }

    public synchronized int getAvatar() {
        return this.avatar;
    }

    public synchronized void setAvatar(int avatar) {
        this.avatar = avatar;
    }
}

