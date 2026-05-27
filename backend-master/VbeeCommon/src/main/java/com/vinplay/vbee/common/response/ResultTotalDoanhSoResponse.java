/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.TotalDoanhSoResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultTotalDoanhSoResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<TotalDoanhSoResponse> transactions = new ArrayList<TotalDoanhSoResponse>();

    public ResultTotalDoanhSoResponse(boolean success, String errorCode) {
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

    public List<TotalDoanhSoResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TotalDoanhSoResponse> transactions) {
        this.transactions = transactions;
    }
}

