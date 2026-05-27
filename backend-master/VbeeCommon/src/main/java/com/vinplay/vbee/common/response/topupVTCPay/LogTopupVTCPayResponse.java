/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.topupVTCPay;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.topupVTCPay.LogTopupVTCPay;
import java.util.List;

public class LogTopupVTCPayResponse
extends BaseResponseModel {
    private List<LogTopupVTCPay> trans;

    public LogTopupVTCPayResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<LogTopupVTCPay> getTrans() {
        return this.trans;
    }

    public void setTrans(List<LogTopupVTCPay> trans) {
        this.trans = trans;
    }
}

