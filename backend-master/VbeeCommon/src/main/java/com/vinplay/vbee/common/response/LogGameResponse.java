/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.messages.LogGameMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogGameResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<LogGameMessage> transactions = new ArrayList<LogGameMessage>();

    public LogGameResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogGameMessage> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogGameMessage> transactions) {
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

