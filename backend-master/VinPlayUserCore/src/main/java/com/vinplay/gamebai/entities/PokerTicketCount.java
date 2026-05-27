/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class PokerTicketCount {
    private int ticket;
    private int tourType;
    private int quantity;

    public PokerTicketCount(int ticket, int tourType, int quantity) {
        this.ticket = ticket;
        this.tourType = tourType;
        this.quantity = quantity;
    }

    public PokerTicketCount() {
    }

    public int getTicket() {
        return this.ticket;
    }

    public void setTicket(int ticket) {
        this.ticket = ticket;
    }

    public int getTourType() {
        return this.tourType;
    }

    public void setTourType(int tourType) {
        this.tourType = tourType;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

