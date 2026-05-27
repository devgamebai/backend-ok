/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 */
package game.modules.description.XocDiaDescription;

import com.google.gson.Gson;
import game.modules.description.XocDiaDescription.XocDiaBetDescription;
import game.modules.description.XocDiaDescription.XocDiaWinDescription;

public class XocDiaDescriptionUtils {
    public static Gson gson = new Gson();

    public static String getXocDiaBetDescription(String gameID, long referenceId) {
        return gson.toJson(new XocDiaBetDescription(gameID, referenceId));
    }

    public static String getXocDiaWinDescription(String gameID, long referenceId) {
        return gson.toJson(new XocDiaWinDescription(gameID, referenceId));
    }
}

