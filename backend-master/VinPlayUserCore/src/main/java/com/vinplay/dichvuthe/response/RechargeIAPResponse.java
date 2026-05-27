/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.response;

public class RechargeIAPResponse {
    private int code;
    private long currentMoney;
    private int productId;

    public RechargeIAPResponse(int code, long currentMoney, int productId) {
        this.code = code;
        this.currentMoney = currentMoney;
        this.productId = productId;
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

    public int getProductId() {
        return this.productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }
}

