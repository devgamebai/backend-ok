/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.UserAdminInfo;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ResultUserReponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<UserAdminInfo> transactions = new ArrayList<UserAdminInfo>();

    public ResultUserReponse(boolean success, String errorCode) {
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

    public List<UserAdminInfo> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<UserAdminInfo> transactions) {
        this.transactions = transactions;
    }
}

