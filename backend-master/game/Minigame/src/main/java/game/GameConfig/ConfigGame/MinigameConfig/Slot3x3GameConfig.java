/*
 * Decompiled with CFR 0.152.
 */
package game.GameConfig.ConfigGame.MinigameConfig;

import game.utils.GameUtil;

public class Slot3x3GameConfig {
    public int[] winRate = new int[]{-1, 850, 400, 200, 80, 8, 30, 4};
    public byte[] rateIcon = new byte[]{1, 4, 5, 6, 7, 8};
    public int totalRate = 31;
    public int FEE = 3;
    public int MONEY_TO_FUND_JACKPOT = 1;
    public int MONEY_TO_JACKPOT = 1;
    public int MULTI_JACKPOT = 2;

    public byte randomIcon(int totalNumber, byte[] listVal) {
        int random = GameUtil.randomMax(totalNumber);
        for (byte i = 0; i < listVal.length; i = (byte)(i + 1)) {
            if (random < listVal[i]) {
                return i;
            }
            random -= listVal[i];
        }
        return (byte)(listVal.length - 1);
    }

    public byte[][] getTableValue() {
        byte[][] tableValue = new byte[3][3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                tableValue[i][j] = this.randomIcon(this.totalRate, this.rateIcon);
            }
        }
        return tableValue;
    }
}

