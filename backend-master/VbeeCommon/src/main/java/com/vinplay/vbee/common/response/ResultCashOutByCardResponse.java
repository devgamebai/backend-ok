/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.CashOutByCardResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultCashOutByCardResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long totalMoney;
    private List<CashOutByCardResponse> transactions = new ArrayList<CashOutByCardResponse>();

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public ResultCashOutByCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CashOutByCardResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<CashOutByCardResponse> transactions) {
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

