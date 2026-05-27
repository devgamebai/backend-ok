/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultAgentTranferResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long totalVinSend;
    private long totalVinReceive;
    private long totalFee;
    private List<LogAgentTranferMoneyResponse> transactions = new ArrayList<LogAgentTranferMoneyResponse>();

    public long getTotalFee() {
        return this.totalFee;
    }

    public void setTotalFee(long totalFee) {
        this.totalFee = totalFee;
    }

    public long getTotalVinSend() {
        return this.totalVinSend;
    }

    public void setTotalVinSend(long totalVinSend) {
        this.totalVinSend = totalVinSend;
    }

    public long getTotalVinReceive() {
        return this.totalVinReceive;
    }

    public void setTotalVinReceive(long totalVinReceive) {
        this.totalVinReceive = totalVinReceive;
    }

    public ResultAgentTranferResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogAgentTranferMoneyResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogAgentTranferMoneyResponse> transactions) {
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

