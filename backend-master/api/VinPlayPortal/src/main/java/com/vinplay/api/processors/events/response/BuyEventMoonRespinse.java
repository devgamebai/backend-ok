/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.events.response;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class BuyEventMoonRespinse
extends BaseResponseModel {
    private long money = 0L;

    public BuyEventMoonRespinse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}

