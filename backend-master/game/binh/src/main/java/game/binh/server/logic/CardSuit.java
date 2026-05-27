/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.GameUtils
 */
package game.binh.server.logic;

import game.binh.server.logic.Card;
import game.binh.server.logic.GroupCard;
import game.binh.server.logic.ai.BinhAuto;
import game.binh.server.logic.ai.BinhGroup;
import game.binh.server.logic.ai.BinhSuit;
import game.utils.GameUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class CardSuit {
    public static final int SO_CHI = 2;
    public static final int MAX_PLAYERS = 4;
    public static final int SOLO_PLAYERS = 2;
    public static final int MAX_NUMBER_OF_CARDS = 13;
    public static final int MAX_NUMBER_OF_CARDS_CHI1 = 5;
    public static final int MAX_NUMBER_OF_CARDS_CHI2 = 5;
    public static final int MAX_NUMBER_OF_CARDS_CHI3 = 3;
    public List<Integer> ids = new ArrayList<Integer>();
    public List<Integer> chat = new ArrayList<Integer>();
    public int cheat = 0;
    public volatile boolean noHu = false;
    public volatile int chairNoHu = -1;

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
        this.cheat = 0;
    }

    public boolean setOrder(byte[] cards) {
        boolean[] hasId = new boolean[52];
        this.ids.clear();
        for (int i = 0; i < cards.length; ++i) {
            this.ids.add(Integer.valueOf(cards[i]));
            hasId[cards[i]] = true;
        }
        Integer i = 0;
        while (i < 52) {
            if (!hasId[i]) {
                this.ids.add(i);
            }
            Integer n = i;
            Integer n2 = i = Integer.valueOf(i + 1);
        }
        this.cheat = 500;
        return true;
    }

    public void setRandomFirstTurn() {
        Collections.shuffle(this.ids);
    }

    public void setRandom() {
    }

    public void removeRandom() {
        Collections.sort(this.ids);
    }

    public List<BinhGroup> dealCards(int rule, boolean canJackpot) {
        List<BinhGroup> groups = null;
        if (this.noHu) {
            BinhSuit suit = BinhAuto.instance().getSuitJackpot(rule);
            groups = suit.getListGroup();
            int indexJackpot = 0;
            for (int i = 0; i < groups.size(); ++i) {
                BinhGroup g = groups.get(i);
                if (!g.isJackpot()) continue;
                indexJackpot = i;
                break;
            }
            Collections.swap(groups, indexJackpot, this.chairNoHu);
            this.noHu = false;
            this.chairNoHu = -1;
        } else if (this.cheat > 0) {
            --this.cheat;
            groups = this.cheatCard();
        } else {
            BinhSuit suit = BinhAuto.instance().getSuit(rule, canJackpot);
            groups = suit.getListGroup();
        }
        return groups;
    }

    public List<BinhGroup> cheatCard() {
        LinkedList<BinhGroup> groups = new LinkedList<BinhGroup>();
        int currentIndex = 0;
        int[] cards = new int[13];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 13; ++j) {
                cards[j] = this.ids.get(currentIndex++);
            }
            GroupCard gc = new GroupCard(cards);
            BinhGroup bg = new BinhGroup(gc, 0);
            groups.add(bg);
        }
        return groups;
    }

    public List<GroupCard> dealCards(int rule) {
        ArrayList<GroupCard> groupCards = new ArrayList<GroupCard>();
        int[] cards = new int[13];
        boolean flag = true;
        int count = 0;
        while (flag) {
            flag = false;
            if (count > 0) {
                this.setRandom();
            }
            groupCards.clear();
            int curentIndex = 0;
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 13; ++j) {
                    cards[j] = this.ids.get(curentIndex++);
                }
                GroupCard gc = new GroupCard(cards);
                groupCards.add(gc);
                if (!gc.isNoHu(rule) || this.noHu || GameUtils.isCheat) continue;
                flag = true;
                ++count;
            }
        }
        return groupCards;
    }

    public String toCardString(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            Card c = Card.createCard(this.ids.get(i));
            sb.append(c.toString());
        }
        return sb.toString();
    }

    public String toCardString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.ids.size(); ++i) {
            Card c = Card.createCard(this.ids.get(i));
            sb.append(c.toString());
        }
        return sb.toString();
    }

    public String toIdList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.ids.size(); ++i) {
            sb.append(this.ids.get(i)).append(",");
        }
        return sb.toString();
    }

    public void fromIdList(String s) {
        String[] ss = s.split(",");
        this.ids.clear();
        for (int i = 0; i < ss.length; ++i) {
            Integer b = Integer.parseInt(ss[i]);
            this.ids.add(b);
        }
    }

    public synchronized void noHuAt(int chair) {
        this.setNoHu(chair);
        this.chairNoHu = chair;
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
        for (int i2 = 2; i2 <= 14; ++i2) {
            int random = Math.abs(rd.nextInt() % 4);
            Card c = Card.createCard(i2, random);
            subCard.add(c.ID);
        }
        fullCard.removeAll(subCard);
        Collections.shuffle(fullCard);
        this.ids.clear();
        int index1 = 0;
        int index2 = 0;
        for (int i3 = 0; i3 < 4; ++i3) {
            for (int j = 0; j < 13; ++j) {
                if (i3 == chair) {
                    this.ids.add((Integer)subCard.get(index2++));
                    continue;
                }
                this.ids.add((Integer)fullCard.get(index1++));
            }
        }
    }
}

