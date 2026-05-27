/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 */
package game.modules.description.CaoThapDescription;

import com.google.gson.Gson;
import game.modules.description.CaoThapDescription.CaoThapBetDescription;
import game.modules.description.CaoThapDescription.CaoThapJackpotDescription;
import game.modules.description.CaoThapDescription.CaoThapWinDescription;

public class CaoThapDescriptionUtils {
    public static Gson gson = new Gson();

    public static String getCaoThapBetDesciption(String gameID, long referenceId) {
        return gson.toJson(new CaoThapBetDescription(gameID, referenceId));
    }

    public static String getCaoThapJackPotDesciption(String gameID, long referenceId, short step) {
        return gson.toJson(new CaoThapJackpotDescription(gameID, referenceId, step));
    }

    public static String getCaoThapWinDesciption(String gameID, long referenceId, short step) {
        return gson.toJson(new CaoThapWinDescription(gameID, referenceId, step));
    }
}

