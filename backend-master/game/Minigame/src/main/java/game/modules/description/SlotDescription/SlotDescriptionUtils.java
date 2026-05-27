/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 */
package game.modules.description.SlotDescription;

import com.google.gson.Gson;
import game.modules.description.SlotDescription.BetDescription;
import game.modules.description.SlotDescription.MultiJackpotDescription;
import game.modules.description.SlotDescription.PayDescription;

public class SlotDescriptionUtils {
    public static Gson gson = new Gson();

    public static String getBetDescription(String gameID) {
        return gson.toJson(new BetDescription(gameID));
    }

    public static String getMultiJackpotDescription(String gameID) {
        return gson.toJson(new MultiJackpotDescription(gameID));
    }

    public static String getPayDescription(String gameID, long totalbet, long totalPrizes, short result) {
        return gson.toJson(new PayDescription(gameID, totalbet, totalPrizes, result));
    }
}

