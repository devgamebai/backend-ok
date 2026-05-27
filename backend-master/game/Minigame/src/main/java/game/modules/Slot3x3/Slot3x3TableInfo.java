/*
 * Decompiled with CFR 0.152.
 */
package game.modules.Slot3x3;

import game.GameConfig.GameConfig;
import game.modules.Slot3x3.Slot3x3Util;
import game.utils.GameUtil;
import java.util.ArrayList;

public class Slot3x3TableInfo {
    public byte[][] table;
    public long money = 0L;
    public boolean isJackPot;
    public ArrayList<Integer> lineWin = new ArrayList();
    public ArrayList<Long> moneyWin = new ArrayList();
    public long betLevel = 0L;
    public long moneyEatPot = 0L;

    public Slot3x3TableInfo(byte[][] table, long betLevel, long moneyEatPot) {
        this.table = table;
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
    }

    public Slot3x3TableInfo(int giftType, long betLevel, long moneyEatPot) {
        this((byte) giftType, betLevel, moneyEatPot);
    }

    public Slot3x3TableInfo(byte giftType, long betLevel, long moneyEatPot) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        if (giftType == 0) {
            this.table = GameConfig.getInstance().slot3x3GameConfig.getTableValue();
            byte[] rows = Slot3x3Util.ROWS[GameUtil.randomMax(Slot3x3Util.ROWS.length)];
            for (int i = 0; i < this.table.length; ++i) {
                this.table[i][rows[i]] = 0;
            }
        }
    }

    public void calculateRowIndex(int[] rowIndex) {
        for (int i = 0; i < rowIndex.length; ++i) {
            byte[] row = Slot3x3Util.ROWS[rowIndex[i] - 1];
            long value = this.getMoneyWithRow(row);
            if (value < 0L) {
                this.isJackPot = true;
                this.lineWin.add(rowIndex[i]);
                this.moneyWin.add(this.moneyEatPot);
            }
            if (value <= 0L) continue;
            this.money += value;
            this.lineWin.add(rowIndex[i]);
            this.moneyWin.add(value * this.betLevel / 10L);
        }
    }

    public long getMoneyWithRow(byte[] row) {
        int j;
        byte[] listCheckPoint = new byte[6];
        for (j = 0; j < row.length; ++j) {
            if (this.table[j][row[j]] == 0) {
                int k = 0;
                while (k < listCheckPoint.length) {
                    int n = k++;
                    listCheckPoint[n] = (byte)(listCheckPoint[n] + 1);
                }
                continue;
            }
            byte by = this.table[j][row[j]];
            listCheckPoint[by] = (byte)(listCheckPoint[by] + 1);
        }
        for (j = 0; j < 5; ++j) {
            if (listCheckPoint[j] != 3) continue;
            return GameConfig.getInstance().slot3x3GameConfig.winRate[j];
        }
        if (listCheckPoint[5] == 3) {
            return GameConfig.getInstance().slot3x3GameConfig.winRate[6];
        }
        long value = 0L;
        if (listCheckPoint[4] == 2) {
            value += (long)GameConfig.getInstance().slot3x3GameConfig.winRate[5];
        }
        if (listCheckPoint[5] == 2) {
            value += (long)GameConfig.getInstance().slot3x3GameConfig.winRate[7];
        }
        return value;
    }

    public String lineWinToString() {
        StringBuilder s = new StringBuilder();
        for (Integer integer : this.lineWin) {
            int line = integer;
            if (s.length() == 0) {
                s.append(line);
                continue;
            }
            s.append(",").append(line);
        }
        return s.toString();
    }

    public String moneyWinToString() {
        StringBuilder s = new StringBuilder();
        for (Long value : this.moneyWin) {
            if (s.length() == 0) {
                s.append(value);
                continue;
            }
            s.append(",").append(value);
        }
        return s.toString();
    }

    public String matrixToString() {
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < 3; ++j) {
            for (int i = 0; i < 3; ++i) {
                byte value = this.table[i][j];
                if (s.length() == 0) {
                    s.append(value);
                    continue;
                }
                s.append(",").append(value);
            }
        }
        return s.toString();
    }
}

