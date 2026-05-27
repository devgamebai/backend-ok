/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.recharge;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoneyTotalRechargeByCardReponse;
import java.util.ArrayList;
import java.util.List;

public class DoiSoatRechargeByVinplayCardResponse
extends BaseResponseModel {
    private List<MoneyTotalRechargeByCardReponse> moneyReponse = new ArrayList<MoneyTotalRechargeByCardReponse>();

    public DoiSoatRechargeByVinplayCardResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<MoneyTotalRechargeByCardReponse> getMoneyReponse() {
        return this.moneyReponse;
    }

    public void setMoneyReponse(List<MoneyTotalRechargeByCardReponse> moneyReponse) {
        this.moneyReponse = moneyReponse;
    }
}

