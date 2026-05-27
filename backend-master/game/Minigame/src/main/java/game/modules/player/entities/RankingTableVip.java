/*
 * Decompiled with CFR 0.144.
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
import game.entities.VipMoneyInfo;
import game.utils.GameUtils;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;

public class RankingTableVip {
    public static final int RANK_USER_COUNT = 10;
    public static final int SORTED_USER_COUNT = 100;
    public final long UPDATE_INTERVAL = 120000L;
    private Comparator moneyWinTodaySortting = new Comparator<VipMoneyInfo>(){

        @Override
        public int compare(VipMoneyInfo o1, VipMoneyInfo o2) {
            if (o1.moneyWinToday == o2.moneyWinToday) {
                return 0;
            }
            if (o1.moneyWinToday > o2.moneyWinToday) {
                return -1;
            }
            return 1;
        }
    };
    private static RankingTableVip ins = null;
    private String dbKey = null;
    public Set<VipMoneyInfo> topInfoList = new TreeSet<VipMoneyInfo>(this.moneyWinTodaySortting);
    private String topInfoWinTodayString = "[{\"m\":996,\"n\":\"vp_5\"}, \n{\"m\":902,\"n\":\"vp_3\"}, \n{\"m\":857,\"n\":\"vp_7\"}, \n{\"m\":838,\"n\":\"vp_8\"}, \n{\"m\":622,\"n\":\"vp_6\"}, \n{\"m\":589,\"n\":\"vp_1\"}, \n{\"m\":318,\"n\":\"vp_2\"}, \n{\"m\":302,\"n\":\"vp_9\"}, \n{\"m\":264,\"n\":\"vp_4\"}, \n{\"m\":100,\"n\":\"vp_0\"}]";
    public Object lock = new Object();
    public volatile long lastimeUpdate = 0L;
    public volatile boolean isUpdate = false;

    public static RankingTableVip getIntansce() {
        if (ins == null) {
            ins = new RankingTableVip();
        }
        return ins;
    }

    public RankingTableVip() {
        this.saveDB();
        this.loadFromDB();
    }

    public void addTopInfo(VipMoneyInfo info) {
        this.topInfoList.add(info);
        this.updateInfo();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateInfo() {
        if (this.isUpdate) {
            return;
        }
        this.isUpdate = true;
        try {
            long now = System.currentTimeMillis();
            long interval = now - this.lastimeUpdate;
            if (interval > 120000L) {
                this.lastimeUpdate = now;
                this.updateValue();
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        finally {
            this.isUpdate = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateValue() {
        Object object;
        JSONArray arr = new JSONArray();
        int count = 0;
        for (VipMoneyInfo info2 : this.topInfoList) {
            if (++count <= 10) {
                try {
                    arr.put(info2.toJSONObject());
                    continue;
                }
                catch (Exception e) {
                    CommonHandle.writeErrLog((Throwable)e);
                    return;
                }
            }
            if (count < 100) continue;
            this.topInfoList.remove(info2);
        }
        Object info2 = object = this.lock;
        synchronized (info2) {
            this.topInfoWinTodayString = arr.toString();
        }
    }

    public String getTopInfoWinToday() {
        return this.topInfoWinTodayString;
    }

    public void loadFromDB() {
        try {
            this.topInfoWinTodayString = (String)DataController.getController().get(this.getDBKey());
            if (this.topInfoWinTodayString == null) {
                JSONArray arr = new JSONArray(this.topInfoWinTodayString);
                for (int i = 0; i < arr.length(); ++i) {
                    JSONObject json = arr.getJSONObject(i);
                    VipMoneyInfo info = VipMoneyInfo.copyFromDB(json.getString("n"));
                    this.topInfoList.add(info);
                }
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            this.topInfoWinTodayString = "[]";
            this.topInfoList.clear();
        }
    }

    public void saveDB() {
        try {
            DataController.getController().set(this.getDBKey(), this.topInfoWinTodayString);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public String getDBKey() {
        if (this.dbKey == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(GameUtils.gameName).append("RankingTable");
            this.dbKey = sb.toString();
        }
        return this.dbKey;
    }

}

