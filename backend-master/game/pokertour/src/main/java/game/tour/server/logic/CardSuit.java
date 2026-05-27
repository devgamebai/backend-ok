/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.GameUtils
 */
package game.tour.server.logic;

import game.utils.GameUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CardSuit {
    public static final int MAX_PLAYERS = 6;
    public static final int SOLO_PLAYERS = 2;
    public static final int MAX_NUMBER_OF_CARDS = 2;
    public static final int MAX_NUMBER_OF_PUBLIC_CARDS = 5;
    public List<Integer> ids = new ArrayList<Integer>();
    public List<Integer> chat = new ArrayList<Integer>();
    public int currentIndex = 0;
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
        int i;
        boolean[] hasId = new boolean[52];
        this.ids.clear();
        for (i = 5; i < 17; ++i) {
            this.ids.add(Integer.valueOf(cards[i]));
            hasId[cards[i]] = true;
        }
        for (i = 0; i < 5; ++i) {
            this.ids.add(Integer.valueOf(cards[i]));
            hasId[cards[i]] = true;
        }
        for (i = 0; i < 52; ++i) {
            if (hasId[i]) continue;
            this.ids.add(i);
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

    public void initFromData(String data) {
        GroupCard gc = new GroupCard(data);
        this.ids.clear();
        for (int i = 0; i < gc.cards.size(); ++i) {
            this.ids.add(gc.cards.get((int)i).ID);
        }
    }

    public void removeRandom() {
        Collections.sort(this.ids);
    }

    public List<GroupCard> dealCards() {
        ArrayList<GroupCard> groupCards = new ArrayList<GroupCard>();
        int[] cards = new int[2];
        boolean flag = true;
        int count = 0;
        while (flag) {
            flag = false;
            if (count > 0) {
                this.setRandom();
            }
            groupCards.clear();
            this.currentIndex = 0;
            for (int i = 0; i < 6; ++i) {
                for (int j = 0; j < 2; ++j) {
                    cards[j] = this.ids.get(this.currentIndex++);
                }
                GroupCard gc = new GroupCard(cards);
                groupCards.add(gc);
                if (!gc.isNoHu() || this.noHu || GameUtils.isCheat) continue;
                flag = true;
                ++count;
            }
        }
        int[] publicCard = new int[5];
        for (int i = 0; i < 5; ++i) {
            publicCard[i] = this.ids.get(this.currentIndex++);
        }
        GroupCard gc = new GroupCard(publicCard);
        groupCards.add(gc);
        return groupCards;
    }

    public String toCardString(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            Card c = new Card(this.ids.get(i).byteValue());
            sb.append(c.toString());
        }
        return sb.toString();
    }

    public synchronized void noHuAt(int chair) {
        this.setNoHu(chair);
        this.noHu = true;
    }

    public void setNoHu(int chair) {
        ArrayList<Integer> fullCard = new ArrayList<Integer>();
        ArrayList<Integer> subCard = new ArrayList<Integer>();
        Integer i = 0;
        while (i < 52) {
            fullCard.add(i);
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
        Random rd = new Random();
        for (int i2 = 5; i2 <= 14; ++i2) {
            int random = Math.abs(rd.nextInt() % 4);
            Card c = new Card(i2, random);
            subCard.add(c.ID);
        }
        fullCard.removeAll(subCard);
        Collections.shuffle(fullCard);
        this.ids.clear();
        int index1 = 0;
        int index2 = 0;
        for (int i3 = 0; i3 < 6; ++i3) {
            for (int j = 0; j < 2; ++j) {
                if (i3 == chair) {
                    this.ids.add((Integer)subCard.get(index2++));
                    continue;
                }
                this.ids.add((Integer)fullCard.get(index1++));
            }
        }
    }
}

