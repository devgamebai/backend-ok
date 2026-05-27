package game.modules.ThanTai;

import game.GameConfig.GameConfig;
import game.GameConfig.RollLoseConfig.ThanTaiRollLoseConfig;
import game.modules.GameUtil;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Thần Tài (God of Wealth) slot — uses same 5x3 grid, 25 paylines,
 * wild expansion as Chiêm Tinh but with its own paytable.
 */
public class ThanTaiUtil {

    public static final int NUMBER_ICONS = 11;

    public static final byte FREE_SPIN = 0;
    public static final byte BONUS = 1;
    public static final byte WILD = 2;
    public static final byte JACKPOT = 3;

    // Reuse same 25 paylines and column layout as Slot11IconWildLienTuc
    public static final byte[][] ROWS = {
            {5, 6, 7, 8, 9},
            {0, 1, 2, 3, 4},
            {10, 11, 12, 13, 14},
            {10, 6, 2, 8, 14},
            {0, 6, 12, 8, 4},

            {5, 1, 2, 3, 9},
            {5, 11, 12, 13, 9},
            {0, 1, 7, 13, 14},
            {10, 11, 7, 3, 4},
            {5, 11, 7, 3, 9},

            {5, 1, 7, 13, 9},
            {0, 6, 7, 8, 4},
            {10, 6, 7, 8, 14},
            {0, 6, 2, 8, 4},
            {10, 6, 12, 8, 14},

            {5, 6, 2, 8, 9},
            {5, 6, 12, 8, 9},
            {0, 1, 12, 3, 4},
            {10, 11, 2, 13, 14},
            {0, 11, 12, 13, 4},

            {10, 1, 2, 3, 14},
            {5, 1, 12, 3, 9},
            {5, 11, 2, 13, 9},
            {0, 11, 2, 13, 4},
            {10, 1, 12, 3, 14},
    };

    public static final byte[][] COLUMNS = {
            {0, 5, 10},
            {1, 6, 11},
            {2, 7, 12},
            {3, 8, 13},
            {4, 9, 14},
    };

    // Thần Tài paytable from "Paytable Thần tài.html"
    // Symbols: 0=SCATTER, 1=BONUS, 2=WILD, 3=JACKPOT/Wild, 4=Chậu Cây, 5=Túi Tiền, 6=A, 7=K, 8=Q, 9=J, 10=10
    // Each row: [1match, 2match, 3match, 4match, 5match, 6+match]
    public static Gift[][] GIFT_TABLES = {
            // 0: SCATTER — 3=3free, 4=6free, 5=18free
            {null, null, null,
                    new Gift(GiftType.FREE_SPIN, 3), new Gift(GiftType.FREE_SPIN, 6), new Gift(GiftType.FREE_SPIN, 18)},
            // 1: BONUS — 3=bonus x1, 4=bonus x2, 5=bonus x5
            {null, null, null,
                    new Gift(GiftType.MINI_GAME, 1), new Gift(GiftType.MINI_GAME, 2), new Gift(GiftType.MINI_GAME, 5)},
            // 2: WILD — 2=8x, 3=50x, 4=1000x, 5=8000x
            {null, new Gift(GiftType.MONEY, 8), new Gift(GiftType.MONEY, 50),
                    new Gift(GiftType.MONEY, 1000), new Gift(GiftType.MONEY, 8000), null},
            // 3: JACKPOT — 2=4x, 3=25x, 4=100x, 5=JACKPOT
            {null, new Gift(GiftType.MONEY, 4), new Gift(GiftType.MONEY, 25),
                    new Gift(GiftType.MONEY, 100), new Gift(GiftType.JACKPOT, 1), null},
            // 4: Chậu Cây — 3=20x, 4=75x, 5=500x
            {null, null, new Gift(GiftType.MONEY, 20),
                    new Gift(GiftType.MONEY, 75), new Gift(GiftType.MONEY, 500), null},
            // 5: Túi Tiền — 3=16x, 4=60x, 5=375x
            {null, null, new Gift(GiftType.MONEY, 16),
                    new Gift(GiftType.MONEY, 60), new Gift(GiftType.MONEY, 375), null},
            // 6: A — 3=12x, 4=45x, 5=275x
            {null, null, new Gift(GiftType.MONEY, 12),
                    new Gift(GiftType.MONEY, 45), new Gift(GiftType.MONEY, 275), null},
            // 7: K — 3=10x, 4=30x, 5=150x
            {null, null, new Gift(GiftType.MONEY, 10),
                    new Gift(GiftType.MONEY, 30), new Gift(GiftType.MONEY, 150), null},
            // 8: Q — 3=5x, 4=25x, 5=50x
            {null, null, new Gift(GiftType.MONEY, 5),
                    new Gift(GiftType.MONEY, 25), new Gift(GiftType.MONEY, 50), null},
            // 9: J — 3=3x, 4=10x, 5=25x
            {null, null, new Gift(GiftType.MONEY, 3),
                    new Gift(GiftType.MONEY, 10), new Gift(GiftType.MONEY, 25), null},
            // 10: 10 — 3=2x, 4=5x, 5=10x
            {null, null, new Gift(GiftType.MONEY, 2),
                    new Gift(GiftType.MONEY, 5), new Gift(GiftType.MONEY, 10), null},
    };

    public static Gift getGift(RowValue rowValue) {
        return GIFT_TABLES[rowValue.icon][rowValue.number];
    }

    public static byte[] getIconsInRow(byte[] table, byte[] pos) {
        if (pos.length != 5) {
            throw new IllegalArgumentException();
        }
        byte[] row = new byte[5];
        for (int i = 0; i < pos.length; i++) {
            row[i] = table[pos[i]];
        }
        return row;
    }

