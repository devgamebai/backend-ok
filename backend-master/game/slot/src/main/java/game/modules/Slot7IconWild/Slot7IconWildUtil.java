package game.modules.Slot7IconWild;

import game.GameConfig.GameConfig;
import game.GameConfig.RollLoseConfig.Slot7IconWildRollLoseConfig;
import game.modules.GameUtil;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;

import java.util.ArrayList;
import java.util.List;

public class Slot7IconWildUtil {
    public static final int NUMBER_ICONS = 7;

    public static final byte JACKPOT = 0;
    public static final byte WILD = 1;
    public static final byte BONUS = 2;

//    public static final byte[][] ROWS = {
//            {1, 4, 7, 10, 13},
//            {2, 5, 8, 11, 14},
//            {0, 3, 6, 9, 12},
//            {1, 4, 8, 10, 13},
//            {1, 4, 6, 10, 13},
//
//            {2, 5, 7, 11, 14},
//            {0, 3, 7, 9, 12},
//            {2, 3, 8, 9, 14},
//            {0, 5, 6, 11, 12},
//            {1, 5, 6, 11, 13},
//
//            {0, 4, 8, 10, 12},
//            {2, 4, 6, 10, 14},
//            {1, 3, 7, 11, 13},
//            {1, 5, 7, 9, 13},
//            {0, 4, 7, 10, 12},
//
//            {2, 4, 7, 10, 14},
//            {1, 3, 6, 9, 13},
//            {1, 5, 8, 11, 13},
//            {0, 3, 7, 11, 14},
//            {2, 5, 7, 9, 12},
//    };

    public static final byte[][] ROWS = {
            {5, 6, 7, 8, 9},
            {0, 1, 2, 3, 4},
            {10, 11, 12, 13, 14},
            {5, 6, 2, 8, 9},
            {5, 6, 12, 8, 9},

            {0, 1, 7, 3, 4},
            {10, 11, 7, 13, 14},
            {0, 11, 2, 13, 4},
            {10, 1, 12, 3, 14},
            {5, 1, 12, 3, 9},

            {10, 6, 2, 8, 14},
            {0, 6, 12, 8, 4},
            {5, 11, 7, 3, 9},
            {5, 1, 7, 13, 9},
            {10, 6, 7, 8, 14},

            {0, 6, 7, 8, 4},
            {5, 11, 12, 13, 9},
            {5, 1, 2, 3, 9},
            {10, 11, 7, 3, 4},
            {0, 1, 7, 13, 14},
    };

