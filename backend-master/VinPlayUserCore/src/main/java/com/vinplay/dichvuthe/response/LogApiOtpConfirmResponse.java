/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.dichvuthe.response;

import com.vinplay.usercore.entities.LogApiOtpConfirm;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogApiOtpConfirmResponse
extends BaseResponseModel {
    private long totalPages;
    public long totalSuccess;
    private long totalMoney;
    private List<LogApiOtpConfirm> transactions = new ArrayList<LogApiOtpConfirm>();

    public LogApiOtpConfirmResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public LogApiOtpConfirmResponse(boolean success, String errorCode, long totalPages, long totalSuccess, long totalMoney, List<LogApiOtpConfirm> transactions) {
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

    public List<LogApiOtpConfirm> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogApiOtpConfirm> transactions) {
        this.transactions = transactions;
    }

    public long getTotalSuccess() {
        return this.totalSuccess;
    }

    public void setTotalSuccess(long totalSuccess) {
        this.totalSuccess = totalSuccess;
    }
}

