/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.datacontroller.business.DataController
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.modules.player.entities;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataController;
import game.modules.player.entities.MoneyRanking;
import game.modules.player.entities.NormalMoneyInfo;
import game.modules.player.entities.RankingTable;
import java.util.Calendar;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class RankingTableNormal
extends RankingTable {
    private static RankingTableNormal ins = null;
    private String dbKey = null;

    public static RankingTableNormal getIntansce() {
        if (ins == null) {
            ins = new RankingTableNormal();
        }
        return ins;
    }

    @Override
    public void loadFromDB() {
        try {
            JSONObject json;
            NormalMoneyInfo info;
            int i;
            String s = (String)DataController.getController().get(this.getDBKey());
            if (s == null) {
                this.topDayStr = "[]";
                this.topDayWinStr = "[]";
                this.topWeekStr = "[]";
                this.topAllStr = "[]";
                this.topWeekWinStr = "[]";
                this.topAllWinStr = "[]";
            } else {
                JSONArray array = new JSONArray(s);
                if (array.length() != 6) {
                    this.topDayStr = "[]";
                    this.topDayWinStr = "[]";
                    this.topWeekStr = "[]";
                    this.topAllStr = "[]";
                    this.topWeekWinStr = "[]";
                    this.topAllWinStr = "[]";
                    return;
                }
                this.topDayStr = array.getJSONArray(0).toString();
                this.topWeekStr = array.getJSONArray(1).toString();
                this.topAllStr = array.getJSONArray(2).toString();
                this.topDayWinStr = array.getJSONArray(3).toString();
                this.topWeekWinStr = array.getJSONArray(4).toString();
                this.topAllWinStr = array.getJSONArray(5).toString();
            }
            Calendar cal = Calendar.getInstance();
            int today = cal.get(6);
            int thisWeek = cal.get(3);
            JSONArray arr = new JSONArray(this.topDayStr);
            for (i = 0; i < arr.length(); ++i) {
                json = arr.getJSONObject(i);
                info = NormalMoneyInfo.copyFromDB(json.getString("n"));
                if (info == null || info.lastDay != today) continue;
                this.topDay.add(info);
            }
            arr = new JSONArray(this.topWeekStr);
            for (i = 0; i < arr.length(); ++i) {
                json = arr.getJSONObject(i);
                info = NormalMoneyInfo.copyFromDB(json.getString("n"));
                if (info == null || info.lastWeek != thisWeek) continue;
                this.topWeek.add(info);
            }
            arr = new JSONArray(this.topAllStr);
            for (i = 0; i < arr.length(); ++i) {
                json = arr.getJSONObject(i);
                info = NormalMoneyInfo.copyFromDB(json.getString("n"));
                if (info == null) continue;
                this.topAll.add(info);
            }
            arr = new JSONArray(this.topDayWinStr);
            for (i = 0; i < arr.length(); ++i) {
                json = arr.getJSONObject(i);
                info = NormalMoneyInfo.copyFromDB(json.getString("n"));
                if (info == null || info.lastDay != today) continue;
                this.topWeekWin.add(info);
            }
            arr = new JSONArray(this.topWeekWinStr);
            for (i = 0; i < arr.length(); ++i) {
                json = arr.getJSONObject(i);
                info = NormalMoneyInfo.copyFromDB(json.getString("n"));
                if (info == null || info.lastWeek != thisWeek) continue;
                this.topWeekWin.add(info);
            }
            arr = new JSONArray(this.topAllWinStr);
            for (i = 0; i < arr.length(); ++i) {
                json = arr.getJSONObject(i);
                info = NormalMoneyInfo.copyFromDB(json.getString("n"));
                if (info == null) continue;
                this.topAllWin.add(info);
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            this.topDayStr = "[]";
            this.topDayWinStr = "[]";
            this.topWeekStr = "[]";
            this.topAllStr = "[]";
            this.topWeekWinStr = "[]";
            this.topAllWinStr = "[]";
        }
    }
}

