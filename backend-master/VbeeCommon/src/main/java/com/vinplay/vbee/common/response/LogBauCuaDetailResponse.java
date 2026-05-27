/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.BauCuaReponseDetail;
import java.util.ArrayList;
import java.util.List;

public class LogBauCuaDetailResponse
extends BaseResponseModel {
    private List<BauCuaReponseDetail> transactions = new ArrayList<BauCuaReponseDetail>();
    private long total;
    private long totalRecord;

    public LogBauCuaDetailResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<BauCuaReponseDetail> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<BauCuaReponseDetail> transactions) {
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

