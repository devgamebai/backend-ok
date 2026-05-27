/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.Hand;
import com.vinplay.cardlib.models.Rank;
import com.vinplay.cardlib.models.Suit;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Deck {
    protected Card[] deck;
    private int count = 0;

    public Deck() {
        this.deck = new Card[52];
        int i = 0;
        this.deck[i++] = new Card(Rank.Two, Suit.Spades);
        this.deck[i++] = new Card(Rank.Two, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Two, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Two, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Three, Suit.Spades);
        this.deck[i++] = new Card(Rank.Three, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Three, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Three, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Four, Suit.Spades);
        this.deck[i++] = new Card(Rank.Four, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Four, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Four, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Five, Suit.Spades);
        this.deck[i++] = new Card(Rank.Five, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Five, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Five, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Six, Suit.Spades);
        this.deck[i++] = new Card(Rank.Six, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Six, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Six, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Seven, Suit.Spades);
        this.deck[i++] = new Card(Rank.Seven, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Seven, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Seven, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Eight, Suit.Spades);
        this.deck[i++] = new Card(Rank.Eight, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Eight, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Eight, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Nine, Suit.Spades);
        this.deck[i++] = new Card(Rank.Nine, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Nine, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Nine, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Ten, Suit.Spades);
        this.deck[i++] = new Card(Rank.Ten, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Ten, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Ten, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Jack, Suit.Spades);
        this.deck[i++] = new Card(Rank.Jack, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Jack, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Jack, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Queen, Suit.Spades);
        this.deck[i++] = new Card(Rank.Queen, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Queen, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Queen, Suit.Hearts);
        this.deck[i++] = new Card(Rank.King, Suit.Spades);
        this.deck[i++] = new Card(Rank.King, Suit.Clubs);
        this.deck[i++] = new Card(Rank.King, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.King, Suit.Hearts);
        this.deck[i++] = new Card(Rank.Ace, Suit.Spades);
        this.deck[i++] = new Card(Rank.Ace, Suit.Clubs);
        this.deck[i++] = new Card(Rank.Ace, Suit.Diamonds);
        this.deck[i++] = new Card(Rank.Ace, Suit.Hearts);
        this.shuffle();
    }

    public Deck(Card[] deck, int count) {
        this.deck = deck;
        this.count = count;
    }

    public Card deal() {
        Random rnd = new Random(System.nanoTime());
        int number = rnd.nextInt(this.deck.length - this.count);
        Card temp = this.deck[this.deck.length - 1 - this.count];
        this.deck[this.deck.length - 1 - this.count] = this.deck[number];
        this.deck[number] = temp;
        return this.deck[this.deck.length - 1 - this.count++];
    }

    public boolean popCard(Card card) {
        boolean exist = false;
        for (int i = 0; i < this.deck.length - this.count; ++i) {
            Card entry = this.deck[i];
            if (!entry.compareTo(card)) continue;
            exist = true;
            Card temp = this.deck[this.deck.length - 1 - this.count];
            this.deck[this.deck.length - 1 - this.count] = this.deck[i];
            this.deck[i] = temp;
            ++this.count;
            break;
        }
        return exist;
    }

    public Card popCard(Rank rank, Suit suit) {
        for (int i = 0; i < this.deck.length - this.count; ++i) {
            Card entry = this.deck[i];
            if (entry.getRank() != rank || entry.getSuit() != suit) continue;
            Card temp = this.deck[this.deck.length - 1 - this.count];
            this.deck[this.deck.length - 1 - this.count] = this.deck[i];
            this.deck[i] = temp;
            ++this.count;
            return entry;
        }
        return null;
    }

    public void shuffle() {
        this.count = 0;
    }

    public boolean isEmpty() {
        return this.count == this.deck.length;
    }

    public Hand getHand(int number) {
        ArrayList<Card> cardarr = new ArrayList<Card>();
        for (int i = 0; i < number; ++i) {
            Card card = this.deal();
            cardarr.add(card);
        }
        return new Hand(cardarr);
    }

    public static void main(String[] args) {
        Deck deck = new Deck();
        for (int i = 0; i < deck.deck.length; ++i) {
            System.out.println(deck.deck[i].toString() + " - " + deck.deck[i].getCode());
        }
    }

    public int getSize() {
        return 52 - this.count;
    }

    public Card[] getDeck() {
        return this.deck;
    }

    public void setDeck(Card[] deck) {
        this.deck = deck;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Card> getHand() {
        ArrayList<Card> cardarr = new ArrayList<Card>();
        for (int i = 0; i < this.deck.length - this.count; ++i) {
            Card entry = this.deck[i];
            cardarr.add(entry);
        }
        return cardarr;
    }
}

