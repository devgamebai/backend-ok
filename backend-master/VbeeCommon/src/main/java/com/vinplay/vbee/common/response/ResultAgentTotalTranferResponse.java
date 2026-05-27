/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.TranferAgentResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultAgentTotalTranferResponse
extends BaseResponseModel {
    private double ratio1;
    private double ratio2;
    private long total;
    private long totalRecord;
    private List<TranferAgentResponse> transactions = new ArrayList<TranferAgentResponse>();

    public double getRatio1() {
        return this.ratio1;
    }

    public void setRatio1(double ratio1) {
        this.ratio1 = ratio1;
    }

    public double getRatio2() {
        return this.ratio2;
    }

    public void setRatio2(double ratio2) {
        this.ratio2 = ratio2;
    }

    public ResultAgentTotalTranferResponse(boolean success, String errorCode) {
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

    public List<TranferAgentResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TranferAgentResponse> transactions) {
        this.transactions = transactions;
    }
}

