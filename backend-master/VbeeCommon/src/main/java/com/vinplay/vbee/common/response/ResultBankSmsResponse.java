/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.BankSmsModel;
import com.vinplay.vbee.common.models.UserAdminInfo;

import java.util.ArrayList;
import java.util.List;

public class ResultBankSmsResponse
        extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<BankSmsModel> transactions = new ArrayList<BankSmsModel>();

    public ResultBankSmsResponse(boolean success, String errorCode) {
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

    public List<BankSmsModel> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<BankSmsModel> transactions) {
        this.transactions = transactions;
    }
}

