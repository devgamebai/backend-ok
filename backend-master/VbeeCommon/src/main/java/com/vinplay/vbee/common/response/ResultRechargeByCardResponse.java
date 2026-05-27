/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import com.vinplay.vbee.common.response.RechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;

public class ResultRechargeByCardResponse
extends BaseResponseModel {
    private long total;
    private long totalRecord;
    private long totalMoney;
    private List<RechargeByCardReponse> transactions = new ArrayList<RechargeByCardReponse>();
    private List<MoneyTotalRechargeByCardReponse> moneyReponse = new ArrayList<MoneyTotalRechargeByCardReponse>();

    public List<MoneyTotalRechargeByCardReponse> getMoneyReponse() {
        return this.moneyReponse;
    }

    public void setMoneyReponse(List<MoneyTotalRechargeByCardReponse> moneyReponse) {
        this.moneyReponse = moneyReponse;
    }

    public ResultRechargeByCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<RechargeByCardReponse> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<RechargeByCardReponse> transactions) {
        this.transactions = transactions;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(long totalRecord) {
        this.totalRecord = totalRecord;
    }

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }
}

