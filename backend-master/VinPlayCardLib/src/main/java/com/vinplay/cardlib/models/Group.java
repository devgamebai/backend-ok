/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.cardlib.models;

import com.vinplay.cardlib.models.Card;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Group {
    private List<Card> listCard = new LinkedList<Card>();
    private int number = 0;

    public void addCard(Card card) {
        this.listCard.add(card);
        ++this.number;
    }

    public void removeCard(Card card) {
        this.listCard.remove(card);
        --this.number;
    }

    public void removeDouble() {
        if (this.listCard.size() >= 2) {
            int number = this.listCard.size() - 1;
            this.listCard.remove(number);
            this.listCard.remove(--number);
        }
    }

    public void incNumber() {
        ++this.number;
    }

    public void descNumber() {
        --this.number;
    }

    public List<Card> getListCard() {
        return this.listCard;
    }

    public void setListCard(ArrayList<Card> listCard) {
        this.listCard = listCard;
    }

    public int getNumber() {
        return this.number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}

