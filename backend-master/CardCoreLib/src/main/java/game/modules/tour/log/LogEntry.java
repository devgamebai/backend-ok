/*
 * Decompiled with CFR 0_116.
 */
package game.modules.tour.log;

import game.modules.tour.control.TourUserInfo;
import game.utils.GameUtils;

public class LogEntry {
    public int tourId;
    public ACTION action;
    public String error = null;
    public String user = "";
    public long chip = 0;
    public long time = 0;
    public int timeBuyTicket = 0;
    public int timeOutTour = 0;
    public int fee = 0;
    public int firstRoom = 0;
    public int secondRoom = 0;
    public int rank = 0;
    public int prize = 0;

    public LogEntry(ACTION action) {
        this.action = action;
    }

    public void initTourInfo(TourUserInfo info) {
        this.chip = info.chip;
        this.timeBuyTicket = info.timeBuyTicket;
        this.rank = info.rank;
        this.prize = info.prize;
        this.user = info.nickName;
        this.timeOutTour = info.timeOutTour;
        this.time = info.outTourTimeStamp;
    }

    public String toString() {
        return GameUtils.toJsonString(this);
    }

    public static enum ACTION {
        REGISTER,
        CHARGE_FEE,
        FREE_TICKET,
        COMBINE_ROOM,
        RECONNECT_TOUR,
        OUT_TOUR,
        GIVE_PRIZE,
        REFUND,
        JOIN_ROOM;
        

        private ACTION() {
        }
    }

}

