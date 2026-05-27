package game.modules.Slot3x3;

import game.GameConfig.SlotConfig.Slot3x3Config;

/**
 * Utility for 3x3 slot games.
 * 3 paylines (horizontal rows), 6 symbols, WILD substitution.
 *
 * Payline definitions (3x3 grid):
 *   Line 1: row 0 → cells [0,1,2]  (top)
 *   Line 2: row 1 → cells [3,4,5]  (middle)
 *   Line 3: row 2 → cells [6,7,8]  (bottom)
 *
 * Client sends up to 20 "lines" but they map to these 3 physical lines:
 *   Lines 1,6,10,12,14,16,18  → physical line 1 (top row)
 *   Lines 2,4,5,7,8,20        → physical line 2 (middle row)
 *   Lines 3,9,11,13,15,17,19  → physical line 3 (bottom row)
 */
public class Slot3x3Util {

    // Physical payline positions: each is [col0_cell, col1_cell, col2_cell]
    // Cell index = row * 3 + col
    public static final int[][] PAYLINES = {
        {0, 1, 2},  // Line 1: top row
        {3, 4, 5},  // Line 2: middle row
        {6, 7, 8},  // Line 3: bottom row
    };

    // Map client line IDs (1-20) to physical payline index (0-2)
    // -1 means unmapped
    public static final int[] CLIENT_LINE_TO_PHYSICAL = {
        -1,  // index 0 unused
         0,  // line 1  → top
         1,  // line 2  → middle
         2,  // line 3  → bottom
         1,  // line 4  → middle
         1,  // line 5  → middle
         0,  // line 6  → top
         1,  // line 7  → middle
         1,  // line 8  → middle
         2,  // line 9  → bottom
         0,  // line 10 → top
         2,  // line 11 → bottom
         0,  // line 12 → top
         2,  // line 13 → bottom
         0,  // line 14 → top
         2,  // line 15 → bottom
         0,  // line 16 → top
         2,  // line 17 → bottom
         0,  // line 18 → top
         2,  // line 19 → bottom
         1,  // line 20 → middle
    };

    /**
     * Check if 3 symbols form a winning line.
     * WILD (0) substitutes for any symbol.
     * Returns the matched symbol ID, or -1 if no match.
     */
    public static int checkLineMatch(byte s0, byte s1, byte s2) {
        // All wild = wild win
        if (s0 == Slot3x3Config.WILD && s1 == Slot3x3Config.WILD && s2 == Slot3x3Config.WILD) {
            return Slot3x3Config.WILD;
        }

        // Find the non-wild symbol to match against
        int matchSymbol = -1;
        byte[] symbols = {s0, s1, s2};
        for (byte s : symbols) {
            if (s != Slot3x3Config.WILD) {
                if (matchSymbol == -1) {
                    matchSymbol = s;
                } else if (s != matchSymbol) {
                    return -1; // Different non-wild symbols = no match
                }
            }
        }
        return matchSymbol;
    }

    /**
     * Get payout multiplier for a 3-match of the given symbol.
     */
    public static int getPayout(int symbolId) {
        if (symbolId < 0 || symbolId >= Slot3x3Config.NUM_SYMBOLS) return 0;
        return Slot3x3Config.GIFT_TABLE[symbolId][3];
    }

    /**
     * Get the physical payline index for a client line ID.
     * Returns -1 if invalid.
     */
    public static int getPhysicalLine(int clientLineId) {
        if (clientLineId < 1 || clientLineId > 20) return -1;
        return CLIENT_LINE_TO_PHYSICAL[clientLineId];
    }

    /**
     * Generate a losing table (no winning lines).
     */
    public static Slot3x3TableInfo rollLose0(int[] clientLineIds, int betValue, int moneyEatPot) {
        Slot3x3Config config = new Slot3x3Config();
        java.util.List<Integer> boxValues = new java.util.ArrayList<>();
        boxValues.add(10);
        Slot3x3TableInfo info = Slot3x3TableInfo.createLosingTable(config, betValue, moneyEatPot, boxValues);
        info.calculate(clientLineIds);
        return info;
    }

    /**
     * Main entry: generate a 3x3 table with fund/jackpot constraints.
     */
    public static Slot3x3TableInfo getSlot3x1TableInfo(
            int[] clientLineIds, int betValue, long fund, long fundJackpot,
            boolean isX2, long pot, long initPotValue, long maxWin) {
        Slot3x3Config config = new Slot3x3Config();
        java.util.List<Integer> boxValues = new java.util.ArrayList<>();
        boxValues.add(10);
        return Slot3x3TableInfo.generate(config, clientLineIds, betValue, fund, fundJackpot, 0, isX2, pot, initPotValue, boxValues, false, maxWin);
    }
}
