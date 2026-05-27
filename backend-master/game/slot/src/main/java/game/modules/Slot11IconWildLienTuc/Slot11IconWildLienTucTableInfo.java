package game.modules.Slot11IconWildLienTuc;

import game.GameConfig.GameConfig;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;
import game.modules.SlotUtils.TableInfo;

import java.util.List;

public class Slot11IconWildLienTucTableInfo extends TableInfo {
    public byte[] tableCalculate;

    public Slot11IconWildLienTucTableInfo(byte[] table, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = table;
        this.table = table;
        this.tableCalculate = this.table.clone();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot11IconWildLienTucTableInfo(long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = GameConfig.getInstance().slot11IconWildLienTucConfig.generateRandomTable();
        this.table = Slot11IconWildLienTucUtil.validateTable(this.table);
        this.tableCalculate = this.table.clone();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot11IconWildLienTucTableInfo(short giftType, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;

        this.table = GameConfig.getInstance().slot11IconWildLienTucConfig.generateRandomTableNoWild();
        int randOrder = (int) Math.floor(Slot11IconWildLienTucUtil.ROWS.length * Math.random());
        byte[] randRow = Slot11IconWildLienTucUtil.ROWS[randOrder];

        if (giftType == GiftType.FREE_SPIN) {
            for (int i = 0; i < 3; i++) {
                this.table[randRow[i]] = Slot11IconWildLienTucUtil.FREE_SPIN;
            }
        }
        if (giftType == GiftType.MINI_GAME) {
            for (int i = 0; i < 4; i++) {
                this.table[randRow[i]] = Slot11IconWildLienTucUtil.BONUS;
            }
        }

        if (giftType == GiftType.JACKPOT) {
            for (int i = 0; i < 5; i++) {
                this.table[randRow[i]] = Slot11IconWildLienTucUtil.JACKPOT;
            }
        }

        this.tableCalculate = this.table.clone();
    }

    public void calculate(int[] rowsIndex) {
        this.money = 0;
        this.jackpot = false;
        this.freeSpin = 0;
        this.miniGame = 0;
        // System.out.println(this.matrixToString());
        for (int i = 0; i < Slot11IconWildLienTucUtil.COLUMNS.length; i++) {
            if (i == 0 || i == 4) {
                continue;
            }
            byte[] columns = Slot11IconWildLienTucUtil.COLUMNS[i];
            for (int j = 0; j < columns.length; j++) {
                if (this.tableCalculate[columns[j]] == Slot11IconWildLienTucUtil.WILD) {
                    Slot11IconWildLienTucUtil.changeColumnWild(tableCalculate, columns);
                    break;
                }
            }
        }
        //System.out.println(this.matrixToString());
        //System.out.println();

        for (int i = 0; i < rowsIndex.length; i++) {
            byte row[] = Slot11IconWildLienTucUtil.getIconsInRow(this.tableCalculate, Slot11IconWildLienTucUtil.ROWS[rowsIndex[i] - 1]);

            RowValue rowValue = Slot11IconWildLienTucUtil.getRowValue(row);
            Gift gift = Slot11IconWildLienTucUtil.getGift(rowValue);

            if (gift == null)
                continue;
            switch (gift.type) {
                case GiftType.MONEY:
                    money += gift.number;
                    break;
                case GiftType.FREE_SPIN:
                    freeSpin += gift.number;
                    break;
                case GiftType.JACKPOT:
                    jackpot = true;
                    break;
                case GiftType.MINI_GAME:
                    miniGame += gift.number;
                    break;
            }
            this.lineWin.add(rowsIndex[i]);
            if (gift.type == GiftType.MONEY) {
                this.moneyWin.add(gift.number * this.betLevel);
            }
            if (gift.type == GiftType.JACKPOT) {
                this.moneyWin.add(this.moneyEatPot);
            }
        }
        if (this.miniGame > 0) {
            this.miniGameSlotResponse = this.generatePickStars(this.miniGame);
            this.moneyWin.add(this.miniGameSlotResponse.getTotalPrize());
        }
    }

    public void printTable() {
        for (int i = 0; i < this.table.length; i++) {
            if (i % 5 == 0) {
                System.out.println();
            }
            System.out.print(" " + this.table[i]);
        }
        System.out.println();
        for (int i = 0; i < this.tableCalculate.length; i++) {
            if (i % 5 == 0) {
                System.out.println();
            }
            System.out.print(" " + this.tableCalculate[i]);
        }
        System.out.println();
    }


}
