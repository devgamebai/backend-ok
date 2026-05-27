/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class TopCaoThu {
    private String nickname;
    private long moneyWin;

    public TopCaoThu(String nickname, long moneyWin) {
        this.nickname = nickname;
        this.moneyWin = moneyWin;
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
}

