/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  game.utils.GameUtils
 */
package game.baicao.server.logic;

import bitzero.util.common.business.Debug;
import game.baicao.server.logic.Card;
import game.baicao.server.logic.GroupCard;
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
        for (int i = 0; i < 52; ++i) {
            this.ids.add(i);
        }
        Collections.shuffle(this.ids);
    }

    public void initCard() {
        this.ids.clear();
        for (int i = 0; i < 52; ++i) {
            this.ids.add(i);
        }
    }

    public boolean setOrder(byte[] cards) {
        Debug.trace((Object)"List bai cheat:");
        StringBuilder sb = new StringBuilder();
        this.ids.clear();
        for (int i = 0; i < cards.length; ++i) {
            this.ids.add(Integer.valueOf(cards[i]));
            sb.append(cards[i]).append(",");
        }
        Debug.trace((Object)sb);
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
        byte[] data = new byte[this.ids.size()];
        for (int i = 0; i < this.ids.size(); ++i) {
            data[i] = this.ids.get(i).byteValue();
        }
        return sb.toString();
    }

    public String toIdList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.ids.size(); ++i) {
            sb.append(this.ids.get(i).byteValue()).append(",");
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
            byte b = (byte)c.ID;
            this.ids.add(Integer.valueOf(b));
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
            int b = Integer.parseInt(ss[i]);
            this.ids.add(b);
        }
        for (i = ss.length; i < 52; ++i) {
            this.ids.add(Integer.parseInt("0"));
        }
    }

    public synchronized void noHuAt(int chair) {
        this.setNoHu(chair);
        this.noHu = true;
    }

    public void setNoHu(int chair) {
        int i;
        ArrayList<Integer> fullCard = new ArrayList<Integer>();
        ArrayList<Integer> subCard = new ArrayList<Integer>();
        Integer i2 = 0;
        while (i2 < 52) {
            fullCard.add(i2);
            Integer n = i2;
            Integer n2 = i2 = Integer.valueOf(i2 + 1);
        }
        Card C_7R = new Card(11, 3);
        Card C_8R = new Card(12, 3);
        Card C_9R = new Card(13, 3);
        subCard.add(C_7R.ID);
        subCard.add(C_8R.ID);
        subCard.add(C_9R.ID);
        fullCard.removeAll(subCard);
        Collections.shuffle(fullCard);
        this.ids.clear();
        int index1 = 0;
        int index2 = 0;
        for (i = 0; i < 8; ++i) {
            for (int j = 0; j < 3; ++j) {
                if (i == chair) {
                    this.ids.add((Integer)subCard.get(index2++));
                    continue;
                }
                this.ids.add((Integer)fullCard.get(index1++));
            }
        }
        for (i = index1; i < fullCard.size(); ++i) {
            this.ids.add((Integer)fullCard.get(index1++));
        }
    }
}

