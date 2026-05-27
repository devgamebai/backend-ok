/*
 * Decompiled with CFR 0.152.
 */
package game.modules.Slot3x3;

import game.GameConfig.GameConfig;
import game.modules.Slot3x3.Slot3x3TableInfo;
import game.utils.GameUtil;

public class Slot3x3Util {
    public static final byte WILD = 0;
    public static final byte MAX_ICON = 6;
    public static final byte[][] ROWS = new byte[][]{{0, 0, 0}, {1, 1, 1}, {2, 2, 2}, {0, 2, 0}, {2, 0, 2}, {0, 1, 0}, {0, 1, 2}, {2, 1, 0}, {1, 2, 1}, {1, 0, 1}, {2, 1, 2}, {0, 0, 1}, {1, 1, 2}, {1, 1, 0}, {2, 2, 1}, {1, 0, 0}, {2, 1, 1}, {0, 1, 1}, {1, 2, 2}, {0, 2, 1}};

    public static Slot3x3TableInfo rollLose0(int[] rowIndex, long betLevel, long moneyEatJackpot) {
        Slot3x3TableInfo slot3x3TableInfo;
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
            slot3x3TableInfo.calculateRowIndex(rowIndex);
            if (slot3x3TableInfo.lineWin.size() == 0) {
                return slot3x3TableInfo;
            }
            if (++retryCount % 100 != 0) continue;
            try {
                Thread.sleep(1L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
        slot3x3TableInfo.calculateRowIndex(rowIndex);
        return slot3x3TableInfo;
    }

    public static Slot3x3TableInfo rollLose20(int[] rowIndex, long betLevel, long moneyEatJackpot) {
        Slot3x3TableInfo slot3x3TableInfo;
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
            slot3x3TableInfo.calculateRowIndex(rowIndex);
            if (!slot3x3TableInfo.isJackPot && slot3x3TableInfo.money <= (long)(rowIndex.length * 10)) {
                return slot3x3TableInfo;
            }
            if (++retryCount % 100 != 0) continue;
            try {
                Thread.sleep(1L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
        slot3x3TableInfo.calculateRowIndex(rowIndex);
        return slot3x3TableInfo;
    }

    public static long getMoneyJackPot(long jackPotMoney, boolean isX2, long initPotMoney) {
        long moneyEatJackpot = jackPotMoney;
        if (isX2) {
            moneyEatJackpot = jackPotMoney * (long)GameConfig.getInstance().slot3x3GameConfig.MULTI_JACKPOT - (jackPotMoney - initPotMoney);
        }
        return moneyEatJackpot;
    }

    public static Slot3x3TableInfo getSlot3x1TableInfoBigWin(int[] rowIndex, long betLevel, boolean isX2, long jackPotMoney, long initPotMoney) {
        Slot3x3TableInfo slot3x3TableInfo;
        long moneyEatJackpot = Slot3x3Util.getMoneyJackPot(jackPotMoney, isX2, initPotMoney);
        int x = GameUtil.randomMax(100);
        if (x < 50) {
            return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
        }
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
            slot3x3TableInfo.calculateRowIndex(rowIndex);
            if (slot3x3TableInfo.isJackPot) {
                if (++retryCount % 1000 != 0) continue;
                try {
                    Thread.sleep(1L);
                    continue;
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return slot3x3TableInfo;
        }
        slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
        slot3x3TableInfo.calculateRowIndex(rowIndex);
        return slot3x3TableInfo;
    }

    public static Slot3x3TableInfo getSlot3x1TableInfo(int[] rowIndex, long betLevel, long fund, long fundJackpot, boolean isX2, long jackPotMoney, long initPotMoney, long maxiumWin) {
        long moneyEatJackpot = Slot3x3Util.getMoneyJackPot(jackPotMoney, isX2, initPotMoney);
        Slot3x3TableInfo slot3x3TableInfo = new Slot3x3TableInfo(GameConfig.getInstance().slot3x3GameConfig.getTableValue(), betLevel, moneyEatJackpot);
        slot3x3TableInfo.calculateRowIndex(rowIndex);
        if (slot3x3TableInfo.isJackPot) {
            long moneyToFundJackpot;
            if (rowIndex.length < ROWS.length) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
            if (isX2 ? fundJackpot < (moneyToFundJackpot = jackPotMoney * (long)GameConfig.getInstance().slot3x3GameConfig.MULTI_JACKPOT) : fundJackpot < jackPotMoney) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
            if (slot3x3TableInfo.money * betLevel / 10L > fund) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
            if (slot3x3TableInfo.money * betLevel / 10L + moneyEatJackpot > maxiumWin) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
        } else {
            if (slot3x3TableInfo.money * betLevel / 10L > maxiumWin) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
            if (slot3x3TableInfo.money * betLevel / 10L > fund) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
            if (rowIndex.length < ROWS.length && slot3x3TableInfo.money > (long)(rowIndex.length * 10 * 2)) {
                return Slot3x3Util.rollLose0(rowIndex, betLevel, moneyEatJackpot);
            }
        }
        return slot3x3TableInfo;
    }
}

