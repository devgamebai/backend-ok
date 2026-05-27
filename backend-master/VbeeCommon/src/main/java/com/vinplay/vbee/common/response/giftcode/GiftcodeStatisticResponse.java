/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response.giftcode;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticObj;
import java.util.ArrayList;
import java.util.List;

public class GiftcodeStatisticResponse
extends BaseResponseModel {
    private List<GiftcodeStatisticObj> trans = new ArrayList<GiftcodeStatisticObj>();

    public GiftcodeStatisticResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<GiftcodeStatisticObj> getTrans() {
        return this.trans;
    }

    public void setTrans(List<GiftcodeStatisticObj> trans) {
        this.trans = trans;
    }
}

