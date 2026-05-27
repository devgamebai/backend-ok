/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BaseResponseModel
 */
package com.vinplay.api.processors.giftCode;

import com.vinplay.vbee.common.response.BaseResponseModel;

public class GiftCodeResponse
extends BaseResponseModel {
    public long currentMoney;

    public GiftCodeResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }
}

