/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.TaiXiuItemResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultTaiXiuDetailNowResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<TaiXiuItemResponse> transactions = new ArrayList<TaiXiuItemResponse>();

    public ResultTaiXiuDetailNowResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TaiXiuItemResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TaiXiuItemResponse> transactions) {
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

