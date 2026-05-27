/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.VinCardResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultVinCardResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long totalMoney;
    private List<VinCardResponse> transactions = new ArrayList<VinCardResponse>();

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public ResultVinCardResponse(boolean success, String errorCode) {
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

    public List<VinCardResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<VinCardResponse> transactions) {
        this.transactions = transactions;
    }
}

