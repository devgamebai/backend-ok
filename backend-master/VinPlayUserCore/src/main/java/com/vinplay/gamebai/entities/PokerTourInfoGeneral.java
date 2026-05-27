/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class PokerTourInfoGeneral {
    public int tourId;
    public String startTime;
    public String startTimeSchedule;
    public int ticket;
    public long fund;
    public int tourState;
    public int tourType;
    public int userCount;
    public int ticketCount;

    public PokerTourInfoGeneral(int tourId, String startTime, String startTimeSchedule, int ticket, long fund, int tourState, int tourType, int userCount, int ticketCount) {
        this.tourId = tourId;
        this.startTime = startTime;
        this.startTimeSchedule = startTimeSchedule;
        this.ticket = ticket;
        this.fund = fund;
        this.tourState = tourState;
        this.tourType = tourType;
        this.userCount = userCount;
        this.ticketCount = ticketCount;
    }

    public PokerTourInfoGeneral() {
    }
}

