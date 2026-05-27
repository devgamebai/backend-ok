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
import java.util.List;

public class CardMap {
    private Hand hand;
    private CardGroup cardGroup = null;
    private int[][] map = new int[5][15];

    public CardGroup getCardGroup(Hand hand) {
        this.hand = hand;
        this.map = new int[5][15];
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
        int j;
        int i;
        int length = this.hand.size();
        for (i = 0; i < length; ++i) {
            Card element = this.hand.getCards().get(i);
            if (element.getRank().getRank() == Rank.Ace.getRank()) {
                this.map[element.getSuit().getSuitValue()][0] = 1;
                this.map[element.getSuit().getSuitValue()][Rank.Ace.getRank() + 1] = 1;
                continue;
            }
            this.map[element.getSuit().getSuitValue()][element.getRank().getRank() + 1] = 1;
        }
        for (i = 0; i < 14; ++i) {
            int num = 0;
            for (j = 0; j < 4; ++j) {
                if (this.map[j][i] <= 0) continue;
                ++num;
            }
            this.map[4][i] = num;
        }
        for (i = 0; i < 4; ++i) {
            int num = 0;
            for (j = 0; j < 14; ++j) {
                if (this.map[i][j] <= 0) continue;
                ++num;
            }
            this.map[i][14] = num;
        }
    }

    private void getStraightFlush() {
        int num = 0;
        int sizeOfStraight = 0;
        int location = 0;
        for (int i = 0; i < 4; ++i) {
            if (this.map[i][14] < 5) continue;
            for (int j = 0; j < 14; ++j) {
                if (this.map[i][j] > 0) {
                    ++num;
                    if (j != 13) continue;
                    if (num > sizeOfStraight) {
                        sizeOfStraight = num;
                        location = j;
                    }
                    num = 0;
                    continue;
                }
                if (num > sizeOfStraight) {
                    sizeOfStraight = num;
                    location = j - 1;
                }
                num = 0;
            }
            if (sizeOfStraight < 5) continue;
            Hand resultHand = new Hand();
            for (int j = 0; j < 5; ++j) {
                resultHand.addCard(new Card(Rank.getRank(location - 1), Suit.getSuit(i)));
                --location;
            }
            this.cardGroup = new CardGroup(GroupType.StraightFlush.getCode(), resultHand);
        }
    }

