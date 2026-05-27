/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.usercore.entities;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class VippointResponse
extends BaseResponseModel {
    private long currentMoney;
    private long moneyAdd;

    public VippointResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }

    public long getMoneyAdd() {
        return this.moneyAdd;
    }

    public void setMoneyAdd(long moneyAdd) {
        this.moneyAdd = moneyAdd;
    }
}

