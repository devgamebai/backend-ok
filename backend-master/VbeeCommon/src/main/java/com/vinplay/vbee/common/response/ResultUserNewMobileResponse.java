/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.UserNewMobileResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultUserNewMobileResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<UserNewMobileResponse> transactions = new ArrayList<UserNewMobileResponse>();

    public ResultUserNewMobileResponse(boolean success, String errorCode) {
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

    public List<UserNewMobileResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<UserNewMobileResponse> transactions) {
        this.transactions = transactions;
    }
}

