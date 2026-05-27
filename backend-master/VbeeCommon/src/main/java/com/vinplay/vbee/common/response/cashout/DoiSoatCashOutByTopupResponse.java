/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.cashout;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;

public class DoiSoatCashOutByTopupResponse
extends BaseResponseModel {
    private List<MoneyTotalRechargeByCardReponse> moneyResponse = new ArrayList<MoneyTotalRechargeByCardReponse>();

    public DoiSoatCashOutByTopupResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<MoneyTotalRechargeByCardReponse> getMoneyResponse() {
        return this.moneyResponse;
    }

    public void setMoneyResponse(List<MoneyTotalRechargeByCardReponse> moneyResponse) {
        this.moneyResponse = moneyResponse;
    }
}