    private void getFourOfKind() {
        for (int i = 13; i > 0; --i) {
            if (this.map[4][i] != 4) continue;
            Hand newHand = new Hand();
            newHand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(0)));
            newHand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(1)));
            newHand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(2)));
            newHand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(3)));
            for (int j = 13; j > 0; --j) {
                if (j == i || this.map[4][j] <= 0) continue;
                if (this.map[0][j] > 0) {
                    newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(0)));
                    break;
                }
                if (this.map[1][j] > 0) {
                    newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(0)));
                    break;
                }
                if (this.map[2][j] > 0) {
                    newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(0)));
                    break;
                }
                if (this.map[3][j] <= 0) continue;
                newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(0)));
                break;
            }
            this.cardGroup = new CardGroup(GroupType.FourOfKind.getCode(), newHand);
            break;
        }
    }

    private void getFullHouse() {
        int i;
        boolean isOnePair = false;
        boolean isThreeOfKind = false;
        int locationThree = 0;
        int locationPair = 0;
        for (i = 13; i > 0; --i) {
            if (this.map[4][i] == 3) {
                locationThree = i;
                isThreeOfKind = true;
            }
            if (this.map[4][i] != 2) continue;
            locationPair = i;
            isOnePair = true;
        }
        if (isOnePair && isThreeOfKind) {
            Hand newHand = new Hand();
            for (i = 0; i < 4; ++i) {
                if (this.map[i][locationThree] <= 0) continue;
                newHand.addCard(new Card(Rank.getRank(locationThree - 1), Suit.getSuit(i)));
            }
            for (i = 0; i < 4; ++i) {
                if (this.map[i][locationPair] <= 0) continue;
                newHand.addCard(new Card(Rank.getRank(locationPair - 1), Suit.getSuit(i)));
            }
            this.cardGroup = new CardGroup(GroupType.FullHouse.getCode(), newHand);
        }
    }

    private void getFlush() {
        for (int i = 0; i < 4; ++i) {
            if (!(this.map[i][14] - 1 >= 5 && this.map[i][0] > 0 || this.map[i][14] >= 5 && this.map[i][0] == 0)) continue;
            Hand newHand = new Hand();
            for (int j = 13; j > 0; --j) {
                if (this.map[i][j] <= 0) continue;
                newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(i)));
                if (newHand.getCards().size() == 5) break;
            }
            this.cardGroup = new CardGroup(GroupType.Flush.getCode(), newHand);
        }
    }

    private void getStraight() {
        int currentLenght = 0;
        int max = 0;
        int location = 0;
        for (int i = 0; i < 14; ++i) {
            if (this.map[4][i] > 0) {
                ++currentLenght;
                continue;
            }
            if (currentLenght > max) {
                max = currentLenght;
                location = i - 1;
            }
            currentLenght = 0;
        }
        if (currentLenght > max) {
            max = currentLenght;
            location = 13;
        }
        if (max >= 5) {
            Hand newHand = new Hand();
            for (int j = location; j > location - 5; --j) {
                for (int k = 0; k < 4; ++k) {
                    if (this.map[k][j] <= 0) continue;
                    if (j == 0) {
                        newHand.addCard(new Card(Rank.getRank(12), Suit.getSuit(k)));
                        continue;
                    }
                    newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(k)));
                }
            }
            this.cardGroup = new CardGroup(GroupType.Straight.getCode(), newHand);
        }
    }

    private void getThreeOfKind() {
        for (int i = 13; i > 0; --i) {
            if (this.map[4][i] != 3) continue;
            Hand newhand = new Hand();
            for (int j = 0; j < 4; ++j) {
                if (this.map[j][i] <= 0) continue;
                newhand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(j)));
            }
            int num = 0;
            for (int j = 13; j > 0; --j) {
                if (j == i) continue;
                for (int k = 0; k < 4; ++k) {
                    if (this.map[k][j] <= 0) continue;
                    newhand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(k)));
                    ++num;
                    break;
                }
                if (num == 2) break;
            }
            this.cardGroup = new CardGroup(GroupType.ThreeOfKind.getCode(), newhand);
            break;
        }
    }

    private void getTwoPair() {
        int numPair = 0;
        Hand newHand = new Hand();
        for (int i = 13; i > 0; --i) {
            int j;
            if (this.map[4][i] != 2) continue;
            ++numPair;
            for (j = 0; j < 4; ++j) {
                if (this.map[j][i] <= 0) continue;
                newHand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(j)));
            }
            if (numPair != 2) continue;
            for (j = 13; j > 0; --j) {
                if (this.map[4][j] == 1) {
                    for (int k = 0; k < 4; ++k) {
                        if (this.map[k][j] <= 0) continue;
                        newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(k)));
                        break;
                    }
                }
                if (newHand.getCards().size() == 5) break;
            }
            this.cardGroup = new CardGroup(GroupType.TwoPair.getCode(), newHand);
            break;
        }
    }

    private void getOnePair() {
        for (int i = 13; i > 0; --i) {
            if (this.map[4][i] != 2) continue;
            Hand newHand = new Hand();
            for (int j = 0; j < 4; ++j) {
                if (this.map[j][i] <= 0) continue;
                newHand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(j)));
            }
            int num = 0;
            for (int j = 13; j > 0; --j) {
                if (this.map[4][j] == 1) {
                    for (int k = 0; k < 4; ++k) {
                        if (this.map[k][j] <= 0) continue;
                        ++num;
                        newHand.addCard(new Card(Rank.getRank(j - 1), Suit.getSuit(k)));
                        break;
                    }
                }
                if (num == 3) break;
            }
            this.cardGroup = new CardGroup(GroupType.OnePair.getCode(), newHand);
            break;
        }
    }

    private void getHighCard() {
        Hand newhand = new Hand();
        for (int i = 13; i > 0; --i) {
            if (this.map[4][i] <= 0) continue;
            for (int j = 0; j < 4; ++j) {
                if (this.map[j][i] <= 0) continue;
                newhand.addCard(new Card(Rank.getRank(i - 1), Suit.getSuit(j)));
                break;
            }
            this.cardGroup = new CardGroup(GroupType.HighCard.getCode(), newhand);
            break;
        }
    }
}

