/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import com.vinplay.gamebai.entities.PokerTourUserDetail;
import java.util.List;

public class PokerTourInfoDetail {
    public int tourId;
    public int ticket;
    public long fund;
    public int tourState;
    public int userCount;
    public int ticketCount;
    public List<PokerTourUserDetail> playerList;

    public PokerTourInfoDetail(int tourId, int ticket, long fund, int tourState, int userCount, int ticketCount, List<PokerTourUserDetail> playerList) {
        this.tourId = tourId;
        this.ticket = ticket;
        this.fund = fund;
        this.tourState = tourState;
        this.userCount = userCount;
        this.ticketCount = ticketCount;
        this.playerList = playerList;
    }

    public PokerTourInfoDetail() {
    }
}

