package game.modules.Slot9Icon;

import bitzero.util.common.business.Debug;
import game.GameConfig.GameConfig;
import game.GameConfig.RollLoseConfig.Slot9IconRollLoseConfig;
import game.modules.GameUtil;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;

import java.util.ArrayList;
import java.util.List;

public class Slot9IconUtil {
    public static final int NUMBER_ICONS = 9;

    public static final byte JACKPOT = 0;
    public static final byte BONUS = 2;

//    public static final byte[][] ROWS = {
//            {5, 6, 7, 8, 9},
//            {0, 1, 2, 3, 4},
//            {10, 11, 12, 13, 14},
//            {5, 6, 2, 8, 9},
//            {5, 6, 12, 8, 9},
//
//            {0, 1, 7, 3, 4},
//            {10, 11, 7, 13, 14},
//            {0, 11, 2, 13, 4},
//            {10, 1, 12, 3, 14},
//            {5, 1, 12, 3, 9},
//
//            {10, 6, 2, 8, 14},
//            {0, 6, 12, 8, 4},
//            {5, 11, 7, 3, 9},
//            {5, 1, 7, 13, 9},
//            {10, 6, 7, 8, 14},
//
//            {0, 6, 7, 8, 4},
//            {5, 11, 12, 13, 9},
//            {5, 1, 2, 3, 9},
//            {10, 11, 7, 3, 4},
//            {0, 1, 7, 13, 14},
//    };


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

