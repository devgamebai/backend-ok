/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service.model;

public class CardGameServiceData {
    public int type = 3;
    public String gameID;
    public String roomID;
    public String matchID;

    public CardGameServiceData(String gameID, String roomID, String matchID) {
        this.gameID = gameID;
        this.roomID = roomID;
        this.matchID = matchID;
    }
}

