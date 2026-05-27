/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiu
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LichSuGiaoDichTXResponse
extends BaseResponseModel {
    private int totalPages;
    private List<TransactionTaiXiu> transactions = new ArrayList<TransactionTaiXiu>();

    public LichSuGiaoDichTXResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TransactionTaiXiu> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TransactionTaiXiu> transactions) {
        this.transactions = transactions;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

