/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.minigame.CurrentTransactionSicboDetails;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultSicboDetailNowResponse
extends BaseResponseModel {
    private List<CurrentTransactionSicboDetails> transactions = new ArrayList<CurrentTransactionSicboDetails>();
    private long timeEnd;
    private boolean isBetting;

    public ResultSicboDetailNowResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CurrentTransactionSicboDetails> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<CurrentTransactionSicboDetails> transactions) {
        this.transactions = transactions;
    }

    public long getTimeEnd() {
        return this.timeEnd;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
    }

    public boolean isBetting() {
        return this.isBetting;
    }

    public void setBetting(boolean betting) {
        this.isBetting = betting;
    }
}

