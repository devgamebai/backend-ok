/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.GiftCodeGameResponse;
import java.util.ArrayList;
import java.util.List;

public class GiftCodeGameSearchResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<GiftCodeGameResponse> transactions = new ArrayList<GiftCodeGameResponse>();

    public GiftCodeGameSearchResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<GiftCodeGameResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<GiftCodeGameResponse> transactions) {
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