    public static void changeColumnWild(byte[] table, byte[] columns) {
        for (int i = 0; i < columns.length; i++) {
            table[columns[i]] = WILD;
        }
    }

    public static void changeColumnNotWild(byte[] table, byte[] columns) {
        for (int i = 0; i < columns.length; i++) {
            if (table[columns[i]] < WILD || table[columns[i]] == JACKPOT) {
                table[columns[i]] = (byte) GameUtil.randomBetween(4, 9);
            }
        }
    }

    public static byte[] validateTable(byte[] table) {
        for (int i = 0; i < COLUMNS.length; i++) {
            if (i == 0 || i == 4) continue;
            byte[] columns = COLUMNS[i];
            for (int j = 0; j < columns.length; j++) {
                if (table[columns[j]] == WILD) {
                    changeColumnNotWild(table, columns);
                }
            }
        }
        return table;
    }

    public static RowValue getRowValue(byte[] row) {
        byte indexMax = row[0];
        byte valueMax = 0;
        for (byte i = 0; i < row.length; i++) {
            if (indexMax < WILD || indexMax == JACKPOT) {
                if (row[i] == indexMax) {
                    valueMax += 1;
                } else {
                    break;
                }
            } else {
                if (row[i] == indexMax || row[i] == WILD) {
                    valueMax += 1;
                } else {
                    break;
                }
            }
        }
        return new RowValue(indexMax, valueMax);
    }

    public static ThanTaiTableInfo rollLose0(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
        ThanTaiTableInfo tableInfo = new ThanTaiTableInfo(
                GameConfig.getInstance().thanTaiRollLoseConfig.getTableRollLose(),
                betLevel, moneyEatJackpot, boxValues);
        tableInfo.calculate(rowIndex);
        return tableInfo;
    }

    public static ThanTaiTableInfo rollLose20(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            ThanTaiTableInfo tableInfo = new ThanTaiTableInfo(betLevel, moneyEatJackpot, boxValues);
            tableInfo.calculate(rowIndex);
            if (!tableInfo.jackpot && !(tableInfo.freeSpin > 0) && !(tableInfo.miniGame > 0)
                    && !(tableInfo.money > rowIndex.length * 10)) {
                return tableInfo;
            }
            retryCount++;
            if (retryCount % 100 == 0) {
                try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
        ThanTaiTableInfo tableInfo = new ThanTaiTableInfo(betLevel, moneyEatJackpot, boxValues);
        tableInfo.calculate(rowIndex);
        return tableInfo;
    }

    public static long getMoneyJackPot(long jackPotMoney, boolean isX2, long initPotMoney) { // initPotMoney kept for API compat
        long moneyEatJackpot = jackPotMoney;
        if (isX2) {
            moneyEatJackpot = jackPotMoney * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT;
        }
        return moneyEatJackpot;
    }

    public static ThanTaiTableInfo getBigWinTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                      long fundMinigame, boolean isX2, long jackPotMoney, long initPotMoney,
                                                      List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {
        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);
        int x = GameUtil.randomMax(100);
        if (x < 50) {
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            ThanTaiTableInfo tableInfo = new ThanTaiTableInfo(betLevel, moneyEatJackpot, boxValues);
            tableInfo.calculate(rowIndex);
            if (tableInfo.jackpot) { retryCount++; if (retryCount % 1000 == 0) { try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; } } continue; }
            if (tableInfo.freeSpin > 0) { retryCount++; if (retryCount % 1000 == 0) { try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; } } continue; }
            return tableInfo;
        }
        ThanTaiTableInfo tableInfo = new ThanTaiTableInfo(betLevel, moneyEatJackpot, boxValues);
        tableInfo.calculate(rowIndex);
        return tableInfo;
    }

    public static ThanTaiTableInfo getTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                long fundMinigame, boolean isX2, long jackPotMoney, long initPotMoney,
                                                List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {
        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);
        ThanTaiTableInfo tableInfo = new ThanTaiTableInfo(betLevel, moneyEatJackpot, boxValues);

        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            tableInfo.calculate(rowIndex);
            if (!tableInfo.jackpot) break;
            retryCount++;
            if (retryCount % 1000 == 0) { try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; } }
        }

        long totalWin = 0;
        if (tableInfo.jackpot) {
            if (isSpinFree) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (rowIndex.length < ROWS.length) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.miniGame > 0) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.freeSpin > 0) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (isX2) {
                if (fundJackpot < jackPotMoney * GameConfig.getInstance().thanTaiConfig.MULTI_JACKPOT)
                    return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            } else {
                if (fundJackpot < jackPotMoney) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += moneyEatJackpot;
        }
        if (tableInfo.miniGame > 0) {
            if (isSpinFree) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.miniGame > 5) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.freeSpin > 0) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (rowIndex.length < ROWS.length) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.miniGameSlotResponse == null) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.miniGameSlotResponse.getTotalPrize() > fundMinigame) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            totalWin += tableInfo.miniGameSlotResponse.getTotalPrize();
        }
        if (tableInfo.freeSpin > 0) {
            if (isSpinFree) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.freeSpin > 4) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (rowIndex.length < ROWS.length) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            if (tableInfo.freeSpin * ROWS.length * betLevel > fund) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }
        if (rowIndex.length < ROWS.length) {
            if (tableInfo.money > rowIndex.length * 2) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }
        if (tableInfo.money * betLevel > fund) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        totalWin += tableInfo.money * betLevel;
        if (totalWin > maxiumWin) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        return tableInfo;
    }
}
