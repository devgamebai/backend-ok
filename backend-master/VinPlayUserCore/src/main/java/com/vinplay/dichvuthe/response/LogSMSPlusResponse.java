/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.dichvuthe.response;

import com.vinplay.usercore.entities.LogSMSPlus;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogSMSPlusResponse
extends BaseResponseModel {
    private long totalPages;
    public long totalSuccess;
    private long totalMoney;
    private List<LogSMSPlus> transactions = new ArrayList<LogSMSPlus>();

    public LogSMSPlusResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogSMSPlusResponse(boolean success, String errorCode, long totalPages, long totalSuccess, long totalMoney, List<LogSMSPlus> transactions) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.totalSuccess = totalSuccess;
        this.totalMoney = totalMoney;
        this.transactions = transactions;
    }

    public long getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public List<LogSMSPlus> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogSMSPlus> transactions) {
        this.transactions = transactions;
    }

    public long getTotalSuccess() {
        return this.totalSuccess;
    }

    public void setTotalSuccess(long totalSuccess) {
        this.totalSuccess = totalSuccess;
    }
}

