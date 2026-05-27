package game.modules.Slot7IconWild;

import game.GameConfig.GameConfig;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;
import game.modules.SlotUtils.TableInfo;

import java.util.List;

public class Slot7IconWildTableInfo extends TableInfo {

    public Slot7IconWildTableInfo(byte[] table, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = table;
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot7IconWildTableInfo(long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = GameConfig.getInstance().slot7IconWildConfig.generateRandomTable();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot7IconWildTableInfo(short giftType, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
        this.table = GameConfig.getInstance().slot7IconWildConfig.generateRandomTable();

        int randOrder = (int) Math.floor(Slot7IconWildUtil.ROWS.length * Math.random());
        byte[] randRow = Slot7IconWildUtil.ROWS[randOrder];
        if (giftType == GiftType.MINI_GAME) {
            for (int i = 0; i < 4; i++) {
                this.table[randRow[i]] = Slot7IconWildUtil.BONUS;
            }
        }

        if (giftType == GiftType.JACKPOT) {
            for (int i = 0; i < 5; i++) {
                this.table[randRow[i]] = Slot7IconWildUtil.JACKPOT;
            }
        }
    }

    public void calculate(int[] rowsIndex) {
        this.money = 0;
        this.jackpot = false;
        this.freeSpin = 0;
        this.miniGame = 0;

        for (int i = 0; i < rowsIndex.length; i++) {
            byte row[] = Slot7IconWildUtil.getIconsInRow(this.table, Slot7IconWildUtil.ROWS[rowsIndex[i]-1]);

            RowValue rowValue = Slot7IconWildUtil.getRowValue(row);
            Gift gift = Slot7IconWildUtil.getGift(rowValue);

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
