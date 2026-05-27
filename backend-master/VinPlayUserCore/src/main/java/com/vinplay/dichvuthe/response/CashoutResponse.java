/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

public class CashoutResponse {
    private int code;
    private long currentMoney;

    public CashoutResponse(int code, long currentMoney) {
        this.code = code;
        this.currentMoney = currentMoney;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }
}

