/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 */
package com.vinplay.usercore.service.model;

import com.google.gson.Gson;
import com.vinplay.usercore.service.model.CardGameServiceData;

public class GetCardGameDataInfo {
    private static Gson gson = new Gson();

    public static String getCardGameDataInfo(String gameID, String roomID, String matchID) {
        return gson.toJson(new CardGameServiceData(gameID, roomID, matchID));
    }
}

