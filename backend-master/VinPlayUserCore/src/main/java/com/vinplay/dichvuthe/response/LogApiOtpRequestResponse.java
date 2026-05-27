/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.dichvuthe.response;

import com.vinplay.usercore.entities.LogApiOtpRequest;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogApiOtpRequestResponse
extends BaseResponseModel {
    private long totalPages;
    private List<LogApiOtpRequest> transactions = new ArrayList<LogApiOtpRequest>();

    public LogApiOtpRequestResponse(boolean success, String errorCode, long totalPages, List<LogApiOtpRequest> transactions) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.transactions = transactions;
    }

    public LogApiOtpRequestResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public List<LogApiOtpRequest> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogApiOtpRequest> transactions) {
        this.transactions = transactions;
    }
}

