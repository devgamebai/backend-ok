/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class PotResponse
extends BaseResponseModel {
    private long value;
    private long moneyExchange;
    private long currentMoneyUser;
    private long freezeMoneyUser;

    public PotResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getValue() {
        return this.value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getCurrentMoneyUser() {
        return this.currentMoneyUser;
    }

    public void setCurrentMoneyUser(long currentMoneyUser) {
        this.currentMoneyUser = currentMoneyUser;
    }

    public long getFreezeMoneyUser() {
        return this.freezeMoneyUser;
    }

    public void setFreezeMoneyUser(long freezeMoneyUser) {
        this.freezeMoneyUser = freezeMoneyUser;
    }

    public long getMoneyExchange() {
        return this.moneyExchange;
    }

    public void setMoneyExchange(long moneyExchange) {
        this.moneyExchange = moneyExchange;
    }
}

