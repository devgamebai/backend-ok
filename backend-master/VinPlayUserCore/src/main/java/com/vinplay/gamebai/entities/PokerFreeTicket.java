/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import com.vinplay.gamebai.entities.PokerTourFreeCreateType;
import com.vinplay.gamebai.entities.PokerTourType;
import java.util.Calendar;

public class PokerFreeTicket {
    public int id;
    public String nickname;
    public int ticket;
    public PokerTourType tourType;
    public boolean used;
    public Calendar createTime;
    public Calendar availableTime;
    public Calendar limitTime;
    public PokerTourFreeCreateType createType;
    public Calendar useTime;
    public String addInfo;
    public boolean isBot;
    public int tourId;

    public PokerFreeTicket(int id, String nickname, int ticket, PokerTourType tourType, boolean used, Calendar createTime, Calendar availableTime, Calendar limitTime, PokerTourFreeCreateType createType, Calendar useTime, String addInfo, boolean isBot, int tourId) {
        this.id = id;
        this.nickname = nickname;
        this.ticket = ticket;
        this.tourType = tourType;
        this.used = used;
        this.createTime = createTime;
        this.availableTime = availableTime;
        this.limitTime = limitTime;
        this.createType = createType;
        this.useTime = useTime;
        this.addInfo = addInfo;
        this.isBot = isBot;
        this.tourId = tourId;
    }

    public PokerFreeTicket() {
    }
}

