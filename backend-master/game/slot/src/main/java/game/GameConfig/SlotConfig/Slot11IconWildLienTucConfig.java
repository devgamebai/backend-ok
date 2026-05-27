package game.GameConfig.SlotConfig;

import game.modules.GameUtil;

/**
 * Symbol distribution config for Chiêm Tinh (Slot6) — 11 symbols, 5 reels.
 * Updated per RTP.xlsx spec (SUN-65).
 *
 * Symbol IDs: 0=SCATTER, 1=BONUS, 2=WILD, 3=JACKPOT,
 *             4=A, 5=B, 6=C, 7=D, 8=E, 9=F, 10=G
 */
public class Slot11IconWildLienTucConfig {

    // Legacy single-reel weights (kept for backward compat with JSON config overlay)
    public byte[] rateIcon = {1, 3, 5, 4, 7, 7, 3, 3, 3, 2, 2};
    public int totalRate = 40;
    public byte[] rateIconNotWild = {1, 3, 0, 4, 7, 7, 3, 3, 3, 2, 2};
    public int totalRateNotWild = 35;

    // Per-reel weights from RTP.xlsx: [SCATTER, BONUS, WILD, JACKPOT, A, B, C, D, E, F, G]
    // Reel1=col0, Reel2=col1, Reel3=col2, Reel4=col3, Reel5=col4
    private static final byte[][] REEL_WEIGHTS = {
        {1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 8},   // Reel 1 (total=39)
        {1, 1, 12, 3, 1, 1, 1, 2, 3, 5, 7},   // Reel 2 (total=37) — Wild=12 here
        {1, 1, 1, 1, 1, 1, 1, 1, 3, 5, 7},    // Reel 3 (total=23)
        {1, 1, 1, 1, 1, 1, 1, 1, 2, 4, 6},    // Reel 4 (total=20)
        {1, 1, 1, 0, 1, 1, 1, 1, 2, 4, 6},    // Reel 5 (total=19) — Jackpot=0 here
    };

    private static final int[] REEL_TOTALS = {39, 37, 23, 20, 19};

    public int RATE_TO_JACKPOT = 1;
    public int RATE_TO_FUND_JACKPOT = 1;
    public int RATE_TO_FUND_MINIGAME = 1;
    public int FEE = 4;
    public int MULTI_JACKPOT = 3;

    /**
     * Generate random 15-cell matrix using per-reel weights (RTP.xlsx spec).
     * Layout: cells 0-4 = row 0, cells 5-9 = row 1, cells 10-14 = row 2.
     * Column (reel) = cell index % 5.
     */
    public byte[] generateRandomTable() {
        byte[] table = new byte[15];
        for (int i = 0; i < 15; i++) {
            int col = i % 5; // reel index 0-4
            table[i] = getValueForReel(col);
        }
        return table;
    }

    /**
     * Generate random table WITHOUT wild symbols (for free spin / bonus triggers).
     */
    public byte[] generateRandomTableNoWild() {
        byte[] table = new byte[15];
        for (int i = 0; i < 15; i++) {
            int col = i % 5;
            table[i] = getValueForReelNoWild(col);
        }
        return table;
    }

    /**
     * Pick a random symbol for a specific reel using per-reel weights.
     */
    private byte getValueForReel(int reelIndex) {
        byte[] weights = REEL_WEIGHTS[reelIndex];
        int total = REEL_TOTALS[reelIndex];
        int random = GameUtil.randomMax(total);
        for (byte i = 0; i < weights.length; i++) {
            if (random < weights[i]) {
                return i;
            }
            random -= weights[i];
        }
        return (byte) (weights.length - 1);
    }

    /**
     * Pick a random symbol excluding WILD (id=2) for a specific reel.
     */
    private byte getValueForReelNoWild(int reelIndex) {
        byte[] weights = REEL_WEIGHTS[reelIndex];
        int totalNoWild = REEL_TOTALS[reelIndex] - weights[2]; // subtract WILD weight
        if (totalNoWild <= 0) totalNoWild = 1;
        int random = GameUtil.randomMax(totalNoWild);
        for (byte i = 0; i < weights.length; i++) {
            if (i == 2) continue; // skip WILD
            if (random < weights[i]) {
                return i;
            }
            random -= weights[i];
        }
        return (byte) (weights.length - 1);
    }

    // Legacy methods (kept for backward compat if called elsewhere)
    public byte getValueOfIconNotWild() {
        return getValueForReelNoWild(0);
    }

    public byte getValueOfIcon() {
        return getValueForReel(0);
    }
}
