/*
 * Decompiled with CFR 0.144.
 */
package game.xocdia.entities;

public class RefundModel {
    public long moneyRefund;
    public long currentMoney;
    public String pots;
    public String moneyRfPots;

    public RefundModel(long moneyRefund, long currentMoney, String pots, String moneyRfPots) {
        this.moneyRefund = moneyRefund;
        this.currentMoney = currentMoney;
        this.pots = pots;
        this.moneyRfPots = moneyRfPots;
    }

    public RefundModel() {
    }
}

