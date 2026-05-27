/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities.pokego;

public class PickStarResponse {
    private long totalValue;
    private String prizes;

    public void setTotalValue(long totalValue) {
        this.totalValue = totalValue;
    }

    public long getTotalValue() {
        return this.totalValue;
    }

    public void setPrizes(String prizes) {
        this.prizes = prizes;
    }

    public String getPrizes() {
        return this.prizes;
    }
}

