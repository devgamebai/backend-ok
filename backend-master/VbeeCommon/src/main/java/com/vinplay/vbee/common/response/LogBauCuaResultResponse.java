/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.BauCuaResultResponse;
import java.util.ArrayList;
import java.util.List;

public class LogBauCuaResultResponse
extends BaseResponseModel {
    private List<BauCuaResultResponse> transactions = new ArrayList<BauCuaResultResponse>();
    private long total;
    private long totalRecord;

    public LogBauCuaResultResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<BauCuaResultResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<BauCuaResultResponse> transactions) {
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

