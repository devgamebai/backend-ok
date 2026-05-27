package game.modules.Slot9Icon;

import game.GameConfig.GameConfig;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;
import game.modules.SlotUtils.TableInfo;

import java.util.List;

public class Slot9IconTableInfo extends TableInfo {

    public Slot9IconTableInfo(byte[] table, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = table;
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot9IconTableInfo(long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = GameConfig.getInstance().slot9IconConfig.generateRandomTable();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot9IconTableInfo(short giftType, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
        this.table = GameConfig.getInstance().slot9IconConfig.generateRandomTable();

        int randOrder = (int) Math.floor(Slot9IconUtil.ROWS.length * Math.random());
        byte[] randRow = Slot9IconUtil.ROWS[randOrder];
        if (giftType == GiftType.MINI_GAME) {
            for (int i = 0; i < 4; i++) {
                this.table[randRow[i]] = Slot9IconUtil.BONUS;
            }
        }

        if (giftType == GiftType.JACKPOT) {
            for (int i = 0; i < 5; i++) {
                this.table[randRow[i]] = Slot9IconUtil.JACKPOT;
            }
        }
    }

    public void calculate(int[] rowsIndex) {
        this.money = 0;
        this.jackpot = false;
        this.freeSpin = 0;
        this.miniGame = 0;

        for (int i = 0; i < rowsIndex.length; i++) {
            byte row[] = Slot9IconUtil.getIconsInRow(table, Slot9IconUtil.ROWS[rowsIndex[i]-1]);

            RowValue rowValue = Slot9IconUtil.getRowValue(row);
            Gift gift = Slot9IconUtil.getGift(rowValue);

            if (gift == null)
                continue;
            switch (gift.type) {
                case GiftType.MONEY:
                    this.money += gift.number;
                    break;
                case GiftType.FREE_SPIN:
                    this.freeSpin += gift.number;
                    break;
                case GiftType.JACKPOT:
                    this.jackpot = true;
                    break;
                case GiftType.MINI_GAME:
                    this.miniGame += gift.number;
                    break;
            }
            this.lineWin.add(rowsIndex[i]);
            if(gift.type == GiftType.MONEY){
                this.moneyWin.add(gift.number*this.betLevel);
            }
            if(gift.type == GiftType.JACKPOT){
                this.moneyWin.add(this.moneyEatPot);
            }
        }
        if(this.miniGame > 0){
            this.miniGameSlotResponse = this.generatePickStars(this.miniGame);
            this.moneyWin.add(this.miniGameSlotResponse.getTotalPrize());
        }
    }


}
