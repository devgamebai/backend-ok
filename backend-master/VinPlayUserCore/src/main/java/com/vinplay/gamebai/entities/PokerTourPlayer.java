/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class PokerTourPlayer {
    public int tourId;
    public String nickname;
    public long currentChip;
    public long lastChip;
    public int ticketCount;
    public int outTourCount;
    public long outTourTimestamp;
    public int rank;
    public int mark;
    public int prize;

    public PokerTourPlayer(int tourId, String nickname, long currentChip, long lastChip, int ticketCount, int outTourCount, long outTourTimestamp, int rank, int mark, int prize) {
        this.tourId = tourId;
        this.nickname = nickname;
        this.currentChip = currentChip;
        this.lastChip = lastChip;
        this.ticketCount = ticketCount;
        this.outTourCount = outTourCount;
        this.outTourTimestamp = outTourTimestamp;
        this.rank = rank;
        this.mark = mark;
        this.prize = prize;
    }

    public PokerTourPlayer() {
    }
}

