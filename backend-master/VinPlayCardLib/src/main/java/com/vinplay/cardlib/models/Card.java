/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

import com.vinplay.cardlib.models.Rank;
import com.vinplay.cardlib.models.Suit;

public class Card {
    private Suit suit;
    private Rank rank;

    public Card(Rank r, Suit s) {
        this.suit = s;
        this.rank = r;
    }

    public Card(int code) {
        this.suit = Suit.getSuit(code % 4);
        this.rank = Rank.getRank(code / 4);
    }

    public int getCompare(Card other) {
        int result = 0;
        if (this.rank.getRank() > other.getRank().getRank()) {
            result = 2;
        } else if (this.rank.getRank() == other.getRank().getRank()) {
            result = 1;
        }
        return result;
    }

    public boolean compareTo(Card other) {
        return this.getCode() == other.getCode();
    }

    public int getCode() {
        return this.rank.getRank() * 4 + this.suit.getSuitValue();
    }

    public Rank getRank() {
        return this.rank;
    }

    public Suit getSuit() {
        return this.suit;
    }

    public String toString() {
        return this.rank.getName() + this.suit.getName();
    }
}

