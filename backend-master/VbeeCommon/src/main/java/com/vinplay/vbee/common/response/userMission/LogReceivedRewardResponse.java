/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.userMission;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.userMission.LogReceivedRewardObj;
import java.util.List;

public class LogReceivedRewardResponse
extends BaseResponseModel {
    private List<LogReceivedRewardObj> transactions;

    public LogReceivedRewardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogReceivedRewardObj> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogReceivedRewardObj> transactions) {
        this.transactions = transactions;
    }
}

