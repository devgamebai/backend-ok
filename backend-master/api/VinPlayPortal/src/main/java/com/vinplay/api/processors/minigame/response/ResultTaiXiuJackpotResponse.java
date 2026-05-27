/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.api.processors.minigame.response.JackpotTaiXiuResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultTaiXiuJackpotResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<JackpotTaiXiuResponse> transactions = new ArrayList<JackpotTaiXiuResponse>();

    public ResultTaiXiuJackpotResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<JackpotTaiXiuResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<JackpotTaiXiuResponse> transactions) {
        this.transactions = transactions;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(long totalRecord) {
        this.totalRecord = totalRecord;
    }
}

