/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import com.vinplay.gamebai.entities.PokerTourState;
import com.vinplay.gamebai.entities.PokerTourType;
import java.util.Calendar;

public class PokerTourInfo {
    public int tourId;
    public Calendar startTimeSchedule;
    public Calendar endRegisterSchedule;
    public PokerTourState tourState;
    public PokerTourType tourType;
    public Calendar startTime;
    public Calendar endRegisterTime;
    public Calendar cancelTime;
    public Calendar endTime;
    public int level;
    public int ticket;
    public int countTimeUpLevel;
    public long fund;

    public PokerTourInfo(int tourId, Calendar startTimeSchedule, Calendar endRegisterSchedule, PokerTourState tourState, PokerTourType tourType, Calendar startTime, Calendar endRegisterTime, Calendar cancelTime, Calendar endTime, int level, int ticket, int countTimeUpLevel, long fund) {
        this.tourId = tourId;
        this.startTimeSchedule = startTimeSchedule;
        this.endRegisterSchedule = endRegisterSchedule;
        this.tourState = tourState;
        this.tourType = tourType;
        this.startTime = startTime;
        this.endRegisterTime = endRegisterTime;
        this.cancelTime = cancelTime;
        this.endTime = endTime;
        this.level = level;
        this.ticket = ticket;
        this.countTimeUpLevel = countTimeUpLevel;
        this.fund = fund;
    }

    public PokerTourInfo() {
    }
}

