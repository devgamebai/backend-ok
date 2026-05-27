/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.json.JSONObject
 */
package game.modules.tour.control;

import game.utils.GameUtils;
import org.json.JSONObject;

public class TourUserInfo
implements Comparable {
    public static final String TOUR_USER_INFO = "TOUR_USER_INFO";
    public static final int MAX_TIME_REBUY = 3;
    public int tourId;
    public String nickName;
    public int ticket;
    public int timeBuyTicket = 0;
    public int timeOutTour = 0;
    public long chip;
    public int score = 0;
    public int rank = 2000;
    public int prize = 0;
    public long outTourTimeStamp = 1;
    public long lastChip = 0;
    public volatile int roomId = 0;
    public long outRoomTimeStamp = 0;
    public int fixRank = 0;

    public String toString() {
        return GameUtils.toJsonString(this);
    }

    public int compareTo(Object o) {
        if (o instanceof TourUserInfo) {
            TourUserInfo info = (TourUserInfo)o;
            if (this.outTourTimeStamp > info.outTourTimeStamp) {
                return -1;
            }
            if (this.outTourTimeStamp < info.outTourTimeStamp) {
                return 1;
            }
            if (this.chip > info.chip) {
                return -1;
            }
            if (this.chip < info.chip) {
                return 1;
            }
            if (this.lastChip > info.lastChip) {
                return -1;
            }
            if (this.lastChip < info.lastChip) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    public JSONObject toRankJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("n", (Object)this.nickName);
            json.put("r", this.rank);
            json.put("p", this.prize);
            json.put("c", this.chip);
            json.put("l", this.lastChip);
        }
        catch (Exception e) {
            return null;
        }
        return json;
    }
}

