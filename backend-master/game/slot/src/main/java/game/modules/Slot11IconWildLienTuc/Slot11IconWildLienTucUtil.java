package game.modules.Slot11IconWildLienTuc;

import com.pengrad.telegrambot.model.Game;
import game.GameConfig.GameConfig;
import game.GameConfig.RollLoseConfig.Slot11IconWildLienTucRollLoseConfig;
import game.modules.GameUtil;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;

import java.util.ArrayList;
import java.util.List;

public class Slot11IconWildLienTucUtil {

    public static final int NUMBER_ICONS = 11;

    public static final byte FREE_SPIN = 0;
    public static final byte BONUS = 1;
    public static final byte WILD = 2;
    public static final byte JACKPOT = 3;

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

    // GIFT_TABLES[icon][matchCount] — index 0=0match,1=1match,2=2match,3=3match,4=4match,5=5match
    // Scatter: 4 match → value 4 (room translates: 8 free spins ratio 2), 5 match → value 5 (8 free + ratio 3)
    // Bonus: 3 match = 1 mini, 4 match = 3 mini, 5 match = 5 mini
    // Wild: 2=8x, 3=50x, 4=1000x, 5=8000x
    public static Gift[][] GIFT_TABLES = {
            {null, null, null, null,
                    new Gift(GiftType.FREE_SPIN, 8), new Gift(GiftType.FREE_SPIN, 8)},  // Scatter: 4=8free, 5=8free (2x handled by client via ratio)
            {null, null, null,
                    new Gift(GiftType.MINI_GAME, 1), new Gift(GiftType.MINI_GAME, 3), new Gift(GiftType.MINI_GAME, 5)},  // Bonus: 3=1,4=3,5=5
            {null, null, new Gift(GiftType.MONEY, 8),
                    new Gift(GiftType.MONEY, 50), new Gift(GiftType.MONEY, 1000), new Gift(GiftType.MONEY, 8000)},  // Wild payouts
            {null, null, new Gift(GiftType.MONEY, 5),
                    new Gift(GiftType.MONEY, 50), new Gift(GiftType.MONEY, 200), new Gift(GiftType.JACKPOT, 1)},  // Jackpot
            {null, null, new Gift(GiftType.MONEY, 2),
                    new Gift(GiftType.MONEY, 15), new Gift(GiftType.MONEY, 100), new Gift(GiftType.MONEY, 200)},  // A
            {null, null, new Gift(GiftType.MONEY, 2),
                    new Gift(GiftType.MONEY, 10), new Gift(GiftType.MONEY, 55), new Gift(GiftType.MONEY, 150)},   // K
            {null, null, new Gift(GiftType.MONEY, 2),
                    new Gift(GiftType.MONEY, 10), new Gift(GiftType.MONEY, 40), new Gift(GiftType.MONEY, 100)},   // Q
            {null, null, null,
                    new Gift(GiftType.MONEY, 5), new Gift(GiftType.MONEY, 30), new Gift(GiftType.MONEY, 70)},     // J
            {null, null, null,
                    new Gift(GiftType.MONEY, 5), new Gift(GiftType.MONEY, 20), new Gift(GiftType.MONEY, 55)},     // 10
            {null, null, null,
                    new Gift(GiftType.MONEY, 3), new Gift(GiftType.MONEY, 15), new Gift(GiftType.MONEY, 40)},     // 9
            {null, null, null,
                    new Gift(GiftType.MONEY, 2), new Gift(GiftType.MONEY, 10), new Gift(GiftType.MONEY, 30)},     // 8 (was 3, spec says 2)

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
//            if (table[columns[i]] >= WILD && table[columns[i]] != JACKPOT) {
            table[columns[i]] = WILD;
//            }
        }
    }

    public static void changeColumnNotWild(byte[] table, byte[] columns) {
        for (int i = 0; i < columns.length; i++) {
            if (table[columns[i]] < WILD || table[columns[i]] == JACKPOT) {
                table[columns[i]] = (byte) GameUtil.randomBetween(4, 9);
            }
        }
    }

    // Wilds should NOT appear on edge columns (0 and 4) — they only expand on center columns (1-3)
    // Strip any randomly generated Wilds from columns 0 and 4
    public static byte[] validateTable(byte[] table) {
        for (int i = 0; i < COLUMNS.length; i++) {
            if (i != 0 && i != 4) continue;  // only process edge columns
            byte[] columns = COLUMNS[i];
            for (int j = 0; j < columns.length; j++) {
                if (table[columns[j]] == WILD) {
                    table[columns[j]] = (byte) GameUtil.randomBetween(4, 9);
                }
            }
        }
        return table;
    }

    // Count consecutive matching symbols from left to right.
    // FREE_SPIN and BONUS: exact match only (Wild does NOT substitute)
    // WILD, JACKPOT, and regular symbols (A-8): Wild CAN substitute
    // If first symbol is WILD, find the first non-WILD symbol to determine the match type
    public static RowValue getRowValue(byte[] row) {
        byte firstIcon = row[0];
        byte valueMax = 0;

        // If line starts with WILD, find the actual symbol it represents
        if (firstIcon == WILD) {
            // Find first non-WILD to determine what we're matching
            byte resolvedIcon = WILD;
            for (byte i = 1; i < row.length; i++) {
                if (row[i] != WILD) {
                    resolvedIcon = row[i];
                    break;
                }
            }
            // If all WILD or resolved to FREE_SPIN/BONUS, treat as WILD line
            if (resolvedIcon == WILD || resolvedIcon == FREE_SPIN || resolvedIcon == BONUS) {
                // All wilds or can't substitute for scatter/bonus — count consecutive wilds
                for (byte i = 0; i < row.length; i++) {
                    if (row[i] == WILD) valueMax++;
                    else break;
                }
                return new RowValue(WILD, valueMax);
            }
            // Count consecutive matching (WILD or resolvedIcon)
            for (byte i = 0; i < row.length; i++) {
                if (row[i] == WILD || row[i] == resolvedIcon) valueMax++;
                else break;
            }
            return new RowValue(resolvedIcon, valueMax);
        }

        // FREE_SPIN(0) and BONUS(1): exact match only, no wild substitution
        if (firstIcon == FREE_SPIN || firstIcon == BONUS) {
            for (byte i = 0; i < row.length; i++) {
                if (row[i] == firstIcon) valueMax++;
                else break;
            }
            return new RowValue(firstIcon, valueMax);
        }

        // JACKPOT(3) and regular symbols (4-10): Wild CAN substitute
        for (byte i = 0; i < row.length; i++) {
            if (row[i] == firstIcon || row[i] == WILD) valueMax++;
            else break;
        }
        return new RowValue(firstIcon, valueMax);
    }



    public static Slot11IconWildLienTucTableInfo rollLose0(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new
                Slot11IconWildLienTucTableInfo(GameConfig.getInstance().slot11IconWildLienTucRollLoseConfig.getTableRollLose(),
                betLevel, moneyEatJackpot, boxValues);
        slot11IconWildLienTucTableInfo.calculate(rowIndex);
        return slot11IconWildLienTucTableInfo;
//        while (true) {
//            Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(betLevel, moneyEatJackpot, boxValues);
//            slot11IconWildLienTucTableInfo.calculate(rowIndex);
//            if (slot11IconWildLienTucTableInfo.lineWin.size() == 0) {
//                return slot11IconWildLienTucTableInfo;
//            }
//        }
    }

    public static Slot11IconWildLienTucTableInfo rollLose20(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot11IconWildLienTucTableInfo.calculate(rowIndex);
            if (!slot11IconWildLienTucTableInfo.jackpot && !(slot11IconWildLienTucTableInfo.freeSpin > 0) && !(slot11IconWildLienTucTableInfo.miniGame > 0)
                    && !(slot11IconWildLienTucTableInfo.money > rowIndex.length * 10)) {
                return slot11IconWildLienTucTableInfo;
            }
            retryCount++;
            // Small sleep every 100 retries to prevent CPU spinning
            if (retryCount % 100 == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        // Fallback: return result even if conditions not met (prevent infinite loop)
        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot11IconWildLienTucTableInfo.calculate(rowIndex);
        return slot11IconWildLienTucTableInfo;
    }

//    public static Slot11IconWildLienTucTableInfo getSlot11IconLienTucTableInfoSpecial(byte type, int[] rowIndex){
//        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(type);
//        slot11IconWildLienTucTableInfo.calculate(rowIndex);
//        return slot11IconWildLienTucTableInfo;
//    }

    public static long getMoneyJackPot(long jackPotMoney, boolean isX2, long initPotMoney){
        long moneyEatJackpot = jackPotMoney;
        if (isX2) {
            moneyEatJackpot = jackPotMoney * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT;
        }
        return moneyEatJackpot;
    }

    public static Slot11IconWildLienTucTableInfo getSlot11IconLienTucBigWinTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                                               long fundMinigame, boolean isX2, long jackPotMoney, long initPotMoney,
                                                                               List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {
        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);

        int x = GameUtil.randomMax(100);

        if(x<50){
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }

        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries){
            Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot11IconWildLienTucTableInfo.calculate(rowIndex);
            if (slot11IconWildLienTucTableInfo.jackpot) {
                retryCount++;
                if (retryCount % 1000 == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                continue;
            }
            if (slot11IconWildLienTucTableInfo.freeSpin > 0) {
                retryCount++;
                if (retryCount % 1000 == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                continue;
            }
            return slot11IconWildLienTucTableInfo;
        }
        // Fallback: return result even if not ideal (prevent infinite loop)
        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot11IconWildLienTucTableInfo.calculate(rowIndex);
        return slot11IconWildLienTucTableInfo;
    }

    public static Slot11IconWildLienTucTableInfo getSlot11IconLienTucTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                           long fundMinigame, boolean isX2, long jackPotMoney, long initPotMoney,
                                                                               List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {

        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);

        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(betLevel, moneyEatJackpot, boxValues);

        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            slot11IconWildLienTucTableInfo.calculate(rowIndex);
            if(!slot11IconWildLienTucTableInfo.jackpot){
                break;
            }
            retryCount++;
            // Small sleep every 1000 retries to prevent CPU spinning
            if (retryCount % 1000 == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long totalWin = 0;
        if (slot11IconWildLienTucTableInfo.jackpot) {
            if(isSpinFree){
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an jackpot
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.miniGame > 0) { // khong cho an minigame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.freeSpin > 0) { // khong cho an freeSpin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if(isX2){
//                long moneyMinusFundJackpot = jackPotMoney * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT - (jackPotMoney - initPotMoney);
//                if(fundJackpot < moneyMinusFundJackpot)
                if(fundJackpot < jackPotMoney * GameConfig.getInstance().slot11IconWildLienTucConfig.MULTI_JACKPOT)
                    return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }else{
//                if(fundJackpot < initPotMoney) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
                if(fundJackpot < jackPotMoney) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += moneyEatJackpot;
        }
        if (slot11IconWildLienTucTableInfo.miniGame > 0) {
            if(isSpinFree){
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.miniGame > 5) {// khong cho roll qua 4 lan minigame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.freeSpin > 0) {// khong cho an free spin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an miniGame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.miniGameSlotResponse == null) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.miniGameSlotResponse.getTotalPrize() > fundMinigame) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += slot11IconWildLienTucTableInfo.miniGameSlotResponse.getTotalPrize();
        }

        if (slot11IconWildLienTucTableInfo.freeSpin > 0) {
            if(isSpinFree){
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot11IconWildLienTucTableInfo.freeSpin > 8) { // max 8 free spins
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an freeSpin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            // Free spin always gives 8 spins regardless of scatter count (4 or 5)
            if (8 * ROWS.length * betLevel > fund) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
        }

        if (rowIndex.length < ROWS.length) { // roll khong du hang
            if (slot11IconWildLienTucTableInfo.money > rowIndex.length * 2) { // an qua 2 lan so tien cuoc thi thua
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
        }

        if (slot11IconWildLienTucTableInfo.money * betLevel > fund) { // qua quy thi cho thua
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }

        totalWin += slot11IconWildLienTucTableInfo.money* betLevel;
        if(totalWin > maxiumWin){
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }
        return slot11IconWildLienTucTableInfo;
    }

    public static void main(String[] args) {
        GameConfig.getInstance().init();
        List<Integer> boxValues = new ArrayList<Integer>();
        boxValues.add(10);
        boxValues.add(10);
        boxValues.add(10);
        boxValues.add(15);
        boxValues.add(20);
        int[] rowIndex = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25};
        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo =
                rollLose0(rowIndex,100,50000,boxValues);

        int number = 10000;
//        byte[][] listRollLose0 = new byte[number][];
//        for(int i =0;i<number;i++){
//            listRollLose0[i] = rollLose0(rowIndex,100,50000,boxValues).table;
//        }
//
//        Slot11IconWildLienTucRollLoseConfig slot11IconWildLienTucRollLoseConfig = new Slot11IconWildLienTucRollLoseConfig();
//        slot11IconWildLienTucRollLoseConfig.rollLose0 = listRollLose0;
//        GameConfig.getInstance().setFileConfig("Slot11IconWildLienTucRollLoseConfig.json",
//                GameConfig.gson.toJson(slot11IconWildLienTucRollLoseConfig.rollLose0));
////        byte[] table = {7, 6, 3, 2, 3, 3, 4, 4, 6, 4, 1, 3, 5, 5, 7};
////        Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(100, 500000, boxValues);
////        slot11IconWildLienTucTableInfo.table = table;
////        slot11IconWildLienTucTableInfo.tableCalculate = table.clone();
////        slot11IconWildLienTucTableInfo.calculate(rowIndex);
////        slot11IconWildLienTucTableInfo.printTable();
////        System.out.println(slot11IconWildLienTucTableInfo.lineWinToString());
////        System.out.println(slot11IconWildLienTucTableInfo.money);
////        System.out.println(slot11IconWildLienTucTableInfo.moneyWinToString());
////        System.out.println("Test");
//        for(int i =0;i<100;i++){
//            System.out.println("index " + i);
//            Slot11IconWildLienTucTableInfo slot11IconWildLienTucTableInfo = new Slot11IconWildLienTucTableInfo(100, 500000, boxValues);
//            slot11IconWildLienTucTableInfo.calculate(rowIndex);
//            slot11IconWildLienTucTableInfo.printTable();
//            System.out.println();
//            System.out.println(slot11IconWildLienTucTableInfo.money);
//            System.out.println(slot11IconWildLienTucTableInfo.lineWinToString());
//            System.out.println(slot11IconWildLienTucTableInfo.moneyWinToString());
//        }
    }

}
