package game.modules.ThanTai;

import game.GameConfig.GameConfig;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;
import game.modules.SlotUtils.TableInfo;

import java.util.List;

public class ThanTaiTableInfo extends TableInfo {
    public byte[] tableCalculate;

    public ThanTaiTableInfo(byte[] table, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = table;
        this.tableCalculate = this.table.clone();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public ThanTaiTableInfo(long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = GameConfig.getInstance().thanTaiConfig.generateRandomTable();
        this.table = ThanTaiUtil.validateTable(this.table);
        this.tableCalculate = this.table.clone();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public ThanTaiTableInfo(short giftType, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;

        this.table = GameConfig.getInstance().thanTaiConfig.generateRandomTableNoWild();
        int randOrder = (int) Math.floor(ThanTaiUtil.ROWS.length * Math.random());
        byte[] randRow = ThanTaiUtil.ROWS[randOrder];

        if (giftType == GiftType.FREE_SPIN) {
            for (int i = 0; i < 3; i++) {
                this.table[randRow[i]] = ThanTaiUtil.FREE_SPIN;
            }
        }
        if (giftType == GiftType.MINI_GAME) {
            for (int i = 0; i < 4; i++) {
                this.table[randRow[i]] = ThanTaiUtil.BONUS;
            }
        }
        if (giftType == GiftType.JACKPOT) {
            for (int i = 0; i < 5; i++) {
                this.table[randRow[i]] = ThanTaiUtil.JACKPOT;
            }
        }

        this.tableCalculate = this.table.clone();
    }

    public void calculate(int[] rowsIndex) {
        this.money = 0;
        this.jackpot = false;
        this.freeSpin = 0;
        this.miniGame = 0;

        for (int i = 0; i < ThanTaiUtil.COLUMNS.length; i++) {
            if (i == 0 || i == 4) continue;
            byte[] columns = ThanTaiUtil.COLUMNS[i];
            for (int j = 0; j < columns.length; j++) {
                if (this.tableCalculate[columns[j]] == ThanTaiUtil.WILD) {
                    ThanTaiUtil.changeColumnWild(tableCalculate, columns);
                    break;
                }
            }
        }

        for (int i = 0; i < rowsIndex.length; i++) {
            byte[] row = ThanTaiUtil.getIconsInRow(this.tableCalculate, ThanTaiUtil.ROWS[rowsIndex[i] - 1]);
            RowValue rowValue = ThanTaiUtil.getRowValue(row);
            Gift gift = ThanTaiUtil.getGift(rowValue);

            if (gift == null) continue;
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
            if (i % 5 == 0) System.out.println();
            System.out.print(" " + this.table[i]);
        }
        System.out.println();
        for (int i = 0; i < this.tableCalculate.length; i++) {
            if (i % 5 == 0) System.out.println();
            System.out.print(" " + this.tableCalculate[i]);
        }
        System.out.println();
    }
}