    public static Gift[][] GIFT_TABLES = {
            {null, null, null,
                    new Gift(GiftType.MONEY, 5), new Gift(GiftType.MONEY, 30), new Gift(GiftType.JACKPOT, 1)},
            {null, null, null,
                    null, null, new Gift(GiftType.MONEY, 1000)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 4), new Gift(GiftType.MINI_GAME, 4), new Gift(GiftType.MONEY, 1000)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 2), new Gift(GiftType.MONEY, 20), new Gift(GiftType.MONEY, 200)},
            {null, null, null,
                    new Gift(GiftType.MONEY, 2), new Gift(GiftType.MONEY, 8), new Gift(GiftType.MONEY, 55)},
            {null, null, null,
                    null, new Gift(GiftType.MONEY, 4), new Gift(GiftType.MONEY, 15)},
            {null, null, null,
                    null, new Gift(GiftType.MONEY, 3), new Gift(GiftType.MONEY, 8)},

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
        //int numberIconWild = 0;
        for (byte i = 0; i < NUMBER_ICONS; i++) {
            mapIcon[i] = 0;
        }
        for (byte i = 0; i < row.length; i++) {
            mapIcon[row[i]] += 1;
        }
        if (mapIcon[WILD] == 5) return new RowValue(WILD, (byte) 5);
        byte indexMax = 0;
        byte valueMax = mapIcon[0];
        for (byte i = 0; i < mapIcon.length; i++) {
            if (i == WILD) continue;
            if (mapIcon[i] > valueMax) {
                indexMax = i;
                valueMax = mapIcon[i];
            }
        }
        return new RowValue(indexMax, (byte) (valueMax + mapIcon[WILD]));
    }

    public static Slot7IconWildTableInfo rollLose0(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
        Slot7IconWildTableInfo slot7IconWildTableInfo =
                new Slot7IconWildTableInfo(GameConfig.getInstance().slot7IconWildRollLoseConfig.getTableRollLose(),
                        betLevel, moneyEatJackpot, boxValues);
        slot7IconWildTableInfo.calculate(rowIndex);
        return slot7IconWildTableInfo;
//        while (true) {
//            Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(betLevel, moneyEatJackpot, boxValues);
//            slot7IconWildTableInfo.calculate(rowIndex);
//            if (slot7IconWildTableInfo.lineWin.size() == 0) {
//                return slot7IconWildTableInfo;
//            }
//        }
    }

    public static Slot7IconWildTableInfo rollLose20(int[] rowIndex, long betLevel, long moneyEatJackpot, List<Integer> boxValues) {
        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot7IconWildTableInfo.calculate(rowIndex);
            if (!slot7IconWildTableInfo.jackpot && !(slot7IconWildTableInfo.freeSpin > 0) && !(slot7IconWildTableInfo.miniGame > 0)
                    && !(slot7IconWildTableInfo.money > rowIndex.length * 10)) {
                return slot7IconWildTableInfo;
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
        Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot7IconWildTableInfo.calculate(rowIndex);
        return slot7IconWildTableInfo;
    }

    //    public static Slot7IconWildTableInfo getSlot7IconTableInfoSpecial(byte type, int[] rowIndex){
//        Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(type);
//        slot7IconWildTableInfo.calculate(rowIndex);
//        return slot7IconWildTableInfo;
//    }
    public static long getMoneyJackPot(long jackPotMoney, boolean isX2, long initPotMoney) {
        long moneyEatJackpot = jackPotMoney;
        if (isX2) {
            moneyEatJackpot = jackPotMoney * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT;
        }
        return moneyEatJackpot;
    }

    public static Slot7IconWildTableInfo getSlot7IconWildBigWinTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                                 long fundMinigame, boolean isX2, long jackPotMoney,
                                                                 long initPotMoney, List<Integer> boxValues, boolean isSpinFree,
                                                                 long maxiumWin){
        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);

        int x = GameUtil.randomMax(100);

        if(x<50){
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }

        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries){
            Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(betLevel, moneyEatJackpot, boxValues);
            slot7IconWildTableInfo.calculate(rowIndex);
            if (slot7IconWildTableInfo.jackpot) {
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
//            if (slot7IconTableInfo.miniGame > 0) {
//                continue;
//            }
            if (slot7IconWildTableInfo.freeSpin > 0) {
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
            return slot7IconWildTableInfo;
        }
        // Fallback: return result even if not ideal (prevent infinite loop)
        Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(betLevel, moneyEatJackpot, boxValues);
        slot7IconWildTableInfo.calculate(rowIndex);
        return slot7IconWildTableInfo;
    }

    public static Slot7IconWildTableInfo getSlot7IconTableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot,
                                                               long fundMinigame, boolean isX2, long jackPotMoney,
                                                               long initPotMoney, List<Integer> boxValues, boolean isSpinFree, long maxiumWin) {
        long moneyEatJackpot = getMoneyJackPot(jackPotMoney, isX2, initPotMoney);
        Slot7IconWildTableInfo slot7IconWildTableInfo = new Slot7IconWildTableInfo(betLevel, moneyEatJackpot, boxValues);

        int retryCount = 0;
        int maxRetries = 10000; // Prevent infinite loop
        while (retryCount < maxRetries) {
            slot7IconWildTableInfo.calculate(rowIndex);
            if(!slot7IconWildTableInfo.jackpot){
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
        if (slot7IconWildTableInfo.jackpot) {
            if (isSpinFree) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an jackpot
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.miniGame > 0) { // khong cho an minigame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.freeSpin > 0) { // khong cho an freeSpin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (isX2) {
//                long moneyMinusFundJackpot = jackPotMoney * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT - (jackPotMoney - initPotMoney);
//                if (fundJackpot < moneyMinusFundJackpot)
                if (fundJackpot < jackPotMoney * GameConfig.getInstance().slot7IconWildConfig.MULTI_JACKPOT)
                    return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            } else {
//                if (fundJackpot < initPotMoney) return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
                if (fundJackpot < jackPotMoney)
                    return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += moneyEatJackpot;
        }
        if (slot7IconWildTableInfo.miniGame > 0) {
            if (isSpinFree) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.miniGame > 5) {// khong cho roll qua 4 lan minigame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.freeSpin > 0) {// khong cho an free spin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an miniGame
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.miniGameSlotResponse == null) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }

            if (slot7IconWildTableInfo.miniGameSlotResponse.getTotalPrize() > fundMinigame) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            totalWin += slot7IconWildTableInfo.miniGameSlotResponse.getTotalPrize();
        }

        if (slot7IconWildTableInfo.freeSpin > 0) {
            if (isSpinFree) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.freeSpin > 4) { // khong cho roll qua 4 lan free spin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (rowIndex.length < ROWS.length) { // roll khong du hang thi khong duoc an freeSpin
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
            if (slot7IconWildTableInfo.freeSpin * ROWS.length * betLevel > fund) {
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
        }

        if (rowIndex.length < ROWS.length) { // roll khong du hang
            if (slot7IconWildTableInfo.money > rowIndex.length * 2) { // an qua 2 lan so tien cuoc thi thua
                return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
            }
        }

        if (slot7IconWildTableInfo.money * betLevel > fund) { // qua quy thi cho thua
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }

        totalWin += slot7IconWildTableInfo.money * betLevel;
        if (totalWin > maxiumWin) {
            return rollLose0(rowIndex, betLevel, moneyEatJackpot, boxValues);
        }
        return slot7IconWildTableInfo;
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
        Slot7IconWildTableInfo slot7IconWildTableInfo = rollLose0(rowIndex, 100, 50000, boxValues);
        int number = 10000;
//        byte[][] listRollLose0 = new byte[number][];
//        for (int i = 0; i < number; i++) {
//            listRollLose0[i] = rollLose0(rowIndex, 100, 50000, boxValues).table;
//        }
//
//        Slot7IconWildRollLoseConfig slot7IconWildRollLoseConfig = new Slot7IconWildRollLoseConfig();
//        slot7IconWildRollLoseConfig.rollLose0 = listRollLose0;
//        GameConfig.getInstance().setFileConfig("Slot7IconWildRollLoseConfig.json",slot7IconWildRollLoseConfig);
    }
}
