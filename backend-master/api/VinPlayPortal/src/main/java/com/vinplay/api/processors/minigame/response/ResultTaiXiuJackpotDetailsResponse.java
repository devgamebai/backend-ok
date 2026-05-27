/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.api.processors.minigame.response.JackpotTaiXiuDetailsResponse;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultTaiXiuJackpotDetailsResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<JackpotTaiXiuDetailsResponse> transactions = new ArrayList<JackpotTaiXiuDetailsResponse>();

    public ResultTaiXiuJackpotDetailsResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<JackpotTaiXiuDetailsResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<JackpotTaiXiuDetailsResponse> transactions) {
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

