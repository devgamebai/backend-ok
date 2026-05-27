/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.Hand;
import java.util.List;

public class CardGroup
implements Comparable<Object> {
    private int type = 0;
    private Hand hand = null;

    public CardGroup() {
    }

    public CardGroup(int type, Hand hand) {
        this.type = type;
        this.hand = hand;
    }

    public int getCompare(CardGroup cardGroup) {
        int result = 0;
        if (this.type > cardGroup.getType()) {
            result = 2;
        } else if (this.type == cardGroup.getType()) {
            result = this.compareWithEqualsType(cardGroup);
        }
        return result;
    }

    public int compareWithEqualsType(CardGroup cardGroup) {
        int result = 0;
        switch (this.type) {
            case 3: 
            case 4: 
            case 6: 
            case 7: 
            case 8: {
                result = this.compareFirstCard(cardGroup.getHand());
                break;
            }
            default: {
                result = this.compareFullHand(cardGroup.getHand());
            }
        }
        return result;
    }

    private int compareFirstCard(Hand hand) {
        return this.hand.getCards().get(0).getCompare(hand.getCards().get(0));
    }

    private int compareFullHand(Hand hand) {
        int result = 1;
        int temp = 0;
        for (int i = 0; i < 5; ++i) {
            temp = this.hand.getCards().get(i).getCompare(hand.getCards().get(i));
            if (temp == 1) continue;
            result = temp;
            break;
        }
        return result;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Hand getHand() {
        return this.hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    @Override
    public int compareTo(Object otherRankObject) {
        return 0;
    }
}

