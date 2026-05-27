/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.LogUpdateCardPendingReponse;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;

public class ResultLogUpdateCardPendingResponse
extends BaseResponseModel {
    private long totalPage;
    private long totalRecord;
    private List<LogUpdateCardPendingReponse> trans = new ArrayList<LogUpdateCardPendingReponse>();
    private List<MoneyTotalRechargeByCardReponse> moneyReponse = new ArrayList<MoneyTotalRechargeByCardReponse>();

    public ResultLogUpdateCardPendingResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public long getTotalPage() {
        return this.totalPage;
    }

    public void setTotalPage(long totalPage) {
        this.totalPage = totalPage;
    }

    public long getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(long totalRecord) {
        this.totalRecord = totalRecord;
    }

    public List<LogUpdateCardPendingReponse> getTrans() {
        return this.trans;
    }

    public void setTrans(List<LogUpdateCardPendingReponse> trans) {
        this.trans = trans;
    }

    public List<MoneyTotalRechargeByCardReponse> getMoneyReponse() {
        return this.moneyReponse;
    }

    public void setMoneyReponse(List<MoneyTotalRechargeByCardReponse> moneyReponse) {
        this.moneyReponse = moneyReponse;
    }
}

