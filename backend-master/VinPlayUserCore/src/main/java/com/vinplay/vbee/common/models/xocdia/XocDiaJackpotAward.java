/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models.xocdia;

public class XocDiaJackpotAward {
    private String userName;
    private long userId;
    private long award;
    private String matchId;

    public XocDiaJackpotAward(String userName, long userId, long award, String matchId) {
        this.userName = userName;
        this.userId = userId;
        this.award = award;
        this.matchId = matchId;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAward() {
        return this.award;
    }

    public void setAward(long award) {
        this.award = award;
    }

    public String getMatchId() {
        return this.matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
}

