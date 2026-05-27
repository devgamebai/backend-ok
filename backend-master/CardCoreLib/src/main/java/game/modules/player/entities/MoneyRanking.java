/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.player.entities;

import game.entities.UserScore;
import game.utils.DataUtils;
import game.utils.GameUtils;
import java.util.Calendar;
import org.json.JSONException;
import org.json.JSONObject;

public class MoneyRanking {
    public String nickName = "";
    public String avatar;
    public int winCount;
    public int winCountToday;
    public int winCountThisWeek;
    public int winCountThisMonth;
    public int winCountThisYear;
    public int lostCount;
    public int lostCountToday;
    public int lostCountThisWeek;
    public int lostCountThisMonth;
    public int lostCountThisYear;
    public long moneyWin;
    public long moneyWinToday;
    public long moneyWinThisWeek;
    public long moneyWinThisMonth;
    public long moneyWinThisYear;
    public int lastDay;
    public int lastWeek;
    public int lastMonth;
    public int lastYear;
    public long moneyLost;
    public long moneyLostToday;
    public long moneyLostThisWeek;
    public long moneyLostThisMonth;
    public long moneyLostThisYear;

    public void save() {
        StringBuilder key = new StringBuilder(this.getClass().getSimpleName());
        key.append(this.nickName);
        key.append(GameUtils.gameName);
        DataUtils.saveToDB(key.toString(), this, this.getClass());
    }

    public JSONObject toJSONObjectRanking() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("n", (Object)this.nickName);
        json.put("m", this.moneyWinThisMonth);
        json.put("mw", this.moneyWinThisWeek);
        json.put("c", this.winCountThisMonth);
        json.put("cw", this.winCountThisWeek);
        json.put("a", (Object)this.avatar);
        json.put("md", this.moneyWinToday);
        json.put("cd", this.winCountToday);
        return json;
    }

    private void updateValueByTime() {
        int today = Calendar.getInstance().get(6);
        int thisWeek = Calendar.getInstance().get(3);
        int thisMonth = Calendar.getInstance().get(2);
        int thisYear = Calendar.getInstance().get(1);
        if (today != this.lastDay) {
            this.lastDay = today;
            this.moneyWinToday = 0;
            this.moneyLostToday = 0;
            this.winCountToday = 0;
            this.lostCountToday = 0;
        }
        if (thisWeek != this.lastWeek) {
            this.lastWeek = thisWeek;
            this.moneyWinThisWeek = 0;
            this.moneyLostThisWeek = 0;
            this.winCountThisWeek = 0;
            this.moneyLostThisWeek = 0;
        }
        if (thisMonth != this.lastMonth) {
            this.lastMonth = thisMonth;
            this.moneyWinThisMonth = 0;
            this.moneyLostThisMonth = 0;
            this.winCountThisMonth = 0;
            this.lostCountThisMonth = 0;
        }
        if (thisYear != this.lastYear) {
            this.lastYear = thisYear;
            this.moneyWinThisYear = 0;
            this.winCountThisYear = 0;
            this.moneyLostThisYear = 0;
            this.lostCountThisYear = 0;
        }
    }

    public void addScore(UserScore score) {
        this.updateValueByTime();
        this.winCount += score.winCount;
        this.winCountToday += score.winCount;
        this.winCountThisWeek += score.winCount;
        this.winCountThisMonth += score.winCount;
        this.winCountThisYear += score.winCount;
        this.lostCount += score.lostCount;
        this.lostCountToday += score.lostCount;
        this.lostCountThisWeek += score.lostCount;
        this.lostCountThisMonth += score.lostCount;
        this.lostCountThisYear += score.lostCount;
        this.lostCount += score.lostCount;
        if (score.money >= 0) {
            this.moneyWin += score.money;
            this.moneyWinToday += score.money;
            this.moneyWinThisWeek += score.money;
            this.moneyWinThisMonth += score.money;
            this.moneyWinThisYear += score.money;
        } else {
            this.moneyLost -= score.money;
            this.moneyLostToday -= score.money;
            this.moneyLostThisWeek -= score.money;
            this.moneyLostThisMonth -= score.money;
            this.moneyLostThisYear -= score.money;
        }
        this.save();
    }

    public int hashCode() {
        return this.nickName.hashCode();
    }

    public boolean equals(Object o) {
        try {
            MoneyRanking m = (MoneyRanking)o;
            return this.nickName.equalsIgnoreCase(m.nickName);
        }
        catch (Exception e) {
            return false;
        }
    }

    public void clear() {
        this.winCount = 0;
        this.winCountToday = 0;
        this.winCountThisWeek = 0;
        this.winCountThisMonth = 0;
        this.winCountThisYear = 0;
        this.lostCount = 0;
        this.lostCountToday = 0;
        this.lostCountThisWeek = 0;
        this.lostCountThisMonth = 0;
        this.lostCountThisYear = 0;
        this.moneyWin = 0;
        this.moneyWinToday = 0;
        this.moneyWinThisWeek = 0;
        this.moneyWinThisMonth = 0;
        this.moneyWinThisYear = 0;
        this.lastDay = 0;
        this.lastWeek = 0;
        this.lastMonth = 0;
        this.lastYear = 0;
        this.moneyLost = 0;
        this.moneyLostToday = 0;
        this.moneyLostThisWeek = 0;
        this.moneyLostThisMonth = 0;
        this.moneyLostThisYear = 0;
    }
}

