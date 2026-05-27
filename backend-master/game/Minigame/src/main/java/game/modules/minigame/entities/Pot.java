/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities;

public class Pot {
    protected long totalValue;

    public void renew() {
        this.totalValue = 0L;
    }

    public long getTotalValue() {
        return this.totalValue;
    }
}

