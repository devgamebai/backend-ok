/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dailyQuest.model;

import com.vinplay.dailyQuest.GiftTypeData;

public class DailyQuestData {
    public int gameId;
    public int valueDone;
    public int valueType;
    public int gift;
    public int giftType;
    public int piority;
    public GiftTypeData giftTypeData;
    public String des;

    public DailyQuestData(int gameId, int valueDone, int valueType, int gift, int giftType, int gameID, int moneyBet, int piority, String des) {
        this.gameId = gameId;
        this.valueDone = valueDone;
        this.valueType = valueType;
        this.gift = gift;
        this.giftType = giftType;
        this.giftTypeData = new GiftTypeData(gameID, moneyBet);
        this.piority = piority;
        this.des = des;
    }
}

