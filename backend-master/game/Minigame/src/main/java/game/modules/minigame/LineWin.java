/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame;

public class LineWin {
    private int line;
    private double prizeAmount;
    private boolean isJackpot;

    public int getLine() {
        return this.line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public boolean isJackpot() {
        return this.isJackpot;
    }

    public void setJackpot(boolean isJackpot) {
        this.isJackpot = isJackpot;
    }

    public double getPrizeAmount() {
        return this.prizeAmount;
    }

    public void setPrizeAmount(double prizeAmount) {
        this.prizeAmount = prizeAmount;
    }

    public String toString() {
        return "LineWin{line=" + this.line + ",amount=" + this.prizeAmount + ",jp=" + this.isJackpot + '}';
    }
}

