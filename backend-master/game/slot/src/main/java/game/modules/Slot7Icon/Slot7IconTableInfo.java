package game.modules.Slot7Icon;

import game.GameConfig.GameConfig;
import game.modules.SlotUtils.Gift;
import game.modules.SlotUtils.GiftType;
import game.modules.SlotUtils.RowValue;
import game.modules.SlotUtils.TableInfo;
import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Slot7IconTableInfo extends TableInfo {

    public Slot7IconTableInfo(byte[] table, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = table;
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot7IconTableInfo(long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.table = GameConfig.getInstance().slot7IconConfig.generateRandomTable();
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
    }

    public Slot7IconTableInfo(short giftType, long betLevel, long moneyEatPot, List<Integer> boxValues) {
        this.betLevel = betLevel;
        this.moneyEatPot = moneyEatPot;
        this.boxValues = boxValues;
        this.table = GameConfig.getInstance().slot7IconConfig.generateRandomTable();
        int randOrder = (int) Math.floor(Slot7IconUtil.ROWS.length * Math.random());
        byte[] randRow = Slot7IconUtil.ROWS[randOrder];
        if (giftType == GiftType.FREE_SPIN) {
            for (int i = 0; i < 3; i++) {
                this.table[randRow[i]] = Slot7IconUtil.FREE_SPIN;
            }
        }

        if (giftType == GiftType.MINI_GAME) {
            for (int i = 0; i < 4; i++) {
                this.table[randRow[i]] = Slot7IconUtil.BONUS;
            }
        }

        if (giftType == GiftType.JACKPOT) {
            for (int i = 0; i < 5; i++) {
                this.table[randRow[i]] = Slot7IconUtil.JACKPOT;
            }
        }
    }

    public void calculate(int[] rowsIndex) {
        this.money = 0;
        this.jackpot = false;
        this.freeSpin = 0;
        this.miniGame = 0;

        for (int i = 0; i < rowsIndex.length; i++) {
            byte row[] = Slot7IconUtil.getIconsInRow(table, Slot7IconUtil.ROWS[rowsIndex[i]-1]);

            RowValue rowValue = Slot7IconUtil.getRowValue(row);
            Gift gift = Slot7IconUtil.getGift(rowValue);

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
