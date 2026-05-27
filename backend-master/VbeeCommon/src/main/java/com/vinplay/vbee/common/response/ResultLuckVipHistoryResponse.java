/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LuckyVipHistoryResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultLuckVipHistoryResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<LuckyVipHistoryResponse> transactions = new ArrayList<LuckyVipHistoryResponse>();

    public ResultLuckVipHistoryResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LuckyVipHistoryResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LuckyVipHistoryResponse> transactions) {
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

