/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LogVipPointEventResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultLogVipPointEventResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long totalMoney;
    private List<LogVipPointEventResponse> transactions = new ArrayList<LogVipPointEventResponse>();

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public ResultLogVipPointEventResponse(boolean success, String errorCode) {
        super(success, errorCode);
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

    public List<LogVipPointEventResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogVipPointEventResponse> transactions) {
        this.transactions = transactions;
    }
}

