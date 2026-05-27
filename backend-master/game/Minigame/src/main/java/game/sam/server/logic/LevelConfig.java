/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.sam.server.logic;

import bitzero.util.common.business.CommonHandle;
import org.json.JSONArray;
import org.json.JSONObject;

public class LevelConfig {
    public static JSONObject addLevelScoreJson;
    public static int maxLevel;
    public static JSONObject leveScoreConfig;
    public static JSONArray levelBonus;

    public static int getLevelScore(int channel, int winType) {
        try {
            String channelId = String.valueOf(channel);
            JSONArray arr = addLevelScoreJson.getJSONArray(channelId);
            return arr.getInt(winType);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            return 0;
        }
    }
}

