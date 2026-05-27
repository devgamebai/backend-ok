/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.tour.control;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TourSetting {
    public int id;
    public String name;
    public int[] levels;
    public int timeLevelUp;
    public int chip;
    public int comission;
    public int pool;
    public int attach;
    public int outEatenRate;
    public int timeBreak;
    public int addon;
    public int rebuy;
    public int maxLevelRebuy;
    public int maxPlayerPerRoom;

    public void init(JSONObject json) throws JSONException {
        JSONArray arr = json.getJSONArray("levels");
        this.levels = new int[arr.length()];
        for (int i = 0; i < arr.length(); ++i) {
            this.levels[i] = arr.getInt(i);
        }
        this.timeLevelUp = json.getInt("timeLevelUp");
        this.chip = json.getInt("chip");
        this.comission = json.getInt("comission");
        this.pool = json.getInt("pool");
        this.attach = json.getInt("attach");
        this.outEatenRate = json.getInt("outEatenRate");
        this.timeBreak = json.getInt("timeBreak");
        this.addon = json.getInt("addon");
        this.rebuy = json.getInt("rebuy");
        this.maxLevelRebuy = json.getInt("maxLevelRebuy");
        this.maxPlayerPerRoom = json.getInt("maxPlayerPerRoom");
    }
}

