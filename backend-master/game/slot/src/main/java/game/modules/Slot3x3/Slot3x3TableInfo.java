package game.modules.Slot3x3;

import game.GameConfig.SlotConfig.Slot3x3Config;
import game.modules.SlotUtils.TableInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Table info for 3x3 slot games.
 * Generates a 3x3 matrix, calculates wins on 3 horizontal paylines.
 * Supports WILD substitution and jackpot detection.
 */
public class Slot3x3TableInfo extends TableInfo {

    public byte[] tableCalculate;
    public boolean isJackPot; // alias for jackpot
    private Slot3x3Config config;

    /**
     * Create with a random table.
     */
    public Slot3x3TableInfo(Slot3x3Config config, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.config = config;
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
        this.table = config.generateRandomTable();
        this.tableCalculate = this.table.clone();
    }

    /**
     * Create with a specific table (for forced results).
     */
    public Slot3x3TableInfo(Slot3x3Config config, byte[] table, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.config = config;
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
        this.table = table;
        this.tableCalculate = table.clone();
    }

    /**
     * Calculate wins for the selected lines.
     * @param clientLineIds array of client line IDs (1-20)
     */
    /**
     * Constructor for forced results with a 2D byte array (rows x cols).
     */
    public Slot3x3TableInfo(byte[][] forcedTable, int betValue, int moneyEatPot) {
        this.config = new Slot3x3Config();
        this.betLevel = betValue;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = new ArrayList<>();
        this.boxValues.add(10);
        // Flatten 2D to 1D
        this.table = new byte[forcedTable.length * forcedTable[0].length];
        for (int i = 0; i < forcedTable.length; i++)
            for (int j = 0; j < forcedTable[i].length; j++)
                this.table[i * forcedTable[i].length + j] = forcedTable[i][j];
        this.tableCalculate = this.table.clone();
    }

    /**
     * Re-calculate wins for given client lines (used after forced result).
     */
    public void calculateRowIndex(int[] clientLineIds) {
        this.calculate(clientLineIds);
    }

    public void calculate(int[] clientLineIds) {
        this.money = 0;
        this.jackpot = false;
        this.isJackPot = false;
        this.freeSpin = 0;
        this.miniGame = 0;
        this.lineWin = new ArrayList<>();
        this.moneyWin = new ArrayList<>();

        // Track which physical lines we've already checked (avoid double-counting)
        boolean[] physicalChecked = new boolean[3];

        for (int clientLine : clientLineIds) {
            int physical = Slot3x3Util.getPhysicalLine(clientLine);
            if (physical < 0 || physical >= Slot3x3Util.PAYLINES.length) continue;
            if (physicalChecked[physical]) {
                // Same physical line already counted — still counts as a bet line
                // but doesn't add additional win
                continue;
            }
            physicalChecked[physical] = true;

            int[] cells = Slot3x3Util.PAYLINES[physical];
            byte s0 = this.tableCalculate[cells[0]];
            byte s1 = this.tableCalculate[cells[1]];
            byte s2 = this.tableCalculate[cells[2]];

            int matchSymbol = Slot3x3Util.checkLineMatch(s0, s1, s2);
            if (matchSymbol >= 0) {
                int payout = Slot3x3Util.getPayout(matchSymbol);
                if (payout > 0) {
                    // Find the first client line ID that maps to this physical line
                    // (so the client can highlight the right line)
                    int winLineId = findFirstClientLine(clientLineIds, physical);
                    this.lineWin.add(winLineId);
                    this.moneyWin.add((long) payout);
                    this.money += payout;

                    // 3x WILD = jackpot
                    if (matchSymbol == Slot3x3Config.WILD) {
                        this.jackpot = true;
                        this.isJackPot = true;
                    }
                }
            }
        }
    }

    /**
     * Find the first client line ID in the selection that maps to a physical line.
     */
    private int findFirstClientLine(int[] clientLineIds, int physicalLine) {
        for (int clientLine : clientLineIds) {
            if (Slot3x3Util.getPhysicalLine(clientLine) == physicalLine) {
                return clientLine;
            }
        }
        return physicalLine + 1; // fallback
    }

    /**
     * Generate a table guaranteed to lose (no winning lines).
     */
    public static Slot3x3TableInfo createLosingTable(Slot3x3Config config, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        byte[] table = config.generateLosingTable();
        Slot3x3TableInfo info = new Slot3x3TableInfo(config, table, betLevel, moneyEatPot, boxValues);
        return info;
    }

    /**
     * Main entry point: generate table, calculate, and apply fund/jackpot constraints.
     * Similar to Slot11IconWildLienTucUtil.getSlot11IconLienTucTableInfo but for 3x3.
     */
    public static Slot3x3TableInfo generate(
            Slot3x3Config config,
            int[] clientLineIds,
            long betLevel,
            long fund,
            long fundJackpot,
            long fundMinigame,
            boolean isX2,
            long pot,
            long initPotValue,
            List<Integer> boxValues,
            boolean isSpinFree,
            long maxWin
    ) {
        int retries = 0;
        while (retries < 20) {
            Slot3x3TableInfo info = new Slot3x3TableInfo(config, betLevel, 0, boxValues);
            info.calculate(clientLineIds);

            long totalPrize = info.money * betLevel;

            // Check jackpot: only allow if fund can cover it
            if (info.jackpot) {
                long jackpotPrize = isX2 ? pot * config.MULTI_JACKPOT : pot;
                if (fundJackpot < jackpotPrize || pot <= initPotValue * 2) {
                    // Can't afford jackpot — retry
                    retries++;
                    continue;
                }
            }

            // Check fund can cover the win
            if (totalPrize > fund && totalPrize > 0) {
                retries++;
                continue;
            }

            // Check max win
            if (maxWin > 0 && totalPrize > maxWin) {
                retries++;
                continue;
            }

            return info;
        }

        // Fallback: return a losing table
        return createLosingTable(config, betLevel, 0, boxValues);
    }
}
