/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.SlotResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultKhoBauResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<SlotResponse> transactions = new ArrayList<SlotResponse>();

    public ResultKhoBauResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<SlotResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<SlotResponse> transactions) {
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

