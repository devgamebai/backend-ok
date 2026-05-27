/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response.lode;

import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LoDeModelResponse
extends BaseResponseModel {
    private List<LotteryMessage> transactions = new ArrayList<LotteryMessage>();
    private long total = 0L;

    public LoDeModelResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LoDeModelResponse(boolean success, String errorCode, Object data) {
        super(success, errorCode, data);
    }

    public List<LotteryMessage> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LotteryMessage> transactions) {
        this.transactions = transactions;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

