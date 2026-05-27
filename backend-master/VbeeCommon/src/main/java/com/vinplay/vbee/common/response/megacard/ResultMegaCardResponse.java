/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.megacard;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.megacard.MegaCardResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultMegaCardResponse
extends BaseResponseModel {
    private long totalRecord;
    private long totalMoney;
    private List<MegaCardResponse> transactions = new ArrayList<MegaCardResponse>();

    public ResultMegaCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(long totalRecord) {
        this.totalRecord = totalRecord;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public List<MegaCardResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<MegaCardResponse> transactions) {
        this.transactions = transactions;
    }
}

