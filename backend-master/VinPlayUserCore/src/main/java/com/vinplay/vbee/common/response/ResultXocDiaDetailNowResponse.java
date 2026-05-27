/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.XocDiaItemResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultXocDiaDetailNowResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long timeEnd;
    private boolean isBetting;
    private List<XocDiaItemResponse> transactions = new ArrayList<XocDiaItemResponse>();

    public ResultXocDiaDetailNowResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<XocDiaItemResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<XocDiaItemResponse> transactions) {
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