    public static Gift[][] GIFT_TABLES = {
            {null, null, null,
                    new Gift(GiftType.MONEY, 30), new Gift(GiftType.MONEY, 100), new Gift(GiftType.JACKPOT, 1)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 25), new Gift(GiftType.MONEY, 800), new Gift(GiftType.MONEY, 1000)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 15), new Gift(GiftType.MINI_GAME, 4), new Gift(GiftType.MONEY, 500)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 8), new Gift(GiftType.MONEY, 15), new Gift(GiftType.MONEY, 200)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 7), new Gift(GiftType.MONEY, 10), new Gift(GiftType.MONEY, 80)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 3), new Gift(GiftType.MONEY, 6), new Gift(GiftType.MONEY, 30)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 2), new Gift(GiftType.MONEY, 5), new Gift(GiftType.MONEY, 15)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 1), new Gift(GiftType.MONEY, 4), new Gift(GiftType.MONEY, 12)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 1), new Gift(GiftType.MONEY, 3), new Gift(GiftType.MONEY, 8)},

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

    public static RowValue getRowValue(byte[] row) {
        byte[] mapIcon = new byte[NUMBER_ICONS];
        for (byte i = 0; i < NUMBER_ICONS; i++) {
            mapIcon[i] = 0;
        }
        for (byte i = 0; i < row.length; i++) {
            mapIcon[row[i]] += 1;
        }
        byte indexMax = 0;
        byte valueMax = mapIcon[0];
        for (byte i = 0; i < mapIcon.length; i++) {
            if (mapIcon[i] > valueMax) {
                indexMax = i;
                valueMax = mapIcon[i];
            }
        }
        return new RowValue(indexMax, valueMax);
    }

    public static Slot9IconTableInfo rollLose0(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
//        Slot9IconTableInfo slot9IconTableInfo =
//                new Slot9IconTableInfo(GameConfig.getInstance().slot9IconRollLoseConfig.getTableRollLose(),
//                        betLevel, moneyEatJackpot, boxValues);
//        slot9IconTableInfo.calculate(rowIndex);
//        return slot9IconTableInfo;
        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot9IconTableInfo.calculate(rowIndex);
            if (slot9IconTableInfo.lineWin.size() == 0) {
                return slot9IconTableInfo;
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
        // Fallback: return result even if not ideal (prevent infinite loop)
        Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot9IconTableInfo.calculate(rowIndex);
        return slot9IconTableInfo;
    }

    public static Slot9IconTableInfo rollLose20(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
       
        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot9IconTableInfo.calculate(rowIndex);
            if (!slot9IconTableInfo.jackpot && !(slot9IconTableInfo.freeSpin > 0) && !(slot9IconTableInfo.miniGame > 0)
                    && !(slot9IconTableInfo.money > rowIndex.length * 10)) {
                return slot9IconTableInfo;
            }
            retryCount++;
            // Small sleep to prevent CPU spinning
            if (retryCount % 100 == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot9IconTableInfo.calculate(rowIndex);
        return slot9IconTableInfo;
    }

//    public static Slot9IconTableInfo getSlot9IconTableInfoSpecial(byte type, int[] rowIndex){
//        Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(type);
//        slot9IconTableInfo.calculate(rowIndex);
//        return slot9IconTableInfo;
//    }

    public static long getMoneyJackPot(long jackPotMoney, boolean isX2, long initPotMoney){
        long moneyEatJackpot = jackPotMoney;
        if (isX2) {
            moneyEatJackpot = jackPotMoney * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT;
        }
        return moneyEatJackpot;
    }

    public static Slot9IconTableInfo getSlot9IconBigWinTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                           long fundMinigame, boolean isX2, long jackPotMoney,
                                                           long initPotMoney, List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {
        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);

        int x = GameUtil.randomMax(100);

        if(x<50){
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }

        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries){
            Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot9IconTableInfo.calculate(rowIndex);
            if (slot9IconTableInfo.jackpot) {
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
                continue;
            }
            return slot9IconTableInfo;
        }
        // Fallback: return result even if jackpot (prevent infinite loop)
        Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot9IconTableInfo.calculate(rowIndex);
        return slot9IconTableInfo;
    }

    public static Slot9IconTableInfo getSlot9IconTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                           long fundMinigame, boolean isX2, long jackPotMoney,
                                                           long initPotMoney, List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {

        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);

        Slot9IconTableInfo slot9IconTableInfo = new Slot9IconTableInfo(betLevel, moneyEatJackpot, boxValues);

        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            slot9IconTableInfo.calculate(rowIndex);
            if(!slot9IconTableInfo.jackpot){
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
        if (slot9IconTableInfo.jackpot) {
            if (isSpinFree) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an jackpot
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.miniGame > 0) { // khong cho an minigame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.freeSpin > 0) { // khong cho an freeSpin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (isX2) {
//                long moneyMinusFundJackpot = jackPotMoney * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT - (jackPotMoney - initPotMoney);
//                if (fundJackpot < moneyMinusFundJackpot)
                if (fundJackpot < jackPotMoney * GameConfig.getInstance().slot9IconConfig.MULTI_JACKPOT)
                    return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            } else {
//                if (fundJackpot < initPotMoney)
                if (fundJackpot < jackPotMoney)
                    return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += moneyEatJackpot;
        }
        if (slot9IconTableInfo.miniGame > 0) {
            if (isSpinFree) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.miniGame > 5) {// khong cho roll qua 4 lan minigame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.freeSpin > 0) {// khong cho an free spin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an miniGame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.miniGameSlotResponse == null) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }

            if (slot9IconTableInfo.miniGameSlotResponse.getTotalPrize() > fundMinigame) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += slot9IconTableInfo.miniGameSlotResponse.getTotalPrize();
        }

        if (slot9IconTableInfo.freeSpin > 0) {
            if (isSpinFree) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.freeSpin > 4) { // khong cho roll qua 4 lan free spin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an freeSpin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot9IconTableInfo.freeSpin * ROWS.length * betLevel > fund) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
        }

        if (rowIndex.length < ROWS.length) { // roll khong du hang
            if (slot9IconTableInfo.money > rowIndex.length * 2) { // an qua 2 lan so tien cuoc thi thua
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
        }

        if (slot9IconTableInfo.money * betLevel > fund) { // qua quy thi cho thua
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }
        totalWin += slot9IconTableInfo.money* betLevel;

        if(totalWin > maxiumWin){
            Debug.trace("totalWin",totalWin, "maxiumWin", maxiumWin);
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }

        return slot9IconTableInfo;
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
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

        Slot9IconTableInfo slot9IconTableInfo = rollLose0(rowIndex,100,50000,boxValues);
        int number = 10000;
//        byte[][] listRollLose0 = new byte[number][];
//        for (int i = 0; i < number; i++) {
//            listRollLose0[i] = rollLose0(rowIndex, 100, 50000, boxValues).table;
//        }
//        Slot9IconRollLoseConfig slot9IconRollLoseConfig = new Slot9IconRollLoseConfig();
//        slot9IconRollLoseConfig.rollLose0 = listRollLose0;
//        GameConfig.getInstance().setFileConfig("Slot9IconRollLoseConfig.json",slot9IconRollLoseConfig);
    }
}
