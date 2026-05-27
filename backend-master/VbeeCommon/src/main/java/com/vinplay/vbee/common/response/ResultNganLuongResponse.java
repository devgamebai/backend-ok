/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.response.NganLuongFollowFaceValue;
import java.util.ArrayList;
import java.util.List;

public class ResultNganLuongResponse
extends BaseResponseModel {
    private List<NganLuongFollowFaceValue> money = new ArrayList<NganLuongFollowFaceValue>();

    public ResultNganLuongResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public List<NganLuongFollowFaceValue> getMoney() {
        return this.money;
    }

    public void setMoney(List<NganLuongFollowFaceValue> money) {
        this.money = money;
    }
}

