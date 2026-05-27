/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public class PokerTourUserDetail {
    public int rank;
    public String nickname;
    public long currentChip;
    public long lastChip;
    public int mark;
    public int prize;
    public String outTime;

    public PokerTourUserDetail(int rank, String nickname, long currentChip, long lastChip, int mark, int prize, String outTime) {
        this.rank = rank;
        this.nickname = nickname;
        this.currentChip = currentChip;
        this.lastChip = lastChip;
        this.mark = mark;
        this.prize = prize;
        this.outTime = outTime;
    }

    public PokerTourUserDetail() {
    }
}

