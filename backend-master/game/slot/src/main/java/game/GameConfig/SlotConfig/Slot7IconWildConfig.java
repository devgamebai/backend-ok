package game.GameConfig.SlotConfig;

import game.modules.GameUtil;

public class Slot7IconWildConfig {
    public byte[] rateIcon = {1, 3, 5, 4, 7, 7, 3};
    public int totalRate = 30;
    public byte[] rateIconNotWild = {1, 3, 0, 4, 7, 7, 3};
    public int totalRateNotWild = 25;

    public int RATE_TO_JACKPOT = 1;
    public int RATE_TO_FUND_JACKPOT = 1;
    public int RATE_TO_FUND_MINIGAME = 1;
    public int FEE = 4;
    public int MULTI_JACKPOT = 3;

    public byte[] generateRandomTable() {
        byte[] table = new byte[15];
        for (int i = 0; i < 15; i++) {
            table[i] = getValueOfIcon();
        }
        return table;
    }

    public byte[] generateRandomTableNoWild() {
        byte[] table = new byte[15];
        for (int i = 0; i < 15; i++) {
            table[i] = getValueOfIconNotWild();
        }
        return table;
    }

    public byte getValueOfIcon() {
        int random = GameUtil.randomMax(totalRate);
        for (byte i = 0; i < rateIcon.length; i++) {
            if (random < rateIcon[i]) return i;
            random -= rateIcon[i];
        }
        return (byte) (rateIcon.length - 1);
    }

    public byte getValueOfIconNotWild() {
        int random = GameUtil.randomMax(totalRateNotWild);
        for (byte i = 0; i < rateIconNotWild.length; i++) {
            if (rateIconNotWild[i] == 0) continue;
            if (random < rateIconNotWild[i]) return i;
            random -= rateIconNotWild[i];
        }
        return (byte) (rateIconNotWild.length - 1);
    }
}
