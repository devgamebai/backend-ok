/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.GameUtils
 */
package game.xizach.server.logic;

import game.utils.GameUtils;
import game.xizach.server.logic.Card;
import game.xizach.server.logic.GroupCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardSuit {
    public static final int MAX_PLAYERS = 6;
    public static final int INITIAL_NUMBER_OF_CARDS = 2;
    public static final int MAX_NUMBER_OF_CARDS = 5;
    public static final int INIT_CARD_SIZE = 2;
    public List<Integer> ids = new ArrayList<Integer>();
    public volatile int nextIndex = 0;

    public CardSuit() {
        Integer i = 0;
        while (i < 52) {
            this.ids.add(i);
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
        Collections.shuffle(this.ids);
    }

    public void initCard() {
        this.ids.clear();
        Integer i = 0;
        while (i < 52) {
            this.ids.add(i);
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
    }

    public boolean setOrder(byte[] cards) {
        StringBuilder sb = new StringBuilder();
        this.ids.clear();
        for (int i = 0; i < cards.length; ++i) {
            this.ids.add(Integer.valueOf(cards[i]));
            sb.append(cards[i]).append(",");
        }
        return true;
    }

    public void setRandomFirstTurn() {
        Collections.shuffle(this.ids);
    }

    public void setRandom() {
        Collections.shuffle(this.ids);
    }

    public void removeRandom() {
        Collections.sort(this.ids);
    }

    public List<GroupCard> dealCards() {
        ArrayList<GroupCard> groupCards = new ArrayList<GroupCard>();
        int[] cards = new int[5];
        boolean flag = true;
        if (!GameUtils.isCheat) {
            this.setRandom();
        }
        groupCards.clear();
        int curentIndex = 0;
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 5; ++j) {
                cards[j] = this.ids.get(curentIndex++);
            }
            GroupCard gc = new GroupCard(cards);
            groupCards.add(gc);
        }
        this.nextIndex = curentIndex;
        return groupCards;
    }

    public Card rutBai(GroupCard c) {
        byte cardId = (byte)this.ids.get(this.nextIndex++).intValue();
        Card card = Card.createCard(cardId);
        c.addCard(card);
        return card;
    }

    public String toCardString() {
        StringBuilder sb = new StringBuilder();
        Integer[] data = new Integer[this.ids.size()];
        for (int i = 0; i < this.ids.size(); ++i) {
            data[i] = (int)this.ids.get(i);
        }
        return sb.toString();
    }

    public String toIdList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.ids.size(); ++i) {
        }
        return sb.toString();
    }

    public void fromCardString(String cardString) {
        GroupCard cardsList = new GroupCard();
        cardsList.fromNotSort(cardString);
        this.fromGroupCard(cardsList);
    }

    public void fromGroupCard(GroupCard gc) {
        int i;
        this.ids.clear();
        for (i = 0; i < gc.cards.size(); ++i) {
            Card c = gc.cards.get(i);
            Integer b = c.ID;
            this.ids.add((int)b);
        }
        for (i = gc.cards.size(); i < 52; ++i) {
            this.ids.add(Integer.parseInt("0"));
        }
    }

    public void fromIdList(String s) {
        int i;
        String[] ss = s.split(",");
        this.ids.clear();
        for (i = 0; i < ss.length; ++i) {
            Integer b = Integer.parseInt(ss[i]);
            this.ids.add(b);
        }
        for (i = ss.length; i < 52; ++i) {
            this.ids.add(Integer.parseInt("0"));
        }
    }
}

