/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

import com.vinplay.cardlib.models.Card;
import java.util.LinkedList;
import java.util.List;

public class Hand {
    private List<Card> listCard;

    public Hand() {
        this.listCard = new LinkedList<Card>();
    }

    public Hand(List<Card> incoming) {
        this.listCard = incoming;
    }

    public List<Card> getCards() {
        return this.listCard;
    }

    public void addCard(Card card) {
        this.listCard.add(card);
    }

    public int size() {
        if (this.listCard == null) {
            return 0;
        }
        return this.listCard.size();
    }

    public int[] toArray() {
        if (this.listCard == null) {
            return null;
        }
        int[] arr = new int[this.listCard.size()];
        for (int i = 0; i < this.listCard.size(); ++i) {
            arr[i] = this.listCard.get(i).getCode();
        }
        return arr;
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < this.listCard.size(); ++i) {
            result = result + this.listCard.get(i).getCode() + " ";
        }
        return result;
    }

    public String cardsToString() {
        StringBuilder builder = new StringBuilder();
        for (Card card : this.listCard) {
            builder.append(card.toString());
            builder.append(" ");
        }
        return builder.toString();
    }
}

