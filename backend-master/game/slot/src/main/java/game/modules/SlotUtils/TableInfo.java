package game.modules.SlotUtils;

import game.modules.slot.entities.slot.MiniGameSlotResponse;
import game.modules.slot.entities.slot.PickStarGift;
import game.modules.slot.entities.slot.PickStarGiftItem;
import game.modules.slot.entities.slot.PickStarGifts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TableInfo {
    public static final int BOX_MINIGAME_CONFIG = 4;
    public byte[] table;
    public int money;
    public boolean jackpot;
    public int freeSpin;

    //public int numberBonusMiniGame;

    public int miniGame;
    public MiniGameSlotResponse miniGameSlotResponse = null;

    public ArrayList<Integer> lineWin = new ArrayList<>();
    protected ArrayList<Long> moneyWin = new ArrayList<>();


    public long betLevel = 0;
    public long moneyEatPot = 0;
    public List<Integer> boxValues;

    private int randomBoxValue() {
        Random rd = new Random();
        int n = rd.nextInt(this.boxValues.size());
        return this.boxValues.get(n);
       // return 10;
    }

    protected MiniGameSlotResponse generatePickStars(int ratio) {
        MiniGameSlotResponse response = new MiniGameSlotResponse();
        int totalMoney = 0;
        ArrayList<PickStarGift> gifts = new ArrayList<PickStarGift>();
        PickStarGifts pickStarGifts = new PickStarGifts();
        String responsePickStars = "";
        int totalKeys = 1;
        block5 : for (int numPicks = 10; numPicks > 0; --numPicks) {
            PickStarGiftItem gift = pickStarGifts.pickRandomAndRandomGift();
            switch (gift) {
                case GOLD: {
                    totalMoney += BOX_MINIGAME_CONFIG * this.betLevel;
                    gifts.add(new PickStarGift(PickStarGiftItem.GOLD, 0));
                    continue block5;
                }
                case KEY: {
                    gifts.add(new PickStarGift(PickStarGiftItem.KEY, 0));
                    totalKeys++;
                    numPicks++;
                    continue block5;
                }
                case BOX: {
                    int boxValue = this.randomBoxValue();
                    totalMoney += boxValue * this.betLevel*totalKeys;
                    gifts.add(new PickStarGift(PickStarGiftItem.BOX, boxValue));
                    break;
                }
                case LOSE: {
                    gifts.add(new PickStarGift(PickStarGiftItem.LOSE, 0));
                    numPicks = 0; // end bonus immediately
                    break;
                }
            }
        }
        totalMoney *= ratio;
        responsePickStars = responsePickStars + ratio;
        for (PickStarGift pickStarGift : gifts) {
            if (responsePickStars.length() == 0) {
                responsePickStars = responsePickStars + pickStarGift.getName();
                continue;
            }
            responsePickStars = responsePickStars + "," + pickStarGift.getName();
        }
        response.setTotalPrize(totalMoney);
        response.setPrizes(responsePickStars);
        return response;
    }

    public String lineWinToString() {
        StringBuilder s = new StringBuilder();
        for (Integer integer : this.lineWin) {
            int line = integer;
            if (s.length() == 0) {
                s.append(line);
            } else {
                s.append(",").append(line);
            }
        }
        return s.toString();
    }

    public String moneyWinToString() {
        StringBuilder s = new StringBuilder();
        for (Long value : this.moneyWin) {
            if (s.length() == 0) {
                s.append(value);
            } else {
                s.append(",").append(value);
            }
        }
        return s.toString();
    }

    public String matrixToString() {
        StringBuilder s = new StringBuilder();
        for (byte value : this.table) {
            if (s.length() == 0) {
                s.append(value);
            } else {
                s.append(",").append(value);
            }
        }
        return s.toString();
    }
}
