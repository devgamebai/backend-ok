/*
 * Decompiled with CFR 0.144.
 */
package game.xocdia.entities;

public class RewardModel {
    public long moneyWin;
    public long moneyBet;
    public long fee;
    public long currentMoney;
    public String potsWin;
    public String moneyWinPots;

    public RewardModel(long moneyWin, long moneyBet, long fee, long currentMoney, String potsWin, String moneyWinPots) {
        this.moneyWin = moneyWin;
        this.moneyBet = moneyBet;
        this.fee = fee;
        this.currentMoney = currentMoney;
        this.potsWin = potsWin;
        this.moneyWinPots = moneyWinPots;
    }

    public RewardModel() {
    }
}

