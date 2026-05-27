/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.dichvuthe.response;

import com.vinplay.usercore.entities.LogSMSPlusCheckMO;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LogSMSPlusCheckMoResponse
extends BaseResponseModel {
    private long totalPages;
    private List<LogSMSPlusCheckMO> transactions = new ArrayList<LogSMSPlusCheckMO>();

    public LogSMSPlusCheckMoResponse(boolean success, String errorCode, long totalPages, List<LogSMSPlusCheckMO> transactions) {
        super(success, errorCode);
        this.totalPages = totalPages;
        this.transactions = transactions;
    }

    public LogSMSPlusCheckMoResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public List<LogSMSPlusCheckMO> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<LogSMSPlusCheckMO> transactions) {
        this.transactions = transactions;
    }
}

