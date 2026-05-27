/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.GroupType
 *  com.vinplay.cardlib.utils.CardLibUtils
 */
package game.modules.Minipoker;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.GroupType;
import com.vinplay.cardlib.utils.CardLibUtils;
import game.modules.minigame.config.MinipokerResultData;
import game.modules.minigame.utils.GenerationMiniPoker;
import game.utils.GameUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DealCardMinipoker {
    public static int[] rate = new int[]{35, 35, 15, 10, 5};
    public static GenerationMiniPoker gen = new GenerationMiniPoker();

    public static byte[] rollJackPot() {
        int start = GameUtil.randomBetween(5, 9);
        ArrayList<Byte> card = new ArrayList<Byte>();
        for (int i = 0; i < 5; ++i) {
            card.add((byte)((start + i) % 13));
        }
        int color = GameUtil.randomMax(4);
        byte[] listCard = new byte[5];
        for (int i = 0; i < listCard.length; ++i) {
            listCard[i] = (byte)((Byte)card.get(i) * 4 + color);
        }
        return listCard;
    }

    public static List<Card> dealCardJackpotMinipoker() {
        byte[] listCard = DealCardMinipoker.rollJackPot();
        ArrayList<Card> listReturn = new ArrayList<Card>();
        for (int i = 0; i < listCard.length; ++i) {
            listReturn.add(new Card((int)listCard[i]));
        }
        return listReturn;
    }

    public static byte[] rollStraighFlush() {
        int start = GameUtil.randomMax(5);
        ArrayList<Byte> card = new ArrayList<Byte>();
        for (int i = 0; i < 5; ++i) {
            card.add((byte)((start - 1 + 13 + i) % 13));
        }
        int color = GameUtil.randomMax(4);
        byte[] listCard = new byte[5];
        for (int i = 0; i < listCard.length; ++i) {
            listCard[i] = (byte)((Byte)card.get(i) * 4 + color);
        }
        return listCard;
    }

    public static byte[] rollStraight() {
        int start = GameUtil.randomMax(10);
        ArrayList<Byte> card = new ArrayList<Byte>();
        for (int i = 0; i < 5; ++i) {
            card.add((byte)((start - 1 + 13 + i) % 13));
        }
        byte[] listCard = new byte[5];
        for (int i = 0; i < listCard.length; ++i) {
            listCard[i] = (byte)((Byte)card.get(i) * 4 + GameUtil.randomMax(4));
        }
        return listCard;
    }

    public static byte[] rollFlush() {
        ArrayList<Byte> card = new ArrayList<Byte>();
        for (int i = 0; i < 13; ++i) {
            card.add((byte)i);
        }
        Collections.shuffle(card);
        int color = GameUtil.randomMax(4);
        ArrayList<Byte> listCard1 = new ArrayList<Byte>();
        for (int i = 0; i < 5; ++i) {
            listCard1.add((byte)((Byte)card.get(i) * 4 + color));
        }
        byte[] listCard = new byte[5];
        for (int i = 0; i < listCard.length; ++i) {
            listCard[i] = (Byte)listCard1.get(i);
        }
        return listCard;
    }

    public static byte[] rollFullHouse() {
        int i;
        byte[] listCard = new byte[5];
        ArrayList<Byte> card = new ArrayList<Byte>();
        for (int i2 = 0; i2 < 13; ++i2) {
            card.add((byte)i2);
        }
        Collections.shuffle(card);
        byte index0 = (Byte)card.get(0);
        byte index1 = (Byte)card.get(1);
        card.clear();
        for (i = 0; i < 4; ++i) {
            card.add((byte)(index0 * 4 + i));
        }
        Collections.shuffle(card);
        for (i = 0; i < 3; ++i) {
            listCard[i] = (Byte)card.get(i);
        }
        card.clear();
        for (i = 0; i < 4; ++i) {
            card.add((byte)(index1 * 4 + i));
        }
        Collections.shuffle(card);
        for (i = 2; i < 4; ++i) {
            listCard[i] = (Byte)card.get(i);
        }
        return listCard;
    }

    public static byte[] rollFourOfAKind() {
        byte[] listCard = new byte[5];
        int value = GameUtil.randomMax(13);
        byte card = (byte)((value + 1) % 13 + GameUtil.randomMax(4));
        for (int i = 0; i < 4; ++i) {
            listCard[i] = (byte)(value * 4 + i);
        }
        listCard[4] = card;
        return listCard;
    }

    public static byte[] rollThreeOfAKind() {
        int i;
        byte[] listCard = new byte[5];
        int value = GameUtil.randomMax(13);
        byte card1 = (byte)((value + 1) % 13 + GameUtil.randomMax(4));
        byte card2 = (byte)((value - 1) % 13 + GameUtil.randomMax(4));
        ArrayList<Byte> listCardThree = new ArrayList<Byte>();
        for (i = 0; i < 4; ++i) {
            listCardThree.add((byte)(value * 4 + i));
        }
        Collections.shuffle(listCardThree);
        for (i = 0; i < 3; ++i) {
            listCard[i] = (Byte)listCardThree.get(i);
        }
        listCard[3] = card1;
        listCard[4] = card2;
        return listCard;
    }

    public static byte[] rollTwoPair() {
        int i;
        byte[] listCard = new byte[5];
        ArrayList<Byte> card = new ArrayList<Byte>();
        for (int i2 = 0; i2 < 13; ++i2) {
            card.add((byte)i2);
        }
        Collections.shuffle(card);
        byte index0 = (Byte)card.get(0);
        byte index1 = (Byte)card.get(1);
        byte index2 = (Byte)card.get(2);
        card.clear();
        for (i = 0; i < 4; ++i) {
            card.add((byte)(index0 * 4 + i));
        }
        Collections.shuffle(card);
        listCard[0] = (Byte)card.get(0);
        listCard[1] = (Byte)card.get(1);
        card.clear();
        for (i = 0; i < 4; ++i) {
            card.add((byte)(index1 * 4 + i));
        }
        Collections.shuffle(card);
        listCard[2] = (Byte)card.get(2);
        listCard[3] = (Byte)card.get(3);
        card.clear();
        for (i = 0; i < 4; ++i) {
            card.add((byte)(index2 * 4 + i));
        }
        Collections.shuffle(card);
        listCard[4] = (Byte)card.get(3);
        return listCard;
    }

    public static List<Card> dealCardForUserBigWin() {
        int x = GameUtil.randomMax(100);
        if (x < 50) {
            return DealCardMinipoker.playLose().cards;
        }
        int index = DealCardMinipoker.getIndexRollBigWin();
        if (index == 0) {
            return DealCardMinipoker.playLose().cards;
        }
        if (index == 1) {
            return DealCardMinipoker.playPairJ();
        }
        if (index == 2) {
            return DealCardMinipoker.byteArrayToList(DealCardMinipoker.rollTwoPair());
        }
        if (index == 3) {
            return DealCardMinipoker.byteArrayToList(DealCardMinipoker.rollThreeOfAKind());
        }
        if (index == 4) {
            return DealCardMinipoker.byteArrayToList(DealCardMinipoker.rollStraight());
        }
        return DealCardMinipoker.playPairJ();
    }

    public static int getIndexRollBigWin() {
        int random = GameUtil.randomMax(100);
        for (int i = 0; i < rate.length; ++i) {
            if (random < rate[i]) {
                return i;
            }
            random -= rate[i];
        }
        return rate.length - 1;
    }

    public static List<Card> playPairJ() {
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            List<Card> cards = gen.randomCards();
            GroupType groupType = CardLibUtils.calculateTypePoker(cards);
            if (groupType == null) {
                ++retryCount;
                continue;
            }
            if (groupType == GroupType.OnePair && CardLibUtils.pairEqualOrGreatJack(cards)) {
                return cards;
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
        return gen.randomCards();
    }

    public static MinipokerResultData playLose() {
        List<Card> cards;
        MinipokerResultData minipokerResultData = new MinipokerResultData();
        int retryCount = 0;
        int maxRetries = 10000;
        while (retryCount < maxRetries) {
            cards = gen.randomCards();
            GroupType groupType = CardLibUtils.calculateTypePoker(cards);
            if (groupType == null) {
                ++retryCount;
                continue;
            }
            if (groupType == GroupType.HighCard) {
                minipokerResultData.cards = cards;
                minipokerResultData.groupType = groupType;
                minipokerResultData.result = (short)11;
                return minipokerResultData;
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
        cards = gen.randomCards();
        minipokerResultData.cards = cards;
        minipokerResultData.groupType = CardLibUtils.calculateTypePoker(cards);
        minipokerResultData.result = (short)11;
        return minipokerResultData;
    }

    public static List<Card> byteArrayToList(byte[] listCard) {
        ArrayList<Card> listReturn = new ArrayList<Card>();
        for (int i = 0; i < listCard.length; ++i) {
            listReturn.add(new Card((int)listCard[i]));
        }
        return listReturn;
    }
}

