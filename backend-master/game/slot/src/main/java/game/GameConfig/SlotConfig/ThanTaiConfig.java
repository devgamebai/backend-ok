package game.GameConfig.SlotConfig;

import game.modules.GameUtil;

/**
 * Symbol distribution config for Thần Tài (God of Wealth) slot — 11 symbols, 5 reels.
 * Uses same reel layout as Chiêm Tinh but with Thần Tài paytable.
 *
 * Symbol IDs: 0=SCATTER, 1=BONUS, 2=WILD, 3=JACKPOT,
 *             4=Chậu Cây, 5=Túi Tiền, 6=A, 7=K, 8=Q, 9=J, 10=10
 */
public class ThanTaiConfig {

    // Legacy single-reel weights (for JSON config overlay)
    public byte[] rateIcon = {1, 3, 5, 4, 7, 7, 3, 3, 3, 2, 2};
    public int totalRate = 40;
    public byte[] rateIconNotWild = {1, 3, 0, 4, 7, 7, 3, 3, 3, 2, 2};
    public int totalRateNotWild = 35;

    // Per-reel weights: [SCATTER, BONUS, WILD, JACKPOT, ChauCay, TuiTien, A, K, Q, J, 10]
    private static final byte[][] REEL_WEIGHTS = {
        {1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8},   // Reel 1 (total=39)
        {1, 1, 12, 3, 1, 1, 1, 2, 3, 5, 7},   // Reel 2 (total=37)
        {1, 1, 1, 1, 1, 1, 1, 1, 3, 5, 7},    // Reel 3 (total=23)
        {1, 1, 1, 1, 1, 1, 1, 1, 2, 4, 6},    // Reel 4 (total=20)
        {1, 1, 1, 0, 1, 1, 1, 1, 2, 4, 6},    // Reel 5 (total=19) — Jackpot=0
    };

    private static final int[] REEL_TOTALS = {39, 37, 23, 20, 19};

    public int RATE_TO_JACKPOT = 1;
    public int RATE_TO_FUND_JACKPOT = 1;
    public int RATE_TO_FUND_MINIGAME = 1;
    public int FEE = 4;
    public int MULTI_JACKPOT = 3;

    public byte[] generateRandomTable() {
        byte[] table = new byte[15];
        for (int i = 0; i < 15; i++) {
            int col = i % 5;
            table[i] = getValueForReel(col);
        }
        return table;
    }

    public byte[] generateRandomTableNoWild() {
        byte[] table = new byte[15];
        for (int i = 0; i < 15; i++) {
            int col = i % 5;
            table[i] = getValueForReelNoWild(col);
        }
        return table;
    }

    private byte getValueForReel(int reelIndex) {
        byte[] weights = REEL_WEIGHTS[reelIndex];
        int total = REEL_TOTALS[reelIndex];
        int random = GameUtil.randomMax(total);
        for (byte i = 0; i < weights.length; i++) {
            if (random < weights[i]) return i;
            random -= weights[i];
        }
        return (byte) (weights.length - 1);
    }

    private byte getValueForReelNoWild(int reelIndex) {
        byte[] weights = REEL_WEIGHTS[reelIndex];
        int totalNoWild = REEL_TOTALS[reelIndex] - weights[2];
        if (totalNoWild <= 0) totalNoWild = 1;
        int random = GameUtil.randomMax(totalNoWild);
        for (byte i = 0; i < weights.length; i++) {
            if (i == 2) continue;
            if (random < weights[i]) return i;
            random -= weights[i];
        }
        return (byte) (weights.length - 1);
    }

    public byte getValueOfIconNotWild() {
        return getValueForReelNoWild(0);
    }

    public byte getValueOfIcon() {
        return getValueForReel(0);
    }
}
