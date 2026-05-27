/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.TranferMoneyResponse;
import java.util.ArrayList;
import java.util.List;

public class MoneyTranferResponse
extends BaseResponseModel {
    private int totalRecord;
    private List<TranferMoneyResponse> listTranfer = new ArrayList<TranferMoneyResponse>();

    public MoneyTranferResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(int totalRecord) {
        this.totalRecord = totalRecord;
    }

    public List<TranferMoneyResponse> getListTranfer() {
        return this.listTranfer;
    }

    public void setListTranfer(List<TranferMoneyResponse> listTranfer) {
        this.listTranfer = listTranfer;
    }
}

