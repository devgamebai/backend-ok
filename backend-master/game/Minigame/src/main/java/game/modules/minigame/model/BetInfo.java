/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.model;

public class BetInfo {
    private String betType;
    private int totalUser;
    private long totalAmount;

    public BetInfo(String betType, int totalUser, long totalAmount) {
        this.betType = betType;
        this.totalUser = totalUser;
        this.totalAmount = totalAmount;
    }

    public String getBetType() {
        return this.betType;
    }

    public void setBetType(String betType) {
        this.betType = betType;
    }

    public int getTotalUser() {
        return this.totalUser;
    }

    public void setTotalUser(int totalUser) {
        this.totalUser = totalUser;
    }

    public long getTotalAmount() {
        return this.totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String toString() {
        return "BetInfo{betType='" + this.betType + '\'' + ", totalUser=" + this.totalUser + ", totalAmount=" + this.totalAmount + '}';
    }
}

