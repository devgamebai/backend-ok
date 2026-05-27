/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.UserInfoResponse;
import java.util.ArrayList;
import java.util.List;

public class ResultUserInfoResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private List<UserInfoResponse> transactions = new ArrayList<UserInfoResponse>();
    private UserModel user = new UserModel();

    public UserModel getUser() {
        return this.user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public ResultUserInfoResponse(boolean success, String errorCode) {
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

    public List<UserInfoResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<UserInfoResponse> transactions) {
        this.transactions = transactions;
    }
}

