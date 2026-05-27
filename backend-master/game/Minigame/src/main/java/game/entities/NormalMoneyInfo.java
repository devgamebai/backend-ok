/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.entities;

import bitzero.server.entities.User;
import game.entities.UserScore;
import game.utils.DataUtils;
import game.utils.GameUtils;
import java.util.Calendar;
import org.json.JSONException;
import org.json.JSONObject;

public class NormalMoneyInfo {
    public static final String NORMAL_DATA_INFO = "NORMAL_DATA_INFO";
    public String nickName;
    public int winCount;
    public int lostCount;
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
    public int exp;
    private static final boolean dependOnGame = true;

    public static NormalMoneyInfo copyFromDB(String userId) {
        StringBuilder key = new StringBuilder(NormalMoneyInfo.class.getName());
        key.append(userId);
        key.append(GameUtils.gameName);
        NormalMoneyInfo info = (NormalMoneyInfo)DataUtils.copyDataFromDB(key.toString(), NormalMoneyInfo.class);
        return info;
    }

    public void save() {
        StringBuilder key = new StringBuilder(this.getClass().getName());
        key.append(this.nickName);
        key.append(GameUtils.gameName);
        DataUtils.saveToDB(key.toString(), this, this.getClass());
    }

    public static NormalMoneyInfo getInfo(User user) {
        NormalMoneyInfo info = (NormalMoneyInfo)user.getProperty(NORMAL_DATA_INFO);
        if (info == null) {
            info = NormalMoneyInfo.copyFromDB(user.getName());
            user.setProperty(NORMAL_DATA_INFO, info);
        }
        return info;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("u", this.nickName);
        json.put("m", this.moneyWinToday);
        return json;
    }

    private void updateValueByTime() {
        int today = Calendar.getInstance().get(6);
        int thisWeek = Calendar.getInstance().get(3);
        int thisMonth = Calendar.getInstance().get(2);
        int thisYear = Calendar.getInstance().get(1);
        if (today != this.lastDay) {
            this.lastDay = today;
            this.moneyWinToday = 0L;
            this.moneyLostToday = 0L;
        }
        if (thisWeek != this.lastWeek) {
            this.lastWeek = thisWeek;
            this.moneyWinThisWeek = 0L;
            this.moneyLostThisWeek = 0L;
        }
        if (thisMonth != this.lastMonth) {
            this.lastMonth = thisMonth;
            this.moneyWinThisMonth = 0L;
            this.moneyLostThisMonth = 0L;
        }
        if (thisYear != this.lastYear) {
            this.lastYear = thisYear;
            this.moneyWinThisYear = 0L;
            this.moneyLostThisYear = 0L;
        }
    }

    public void addScore(UserScore score) {
        this.updateValueByTime();
        if (score.money >= 0L) {
            this.moneyWinToday += score.money;
            this.moneyWinThisWeek += score.money;
            this.moneyWinThisMonth += score.money;
            this.moneyWinThisYear += score.money;
        } else {
            this.moneyLostToday -= score.money;
            this.moneyLostThisWeek -= score.money;
            this.moneyLostThisMonth -= score.money;
            this.moneyLostThisYear -= score.money;
        }
        this.save();
    }
}

