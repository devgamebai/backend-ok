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

public class PrizeRate {
    public int[] players;
    public int prizeCount;
    public double[] rate;

    public void init(JSONObject json) throws JSONException {
        int i;
        JSONArray arr = json.getJSONArray("players");
        this.players = new int[arr.length()];
        for (i = 0; i < arr.length(); ++i) {
            this.players[i] = arr.getInt(i);
        }
        this.prizeCount = json.getInt("prizeCount");
        arr = json.getJSONArray("rate");
        this.rate = new double[arr.length()];
        for (i = 0; i < arr.length(); ++i) {
            this.rate[i] = arr.getDouble(i);
        }
    }
}

