/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models.xocdia;

public class XocDiaAward {
    private String userName;
    private long userId;
    private long award;

    public XocDiaAward(String userName, long userId, long award) {
        this.userName = userName;
        this.userId = userId;
        this.award = award;
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
}

