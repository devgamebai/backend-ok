/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MailBoxResponse;
import java.util.ArrayList;
import java.util.List;

public class ListMailBoxResponse
extends BaseResponseModel {
    private long totalPages;
    private int mailNotRead;
    private List<MailBoxResponse> transactions = new ArrayList<MailBoxResponse>();

    public int getMailNotRead() {
        return this.mailNotRead;
    }

    public void setMailNotRead(int mailNotRead) {
        this.mailNotRead = mailNotRead;
    }

    public ListMailBoxResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<MailBoxResponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<MailBoxResponse> transactions) {
        this.transactions = transactions;
    }

    public long getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}

