/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import com.vinplay.gamebai.entities.PokerFreeTicket;
import java.util.List;

public class PokerFreeTicketStatistic {
    private List<PokerFreeTicket> tickets;
    private int totalAmount;
    private int total;

    public PokerFreeTicketStatistic() {
    }

    public PokerFreeTicketStatistic(List<PokerFreeTicket> tickets, int totalAmount, int total) {
        this.tickets = tickets;
        this.totalAmount = totalAmount;
        this.total = total;
    }

    public List<PokerFreeTicket> getTickets() {
        return this.tickets;
    }

    public void setTickets(List<PokerFreeTicket> tickets) {
        this.tickets = tickets;
    }

    public int getTotalAmount() {
        return this.totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTotal() {
        return this.total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

