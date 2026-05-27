/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.taixiu.ResultTaiXiu
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.minigame.response;

import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.vbee.common.response.BaseResponseModel;
import java.util.ArrayList;
import java.util.List;

public class ChiTietPhienTXResponse
extends BaseResponseModel {
    private List<TransactionTaiXiuDetail> transactions = new ArrayList<TransactionTaiXiuDetail>();
    private ResultTaiXiu resultTX;

    public ChiTietPhienTXResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<TransactionTaiXiuDetail> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<TransactionTaiXiuDetail> transactions) {
        this.transactions = transactions;
    }

    public ResultTaiXiu getResultTX() {
        return this.resultTX;
    }

    public void setResultTX(ResultTaiXiu resultTX) {
        this.resultTX = resultTX;
    }
}

