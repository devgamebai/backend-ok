/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.datacontroller.business.DataController
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.player.entities;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataController;
import game.modules.player.entities.MoneyRanking;
import game.utils.GameUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class RankingTable {
    public static final int RANK_USER_COUNT = 10;
    public static final int SORTED_USER_COUNT = 50;
    public final long UPDATE_INTERVAL = 10000;
    private Comparator<MoneyRanking> dayComparator;
    private Comparator<MoneyRanking> weekComparator;
    private Comparator<MoneyRanking> allComparator;
    private Comparator<MoneyRanking> dayComparatorWin;
    private Comparator<MoneyRanking> allComparatorWin;
    private Comparator<MoneyRanking> weekComparatorWin;
    public Set<MoneyRanking> topDay;
    public Set<MoneyRanking> topWeek;
    public Set<MoneyRanking> topAll;
    public Set<MoneyRanking> topWeekWin;
    public Set<MoneyRanking> topAllWin;
    public Set<MoneyRanking> topDayWin;
    public String topDayStr;
    public String topDayWinStr;
    public String topWeekStr;
    public String topAllStr;
    public String topWeekWinStr;
    public String topAllWinStr;
    public volatile long lastimeUpdate;
    public volatile boolean isUpdate;
    public int lastDay;
    public int lastWeek;
    private String dbKey;

    public RankingTable() {
        this.dayComparator = new Comparator<MoneyRanking>(){

            @Override
            public int compare(MoneyRanking o1, MoneyRanking o2) {
                if (o1.moneyWinToday > o2.moneyWinToday) {
                    return -1;
                }
                if (o1.moneyWinToday < o2.moneyWinToday) {
                    return 1;
                }
                return 0;
            }
        };
        this.weekComparator = new Comparator<MoneyRanking>(){

            @Override
            public int compare(MoneyRanking o1, MoneyRanking o2) {
                if (o1.moneyWinThisWeek > o2.moneyWinThisWeek) {
                    return -1;
                }
                if (o1.moneyWinThisWeek < o2.moneyWinThisWeek) {
                    return 1;
                }
                return 0;
            }
        };
        this.allComparator = new Comparator<MoneyRanking>(){

            @Override
            public int compare(MoneyRanking o1, MoneyRanking o2) {
                if (o1.moneyWin > o2.moneyWin) {
                    return -1;
                }
                if (o1.moneyWin < o2.moneyWin) {
                    return 1;
                }
                return 0;
            }
        };
        this.dayComparatorWin = new Comparator<MoneyRanking>(){

            @Override
            public int compare(MoneyRanking o1, MoneyRanking o2) {
                if (o1.winCountToday > o2.winCountToday) {
                    return -1;
                }
                if (o1.winCountToday < o2.winCountToday) {
                    return 1;
                }
                return 0;
            }
        };
        this.allComparatorWin = new Comparator<MoneyRanking>(){

            @Override
            public int compare(MoneyRanking o1, MoneyRanking o2) {
                if (o1.winCount > o2.winCount) {
                    return -1;
                }
                if (o1.winCount < o2.winCount) {
                    return 1;
                }
                return 0;
            }
        };
        this.weekComparatorWin = new Comparator<MoneyRanking>(){

            @Override
            public int compare(MoneyRanking o1, MoneyRanking o2) {
                if (o1.winCountThisWeek > o2.winCountThisWeek) {
                    return -1;
                }
                if (o1.winCountThisWeek < o2.winCountThisWeek) {
                    return 1;
                }
                return 0;
            }
        };
        this.topDay = null;
        this.topWeek = null;
        this.topAll = null;
        this.topWeekWin = null;
        this.topAllWin = null;
        this.topDayWin = null;
        this.topDayStr = "[]";
        this.topDayWinStr = "[]";
        this.topWeekStr = "[]";
        this.topAllStr = "[]";
        this.topWeekWinStr = "[]";
        this.topAllWinStr = "[]";
        this.lastimeUpdate = 0;
        this.isUpdate = false;
        this.lastDay = -1;
        this.lastWeek = -1;
        this.dbKey = null;
        this.topDay = new HashSet<MoneyRanking>();
        this.topAll = new HashSet<MoneyRanking>();
        this.topWeek = new HashSet<MoneyRanking>();
        this.topAllWin = new HashSet<MoneyRanking>();
        this.topWeekWin = new HashSet<MoneyRanking>();
        this.topDayWin = new HashSet<MoneyRanking>();
        this.loadFromDB();
    }

    public synchronized void addTopInfo(MoneyRanking info, boolean isBot) {
        if (this.topDay.contains(info)) {
            this.topDay.remove(info);
        }
        if (this.topAll.contains(info)) {
            this.topAll.remove(info);
        }
        if (this.topWeek.contains(info)) {
            this.topWeek.remove(info);
        }
        if (this.topDayWin.contains(info)) {
            this.topDayWin.remove(info);
        }
        if (this.topAllWin.contains(info)) {
            this.topAllWin.remove(info);
        }
        if (this.topWeekWin.contains(info)) {
            this.topWeekWin.remove(info);
        }
        //if (!isBot) {
            this.topDay.add(info);
            this.topAll.add(info);
            this.topWeek.add(info);
            this.topDayWin.add(info);
            this.topAllWin.add(info);
            this.topWeekWin.add(info);
            this.updateInfo();
        //}
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private synchronized void updateInfo() {
        if (this.isUpdate) {
            return;
        }
        this.isUpdate = true;
        try {
            long now = System.currentTimeMillis();
            long interval = now - this.lastimeUpdate;
            if (interval >= 10000) {
                this.lastimeUpdate = now;
                this.clearDayWeek();
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

    private void clearDayWeek() {
        MoneyRanking m;
        Iterator<MoneyRanking> iter;
        Calendar cal = Calendar.getInstance();
        int today = cal.get(6);
        int thisWeek = cal.get(3);
        if (this.lastDay != today) {
            this.lastDay = today;
            iter = this.topDay.iterator();
            while (iter.hasNext()) {
                m = iter.next();
                if (m.lastDay == today) continue;
                iter.remove();
            }
            iter = this.topDayWin.iterator();
            while (iter.hasNext()) {
                m = iter.next();
                if (m.lastDay == today) continue;
                iter.remove();
            }
        }
        if (this.lastWeek != thisWeek) {
            this.lastWeek = thisWeek;
            iter = this.topWeek.iterator();
            while (iter.hasNext()) {
                m = iter.next();
                if (m.lastWeek == thisWeek) continue;
                iter.remove();
            }
            iter = this.topWeekWin.iterator();
            while (iter.hasNext()) {
                m = iter.next();
                if (m.lastWeek == thisWeek) continue;
                iter.remove();
            }
        }
    }

    private synchronized void updateValue() {
        this.topDayStr = this.updateValue(this.topDay, this.dayComparator);
        this.topDayWinStr = this.updateValue(this.topDayWin, this.dayComparatorWin);
        this.topWeekStr = this.updateValue(this.topWeek, this.weekComparator);
        this.topAllStr = this.updateValue(this.topAll, this.allComparator);
        this.topWeekWinStr = this.updateValue(this.topWeekWin, this.weekComparatorWin);
        this.topAllWinStr = this.updateValue(this.topAllWin, this.allComparatorWin);
        this.saveDB();
    }

    private synchronized String updateValue(Set<MoneyRanking> set, Comparator<MoneyRanking> comparator) {
        JSONArray arr = new JSONArray();
        int count = 0;
        ArrayList<MoneyRanking> listInfo = new ArrayList<MoneyRanking>();
        listInfo.addAll(set);
        try {
            Collections.sort(listInfo, comparator);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            JSONArray listInfoArray = new JSONArray();
            for (int i = 0; i < listInfo.size(); ++i) {
                MoneyRanking info = (MoneyRanking)listInfo.get(i);
                try {
                    listInfoArray.put((Object)info.toJSONObjectRanking());
                    continue;
                }
                catch (JSONException f) {
                    CommonHandle.writeErrLog((Throwable)e);
                }
            }
            CommonHandle.writeErrLogDebug((Object[])new Object[]{"Error when update ranking table:", listInfoArray});
        }
        ArrayList<MoneyRanking> infoRemoves = new ArrayList<MoneyRanking>();
        for (MoneyRanking info : listInfo) {
            if (++count <= 10) {
                try {
                    arr.put((Object)info.toJSONObjectRanking());
                    continue;
                }
                catch (JSONException e) {
                    CommonHandle.writeErrLog((Throwable)e);
                    return "[]";
                }
            }
            if (count < 50) continue;
            infoRemoves.add(info);
        }
        for (MoneyRanking info2 : infoRemoves) {
            set.remove(info2);
        }
        String str = arr.toString();
        return str;
    }

    public String getTopDay() {
        return this.topDayStr;
    }

    public String getTopWeek() {
        return this.topWeekStr;
    }

    public String getTopAll() {
        return this.topAllStr;
    }

    public String getTopDayWin() {
        return this.topDayWinStr;
    }

    public String getTopWeekWin() {
        return this.topWeekWinStr;
    }

    public String getTopAllWin() {
        return this.topAllWinStr;
    }

    public void saveDB() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(this.topDayStr).append(",").append(this.topWeekStr).append(",").append(this.topAllStr).append(",").append(this.topDayWinStr).append(",").append(this.topWeekWinStr).append(",").append(this.topAllWinStr).append("]");
            DataController.getController().set(this.getDBKey(), (Object)sb.toString());
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public String getDBKey() {
        if (this.dbKey == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(GameUtils.gameName).append(this.getClass().getSimpleName());
            this.dbKey = sb.toString();
        }
        return this.dbKey;
    }

    public abstract void loadFromDB();

}

