/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.TaiXiuResultResponse;
import java.util.ArrayList;
import java.util.List;

public class TaiXiuResultReponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<TaiXiuResultResponse> transactions = new ArrayList<TaiXiuResultResponse>();

    public TaiXiuResultReponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TaiXiuResultResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TaiXiuResultResponse> transactions) {
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

