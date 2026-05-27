/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.topupVTCPay;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.MoneyTotalFollowFaceValue;
import java.util.List;

public class DoiSoatTopupVTCPayResponse
extends BaseResponseModel {
    private List<MoneyTotalFollowFaceValue> trans;

    public DoiSoatTopupVTCPayResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<MoneyTotalFollowFaceValue> getTrans() {
        return this.trans;
    }

    public void setTrans(List<MoneyTotalFollowFaceValue> trans) {
        this.trans = trans;
    }
}

