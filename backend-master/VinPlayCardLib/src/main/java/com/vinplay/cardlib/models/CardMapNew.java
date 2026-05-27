/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.CardGroup;
import com.vinplay.cardlib.models.GroupType;
import com.vinplay.cardlib.models.Hand;
import com.vinplay.cardlib.models.Rank;
import com.vinplay.cardlib.models.Suit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CardMapNew {
    private Hand hand = null;
    private CardGroup cardGroup = null;
    private int[][] map = new int[5][14];

    public CardGroup getCardGroup(Hand hand) {
        this.hand = hand;
        this.map = new int[5][14];
        this.cardGroup = null;
        this.createMap();
        this.getStraightFlush();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getFourOfKind();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getFullHouse();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getFlush();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getStraight();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getThreeOfKind();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getTwoPair();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getOnePair();
        if (this.cardGroup != null) {
            return this.cardGroup;
        }
        this.getHighCard();
        return this.cardGroup;
    }

    private void createMap() {
        for (int z = 0; z < this.hand.size(); ++z) {
            Card element = this.hand.getCards().get(z);
            this.map[element.getSuit().getSuitValue()][element.getRank().getRank()] = 1;
        }
        for (int j = 12; j >= 0; --j) {
            for (int i = 3; i >= 0; --i) {
                if (this.map[i][j] <= 0) continue;
                int[] arrn = this.map[i];
                arrn[13] = arrn[13] + 1;
                int[] arrn2 = this.map[4];
                int n = j;
                arrn2[n] = arrn2[n] + 1;
            }
        }
    }

    private void getStraightFlush() {
        HashMap<Integer, Integer> resMap = new HashMap<Integer, Integer>();
        for (int i = 3; i >= 0; --i) {
            if (this.map[i][13] < 5) continue;
            int num = 0;
            int rankMin = -1;
            for (int j = 12; j >= 0; --j) {
                if (this.map[i][j] > 0) {
                    if (++num != 5) continue;
                    rankMin = j;
                    resMap.put(i, j);
                    break;
                }
                num = 0;
            }
            if (rankMin == Rank.Ten.getRank()) break;
        }
        if (resMap.size() > 0) {
            int sfRank = -1;
            int sfSuit = -1;
            for (Map.Entry entry : resMap.entrySet()) {
                if ((Integer)entry.getValue() <= sfRank) continue;
                sfRank = (Integer)entry.getValue();
                sfSuit = (Integer)entry.getKey();
            }
            sfRank += 4;
            Hand sfHand = new Hand();
            for (int x = 0; x < 5; ++x) {
                sfHand.addCard(new Card(Rank.getRank(sfRank), Suit.getSuit(sfSuit)));
                --sfRank;
            }
            this.cardGroup = new CardGroup(GroupType.StraightFlush.getCode(), sfHand);
        }
    }

    private void getFourOfKind() {
        for (int j = 12; j >= 0; --j) {
            if (this.map[4][j] != 4) continue;
            Hand fkHand = new Hand();
            for (int i = 3; i >= 0; --i) {
                fkHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
            }
            for (int z = 12; z >= 0; --z) {
                if (z == j || this.map[4][z] <= 0) continue;
                for (int i = 3; i >= 0; --i) {
                    if (this.map[i][z] <= 0) continue;
                    fkHand.addCard(new Card(Rank.getRank(z), Suit.getSuit(i)));
                    this.cardGroup = new CardGroup(GroupType.FourOfKind.getCode(), fkHand);
                    return;
                }
            }
        }
    }

    private void getFullHouse() {
        int cnt = 0;
        int rankThree = -1;
        int rankPair = -1;
        for (int j = 12; j >= 0; --j) {
            int i;
            if (this.map[4][j] < 2) continue;
            if (rankThree == -1 && this.map[4][j] == 3) {
                ++cnt;
                rankThree = j;
            } else if (rankPair == -1) {
                ++cnt;
                rankPair = j;
            }
            if (cnt != 2) continue;
            Hand fhHand = new Hand();
            for (i = 3; i >= 0; --i) {
                if (this.map[i][rankThree] <= 0) continue;
                fhHand.addCard(new Card(Rank.getRank(rankThree), Suit.getSuit(i)));
            }
            cnt = 0;
            for (i = 3; i >= 0; --i) {
                if (this.map[i][rankPair] <= 0) continue;
                fhHand.addCard(new Card(Rank.getRank(rankPair), Suit.getSuit(i)));
                if (++cnt != 2) continue;
                this.cardGroup = new CardGroup(GroupType.FullHouse.getCode(), fhHand);
                return;
            }
        }
    }

    private void getFlush() {
        for (int i = 3; i >= 0; --i) {
            if (this.map[i][13] < 5) continue;
            Hand fHand = new Hand();
            int cnt = 0;
            for (int j = 12; j >= 0; --j) {
                if (this.map[i][j] <= 0) continue;
                fHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
                if (++cnt != 5) continue;
                this.cardGroup = new CardGroup(GroupType.Flush.getCode(), fHand);
                return;
            }
        }
    }

    private void getStraight() {
        int size = 0;
        for (int j = 12; j >= 0; --j) {
            if (this.map[4][j] > 0) {
                if (++size != 5) continue;
                Hand sHand = new Hand();
                j += 4;
                for (int x = 0; x < 5; ++x) {
                    for (int i = 3; i >= 0; --i) {
                        if (this.map[i][j] <= 0) continue;
                        sHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
                        break;
                    }
                    --j;
                }
                this.cardGroup = new CardGroup(GroupType.Straight.getCode(), sHand);
                return;
            }
            size = 0;
        }
    }

    private void getThreeOfKind() {
        for (int j = 12; j >= 0; --j) {
            if (this.map[4][j] != 3) continue;
            Hand tkHand = new Hand();
            for (int i = 3; i >= 0; --i) {
                if (this.map[i][j] <= 0) continue;
                tkHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
            }
            int cnt = 0;
            block2 : for (int z = 12; z >= 0; --z) {
                if (z == j || this.map[4][z] <= 0) continue;
                for (int i = 3; i >= 0; --i) {
                    if (this.map[i][z] <= 0) continue;
                    tkHand.addCard(new Card(Rank.getRank(z), Suit.getSuit(i)));
                    if (++cnt != 2) continue block2;
                    this.cardGroup = new CardGroup(GroupType.ThreeOfKind.getCode(), tkHand);
                    return;
                }
            }
        }
    }

    private void getTwoPair() {
        int cnt = 0;
        int rankPair1 = -1;
        Hand tpHand = new Hand();
        for (int j = 12; j >= 0; --j) {
            if (this.map[4][j] != 2) continue;
            ++cnt;
            for (int i = 3; i >= 0; --i) {
                if (this.map[i][j] <= 0) continue;
                tpHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
            }
            if (cnt == 1) {
                rankPair1 = j;
                continue;
            }
            if (cnt != 2) continue;
            for (int z = 12; z >= 0; --z) {
                if (z == rankPair1 || z == j || this.map[4][z] <= 0) continue;
                for (int i = 3; i >= 0; --i) {
                    if (this.map[i][z] <= 0) continue;
                    tpHand.addCard(new Card(Rank.getRank(z), Suit.getSuit(i)));
                    this.cardGroup = new CardGroup(GroupType.TwoPair.getCode(), tpHand);
                    return;
                }
            }
        }
    }

    private void getOnePair() {
        Hand opHand = new Hand();
        for (int j = 12; j >= 0; --j) {
            if (this.map[4][j] != 2) continue;
            for (int i = 3; i >= 0; --i) {
                if (this.map[i][j] <= 0) continue;
                opHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
            }
            int cnt = 0;
            block2 : for (int z = 12; z >= 0; --z) {
                if (z == j || this.map[4][z] <= 0) continue;
                for (int i = 3; i >= 0; --i) {
                    if (this.map[i][z] <= 0) continue;
                    opHand.addCard(new Card(Rank.getRank(z), Suit.getSuit(i)));
                    if (++cnt != 3) continue block2;
                    this.cardGroup = new CardGroup(GroupType.OnePair.getCode(), opHand);
                    return;
                }
            }
        }
    }

    private void getHighCard() {
        Hand hHand = new Hand();
        int cnt = 0;
        block0 : for (int j = 12; j >= 0; --j) {
            if (this.map[4][j] <= 0) continue;
            for (int i = 3; i >= 0; --i) {
                if (this.map[i][j] <= 0) continue;
                hHand.addCard(new Card(Rank.getRank(j), Suit.getSuit(i)));
                if (++cnt != 5) continue block0;
                this.cardGroup = new CardGroup(GroupType.HighCard.getCode(), hHand);
                return;
            }
        }
    }
}

