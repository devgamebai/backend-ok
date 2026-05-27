/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.vbee.common.models.minigame.baucua.TransactionBauCua;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class LSGDBauCuaResponse
extends BaseResponseModel {
    private int totalPages;
    private List<TransactionBauCua> transactions = new ArrayList<TransactionBauCua>();

    public LSGDBauCuaResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<TransactionBauCua> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TransactionBauCua> transactions) {
        this.transactions = transactions;
    }
}

