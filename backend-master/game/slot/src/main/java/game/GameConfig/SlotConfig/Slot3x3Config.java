package game.GameConfig.SlotConfig;

import game.modules.GameUtil;

/**
 * Configuration for 3x3 slot games (Kim Cuong / DragonBall).
 * 3 reels, 3 rows, 6 symbols (0-5), 3 unique paylines.
 *
 * Symbol IDs:
 *   0 = WILD (substitutes for any symbol)
 *   1 = Red (Do)
 *   2 = Green (Xanh Luc)
 *   3 = Leaf (La)
 *   4 = Purple (Tim)
 *   5 = Blue (Xanh Duong)
 */
public class Slot3x3Config {

    public static final int NUM_SYMBOLS = 6;
    public static final int ROWS = 3;
    public static final int COLS = 3;
    public static final int CELLS = ROWS * COLS; // 9

    public static final int WILD = 0;

    // Per-reel symbol weights (tunable for RTP)
    // Higher weight = more frequent. WILD is rare.
    public static final int[][] REEL_WEIGHTS = {
        // Reel 0 (left):   WILD, Red, Green, Leaf, Purple, Blue
        {1, 4, 5, 6, 7, 7},   // total = 30
        // Reel 1 (center):
        {1, 4, 5, 6, 7, 7},   // total = 30
        // Reel 2 (right):
        {1, 4, 5, 6, 7, 7},   // total = 30
    };

    public static final int[] REEL_TOTALS = {30, 30, 30};

    // Payout table: GIFT[symbolId][matchCount]
    // matchCount: index 0=unused, 1=unused, 2=unused, 3=three-of-a-kind
    // Values are multipliers of bet per line
    public static final int[][] GIFT_TABLE = {
        // WILD:    3-match = 50x (jackpot trigger handled separately)
        {0, 0, 0, 50},
        // Red:     3-match = 20x
        {0, 0, 0, 20},
        // Green:   3-match = 15x
        {0, 0, 0, 15},
        // Leaf:    3-match = 10x
        {0, 0, 0, 10},
        // Purple:  3-match = 5x
        {0, 0, 0, 5},
        // Blue:    3-match = 3x
        {0, 0, 0, 3},
    };

    // Fund distribution (percentage of total bet)
    public int FEE = 4;
    public int RATE_TO_JACKPOT = 1;
    public int RATE_TO_FUND_JACKPOT = 1;
    public int RATE_TO_FUND_MINIGAME = 1;
    public int MULTI_JACKPOT = 3;

    /**
     * Generate a random 3x3 table (9 cells) using per-reel weights.
     * Layout: [row0col0, row0col1, row0col2, row1col0, row1col1, row1col2, row2col0, row2col1, row2col2]
     */
    public byte[] generateRandomTable() {
        byte[] table = new byte[CELLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                table[row * COLS + col] = getValueForReel(col);
            }
        }
        return table;
    }

    /**
     * Generate a random table without WILD symbols.
     */
    public byte[] generateRandomTableNoWild() {
        byte[] table = new byte[CELLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                table[row * COLS + col] = getValueForReelNoWild(col);
            }
        }
        return table;
    }

    /**
     * Generate a table guaranteed to have no winning lines.
     */
    public byte[] generateLosingTable() {
        byte[] table;
        int attempts = 0;
        do {
            table = generateRandomTableNoWild();
            attempts++;
        } while (hasAnyWin(table) && attempts < 100);
        return table;
    }

    private boolean hasAnyWin(byte[] table) {
        // Check 3 horizontal lines
        for (int row = 0; row < ROWS; row++) {
            byte s0 = table[row * COLS];
            byte s1 = table[row * COLS + 1];
            byte s2 = table[row * COLS + 2];
            if (s0 == s1 && s1 == s2) return true;
        }
        return false;
    }

    public byte getValueForReel(int reelIndex) {
        int total = REEL_TOTALS[reelIndex];
        int random = GameUtil.randomMax(total);
        int[] weights = REEL_WEIGHTS[reelIndex];
        for (byte i = 0; i < weights.length; i++) {
            if (random < weights[i]) return i;
            random -= weights[i];
        }
        return (byte) (NUM_SYMBOLS - 1);
    }

    public byte getValueForReelNoWild(int reelIndex) {
        // Sum weights excluding WILD (index 0)
        int[] weights = REEL_WEIGHTS[reelIndex];
        int total = 0;
        for (int i = 1; i < weights.length; i++) total += weights[i];

        int random = GameUtil.randomMax(total);
        for (byte i = 1; i < weights.length; i++) {
            if (random < weights[i]) return i;
            random -= weights[i];
        }
        return (byte) (NUM_SYMBOLS - 1);
    }
}
