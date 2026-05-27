/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.CashOutByBankReponse;
import java.util.ArrayList;
import java.util.List;

public class ResultCashOutByBankResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long totalMoney;
    private List<CashOutByBankReponse> transactions = new ArrayList<CashOutByBankReponse>();

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public ResultCashOutByBankResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<CashOutByBankReponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<CashOutByBankReponse> transactions) {
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

