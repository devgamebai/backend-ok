/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class TopUserPlayGame {
    private String nickname;
    private long moneyWin;
    private String moneyType;
    private String date;

    public TopUserPlayGame(String nickname, long moneyWin, String moneyType, String date) {
        this.nickname = nickname;
        this.moneyWin = moneyWin;
        this.moneyType = moneyType;
        this.date = date;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getMoneyWin() {
        return this.moneyWin;
    }

    public void setMoneyWin(long moneyWin) {
        this.moneyWin = moneyWin;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

