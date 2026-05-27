/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.GameUtils
 */
package game.bacay.server.logic;

import game.bacay.server.logic.Card;
import game.bacay.server.logic.GroupCard;
import game.utils.GameUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CardSuit {
    public static final int SO_CHI = 2;
    public static final int MAX_PLAYERS = 8;
    public static final int SOLO_PLAYERS = 2;
    public static final int MAX_NUMBER_OF_CARDS = 3;
    public List<Integer> ids = new ArrayList<Integer>();
    public volatile boolean noHu = false;

    public CardSuit() {
        Integer i = 0;
        while (i < 36) {
            this.ids.add(i);
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
        Collections.shuffle(this.ids);
    }

    public void initCard() {
        this.ids.clear();
        Integer i = 0;
        while (i < 36) {
            this.ids.add(i);
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
    }

    public boolean setOrder(byte[] cards) {
        this.ids.clear();
        for (int i = 0; i < cards.length; ++i) {
            this.ids.add(Integer.valueOf(cards[i]));
        }
        return true;
    }

    public void setRandomFirstTurn() {
        Collections.shuffle(this.ids);
    }

    public void setRandom() {
        if (!this.noHu) {
            Collections.shuffle(this.ids);
        } else {
            this.noHu = false;
        }
    }

    public void removeRandom() {
        Collections.sort(this.ids);
    }

    public List<GroupCard> dealCards(boolean allowNoHu) {
        ArrayList<GroupCard> groupCards = new ArrayList<GroupCard>();
        int[] cards = new int[3];
        int count = 0;
        boolean flag = true;
        int curentIndex = 0;
        while (flag) {
            flag = false;
            if (count > 0) {
                this.setRandom();
            }
            groupCards.clear();
            curentIndex = 0;
            for (int i = 0; i < 8; ++i) {
                for (int j = 0; j < 3; ++j) {
                    cards[j] = this.ids.get(curentIndex++);
                }
                GroupCard gc = new GroupCard(cards);
                groupCards.add(gc);
                if (!gc.isNoHu() || this.noHu || GameUtils.isCheat) continue;
                flag = true;
                if (allowNoHu) {
                    flag = false;
                }
                ++count;
            }
        }
        for (int j = 0; j < 3; ++j) {
            cards[j] = this.ids.get(curentIndex++);
        }
        GroupCard gc = new GroupCard(cards);
        groupCards.add(gc);
        return groupCards;
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
        for (i = gc.cards.size(); i < 36; ++i) {
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
        for (i = ss.length; i < 36; ++i) {
            this.ids.add(Integer.parseInt("0"));
        }
    }

    public synchronized void noHuAt(int chair) {
        this.setNoHu(chair);
        this.noHu = true;
    }

    public void setNoHu(int chair) {
        ArrayList<Integer> fullCard = new ArrayList<Integer>();
        ArrayList<Integer> subCard = new ArrayList<Integer>();
        Integer i = 0;
        while (i < 36) {
            fullCard.add(i);
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
        Card C_7R = new Card(7, 3);
        Card C_8R = new Card(8, 3);
        Card C_9R = new Card(9, 3);
        subCard.add(C_7R.ID);
        subCard.add(C_8R.ID);
        subCard.add(C_9R.ID);
        fullCard.removeAll(subCard);
        Collections.shuffle(fullCard);
        this.ids.clear();
        int index1 = 0;
        int index2 = 0;
        for (int i2 = 0; i2 < 8; ++i2) {
            for (int j = 0; j < 3; ++j) {
                if (i2 == chair) {
                    this.ids.add((Integer)subCard.get(index2++));
                    continue;
                }
                this.ids.add((Integer)fullCard.get(index1++));
            }
        }
    }
}

